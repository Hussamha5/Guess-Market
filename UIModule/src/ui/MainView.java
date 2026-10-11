package ui;

import ui.model.MarketModel;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

/** The main window after the login: the header, the two screens (Events and Account) and the status line. */
public final class MainView extends BorderPane {

    public MainView(UiContext ctx, Runnable onLogout) {
        MarketModel model = ctx.model;

        Tab eventsTab = new Tab("Events", new EventsTab(ctx));
        Tab accountTab = new Tab("Account", new AccountTab(ctx));
        for (Tab tab : new Tab[]{eventsTab, accountTab}) {
            tab.setClosable(false);
        }
        TabPane tabs = new TabPane(eventsTab, accountTab);

        setTop(buildHeader(model, onLogout));
        setCenter(tabs);
        setBottom(new StatusBar(model));
    }

    private FlowPane buildHeader(MarketModel model, Runnable onLogout) {
        Label title = new Label("Guess Market");
        title.getStyleClass().add("app-title");

        Label user = new Label();
        user.textProperty().bind(Bindings.createStringBinding(
                () -> "User: " + model.getUserName() + "   |   Balance: " + Fmt.money(model.getBalance()),
                model.userNameProperty(), model.balanceProperty()));
        user.getStyleClass().add("caption");

        ComboBox<String> themeBox = new ComboBox<>();
        themeBox.getItems().addAll(Themes.NAMES);
        themeBox.setValue(Themes.current());
        themeBox.setOnAction(e -> {
            if (getScene() != null) {
                Themes.apply(getScene(), themeBox.getValue());
            }
        });

        CheckBox animations = new CheckBox("Enable Animations");
        animations.setSelected(Animations.isEnabled());
        animations.setOnAction(e -> Animations.setEnabled(animations.isSelected()));

        Button logout = new Button("Logout");
        logout.setOnAction(e -> onLogout.run());

        // a FlowPane: when the window is small the controls move to a second line instead of being cut
        FlowPane header = new FlowPane(14, 6, title, user, animations, themeBox, logout);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(8, 10, 8, 10));
        header.getStyleClass().add("header");
        return header;
    }
}