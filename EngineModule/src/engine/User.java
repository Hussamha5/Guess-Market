package engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User {
    private final String name;
    private double balance;

    // every change of the balance is one row here
    private final List<LedgerEntry> ledger = new ArrayList<>();

    // event name -> (option index -> number of shares)
    private final Map<String, Map<Integer, Integer>> portfolio = new HashMap<>();

    public User(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public double getBalance() { return balance; }

    // a user with a negative balance is blocked, until the balance is not negative any more
    public boolean isBlocked() { return balance < 0; }

    /** Changes the balance and records the change as a new row in the account of the user. */
    public void updateBalance(double amount, LedgerKind kind, String eventName, String description) {
        double rounded = Money.round(amount);
        this.balance = Money.round(this.balance + rounded);
        ledger.add(new LedgerEntry(ledger.size() + 1, System.currentTimeMillis(), description, rounded,
                this.balance, kind, eventName));
    }

    /** The rows whose serial number is bigger than lastSerialSeen (the serial numbers start at 1). */
    public List<LedgerEntry> ledgerAfter(int lastSerialSeen) {
        int from = Math.max(0, lastSerialSeen);
        if (from >= ledger.size()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(ledger.subList(from, ledger.size()));
    }

    /** For one event: the total amount of every kind of row. */
    public Map<LedgerKind, Double> totalsOfEvent(String eventName) {
        Map<LedgerKind, Double> totals = new EnumMap<>(LedgerKind.class);
        for (LedgerEntry entry : ledger) {
            if (eventName.equals(entry.getEventName())) {
                totals.merge(entry.getKind(), entry.getAmount(), Double::sum);
            }
        }
        return totals;
    }

    /** The user takes part in the event from his first action (even if it was only an order that waits). */
    public void joinEvent(String eventId) {
        portfolio.putIfAbsent(eventId, new HashMap<>());
    }

    public boolean isInEvent(String eventId) {
        return portfolio.containsKey(eventId);
    }

    public void addShares(String eventId, int optionIndex, int quantity) {
        portfolio.putIfAbsent(eventId, new HashMap<>());
        Map<Integer, Integer> eventShares = portfolio.get(eventId);
        eventShares.put(optionIndex, eventShares.getOrDefault(optionIndex, 0) + quantity);
    }

    public void removeShares(String eventId, int optionIndex, int quantity) {
        if (portfolio.containsKey(eventId)) {
            Map<Integer, Integer> eventShares = portfolio.get(eventId);
            int currentShares = eventShares.getOrDefault(optionIndex, 0);
            if (currentShares >= quantity) {
                eventShares.put(optionIndex, currentShares - quantity);
            }
        }
    }

    public int getShares(String eventId, int optionIndex) {
        if (!portfolio.containsKey(eventId)) return 0;
        return portfolio.get(eventId).getOrDefault(optionIndex, 0);
    }

    public Map<String, Map<Integer, Integer>> getPortfolio() { return portfolio; }
}