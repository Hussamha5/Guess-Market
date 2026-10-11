package ui;

import ui.model.MarketModel;
import ui.model.Poller;
import ui.net.ApiClient;
import ui.net.Reply;
import javafx.scene.control.Label;

/** What every screen needs: the server, the data model, the poller, and what to do when the session is lost. */
public final class UiContext {

    public final ApiClient api;
    public final MarketModel model;
    public final Poller poller;
    private final Runnable onSessionLost;

    public UiContext(ApiClient api, MarketModel model, Poller poller, Runnable onSessionLost) {
        this.api = api;
        this.model = model;
        this.poller = poller;
        this.onSessionLost = onSessionLost;
    }

    /**
     * Shows the answer of the server in a label: green for success, red for an explanation of an error.
     * If the server does not know us any more, the user is sent back to the login screen instead.
     */
    public void show(Label target, Reply reply) {
        if (reply.isSessionLost()) {
            onSessionLost.run();
            return;
        }
        message(target, reply.message(), !reply.ok());
    }

    public static void message(Label target, String text, boolean error) {
        target.setText(text);
        target.getStyleClass().removeAll("error-text", "success-text");
        target.getStyleClass().add(error ? "error-text" : "success-text");
    }
}