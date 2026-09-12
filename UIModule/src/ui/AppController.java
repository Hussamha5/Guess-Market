package ui;

import dto.UserDTO;
import engine.Engine;
import engine.Market;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;
import javafx.scene.control.CheckBox;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;
import javafx.animation.TranslateTransition;
import javafx.scene.control.ComboBox;

public class AppController {

    @FXML private Button btnLoadXml;
    @FXML private ProgressBar progressBar;
    @FXML private Label lblStatus;

    @FXML private ListView<UserDTO> usersListView;
    @FXML private Label lblUserName;
    @FXML private Label lblUserBalance;
    @FXML private ListView<dto.EventDTO> eventsListView;
    @FXML private Label lblEventName;
    @FXML private Label lblEventStatus;
    @FXML private Label lblEventMethod;
    @FXML private Label lblEventCommission;
    @FXML private javafx.scene.control.ComboBox<String> cmbOptions;
    @FXML private javafx.scene.control.TextField txtQuantity;
    @FXML private javafx.scene.control.TextField txtPrice;
    @FXML private javafx.scene.control.Button btnBuy;
    @FXML private javafx.scene.control.Button btnSell;
    @FXML private javafx.scene.control.ListView<String> orderBookListView;

    @FXML private javafx.scene.control.Label lblLast;
    @FXML private javafx.scene.control.Label lblBid;
    @FXML private javafx.scene.control.Label lblAsk;
    @FXML private javafx.scene.control.Label lblMid;
    @FXML private javafx.scene.control.Label lblSpread;
    @FXML private javafx.scene.control.ComboBox<String> cmbTheme;
    @FXML private CheckBox chkAnimations;
    @FXML private ComboBox<String> cmbFilterType;
    @FXML private ComboBox<String> cmbFilterStatus;
    @FXML private ComboBox<String> cmbFilterComm;
    @FXML private Button btnActivate;
    @FXML private Button btnClose;
    @FXML private javafx.scene.control.TextArea txtUserDetails;

    // נשמור את הרשימה המקורית של האירועים כדי לסנן מתוכה
    private List<dto.EventDTO> allEvents;

    private Engine engine = new Market();

