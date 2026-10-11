package ui;

import ui.model.ListSync;
import ui.model.MarketModel;
import dto.EventDTO;
import dto.ParticipantDTO;
import dto.TransactionDTO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * "Event details and trade": the data of one event, the order book of each option, the controls of the
 * Market Maker (activate / close), the trade box, the participants and the history of the trades.
 * The same pane is used in the Events screen and in the Account screen.
 * <p>
 * The pane follows an event by its name: whenever a new version of the events arrives it redraws only what changed,
 * so nothing flickers and the text that the user typed (quantity, price) stays where it is.
 */
public final class EventDetailsPane extends ScrollPane {

    private final UiContext ctx;
    private final MarketModel model;

    private String followedName;
    private EventDTO shown;
    private List<OptionBookPane> optionPanes = new ArrayList<>();

    private final VBox body = new VBox(12);
    private final Label placeholder = new Label("Select an event to see its details and to trade.");
    private final VBox details = new VBox(12);

    // the data of the event
    private final Label title = new Label();
    private final Label status = new Label();
    private final Label method = new Label();
    private final Label commission = new Label();
    private final Label marketMaker = new Label();
    private final Label parameters = new Label();
    private final Label contract = new Label();
    private final Label description = new Label();
    private final Label winner = new Label();

    // the Market Maker
    private final VBox managerBox = new VBox(6);
    private final Button activateButton = new Button("Activate event");
    private final ComboBox<String> winningBox = new ComboBox<>();
    private final Button closeButton = new Button("Close event");
    private final Label managerMessage = new Label();
    private final HBox activateRow = new HBox(8);
    private final HBox closeRow = new HBox(8);

    // the options
    private final FlowPane optionsFlow = new FlowPane(10, 10);

    // trade
    private final ComboBox<String> optionBox = new ComboBox<>();
    private final TextField quantityField = new TextField();
    private final TextField priceField = new TextField();
    private final Button buyButton = new Button("Buy");
    private final Button sellButton = new Button("Sell");
    private final Label tradeHint = new Label();
    private final Label tradeMessage = new Label();
    private final VBox tradeBox = new VBox(6);

    // participants and history
    private final ObservableList<ParticipantDTO> participants = FXCollections.observableArrayList();
    private final ObservableList<TransactionDTO> history = FXCollections.observableArrayList();

    public EventDetailsPane(UiContext ctx) {
        this.ctx = ctx;
        this.model = ctx.model;

        setFitToWidth(true);
        getStyleClass().add("details-pane");

        buildManagerBox();
        buildTradeBox();
        optionsFlow.setPrefWrapLength(400);

        details.getChildren().addAll(title, buildInfoGrid(), managerBox, sectionTitle("Options"), optionsFlow,
                tradeBox, sectionTitle("Participations and history"), buildTabs());
        details.setVisible(false);
        details.setManaged(false);

        body.setPadding(new Insets(10));
        body.getChildren().addAll(placeholder, details);
        setContent(body);

        model.eventsRevisionProperty().addListener((obs, was, now) -> refresh());
        model.blockedProperty().addListener((obs, was, now) -> updateControls());
    }

    // ------------------------------------------------------------------ following an event

    /** From now on this pane shows the event with this name (and keeps showing it, as it changes). */
    public void follow(String eventName) {
        followedName = eventName;
        refresh();
    }

    public String followedName() {
        return followedName;
    }

    private void refresh() {
        show(followedName == null ? null : model.findEvent(followedName));
    }

    private void show(EventDTO event) {
        if (event == null) {
            shown = null;
            placeholder.setVisible(true);
            placeholder.setManaged(true);
            details.setVisible(false);
            details.setManaged(false);
            return;
        }
        if (ListSync.sameContent(event, shown)) {
            return; // nothing changed: do not touch the screen
        }
        boolean sameEvent = shown != null && shown.getName().equals(event.getName());
        shown = event;

        placeholder.setVisible(false);
        placeholder.setManaged(false);
        details.setVisible(true);
        details.setManaged(true);

        title.setText(event.getName());
        status.setText(event.getStatus());
        method.setText(event.getMethod());
        commission.setText(event.getCommissionInfo());
        marketMaker.setText(event.getMarketMakerName());
        description.setText(event.getDescription() == null || event.getDescription().isBlank() ? "-" : event.getDescription());
        parameters.setText(describeParameters(event));
        contract.setText("Event account " + Fmt.money(event.getMmBalance()) + "   |   Fees collected "
                + Fmt.money(event.getTotalFees()));
        winner.setText(event.getWinningOption() == null || event.getWinningOption().isBlank() ? "-" : event.getWinningOption());

        if (!sameEvent) {
            rebuildForNewEvent(event);
        }
        for (int i = 0; i < optionPanes.size() && i < event.getOutcomes().length; i++) {
            optionPanes.get(i).update(event, event.getOutcomes()[i]);
        }
        ListSync.sync(participants, event.getParticipants(), ParticipantDTO::getUserName);
        List<TransactionDTO> newestFirst = new ArrayList<>(event.getTransactions());
        Collections.reverse(newestFirst);
        ListSync.sync(history, newestFirst, TransactionDTO::getNumber);

        updateControls();
        if (!sameEvent) {
            Animations.fadeIn(details);
        }
    }

