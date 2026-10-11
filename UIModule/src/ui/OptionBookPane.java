package ui;

import ui.model.ListSync;
import dto.EventDTO;
import dto.OrderDTO;
import dto.OutcomeDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * One option of an event: its name, its prices and - for an Order Book event - the waiting buy orders (bids) and
 * sell orders (asks). The controls are created once; {@link #update} only changes what really changed.
 */
final class OptionBookPane extends VBox {

    private final int number;
    private final Label title = new Label();
    private final Label stats = new Label();
    private final ObservableList<OrderDTO> bids = FXCollections.observableArrayList();
    private final ObservableList<OrderDTO> asks = FXCollections.observableArrayList();
    private final HBox books;

    OptionBookPane(int number, boolean orderBook) {
        super(6);
        this.number = number;
        getStyleClass().add("option-pane");
        setPadding(new Insets(8));
        title.getStyleClass().add("option-title");
        stats.setWrapText(true);

        books = new HBox(8, bookBox("Bids (buy orders)", bids), bookBox("Asks (sell orders)", asks));
        books.setVisible(orderBook);
        books.setManaged(orderBook);

        setPrefWidth(orderBook ? 330 : 250);
        getChildren().addAll(title, stats, books);
    }

    private static VBox bookBox(String caption, ObservableList<OrderDTO> orders) {
        TableView<OrderDTO> table = new TableView<>(orders);
        Tables.text(table, "User", 0.40, OrderDTO::getUserName);
        Tables.number(table, "Qty", 0.25, OrderDTO::getQuantity, Fmt::integer);
        Tables.number(table, "Price", 0.35, OrderDTO::getPrice, Fmt::money);
        Tables.noSorting(table);
        Tables.placeholder(table, "No orders");
        table.setPrefHeight(130);
        table.setMinHeight(90);
        VBox box = new VBox(3, new Label(caption), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setPrefWidth(150);
        return box;
    }

    void update(EventDTO event, OutcomeDTO outcome) {
        title.setText("Option " + number + ": " + outcome.getName());
        if (event.isOrderBook()) {
            stats.setText("Last " + Fmt.priceOrDash(outcome.getLastPrice())
                    + "   Bid " + Fmt.priceOrDash(outcome.getHighestBid())
                    + "   Ask " + Fmt.priceOrDash(outcome.getLowestAsk())
                    + "   Mid " + Fmt.priceOrDash(outcome.getMidPrice())
                    + "   Spread " + Fmt.priceOrDash(outcome.getSpread())
                    + "   Shares " + outcome.getQuantity());
            ListSync.syncByPosition(bids, outcome.getBids());
            ListSync.syncByPosition(asks, outcome.getAsks());
        } else {
            stats.setText("Price " + Fmt.money(outcome.getPrice()) + " (" + Fmt.percent(outcome.getPrice())
                    + ")   Shares sold " + outcome.getQuantity());
        }
    }
}