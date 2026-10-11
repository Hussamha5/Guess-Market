package server;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * The base of all the servlets: finding the service and the current user, reading the parameters
 * (with clear error messages) and writing text / JSON answers. Errors become HTTP status codes
 * with the explanation as the body of the answer.
 */
public abstract class ApiServlet extends HttpServlet {

    public static final String VERSION_HEADER = "X-Market-Version";
    protected static final long NO_VERSION = -1;

    private static final Gson GSON = new Gson();
    private static final String TEXT = "text/plain;charset=UTF-8";
    private static final String JSON = "application/json;charset=UTF-8";

    /** The code that a servlet runs inside guarded(). */
    @FunctionalInterface
    protected interface Action {
        void run() throws Exception;
    }

    protected MarketService service() {
        return (MarketService) getServletContext().getAttribute(MarketService.CONTEXT_ATTRIBUTE);
    }

    protected static String currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (String) session.getAttribute(AuthFilter.SESSION_USER);
    }

    /** Runs the action and turns every failure into an answer with the right status and a readable message. */
    protected void guarded(HttpServletResponse response, Action action) throws IOException {
        try {
            action.run();
        } catch (IllegalArgumentException e) {
            sendText(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (NoSuchElementException e) {
            sendText(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (SecurityException e) {
            sendText(response, HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        } catch (IllegalStateException e) {
            sendText(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
        } catch (Exception e) {
            log("Unexpected error while handling a request", e);
            sendText(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "The server could not handle the request: " + e.getMessage());
        }
    }

    // ----- answers -----

    protected static void sendText(HttpServletResponse response, int status, String text) throws IOException {
        response.setStatus(status);
        response.setContentType(TEXT);
        response.getWriter().write(text);
    }

    protected static void sendOk(HttpServletResponse response, String text) throws IOException {
        sendText(response, HttpServletResponse.SC_OK, text);
    }

    protected static void sendJson(HttpServletResponse response, Object data, long version) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(JSON);
        response.setHeader(VERSION_HEADER, String.valueOf(version));
        response.getWriter().write(GSON.toJson(data));
    }

    /** "204 No Content" tells the client that it already has the current data. */
    protected static void sendNotChanged(HttpServletResponse response, long version) {
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        response.setHeader(VERSION_HEADER, String.valueOf(version));
    }

    protected static <T> void sendVersioned(HttpServletResponse response, Versioned<T> result) throws IOException {
        if (result.isUnchanged()) {
            sendNotChanged(response, result.version());
        } else {
            sendJson(response, result.data(), result.version());
        }
    }

    // ----- parameters -----

    protected static String requiredText(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("The parameter '" + name + "' is missing.");
        }
        return value.trim();
    }

    protected static int requiredInt(HttpServletRequest request, String name, String description) {
        String text = requiredText(request, name);
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(description + " must be a whole number, but it is '" + text + "'.");
        }
    }

    protected static double requiredDouble(HttpServletRequest request, String name, String description) {
        return parseDouble(requiredText(request, name), description);
    }

    /** A number that may be missing: returns null when the parameter was not sent. */
    protected static Double optionalDouble(HttpServletRequest request, String name, String description) {
        String text = request.getParameter(name);
        return text == null || text.isBlank() ? null : parseDouble(text.trim(), description);
    }

    private static double parseDouble(String text, String description) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(description + " must be a number, but it is '" + text + "'.");
        }
    }

    protected static long optionalLong(HttpServletRequest request, String name, long defaultValue) {
        String text = request.getParameter(name);
        if (text == null || text.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("The parameter '" + name + "' must be a whole number.");
        }
    }
}