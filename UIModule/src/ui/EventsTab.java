package ui;

import ui.model.MarketModel;
import dto.EventDTO;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * The Events screen: on the left all the events of the market with a filter line, on the right the details of the
 * selected event and the trade box (as in the sketch of the exercise).
 */
final class EventsTab extends SplitPane {

    private static final String ALL = "All";

    private final MarketModel model;
    private final FilteredList<EventDTO> filtered;
    private final TableView<EventDTO> table;
    private final EventDetailsPane details;

    private final ComboBox<String> methodFilter = filterBox(ALL, EventDTO.METHOD_LMSR, EventDTO.METHOD_ORDER_BOOK);
    private final ComboBox<String> statusFilter = filterBox(ALL, EventDTO.STATUS_NOT_STARTED,
            EventDTO.STATUS_ACTIVE, EventDTO.STATUS_CLOSED);
    private final ComboBox<String> commissionFilter = filterBox(ALL, "on-purchase", "on-close");

    EventsTab(UiContext ctx) {
        super();
        this.model = ctx.model;
        this.details = new EventDetailsPane(ctx);
        this.filtered = new FilteredList<>(model.events(), event -> true);

        SortedList<EventDTO> sorted = new SortedList<>(filtered);
        table = new TableView<>(sorted);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        Tables.number(table, "#", 0.07, EventDTO::getNumber, Fmt::integer);
        Tables.text(table, "Name", 0.27, EventDTO::getName);
        Tables.text(table, "Method", 0.13, EventDTO::getMethod);
        Tables.text(table, "Status", 0.13, EventDTO::getStatus);
        Tables.text(table, "Commission", 0.18, EventDTO::getCommissionInfo);
        Tables.text(table, "Market Maker", 0.22, EventDTO::getMarketMakerName);
        Tables.placeholder(table, "No events yet. Load an XML file in the Account screen.");

        table.getSelectionModel().selectedItemProperty().addListener((obs, was, now) -> {
            if (now != null) {
                details.follow(now.getName());
            }
        });
        model.eventsRevisionProperty().addListener((obs, was, now) -> {
            restoreSelection();
            if (now.intValue() == 1) {
                Animations.fadeIn(table);
            }
        });
        for (ComboBox<String> box : java.util.List.of(methodFilter, statusFilter, commissionFilter)) {
            box.setOnAction(e -> applyFilter());
        }

        VBox left = new VBox(8, buildFilterLine(), table);
        left.setPadding(new Insets(8));
        VBox.setVgrow(table, Priority.ALWAYS);

        getItems().addAll(left, details);
        setOrientation(Orientation.HORIZONTAL);
        setDividerPositions(0.42);
    }

    private static ComboBox<String> filterBox(String... values) {
        ComboBox<String> box = new ComboBox<>();
        box.getItems().addAll(values);
        box.setValue(ALL);
        return box;
    }

    private FlowPane buildFilterLine() {
        FlowPane line = new FlowPane(14, 6);
        line.getStyleClass().add("filter-line");
        line.getChildren().addAll(labeled("Method", methodFilter), labeled("Status", statusFilter),
                labeled("Commission", commissionFilter));
        return line;
    }

    private static HBox labeled(String caption, ComboBox<String> box) {
        HBox pair = new HBox(5, new Label(caption + ":"), box);
        pair.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return pair;
    }

    private void applyFilter() {
        String method = methodFilter.getValue();
        String status = statusFilter.getValue();
        String commission = commissionFilter.getValue();
        filtered.setPredicate(event -> matches(method, event.getMethod())
                && matches(status, event.getStatus())
                && matches(commission, event.getCommissionType()));
        restoreSelection();
    }

    private static boolean matches(String wanted, String actual) {
        return wanted == null || ALL.equals(wanted) || wanted.equalsIgnoreCase(actual);
    }

    /**
     * When the data is refreshed the table may lose the selected row (that is how the JavaFX lists work).
     * The user must not notice: the row of the event that he looks at is selected again.
     */
    private void restoreSelection() {
        String name = details.followedName();
        if (name == null) {
            return;
        }
        EventDTO current = model.findEvent(name);
        if (current != null && table.getSelectionModel().getSelectedItem() != current
                && table.getItems().contains(current)) {
            table.getSelectionModel().select(current);
        }
    }
}