package ui;

import ui.model.MarketModel;
import ui.model.Poller;
import ui.net.ApiClient;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Owns the one window and moves between the login screen and the main screen.
 * When the user logs in a fresh model and a fresh poller are created, when he logs out (or the session is lost)
 * they are thrown away.
 */
public final class AppShell {

    private final Stage stage;
    private final ApiClient api;
    private final StackPane rootHolder = new StackPane();
    private final Scene scene = new Scene(rootHolder, 1220, 780);

    private Poller poller;
    private boolean loggedIn;

    public AppShell(Stage stage, ApiClient api) {
        this.stage = stage;
        this.api = api;
    }

    public void start() {
        Themes.apply(scene, Themes.DEFAULT);
        stage.setTitle("Guess Market");
        stage.setScene(scene);
        stage.setMinWidth(640);
        stage.setMinHeight(480);
        showLogin("");
        stage.show();
    }

    private void showLogin(String message) {
        loggedIn = false;
        rootHolder.getChildren().setAll(new LoginView(api, message, this::showMain));
    }

    private void showMain(String userName) {
        MarketModel model = new MarketModel(userName);
        Poller newPoller = new Poller(api, model, this::sessionLost);
        poller = newPoller;
        UiContext context = new UiContext(api, model, newPoller, this::sessionLost);
        rootHolder.getChildren().setAll(new MainView(context, this::logout));
        loggedIn = true;
        newPoller.start();
    }

    /** The server does not know us any more (the server was restarted, or no request arrived for 60 seconds): back to the login screen. */
    private void sessionLost() {
        if (!loggedIn) {
            return;
        }
        stopPolling();
        api.forgetSession();
        showLogin("Your session ended (the server was restarted, or the connection was lost for too long). "
                + "Please log in again.");
    }

    private void logout() {
        stopPolling();
        Thread worker = new Thread(() -> {
            api.logoutQuietly();
            javafx.application.Platform.runLater(() -> showLogin(""));
        }, "logout");
        worker.setDaemon(true);
        worker.start();
    }

    private void stopPolling() {
        if (poller != null) {
            poller.stop();
            poller = null;
        }
    }

    /** Called when the window is closed: the user name must become free for the others. */
    public void shutdown() {
        stopPolling();
        if (loggedIn) {
            api.logoutQuietly();
        }
        api.shutdown();
    }
}