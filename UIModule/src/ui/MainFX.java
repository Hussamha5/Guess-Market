package ui;

import ui.net.ApiClient;
import ui.net.ServerConfig;
import javafx.application.Application;
import javafx.stage.Stage;

/** The JavaFX application of the Guess Market client. It talks to the server only over HTTP (see ApiClient). */
public class MainFX extends Application {

    private AppShell shell;

    @Override
    public void start(Stage stage) {
        shell = new AppShell(stage, new ApiClient(ServerConfig.baseUrl()));
        shell.start();
    }

    @Override
    public void stop() {
        if (shell != null) {
            shell.shutdown();
        }
        System.exit(0); // no thread of the HTTP library may keep the program alive
    }

    public static void main(String[] args) {
        launch(MainFX.class, args);
    }
}