    private void rebuildForNewEvent(EventDTO event) {
        optionPanes = new ArrayList<>();
        optionsFlow.getChildren().clear();
        List<String> numbered = new ArrayList<>();
        for (int i = 0; i < event.getOptionNames().size(); i++) {
            OptionBookPane pane = new OptionBookPane(i + 1, event.isOrderBook());
            optionPanes.add(pane);
            optionsFlow.getChildren().add(pane);
            numbered.add((i + 1) + ". " + event.getOptionNames().get(i));
        }
        optionBox.getItems().setAll(numbered);
        optionBox.getSelectionModel().selectFirst();
        winningBox.getItems().setAll(numbered);
        winningBox.getSelectionModel().clearSelection();
        quantityField.clear();
        priceField.clear();
        tradeMessage.setText("");
        managerMessage.setText("");
        participants.clear();
        history.clear();
    }

    private static String describeParameters(EventDTO event) {
        if (event.isOrderBook()) {
            return "Base value per share " + event.getBaseValue() + ", initial investment " + event.getInitialInvestment()
                    + ", minting " + (event.isAllowMint() ? "allowed" : "not allowed");
        }
        return "Liquidity parameter b = " + event.getLiquidityB();
    }

    /** Which controls are visible / enabled depends on the status of the event and on who the user is. */
    private void updateControls() {
        EventDTO event = shown;
        if (event == null) {
            return;
        }
        boolean mine = model.isMyEvent(event);
        boolean notStarted = EventDTO.STATUS_NOT_STARTED.equals(event.getStatus());
        boolean active = EventDTO.STATUS_ACTIVE.equals(event.getStatus());
        boolean blocked = model.isBlocked();

        setShown(managerBox, mine && !EventDTO.STATUS_CLOSED.equals(event.getStatus()));
        setShown(activateRow, notStarted);
        setShown(closeRow, active);
        activateButton.setDisable(blocked);
        closeButton.setDisable(blocked);

        boolean canTrade = active && !blocked;
        optionBox.setDisable(!canTrade);
        quantityField.setDisable(!canTrade);
        priceField.setDisable(!canTrade || !event.isOrderBook());
        buyButton.setDisable(!canTrade);
        sellButton.setDisable(!canTrade || !event.isOrderBook());
        priceField.setPromptText(event.isOrderBook() ? "Price" : "Market price");

        if (notStarted) {
            tradeHint.setText("This event has not started yet. Only its Market Maker can activate it.");
        } else if (EventDTO.STATUS_CLOSED.equals(event.getStatus())) {
            tradeHint.setText("This event is closed. The winning option is: " + winner.getText());
        } else if (blocked) {
            tradeHint.setText("Your balance is negative, so you cannot trade. Load funds in the Account screen.");
        } else if (event.isOrderBook()) {
            tradeHint.setText("Order Book: type a quantity and your limit price (from 0.01 to "
                    + Fmt.money(event.getBaseValue() - 0.01) + ", at most two decimals), then Buy or Sell.");
        } else {
            tradeHint.setText("LMSR: you buy from the market. The price is calculated by the market and grows "
                    + "with the demand. Shares cannot be sold.");
        }
    }

    private static void setShown(javafx.scene.Node node, boolean value) {
        node.setVisible(value);
        node.setManaged(value);
    }

    // ------------------------------------------------------------------ building the controls

    private static Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private GridPane buildInfoGrid() {
        title.getStyleClass().add("event-title");
        description.setWrapText(true);
        parameters.setWrapText(true);
        contract.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(4);
        String[] captions = {"Status", "Method", "Commission", "Market Maker", "Description", "Settings",
                "Money", "Winning option"};
        Label[] values = {status, method, commission, marketMaker, description, parameters, contract, winner};
        for (int row = 0; row < captions.length; row++) {
            Label caption = new Label(captions[row] + ":");
            caption.getStyleClass().add("caption");
            grid.add(caption, 0, row);
            grid.add(values[row], 1, row);
            GridPane.setHgrow(values[row], Priority.ALWAYS);
        }
        return grid;
    }

