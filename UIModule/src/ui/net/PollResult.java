package ui.net;

/**
 * The result of one polling request ("did something change since version X?").
 * CHANGED: new data arrived. UNCHANGED: the server answered 204, the client already has the current data.
 * UNAUTHORIZED: the session is gone (expired or the server restarted). FAILED: the server could not be reached.
 */
public record PollResult<T>(Kind kind, T data, long version) {

    public enum Kind { CHANGED, UNCHANGED, UNAUTHORIZED, FAILED }

    public static <T> PollResult<T> changed(T data, long version) {
        return new PollResult<>(Kind.CHANGED, data, version);
    }

    public static <T> PollResult<T> unchanged(long version) {
        return new PollResult<>(Kind.UNCHANGED, null, version);
    }

    public static <T> PollResult<T> unauthorized() {
        return new PollResult<>(Kind.UNAUTHORIZED, null, -1);
    }

    public static <T> PollResult<T> failed() {
        return new PollResult<>(Kind.FAILED, null, -1);
    }

    public boolean isChanged() {
        return kind == Kind.CHANGED;
    }

    public boolean isUnauthorized() {
        return kind == Kind.UNAUTHORIZED;
    }

    public boolean isFailed() {
        return kind == Kind.FAILED;
    }
}