package ui;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/**
 * Small helpers that build the columns of the tables. The width of every column is a share of the width of the
 * table (bound), so when the window is resized the columns grow and shrink with it.
 */
public final class Tables {

    /** Room for the vertical scroll bar, so that no horizontal scroll bar appears for no reason. */
    private static final double SCROLL_BAR_ROOM = 22;
    private static final double MIN_COLUMN_WIDTH = 36;

    private Tables() {
    }

    public static <T> TableColumn<T, String> text(TableView<T> table, String title, double share,
                                                  Function<T, String> value) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        add(table, column, share);
        return column;
    }

    /** A number column: sorted as a number, shown with the given format, aligned to the right. */
    public static <T> TableColumn<T, Double> number(TableView<T> table, String title, double share,
                                                    ToDoubleFunction<T> value, DoubleFunction<String> format) {
        return numberColumn(table, title, share, value, format, false);
    }

    /** Like number, but positive values are green and negative values are red (profit, loss, account rows). */
    public static <T> TableColumn<T, Double> coloredNumber(TableView<T> table, String title, double share,
                                                           ToDoubleFunction<T> value, DoubleFunction<String> format) {
        return numberColumn(table, title, share, value, format, true);
    }

    private static <T> TableColumn<T, Double> numberColumn(TableView<T> table, String title, double share,
                                                           ToDoubleFunction<T> value, DoubleFunction<String> format,
                                                           boolean colored) {
        TableColumn<T, Double> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleObjectProperty<Double>(value.applyAsDouble(cell.getValue())));
        column.setCellFactory(c -> new TableCell<T, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("positive", "negative");
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(format.apply(item));
                if (colored && item > 0.004) {
                    getStyleClass().add("positive");
                } else if (colored && item < -0.004) {
                    getStyleClass().add("negative");
                }
            }
        });
        column.setStyle("-fx-alignment: CENTER-RIGHT;");
        add(table, column, share);
        return column;
    }

    private static <T> void add(TableView<T> table, TableColumn<T, ?> column, double share) {
        column.setMinWidth(MIN_COLUMN_WIDTH);
        column.setResizable(false); // the width is a share of the table, so it follows the window
        column.prefWidthProperty().bind(table.widthProperty().subtract(SCROLL_BAR_ROOM).multiply(share));
        table.getColumns().add(column);
    }

    /** For tables whose rows are kept in the order of the server (clicking on a header would reorder the data). */
    public static <T> void noSorting(TableView<T> table) {
        for (TableColumn<T, ?> column : table.getColumns()) {
            column.setSortable(false);
        }
    }

    public static <T> void placeholder(TableView<T> table, String text) {
        table.setPlaceholder(new Label(text));
    }
}