    private void buildManagerBox() {
        Label note = new Label("You are the Market Maker of this event.");
        note.getStyleClass().add("caption");

        activateRow.setAlignment(Pos.CENTER_LEFT);
        activateRow.getChildren().add(activateButton);
        activateButton.getStyleClass().add("activate-button");
        activateButton.setOnAction(e -> managerAction(true));

        winningBox.setPromptText("Winning option");
        closeRow.setAlignment(Pos.CENTER_LEFT);
        closeRow.getChildren().addAll(new Label("Winner:"), winningBox, closeButton);
        closeButton.getStyleClass().add("close-button");
        closeButton.setOnAction(e -> managerAction(false));

        managerMessage.setWrapText(true);
        managerBox.getChildren().addAll(note, activateRow, closeRow, managerMessage);
    }

    private void managerAction(boolean activate) {
        EventDTO event = shown;
        if (event == null) {
            return;
        }
        int winning = winningBox.getSelectionModel().getSelectedIndex() + 1;
        if (!activate && winning < 1) {
            UiContext.message(managerMessage, "Choose the winning option first.", true);
            return;
        }
        activateButton.setDisable(true);
        closeButton.setDisable(true);
        var request = activate ? ctx.api.activate(event.getName()) : ctx.api.close(event.getName(), winning);
        request.thenAcceptAsync(reply -> {
            ctx.show(managerMessage, reply);
            updateControls();
            ctx.poller.pollNow();
        }, Platform::runLater);
    }

    private void buildTradeBox() {
        optionBox.setPrefWidth(190);
        optionBox.setPromptText("Option");
        quantityField.setPromptText("Quantity");
        quantityField.setPrefColumnCount(6);
        priceField.setPrefColumnCount(6);
        buyButton.getStyleClass().add("buy-button");
        sellButton.getStyleClass().add("sell-button");
        buyButton.setOnAction(e -> trade(true));
        sellButton.setOnAction(e -> trade(false));
        tradeHint.setWrapText(true);
        tradeMessage.setWrapText(true);

        FlowPane controls = new FlowPane(8, 6, optionBox, quantityField, priceField, buyButton, sellButton);
        controls.setAlignment(Pos.CENTER_LEFT);
        tradeBox.getChildren().addAll(sectionTitle("Trade"), controls, tradeHint, tradeMessage);
    }

    private void trade(boolean buy) {
        EventDTO event = shown;
        if (event == null) {
            return;
        }
        int option = optionBox.getSelectionModel().getSelectedIndex() + 1;
        if (option < 1) {
            UiContext.message(tradeMessage, "Choose an option first.", true);
            return;
        }
        buyButton.setDisable(true);
        sellButton.setDisable(true);
        ctx.api.trade(event.getName(), option, buy, quantityField.getText().trim(), priceField.getText().trim())
                .thenAcceptAsync(reply -> {
                    ctx.show(tradeMessage, reply);
                    updateControls();
                    ctx.poller.pollNow();
                }, Platform::runLater);
    }

    private TabPane buildTabs() {
        TableView<ParticipantDTO> participantsTable = new TableView<>(participants);
        Tables.text(participantsTable, "User", 0.30, ParticipantDTO::getUserName);
        Tables.text(participantsTable, "Shares per option", 0.30, p -> Fmt.shares(p.getShares()));
        Tables.number(participantsTable, "Value", 0.20, ParticipantDTO::getValue, Fmt::money);
        Tables.number(participantsTable, "Open orders", 0.20, ParticipantDTO::getOpenOrders, Fmt::integer);
        Tables.noSorting(participantsTable);
        Tables.placeholder(participantsTable, "Nobody holds shares in this event yet");

        TableView<TransactionDTO> historyTable = new TableView<>(history);
        Tables.number(historyTable, "#", 0.06, TransactionDTO::getNumber, Fmt::integer);
        Tables.text(historyTable, "Type", 0.09, TransactionDTO::getType);
        Tables.text(historyTable, "Option", 0.15, TransactionDTO::getOutcomeName);
        Tables.number(historyTable, "Qty", 0.07, TransactionDTO::getQuantity, Fmt::integer);
        Tables.number(historyTable, "Price", 0.10, TransactionDTO::getUnitPrice, Fmt::money);
        Tables.number(historyTable, "Paid", 0.10, TransactionDTO::getTotalPaid, Fmt::money);
        Tables.number(historyTable, "Fee", 0.08, TransactionDTO::getCommission, Fmt::money);
        Tables.text(historyTable, "Buyer", 0.17, TransactionDTO::getBuyer);
        Tables.text(historyTable, "Seller", 0.17, TransactionDTO::getSeller);
        Tables.noSorting(historyTable);
        Tables.placeholder(historyTable, "No trades yet");

        Tab participantsTab = new Tab("Participations", participantsTable);
        Tab historyTab = new Tab("Trade history (newest first)", historyTable);
        participantsTab.setClosable(false);
        historyTab.setClosable(false);
        TabPane tabs = new TabPane(participantsTab, historyTab);
        tabs.setPrefHeight(230);
        tabs.setMinHeight(170);
        return tabs;
    }
}