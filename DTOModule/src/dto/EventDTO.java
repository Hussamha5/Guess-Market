package dto;

import java.util.List;

/** Everything a client needs in order to display one event. Pure data: no logic, no UI types. */
public class EventDTO {
    public static final String STATUS_NOT_STARTED = "Not Started";
    public static final String STATUS_ACTIVE = "Active";
    public static final String STATUS_CLOSED = "Closed";
    public static final String METHOD_LMSR = "LMSR";
    public static final String METHOD_ORDER_BOOK = "Order Book";

    private final int number;                  // 1, 2, 3 ... in the order of upload
    private final String name;                 // unique in the whole market
    private final String description;
    private final int commission;
    private final String commissionType;
    private final String status;
    private final String method;
    private final String marketMakerName;      // the user that uploaded the event
    private final List<String> optionNames;
    private final OutcomeDTO[] outcomes;
    private final double mmBalance;            // the money in the account of the event
    private final double totalFees;
    private final String winningOption;        // empty until the event is closed
    private final int liquidityB;              // LMSR only
    private final int baseValue;               // Order Book only
    private final int initialInvestment;       // Order Book only
    private final boolean allowMint;           // Order Book only
    private final List<TransactionDTO> transactions;
    private final List<ParticipantDTO> participants;

    public EventDTO(int number, String name, String description, int commission, String commissionType,
                    String status, String method, String marketMakerName, List<String> optionNames,
                    OutcomeDTO[] outcomes, double mmBalance, double totalFees, String winningOption,
                    int liquidityB, int baseValue, int initialInvestment, boolean allowMint,
                    List<TransactionDTO> transactions, List<ParticipantDTO> participants) {
        this.number = number;
        this.name = name;
        this.description = description;
        this.commission = commission;
        this.commissionType = commissionType;
        this.status = status;
        this.method = method;
        this.marketMakerName = marketMakerName;
        this.optionNames = optionNames;
        this.outcomes = outcomes;
        this.mmBalance = mmBalance;
        this.totalFees = totalFees;
        this.winningOption = winningOption;
        this.liquidityB = liquidityB;
        this.baseValue = baseValue;
        this.initialInvestment = initialInvestment;
        this.allowMint = allowMint;
        this.transactions = transactions;
        this.participants = participants;
    }

    public int getNumber() { return number; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getCommission() { return commission; }
    public String getCommissionType() { return commissionType; }
    public String getStatus() { return status; }
    public String getMethod() { return method; }
    public String getMarketMakerName() { return marketMakerName; }
    public List<String> getOptionNames() { return optionNames; }
    public OutcomeDTO[] getOutcomes() { return outcomes; }
    public double getMmBalance() { return mmBalance; }
    public double getTotalFees() { return totalFees; }
    public String getWinningOption() { return winningOption; }
    public int getLiquidityB() { return liquidityB; }
    public int getBaseValue() { return baseValue; }
    public int getInitialInvestment() { return initialInvestment; }
    public boolean isAllowMint() { return allowMint; }
    public List<TransactionDTO> getTransactions() { return transactions; }
    public List<ParticipantDTO> getParticipants() { return participants; }

    public boolean isOrderBook() {
        return METHOD_ORDER_BOOK.equals(method);
    }

    public String getCommissionInfo() {
        return commission + "% (" + commissionType + ")";
    }

    @Override
    public String toString() {
        return name;
    }
}