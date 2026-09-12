package engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User implements Serializable {
    private String name;
    private double balance;
    private List<String> mmEventIds;

    private Map<String, Map<Integer, Integer>> portfolio;
    private boolean isBlocked = false;
    private Map<String, Double> commissionsPaid = new HashMap<>();
    private Map<String, Double> netInvestments = new HashMap<>();
    private Map<String, Double> finalPayouts = new HashMap<>();

    public User(String name, double initialCash) {
        this.name = name;
        this.balance = initialCash;
        this.mmEventIds = new ArrayList<>();
        this.portfolio = new HashMap<>();

    }

    public String getName() { return name; }
    public double getBalance() { return balance; }
    public List<String> getMmEventIds() { return mmEventIds; }

    public void addMmEventId(String eventId) {
        this.mmEventIds.add(eventId);
    }


    public void updateBalance(double amount) {
        this.balance += amount;
        if (this.balance < 0) {
            this.isBlocked = true;
        }
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
    public boolean isBlocked() { return isBlocked; }
    public Map<String, Map<Integer, Integer>> getPortfolio() { return portfolio; }


    public void addCommission(String eventId, double amount) {
        commissionsPaid.put(eventId, commissionsPaid.getOrDefault(eventId, 0.0) + amount);
    }
    public void addInvestment(String eventId, double amount) {
        netInvestments.put(eventId, netInvestments.getOrDefault(eventId, 0.0) + amount);
    }
    public void addPayout(String eventId, double amount) {
        finalPayouts.put(eventId, finalPayouts.getOrDefault(eventId, 0.0) + amount);
    }

    public Map<String, Double> getCommissionsPaid() { return commissionsPaid; }
    public Map<String, Double> getNetInvestments() { return netInvestments; }
    public Map<String, Double> getFinalPayouts() { return finalPayouts; }
}