package ui.net;

/** Where the server lives. The exercise fixes it: localhost:8080 and a WAR called guess-market.war. */
public final class ServerConfig {

    public static final String DEFAULT_BASE_URL = "http://localhost:8080/guess-market";

    /** How often the client asks the server whether something has changed (the exercise asks for 2 seconds at most). */
    public static final int POLL_MILLIS = 500;

    private ServerConfig() {
    }

    /** The base address. A developer can override it with -Dguessmarket.url=... (the default is what the exercise needs). */
    public static String baseUrl() {
        String configured = System.getProperty("guessmarket.url");
        return configured == null || configured.isBlank() ? DEFAULT_BASE_URL : configured.trim();
    }
}