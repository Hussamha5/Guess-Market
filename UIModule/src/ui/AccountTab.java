package ui;

import ui.model.MarketModel;
import dto.LedgerEntryDTO;
import dto.ParticipationDTO;
import dto.UserDTO;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * The Account screen (as in the sketch): the Load File line on top; on the left the table of all the users and my
 * balance with the load funds line and the account rows; on the right my events and the details of the selected one.
 */
final class AccountTab extends BorderPane {

    private final UiContext ctx;
    private final MarketModel model;

    private final Button loadFileButton = new Button("Load File");
    private final TextField pathField = new TextField();
    private final ProgressBar uploadProgress = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
    private final Label uploadResult = new Label();
    private final ScrollPane uploadResultScroll = new ScrollPane(uploadResult);

    private final Label balanceLabel = new Label();
    private final TextField amountField = new TextField();
    private final Button loadFundsButton = new Button("Load funds");
    private final Label fundsMessage = new Label();

    private final TableView<ParticipationDTO> participationsTable;
    private final EventDetailsPane details;

    AccountTab(UiContext ctx) {
        this.ctx = ctx;
        this.model = ctx.model;
        this.details = new EventDetailsPane(ctx);
        this.participationsTable = buildParticipationsTable();

        setTop(buildLoadFileLine());

        SplitPane left = new SplitPane(buildUsersTable(), buildAccountBox());
        left.setOrientation(Orientation.VERTICAL);
        left.setDividerPositions(0.38);

        SplitPane right = new SplitPane(buildMyEventsBox(), details);
        right.setOrientation(Orientation.VERTICAL);
        right.setDividerPositions(0.36);

        SplitPane center = new SplitPane(left, right);
        center.setOrientation(Orientation.HORIZONTAL);
        center.setDividerPositions(0.42);
        setCenter(center);

        model.accountRevisionProperty().addListener((obs, was, now) -> restoreSelection());
    }

    // ------------------------------------------------------------------ Load File

    private VBox buildLoadFileLine() {
        loadFileButton.getStyleClass().add("load-file-button");
        loadFileButton.setOnAction(e -> chooseAndUpload());
        pathField.setEditable(false);
        pathField.setPromptText("File path (the events of the file are added to the market, and you become their Market Maker)");
        HBox.setHgrow(pathField, Priority.ALWAYS);
        uploadProgress.setVisible(false);
        uploadProgress.setManaged(false);

        HBox line = new HBox(10, loadFileButton, pathField, uploadProgress);
        line.setAlignment(Pos.CENTER_LEFT);

        uploadResult.setWrapText(true);
        uploadResultScroll.setFitToWidth(true);
        uploadResultScroll.setPrefViewportHeight(60);
        uploadResultScroll.setMaxHeight(110);
        uploadResultScroll.setVisible(false);
        uploadResultScroll.setManaged(false);

        VBox box = new VBox(6, line, uploadResultScroll);
        box.setPadding(new Insets(8));
        return box;
    }