    @FXML
    public void initialize() {
        progressBar.setProgress(0);
        cmbTheme.getItems().addAll("Default Theme", "Dark Mode", "Blue Mode");

        // אתחול הפילטרים
        cmbFilterType.getItems().addAll("All", "LMSR", "Order Book");
        cmbFilterStatus.getItems().addAll("All", "Not Started", "Active", "Closed");
        cmbFilterComm.getItems().addAll("All", "on-purchase", "on-close");
        cmbFilterType.setValue("All");
        cmbFilterStatus.setValue("All");
        cmbFilterComm.setValue("All");



        usersListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                lblUserName.setText("Name: " + newValue.getName());
                lblUserBalance.setText("Balance: $" + String.format("%.2f", newValue.getBalance()));

                StringBuilder details = new StringBuilder();

                if (newValue.getMmEventIds() != null && !newValue.getMmEventIds().isEmpty()) {
                    details.append("Market Maker For Events: ").append(newValue.getMmEventIds()).append("\n\n");
                }

                details.append("--- Active Portfolio & Financials ---\n");
                if (newValue.getPortfolio() != null && !newValue.getPortfolio().isEmpty()) {
                    for (String eventId : newValue.getPortfolio().keySet()) {
                        details.append("Event ID: ").append(eventId).append("\n");
                        java.util.Map<Integer, Integer> shares = newValue.getPortfolio().get(eventId);
                        for (Integer optIndex : shares.keySet()) {
                            if (shares.get(optIndex) > 0) {
                                details.append("  Option ").append(optIndex + 1).append(" -> Shares: ").append(shares.get(optIndex)).append("\n");
                            }
                        }

                        // חישוב עמלות ורווחים
                        double comms = newValue.getCommissionsPaid().getOrDefault(eventId, 0.0);
                        details.append("  Total Commission Paid: $").append(String.format("%.2f", comms)).append("\n");

                        double payout = newValue.getFinalPayouts().getOrDefault(eventId, 0.0);
                        double invest = newValue.getNetInvestments().getOrDefault(eventId, 0.0);

                        // בדיקה אם האירוע סגור דרך רשימת האירועים הכללית (כדי להציג רווח/הפסד)
                        boolean isClosed = false;
                        for (dto.EventDTO e : eventsListView.getItems()) {
                            if (e.getId().equals(eventId) && e.getStatus().equals("Closed")) {
                                isClosed = true; break;
                            }
                        }

                        if (isClosed) {
                            double profit = payout - invest;
                            details.append("  Profit/Loss (Closed Event): $").append(String.format("%.2f", profit)).append("\n");
                        }
                        details.append("\n");
                    }
                } else {
                    details.append("No active investments.\n");
                }

                txtUserDetails.setText(details.toString());
                updateButtonStates();
            }
        });
        eventsListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                lblEventName.setText("Event Name: " + newValue.getName());
                lblEventStatus.setText("Status: " + newValue.getStatus());
                lblEventMethod.setText("Method: " + newValue.getMethod());
                lblEventCommission.setText("Commission: " + newValue.getCommissionInfo());
                updateButtonStates();

                cmbOptions.getItems().clear();
                if (newValue.getOptionNames() != null) {
                    cmbOptions.getItems().addAll(newValue.getOptionNames());
                }

                orderBookListView.getItems().clear();
                java.util.List<dto.TransactionDTO> trans = newValue.getTransactions();

                if (trans != null && !trans.isEmpty()) {
                    for (int i = trans.size() - 1; i >= 0; i--) {
                        dto.TransactionDTO t = trans.get(i);
                        orderBookListView.getItems().add(
                                "Option: " + t.getOutcomeName() +
                                        " | Qty: " + t.getQuantity() +
                                        " | Paid: $" + String.format("%.2f", t.getTotalPaid())
                        );
                    }
                } else {
                    orderBookListView.getItems().add("No trades yet.");
                }
            }
        });
        cmbOptions.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            int index = newVal.intValue();
            dto.EventDTO evt = eventsListView.getSelectionModel().getSelectedItem();
            if (index >= 0 && evt != null && evt.getOutcomes() != null) {
                dto.OutcomeDTO out = evt.getOutcomes()[index];
                if (evt.getMethod().equals("Order Book")) {
                    lblLast.setText(String.format("Last: $%.2f", out.getLastPrice()));
                    lblBid.setText(String.format("Bid: $%.2f", out.getHighestBid()));
                    lblAsk.setText(String.format("ASK: $%.2f", out.getLowestAsk()));
                    lblMid.setText(String.format("Mid: $%.2f", out.getMidPrice()));
                    lblSpread.setText(String.format("Spread: $%.2f", out.getSpread()));
                } else {
                    lblLast.setText("Last: N/A");
                    lblBid.setText("Bid: N/A");
                    lblAsk.setText("ASK: N/A");
                    lblMid.setText("Mid: N/A");
                    lblSpread.setText("Spread: N/A");
                }
            }
        });
    }


    @FXML
    public void handleLoadXml() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Guess Market XML");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));

        File selectedFile = fileChooser.showOpenDialog(btnLoadXml.getScene().getWindow());

        if (selectedFile != null) {
            Task<Boolean> loadTask = new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {
                    updateMessage("Starting file load...");
                    updateProgress(10, 100);
                    Thread.sleep(500);

                    updateMessage("Parsing XML and validating data...");
                    updateProgress(50, 100);

                    engine.loadXml(selectedFile.getAbsolutePath());

                    updateMessage("Data loaded and processed successfully!");
                    updateProgress(100, 100);
                    return true;
                }
            };

            lblStatus.textProperty().bind(loadTask.messageProperty());
            progressBar.progressProperty().bind(loadTask.progressProperty());

            loadTask.setOnSucceeded(e -> {
                lblStatus.textProperty().unbind();
                lblStatus.setText("Load completed successfully!");

                List<UserDTO> loadedUsers = engine.getUsers();
                usersListView.getItems().clear();
                usersListView.getItems().addAll(loadedUsers);

                allEvents = engine.getEvents();
                applyFilters();

                if (chkAnimations.isSelected()) {
                    FadeTransition ft = new FadeTransition(Duration.millis(1500), eventsListView);
                    ft.setFromValue(0.0);
                    ft.setToValue(1.0);
                    ft.play();
                }
            });

            loadTask.setOnFailed(e -> {
                lblStatus.textProperty().unbind();
                lblStatus.setText("Load error: " + loadTask.getException().getMessage());
                progressBar.setProgress(0);
                usersListView.getItems().clear();// מנקים את הרשימה במקרה של שגיאה
                eventsListView.getItems().clear();
            });

            new Thread(loadTask).start();
        }
    }
    @FXML
    public void handleThemeChange() {
        String selectedTheme = cmbTheme.getValue();
        if (selectedTheme == null) return;

        javafx.scene.Scene scene = cmbTheme.getScene();
        scene.getStylesheets().clear(); // מנקים עיצובים קודמים

        if (selectedTheme.equals("Dark Mode")) {
            scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        } else if (selectedTheme.equals("Blue Mode")) {
            scene.getStylesheets().add(getClass().getResource("/blue-theme.css").toExternalForm());
        }
    }
    @FXML
    public void handleBuy() {
        processOrder(true);
    }

    @FXML
    public void handleSell() {
        processOrder(false);
    }

    // פונקציית עזר שמרכזת את לוגיקת הקנייה/מכירה וחוסכת קוד כפול
    @FXML
    private void processOrder(boolean isBuy) {
        dto.UserDTO selectedUser = usersListView.getSelectionModel().getSelectedItem();
        dto.EventDTO selectedEvent = eventsListView.getSelectionModel().getSelectedItem();
        int optionIndex = cmbOptions.getSelectionModel().getSelectedIndex();

        if (selectedUser == null || selectedEvent == null || optionIndex < 0) {
            lblStatus.setText("Trade Error: Please select a User, Event, and an Option first.");
            return;
        }

        try {
            int quantity = Integer.parseInt(txtQuantity.getText());
            int realEventIndex = getRealEventIndex(selectedEvent); // תיקון האינדקס!
            int userIndex = usersListView.getSelectionModel().getSelectedIndex();

            if (selectedEvent.getMethod().equals("LMSR")) {
                if (!isBuy) {
                    lblStatus.setText("Error: Cannot sell shares in LMSR directly through this menu.");
                    return;
                }
                // קריאה למנוע עבור מסחר LMSR עם קישור למשתמש הקונה!
                ((Market)engine).processLmsrBuy(realEventIndex, selectedUser.getName(), optionIndex, quantity);
                lblStatus.setText("Success: LMSR Buy order submitted!");

            } else {
                double price = Double.parseDouble(txtPrice.getText());
                // קריאה למנוע עבור Order Book
                engine.placeOrder(realEventIndex, selectedUser.getName(), optionIndex, price, quantity, isBuy);
                lblStatus.setText("Success: " + (isBuy ? "Buy" : "Sell") + " order submitted!");
            }

            boolean isBlockedNow = false;
            for (dto.UserDTO u : engine.getUsers()) {
                if (u.getName().equals(selectedUser.getName()) && u.getBalance() < 0) {
                    isBlockedNow = true;
                    break;
                }
            }
            if (isBlockedNow) {
                lblStatus.setText(lblStatus.getText() + " (WARNING: Balance negative, user blocked!)");
            }

            txtQuantity.clear();
            txtPrice.clear();

            refreshData(realEventIndex, userIndex);

            //בונוס 2: אנימציית קפיצה
            if (chkAnimations.isSelected()) {
                Button clickedBtn = isBuy ? btnBuy : btnSell;
                TranslateTransition tt = new TranslateTransition(Duration.millis(150), clickedBtn);
                tt.setByY(-10);
                tt.setAutoReverse(true);
                tt.setCycleCount(2);
                tt.play();
            }

        } catch (NumberFormatException e) {
            lblStatus.setText("Input Error: Invalid numbers.");
        } catch (IllegalArgumentException e) {
            lblStatus.setText("Logic Error: " + e.getMessage());
        } catch (Exception e) {
            lblStatus.setText("Trade Error: " + e.getMessage());
        }
    }
    @FXML
    public void applyFilters() {
        if (allEvents == null) return;

        eventsListView.getItems().clear();
        for (dto.EventDTO e : allEvents) {
            boolean passType = cmbFilterType.getValue().equals("All") || e.getMethod().equals(cmbFilterType.getValue());
            boolean passStatus = cmbFilterStatus.getValue().equals("All") || e.getStatus().equals(cmbFilterStatus.getValue());
            boolean passComm = cmbFilterComm.getValue().equals("All") || e.getCommissionType().equals(cmbFilterComm.getValue());

            if (passType && passStatus && passComm) {
                eventsListView.getItems().add(e);
            }
        }
    }

    @FXML
    public void handleActivateEvent() {
        dto.UserDTO selectedUser = usersListView.getSelectionModel().getSelectedItem();
        dto.EventDTO selectedEvent = eventsListView.getSelectionModel().getSelectedItem();

        if (selectedUser == null || selectedEvent == null) {
            lblStatus.setText("Error: Select a User (MM) and an Event to activate.");
            return;
        }

        if (!selectedUser.getMmEventIds().contains(selectedEvent.getId())) {
            lblStatus.setText("Error: Only the designated Market Maker can activate this event.");
            return;
        }

        int realEventIndex = getRealEventIndex(selectedEvent);

        try {
            engine.activateEvent(realEventIndex, selectedUser.getName());
            lblStatus.setText("Success: Event activated!");
            refreshData(realEventIndex, usersListView.getSelectionModel().getSelectedIndex());
        } catch (Exception e) {
            lblStatus.setText("Activation Error: " + e.getMessage());
        }
    }

    @FXML
    public void handleCloseEvent() {
        dto.UserDTO selectedUser = usersListView.getSelectionModel().getSelectedItem();
        dto.EventDTO selectedEvent = eventsListView.getSelectionModel().getSelectedItem();
        int optionIndex = cmbOptions.getSelectionModel().getSelectedIndex();

        if (selectedUser == null || selectedEvent == null || optionIndex < 0) {
            lblStatus.setText("Error: Select a User (MM), an Event, and the WINNING option.");
            return;
        }

        int realEventIndex = getRealEventIndex(selectedEvent);

        try {
            engine.resolveEvent(realEventIndex, optionIndex);
            lblStatus.setText("Success: Event closed! Winnings distributed.");
            refreshData(realEventIndex, usersListView.getSelectionModel().getSelectedIndex());
        } catch (Exception e) {
            lblStatus.setText("Close Error: " + e.getMessage());
        }
    }

    // פונקציית עזר לרענון התצוגה אחרי פעולות במנוע
    private void refreshData(int realEventIndex, int userIndex) {
        int selectedOption = cmbOptions.getSelectionModel().getSelectedIndex();

        allEvents = engine.getEvents();
        applyFilters();

        for (int i = 0; i < eventsListView.getItems().size(); i++) {
            if (getRealEventIndex(eventsListView.getItems().get(i)) == realEventIndex) {
                eventsListView.getSelectionModel().select(i);
                break;
            }
        }

        usersListView.getItems().clear();
        usersListView.getItems().addAll(engine.getUsers());
        if (userIndex >= 0 && userIndex < usersListView.getItems().size()) {
            usersListView.getSelectionModel().select(userIndex);
        }

        if (selectedOption >= 0) {
            cmbOptions.getSelectionModel().select(selectedOption);
        }
    }
    private int getRealEventIndex(dto.EventDTO selectedEvent) {
        java.util.List<dto.EventDTO> allEngineEvents = engine.getEvents();
        for (int i = 0; i < allEngineEvents.size(); i++) {
            if (allEngineEvents.get(i).getId().equals(selectedEvent.getId())) {
                return i;
            }
        }
        return -1;
    }
    // פונקציית עזר לכיבוי והדלקת כפתורי הניהול לפי הרשאות המשתמש
    private void updateButtonStates() {
        dto.UserDTO selectedUser = usersListView.getSelectionModel().getSelectedItem();
        dto.EventDTO selectedEvent = eventsListView.getSelectionModel().getSelectedItem();

        if (selectedEvent != null && selectedUser != null) {
            boolean isNotStarted = selectedEvent.getStatus().equals("Not Started");
            boolean isActive = selectedEvent.getStatus().equals("Active");

            boolean isMM = false;
            if (selectedUser.getMmEventIds() != null) {
                isMM = selectedUser.getMmEventIds().contains(selectedEvent.getId());
            }

            btnActivate.setDisable(!(isNotStarted && isMM));

            btnClose.setDisable(!(isActive && isMM));
        } else {
            btnActivate.setDisable(true);
            btnClose.setDisable(true);
        }
    }
}