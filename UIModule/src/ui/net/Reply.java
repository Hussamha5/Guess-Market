package ui.net;

/**
 * The answer of the server to an action (login, upload, trade...).
 * {@code ok} is true for HTTP 200. {@code message} is the text for the user: the success text, or the
 * explanation of the error. {@code status} is 0 when the server could not be reached at all.
 */
public record Reply(boolean ok, int status, String message) {

    public static Reply unreachable(String details) {
        return new Reply(false, 0, "Cannot reach the server (" + details + "). Is Tomcat running?");
    }

    public static Reply failure(String message) {
        return new Reply(false, 0, message);
    }

    /** HTTP 401: the server does not know this session any more (it expired, or the server was restarted). */
    public boolean isSessionLost() {
        return status == 401;
    }
}