package ui;

import ui.net.ApiClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * The first screen: a user name and nothing else. If the name is taken (or not valid) the server explains it and
 * the user can try again.
 */
public final class LoginView extends StackPane {

    private final ApiClient api;
    private final Consumer<String> onLoggedIn;
    private final TextField nameField = new TextField();
    private final Button loginButton = new Button("Login");
    private final ProgressIndicator progress = new ProgressIndicator();
    private final Label message = new Label();

    /** @param startMessage text shown at the beginning (for example: "Your session expired"), may be empty */
    public LoginView(ApiClient api, String startMessage, Consumer<String> onLoggedIn) {
        this.api = api;
        this.onLoggedIn = onLoggedIn;

        Label title = new Label("Guess Market");
        title.getStyleClass().add("login-title");
        Label prompt = new Label("Choose a user name to enter the market");

        nameField.setPromptText("User name");
        nameField.setOnAction(e -> login());
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(e -> login());
        progress.setPrefSize(22, 22);
        progress.setVisible(false);
        progress.setManaged(false);

        HBox line = new HBox(8, nameField, loginButton, progress);
        line.setAlignment(Pos.CENTER);
        message.setWrapText(true);
        message.setMaxWidth(380);
        if (startMessage != null && !startMessage.isBlank()) {
            UiContext.message(message, startMessage, true);
        }

        VBox card = new VBox(14, title, prompt, line, message);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(30));
        card.setMaxWidth(460);
        card.getStyleClass().add("login-card");

        setAlignment(Pos.CENTER);
        getChildren().add(card);
        Platform.runLater(nameField::requestFocus);
    }

    private void login() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isEmpty()) {
            UiContext.message(message, "Please type a user name.", true);
            return;
        }
        setBusy(true);
        api.login(name).thenAcceptAsync(reply -> {
            setBusy(false);
            if (reply.ok()) {
                onLoggedIn.accept(reply.message());
            } else {
                UiContext.message(message, reply.message(), true);
                nameField.requestFocus();
                nameField.selectAll();
            }
        }, Platform::runLater);
    }

    private void setBusy(boolean busy) {
        loginButton.setDisable(busy);
        nameField.setDisable(busy);
        progress.setVisible(busy);
        progress.setManaged(busy);
    }
}