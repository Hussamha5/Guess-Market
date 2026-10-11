package ui;

/**
 * The real entry point of the client.
 * It is a separate class (that does NOT extend Application) so that JavaFX can also be started from a plain
 * classpath ("java -cp lib\* ui.Launcher"), which is how run-client.bat starts it.
 */
public class Launcher {

    public static void main(String[] args) {
        MainFX.main(args);
    }
}