package ui.model;

import ui.net.ApiClient;
import ui.net.PollResult;
import ui.net.ServerConfig;
import dto.AccountDTO;
import dto.EventDTO;
import dto.LedgerEntryDTO;
import dto.UserDTO;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Pull refresh: a background thread that asks the server every half a second whether something changed.
 * It remembers, for every kind of data, the version it already has and sends it with the request; the server
 * answers "204 No Content" when nothing happened, so most of the answers are tiny and the screens are not touched.
 * The requests here are synchronous (this is not the UI thread); the model is updated with Platform.runLater.
 */
public final class Poller {

    private final ApiClient api;
    private final MarketModel model;
    private final Runnable onSessionLost;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "market-poller");
        thread.setDaemon(true);
        return thread;
    });

    // used only by the poller thread (the executor runs one task at a time)
    private long eventsVersion = -1;
    private long usersVersion = -1;
    private long accountVersion = -1;
    private int lastLedgerSerial = 0;
    private Boolean lastConnected = null;

    private volatile boolean running = true;

    public Poller(ApiClient api, MarketModel model, Runnable onSessionLost) {
        this.api = api;
        this.model = model;
        this.onSessionLost = onSessionLost;
    }

    public void start() {
        executor.scheduleWithFixedDelay(this::cycle, 0, ServerConfig.POLL_MILLIS, TimeUnit.MILLISECONDS);
    }

    /** Right after the user did something: do not wait for the next tick. */
    public void pollNow() {
        if (running) {
            executor.execute(this::cycle);
        }
    }

    public void stop() {
        running = false;
        executor.shutdownNow();
    }

    private void cycle() {
        if (!running) {
            return;
        }
        try {
            PollResult<List<EventDTO>> events = api.pollEvents(eventsVersion);
            PollResult<List<UserDTO>> users = api.pollUsers(usersVersion);
            PollResult<AccountDTO> account = api.pollAccount(accountVersion, lastLedgerSerial);

            if (events.isUnauthorized() || users.isUnauthorized() || account.isUnauthorized()) {
                sessionLost();
                return;
            }
            reportConnection(!(events.isFailed() || users.isFailed() || account.isFailed()));

            if (events.isChanged()) {
                eventsVersion = events.version();
                List<EventDTO> data = events.data();
                Platform.runLater(() -> model.applyEvents(data));
            }
            if (users.isChanged()) {
                usersVersion = users.version();
                List<UserDTO> data = users.data();
                Platform.runLater(() -> model.applyUsers(data));
            }
            if (account.isChanged()) {
                accountVersion = account.version();
                AccountDTO data = account.data();
                List<LedgerEntryDTO> rows = data.getLedger();
                if (!rows.isEmpty()) {
                    lastLedgerSerial = rows.get(rows.size() - 1).getSerial();
                }
                Platform.runLater(() -> model.applyAccount(data));
            }
        } catch (RuntimeException e) {
            // a poller that dies silently would freeze the screens, so it must survive any surprise
            reportConnection(false);
        }
    }

    private void reportConnection(boolean connected) {
        if (lastConnected == null || lastConnected != connected) {
            lastConnected = connected;
            Platform.runLater(() -> model.connectedProperty().set(connected));
        }
    }

    private void sessionLost() {
        if (running) {
            running = false;
            executor.shutdown();
            Platform.runLater(onSessionLost);
        }
    }
}