package ui.net;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import dto.AccountDTO;
import dto.EventDTO;
import dto.UserDTO;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The only class of the client that talks HTTP. Everything else works with DTOs and plain Java types.
 * <ul>
 *   <li>Actions that the user starts (login, upload, trade...) are asynchronous: they return a CompletableFuture
 *       and never block the JavaFX Application Thread.</li>
 *   <li>The polling methods are synchronous, because they run on the poller thread, not on the UI thread.</li>
 *   <li>Every Response is closed (try-with-resources), otherwise OkHttp leaks connections.</li>
 * </ul>
 * The HTTP session of the server (JSESSIONID cookie) is kept by an in-memory cookie jar.
 */
public final class ApiClient {

    private static final String VERSION_HEADER = "X-Market-Version";
    private static final Type EVENT_LIST = new TypeToken<List<EventDTO>>() { }.getType();
    private static final Type USER_LIST = new TypeToken<List<UserDTO>>() { }.getType();

    private final String baseUrl;
    private final Gson gson = new Gson();
    private final MemoryCookieJar cookies = new MemoryCookieJar();
    private final OkHttpClient http;
    private final ExecutorService fileReader = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "file-reader");
        thread.setDaemon(true);
        return thread;
    });

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.http = new OkHttpClient.Builder()
                .cookieJar(cookies)
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    // ------------------------------------------------------------------ actions (asynchronous)

    public CompletableFuture<Reply> login(String name) {
        return post("/api/login", Map.of("name", name));
    }

    public CompletableFuture<Reply> loadFunds(String amount) {
        return post("/api/funds", Map.of("amount", amount));
    }

    public CompletableFuture<Reply> activate(String eventName) {
        return post("/api/event/activate", Map.of("event", eventName));
    }

    public CompletableFuture<Reply> close(String eventName, int option) {
        return post("/api/event/close", Map.of("event", eventName, "option", String.valueOf(option)));
    }

    /** price may be empty for LMSR events (the server decides the price). */
    public CompletableFuture<Reply> trade(String eventName, int option, boolean buy, String quantity, String price) {
        Map<String, String> form = new java.util.HashMap<>();
        form.put("event", eventName);
        form.put("option", String.valueOf(option));
        form.put("side", buy ? "buy" : "sell");
        form.put("quantity", quantity);
        if (price != null && !price.isBlank()) {
            form.put("price", price);
        }
        return post("/api/event/trade", form);
    }

    /** Sends an XML file as a multipart/form-data request (no third party library on the server). */
    public CompletableFuture<Reply> upload(Path file) {
        return CompletableFuture.supplyAsync(() -> readFile(file), fileReader)
                .thenCompose(bytes -> {
                    RequestBody content = RequestBody.create(bytes, MediaType.get("text/xml"));
                    RequestBody body = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("file", file.getFileName().toString(), content)
                            .build();
                    return enqueue(new Request.Builder().url(url("/api/upload")).post(body).build());
                })
                .exceptionally(error -> Reply.failure("The file could not be read: " + rootMessage(error)));
    }

    private CompletableFuture<Reply> post(String path, Map<String, String> form) {
        FormBody.Builder body = new FormBody.Builder();
        form.forEach(body::add);
        return enqueue(new Request.Builder().url(url(path)).post(body.build()).build());
    }

    private CompletableFuture<Reply> enqueue(Request request) {
        CompletableFuture<Reply> future = new CompletableFuture<>();
        http.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.complete(Reply.unreachable(String.valueOf(e.getMessage())));
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (response) {
                    ResponseBody body = response.body();
                    String text = body == null ? "" : body.string();
                    future.complete(new Reply(response.code() == 200, response.code(), textFor(response.code(), text)));
                } catch (IOException e) {
                    future.complete(Reply.unreachable(String.valueOf(e.getMessage())));
                }
            }
        });
        return future;
    }

    private static String textFor(int status, String text) {
        if (text != null && !text.isBlank()) {
            return text;
        }
        return status == 200 ? "Done." : "The server answered with HTTP status " + status + ".";
    }

    // ------------------------------------------------------------------ polling (synchronous, on the poller thread)

    public PollResult<List<EventDTO>> pollEvents(long sinceVersion) {
        return poll("/api/events", Map.of("since", String.valueOf(sinceVersion)), EVENT_LIST);
    }

    public PollResult<List<UserDTO>> pollUsers(long sinceVersion) {
        return poll("/api/users", Map.of("since", String.valueOf(sinceVersion)), USER_LIST);
    }

    public PollResult<AccountDTO> pollAccount(long sinceVersion, int lastLedgerSerial) {
        return poll("/api/account", Map.of("since", String.valueOf(sinceVersion),
                "ledgerSince", String.valueOf(lastLedgerSerial)), AccountDTO.class);
    }

    private <T> PollResult<T> poll(String path, Map<String, String> query, Type type) {
        HttpUrl.Builder url = HttpUrl.get(baseUrl + path).newBuilder();
        query.forEach(url::addQueryParameter);
        Request request = new Request.Builder().url(url.build()).get().build();
        try (Response response = http.newCall(request).execute()) {
            int status = response.code();
            if (status == 204) {
                return PollResult.unchanged(versionOf(response));
            }
            if (status == 401) {
                return PollResult.unauthorized();
            }
            ResponseBody body = response.body();
            if (status != 200 || body == null) {
                return PollResult.failed();
            }
            T data = gson.fromJson(body.string(), type);
            return data == null ? PollResult.failed() : PollResult.changed(data, versionOf(response));
        } catch (IOException | JsonParseException e) {
            return PollResult.failed();
        }
    }

    private static long versionOf(Response response) {
        String header = response.header(VERSION_HEADER);
        try {
            return header == null ? -1 : Long.parseLong(header.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // ------------------------------------------------------------------ closing

    /** Tells the server that this user leaves (so that the name is free again). Waits a moment, but never long. */
    @SuppressWarnings("try")
    public void logoutQuietly() {
        OkHttpClient quick = http.newBuilder().callTimeout(1500, TimeUnit.MILLISECONDS).build();
        Request request = new Request.Builder().url(url("/api/logout")).post(new FormBody.Builder().build()).build();
        try (Response ignored = quick.newCall(request).execute()) {
            // nothing to read: the answer only matters to the server
        } catch (IOException e) {
            // the server is gone, so there is nothing to free
        } finally {
            cookies.clear();
        }
    }

    /** Forgets the session cookie (used when the session was lost, or after a logout). */
    public void forgetSession() {
        cookies.clear();
    }

    /** Stops the threads of OkHttp, so that the program can end immediately. */
    public void shutdown() {
        http.dispatcher().executorService().shutdown();
        http.connectionPool().evictAll();
        fileReader.shutdownNow();
    }

    // ------------------------------------------------------------------ helpers

    private HttpUrl url(String path) {
        return HttpUrl.get(baseUrl + path);
    }

    private static byte[] readFile(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private static String rootMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return String.valueOf(cause.getMessage());
    }

    /** Keeps the cookies (JSESSIONID) of the server in memory. */
    private static final class MemoryCookieJar implements CookieJar {

        private final Map<String, Cookie> byName = new ConcurrentHashMap<>();

        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> received) {
            for (Cookie cookie : received) {
                byName.put(cookie.name(), cookie);
            }
        }

        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            long now = System.currentTimeMillis();
            return byName.values().stream()
                    .filter(cookie -> cookie.expiresAt() > now && cookie.matches(url))
                    .toList();
        }

        void clear() {
            byName.clear();
        }
    }
}