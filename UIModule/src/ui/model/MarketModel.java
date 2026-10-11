package ui.model;

import dto.AccountDTO;
import dto.EventDTO;
import dto.LedgerEntryDTO;
import dto.ParticipationDTO;
import dto.UserDTO;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public final class MarketModel {

    private final ObservableList<EventDTO> events = FXCollections.observableArrayList();
    private final ObservableList<UserDTO> users = FXCollections.observableArrayList();
    private final ObservableList<ParticipationDTO> participations = FXCollections.observableArrayList();
    /** The newest row is the first one. */
    private final ObservableList<LedgerEntryDTO> ledger = FXCollections.observableArrayList();

    private final StringProperty userName = new SimpleStringProperty("");
    private final DoubleProperty balance = new SimpleDoubleProperty();
    private final BooleanProperty blocked = new SimpleBooleanProperty();
    private final BooleanProperty connected = new SimpleBooleanProperty(true);

    /** These counters grow by one after every update, so that a screen can react once per update. */
    private final IntegerProperty eventsRevision = new SimpleIntegerProperty();
    private final IntegerProperty accountRevision = new SimpleIntegerProperty();

    public MarketModel(String userName) {
        this.userName.set(userName);
    }

    // ----- updates from the server -----

    public void applyEvents(List<EventDTO> fresh) {
        ListSync.sync(events, fresh, EventDTO::getName);
        eventsRevision.set(eventsRevision.get() + 1);
    }

    public void applyUsers(List<UserDTO> fresh) {
        ListSync.sync(users, fresh, UserDTO::getName);
    }

    public void applyAccount(AccountDTO account) {
        balance.set(account.getBalance());
        blocked.set(account.isBlocked());
        ListSync.sync(participations, account.getParticipations(), ParticipationDTO::getEventName);
        if (!account.getLedger().isEmpty()) {
            List<LedgerEntryDTO> newestFirst = new ArrayList<>(account.getLedger());
            Collections.reverse(newestFirst);
            ledger.addAll(0, newestFirst);
        }
        accountRevision.set(accountRevision.get() + 1);
    }

    // ----- reading -----

    public EventDTO findEvent(String name) {
        for (EventDTO event : events) {
            if (event.getName().equals(name)) {
                return event;
            }
        }
        return null;
    }

    /** True when the logged in user is the Market Maker of this event. */
    public boolean isMyEvent(EventDTO event) {
        return event != null && event.getMarketMakerName() != null
                && event.getMarketMakerName().equalsIgnoreCase(userName.get());
    }

    public ObservableList<EventDTO> events() {
        return events;
    }

    public ObservableList<UserDTO> users() {
        return users;
    }

    public ObservableList<ParticipationDTO> participations() {
        return participations;
    }

    public ObservableList<LedgerEntryDTO> ledger() {
        return ledger;
    }

    public String getUserName() {
        return userName.get();
    }

    public StringProperty userNameProperty() {
        return userName;
    }

    public double getBalance() {
        return balance.get();
    }

    public DoubleProperty balanceProperty() {
        return balance;
    }

    public boolean isBlocked() {
        return blocked.get();
    }

    public BooleanProperty blockedProperty() {
        return blocked;
    }

    public boolean isConnected() {
        return connected.get();
    }

    public BooleanProperty connectedProperty() {
        return connected;
    }

    public IntegerProperty eventsRevisionProperty() {
        return eventsRevision;
    }

    public IntegerProperty accountRevisionProperty() {
        return accountRevision;
    }
}