    private void chooseAndUpload() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select an XML file with events");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML files", "*.xml"));
        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        pathField.setText(file.getAbsolutePath());
        setUploading(true);
        ctx.api.upload(file.toPath()).thenAcceptAsync(reply -> {
            setUploading(false);
            ctx.show(uploadResult, reply);
            if (!reply.isSessionLost()) {
                uploadResultScroll.setVisible(true);
                uploadResultScroll.setManaged(true);
            }
            ctx.poller.pollNow();
        }, Platform::runLater);
    }

    private void setUploading(boolean uploading) {
        loadFileButton.setDisable(uploading);
        uploadProgress.setVisible(uploading);
        uploadProgress.setManaged(uploading);
        if (uploading) {
            uploadResultScroll.setVisible(false);
            uploadResultScroll.setManaged(false);
        }
    }

    // ------------------------------------------------------------------ users

    private VBox buildUsersTable() {
        TableView<UserDTO> table = new TableView<>();
        SortedList<UserDTO> sorted = new SortedList<>(model.users());
        table.setItems(sorted);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        Tables.text(table, "User", 0.45, user -> user.getName()
                + (user.getName().equalsIgnoreCase(model.getUserName()) ? " (you)" : ""));
        Tables.number(table, "Balance", 0.30, UserDTO::getBalance, Fmt::money);
        Tables.text(table, "Market Maker", 0.25, user -> user.isMarketMaker() ? "Yes" : "No");
        Tables.placeholder(table, "No users");
        return titled("Users", table);
    }

    private static VBox titled(String caption, javafx.scene.Node content) {
        Label label = new Label(caption);
        label.getStyleClass().add("section-title");
        VBox box = new VBox(4, label, content);
        box.setPadding(new Insets(6));
        VBox.setVgrow(content, Priority.ALWAYS);
        return box;
    }

    // ------------------------------------------------------------------ my account

    private VBox buildAccountBox() {
        balanceLabel.getStyleClass().add("balance");
        balanceLabel.textProperty().bind(Bindings.createStringBinding(
                () -> "Balance: " + Fmt.money(model.getBalance()), model.balanceProperty()));
        model.balanceProperty().addListener((obs, was, now) -> {
            balanceLabel.getStyleClass().remove("negative");
            if (now.doubleValue() < 0) {
                balanceLabel.getStyleClass().add("negative");
            }
            Animations.fadeIn(balanceLabel);
        });

        amountField.setPromptText("Amount");
        amountField.setPrefColumnCount(8);
        loadFundsButton.setOnAction(e -> loadFunds());
        amountField.setOnAction(e -> loadFunds());
        HBox fundsLine = new HBox(8, amountField, loadFundsButton);
        fundsLine.setAlignment(Pos.CENTER_LEFT);
        fundsMessage.setWrapText(true);

        TableView<LedgerEntryDTO> ledger = new TableView<>(model.ledger());
        Tables.number(ledger, "#", 0.08, LedgerEntryDTO::getSerial, Fmt::integer);
        Tables.text(ledger, "Time", 0.15, row -> Fmt.time(row.getTimeMillis()));
        Tables.text(ledger, "Description", 0.41, LedgerEntryDTO::getDescription);
        Tables.coloredNumber(ledger, "Amount", 0.18, LedgerEntryDTO::getAmount, Fmt::signed);
        Tables.number(ledger, "Balance", 0.18, LedgerEntryDTO::getBalanceAfter, Fmt::money);
        Tables.noSorting(ledger);
        Tables.placeholder(ledger, "No account rows yet. Load funds to start.");

        Label ledgerTitle = new Label("Account rows (newest first)");
        ledgerTitle.getStyleClass().add("caption");
        VBox box = new VBox(6, sectionTitle("My account"), balanceLabel, fundsLine, fundsMessage, ledgerTitle, ledger);
        box.setPadding(new Insets(6));
        VBox.setVgrow(ledger, Priority.ALWAYS);
        return box;
    }

    private static Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private void loadFunds() {
        loadFundsButton.setDisable(true);
        ctx.api.loadFunds(amountField.getText().trim()).thenAcceptAsync(reply -> {
            loadFundsButton.setDisable(false);
            ctx.show(fundsMessage, reply);
            if (reply.ok()) {
                amountField.clear();
            }
            ctx.poller.pollNow();
        }, Platform::runLater);
    }

    // ------------------------------------------------------------------ my events

    private VBox buildMyEventsBox() {
        Label caption = new Label("My events (as a Market Maker or as a participant)");
        caption.getStyleClass().add("section-title");
        VBox box = new VBox(4, caption, participationsTable);
        box.setPadding(new Insets(6));
        VBox.setVgrow(participationsTable, Priority.ALWAYS);
        return box;
    }

    private TableView<ParticipationDTO> buildParticipationsTable() {
        TableView<ParticipationDTO> table = new TableView<>(model.participations());
        Tables.text(table, "Event", 0.17, ParticipationDTO::getEventName);
        Tables.text(table, "Role", 0.12, ParticipationDTO::getRole);
        Tables.text(table, "Status", 0.10, ParticipationDTO::getStatus);
        Tables.text(table, "Shares", 0.10, p -> Fmt.shares(p.getShares()));
        Tables.number(table, "Invested", 0.10, ParticipationDTO::getInvested, Fmt::money);
        Tables.number(table, "Fees paid", 0.10, ParticipationDTO::getCommissionPaid, Fmt::money);
        Tables.number(table, "Fees got", 0.10, ParticipationDTO::getCommissionReceived, Fmt::money);
        Tables.number(table, "Payout", 0.10, ParticipationDTO::getPayout, Fmt::money);
        Tables.coloredNumber(table, "Profit/Loss", 0.11, ParticipationDTO::getProfitLoss, Fmt::signed);
        Tables.noSorting(table);
        Tables.placeholder(table, "You do not take part in any event yet");
        table.getSelectionModel().selectedItemProperty().addListener((obs, was, now) -> {
            if (now != null) {
                details.follow(now.getEventName());
            }
        });
        return table;
    }

    /** The refresh of the data may drop the selected row: select it again, the user must not notice. */
    private void restoreSelection() {
        String name = details.followedName();
        if (name == null) {
            return;
        }
        ParticipationDTO selected = participationsTable.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getEventName().equals(name)) {
            return;
        }
        for (ParticipationDTO row : participationsTable.getItems()) {
            if (row.getEventName().equals(name)) {
                participationsTable.getSelectionModel().select(row);
                return;
            }
        }
    }
}