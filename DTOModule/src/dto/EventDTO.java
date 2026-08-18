package dto;

import java.util.List;

public class EventDTO {
    private final String id;
    private final String name;
    private final String description;
    private final int commission;
    private final String commissionType;
    private final boolean isActive;

    private final double mmBalance;
    private final double totalFees;

    private final OutcomeDTO[] outcomes;
    private final List<TransactionDTO> transactions;

    public EventDTO(String id, String name, String description, int commission, String commissionType,
                    boolean isActive, double mmBalance, double totalFees,
                    OutcomeDTO[] outcomes, List<TransactionDTO> transactions) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commission = commission;
        this.commissionType = commissionType;
        this.isActive = isActive;
        this.mmBalance = mmBalance;
        this.totalFees = totalFees;
        this.outcomes = outcomes;
        this.transactions = transactions;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getCommission() { return commission; }
    public String getCommissionType() { return commissionType; }
    public boolean isActive() { return isActive; }
    public double getMmBalance() { return mmBalance; }
    public double getTotalFees() { return totalFees; }
    public OutcomeDTO[] getOutcomes() { return outcomes; }
    public List<TransactionDTO> getTransactions() { return transactions; }
}