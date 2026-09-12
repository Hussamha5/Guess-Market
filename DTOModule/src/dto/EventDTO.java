package dto;

import java.util.List;

public class EventDTO {
    private final String id;
    private final String name;
    private final String description;
    private final int commission;
    private final String commissionType;
    private final String status; // הוחלף מ-boolean ל-String

    private final double mmBalance;
    private final double totalFees;

    private final OutcomeDTO[] outcomes;
    private final List<TransactionDTO> transactions;

    private final String method;
    private final List<String> optionNames;

    public EventDTO(String id, String name, String description, int commission, String commissionType,
                    String status, double mmBalance, double totalFees,
                    OutcomeDTO[] outcomes, List<TransactionDTO> transactions,
                    String method, List<String> optionNames) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commission = commission;
        this.commissionType = commissionType;
        this.status = status;
        this.mmBalance = mmBalance;
        this.totalFees = totalFees;
        this.outcomes = outcomes;
        this.transactions = transactions;
        this.method = method;
        this.optionNames = optionNames;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getCommission() { return commission; }
    public String getCommissionType() { return commissionType; }
    public String getStatus() { return status; }
    public double getMmBalance() { return mmBalance; }
    public double getTotalFees() { return totalFees; }
    public OutcomeDTO[] getOutcomes() { return outcomes; }
    public List<TransactionDTO> getTransactions() { return transactions; }
    public String getMethod() { return method; }
    public List<String> getOptionNames() { return optionNames; }

    public String getCommissionInfo() {
        return commission + "% (" + commissionType + ")";
    }

    @Override
    public String toString() {
        return name;
    }
}