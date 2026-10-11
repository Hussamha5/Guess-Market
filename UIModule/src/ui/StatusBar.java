package ui;

import ui.model.MarketModel;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** The line at the bottom: is the server reachable, and is the user blocked because of a negative balance. */
public final class StatusBar extends HBox {

    public StatusBar(MarketModel model) {
        super(12);
        setPadding(new Insets(4, 10, 4, 10));
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("status-bar");

        Label blocked = new Label("Your balance is negative: you cannot trade, open or close events, or upload files "
                + "until you load funds.");
        blocked.getStyleClass().add("error-text");
        blocked.visibleProperty().bind(model.blockedProperty());
        blocked.managedProperty().bind(model.blockedProperty());

        Label connection = new Label();
        connection.textProperty().bind(Bindings.when(model.connectedProperty())
                .then("Connected to the server")
                .otherwise("The server is not reachable - trying again..."));
        updateConnectionStyle(connection, model.isConnected());
        model.connectedProperty().addListener((obs, was, now) -> updateConnectionStyle(connection, now));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        getChildren().addAll(blocked, spacer, connection);
    }

    private static void updateConnectionStyle(Label label, boolean connected) {
        label.getStyleClass().removeAll("error-text", "success-text");
        label.getStyleClass().add(connected ? "success-text" : "error-text");
    }
}