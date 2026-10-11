package dto;

import java.util.List;

public class ParticipationDTO {
    public static final String ROLE_MARKET_MAKER = "Market Maker";
    public static final String ROLE_PARTICIPANT = "Participant";

    private final String eventName;
    private final String role;
    private final String status;
    private final List<String> optionNames;
    private final List<Integer> shares;
    private final double invested;
    private final double commissionPaid;
    private final double commissionReceived;
    private final double payout;
    private final double profitLoss;    // only for a closed event

    public ParticipationDTO(String eventName, String role, String status, List<String> optionNames,
                            List<Integer> shares, double invested, double commissionPaid,
                            double commissionReceived, double payout, double profitLoss) {
        this.eventName = eventName;
        this.role = role;
        this.status = status;
        this.optionNames = optionNames;
        this.shares = shares;
        this.invested = invested;
        this.commissionPaid = commissionPaid;
        this.commissionReceived = commissionReceived;
        this.payout = payout;
        this.profitLoss = profitLoss;
    }

    public String getEventName() { return eventName; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public List<String> getOptionNames() { return optionNames; }
    public List<Integer> getShares() { return shares; }
    public double getInvested() { return invested; }
    public double getCommissionPaid() { return commissionPaid; }
    public double getCommissionReceived() { return commissionReceived; }
    public double getPayout() { return payout; }
    public double getProfitLoss() { return profitLoss; }
}