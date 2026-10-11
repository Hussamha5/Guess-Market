package server;

/**
 * The answer to a "did anything change since version X?" question.
 * data is null when nothing changed.
 */
public record Versioned<T>(long version, T data) {

    public boolean isUnchanged() {
        return data == null;
    }
}