package dto;

import java.util.List;
import java.util.Map;

public class UserDTO {
    private final String name;
    private final double balance;
    private final Map<String, Map<Integer, Integer>> portfolio;
    private final List<String> mmEventIds;
    private final Map<String, Double> commissionsPaid;
    private final Map<String, Double> netInvestments;
    private final Map<String, Double> finalPayouts;

    public UserDTO(String name, double balance, Map<String, Map<Integer, Integer>> portfolio, List<String> mmEventIds,
                   Map<String, Double> commissionsPaid, Map<String, Double> netInvestments, Map<String, Double> finalPayouts) {
        this.name = name;
        this.balance = balance;
        this.portfolio = portfolio;
        this.mmEventIds = mmEventIds;
        this.commissionsPaid = commissionsPaid;
        this.netInvestments = netInvestments;
        this.finalPayouts = finalPayouts;
    }

    public String getName() { return name; }
    public double getBalance() { return balance; }
    public Map<String, Map<Integer, Integer>> getPortfolio() { return portfolio; }
    public List<String> getMmEventIds() { return mmEventIds; }
    public Map<String, Double> getCommissionsPaid() { return commissionsPaid; }
    public Map<String, Double> getNetInvestments() { return netInvestments; }
    public Map<String, Double> getFinalPayouts() { return finalPayouts; }

    @Override
    public String toString() {
        return name;
    }
}