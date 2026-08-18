package engine;

import java.util.ArrayList;
import java.util.List;

public class Event implements java.io.Serializable {
    //
    private String id;
    private String name;
    private String description;
    private int commission;
    private String commissionType;
    private boolean isActive;


    private double mmBalance;
    private double totalFees;
    private Outcome[] outcomes;
    private MarketMaker mm;
    private double[] totalInvestment;

    private int b;
    private List<Transaction> transactions;


    public Event(String id, String name, String description, int commission,String commissionType ,String[] outcomeN,int b) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commission = commission;
        this.commissionType = commissionType;


        this.isActive = true;
        this.mmBalance = 0.0;
        this.totalFees = 0.0;
        this.outcomes = new Outcome[2];
        this.outcomes[0] = new Outcome(outcomeN[0]);
        this.outcomes[1] = new Outcome(outcomeN[1]);
        this.mm = new MarketMaker();
        this.b = b;
        this.transactions = new ArrayList<>();
        this.totalInvestment = new double[2];
        this.totalInvestment[0] = 0.0;
        this.totalInvestment[1] = 0.0;
    }


    // הגטרים
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getCommission() {
        return commission;
    }

    public boolean isActive() {
        return isActive;
    }

    public double getMmBalance() {
        return mmBalance;
    }

    public double getTotalFees() {
        return totalFees;
    }

    public Outcome[] getOutcomes()
    {
        return outcomes;
    }
    public MarketMaker getMarketMaker()
    {
        return mm;
    }
    public List<Transaction> getTransactions() { return transactions; }

    public String getCommissionType()
    {
        return commissionType;
    }

    // === סגירת אירוע וחלוקת כספים ===
    public void resolveEvent(int winningIndex) {
        this.isActive = false;

        double qWinning = outcomes[winningIndex].getQuantity();
        double payout = qWinning * 1.0;

        // אם שיטת הגביה היא בסוף האירוע, נגבה את העמלה מהמרוויחים כעת
        if (this.commissionType.equalsIgnoreCase("on-close")) {
            // חישוב העמלה מתוך סך ההשקעה באפשרות הזוכה בלבד
            double fee = this.totalInvestment[winningIndex] * (this.commission / 100.0);
            mm.addFee(fee);
            payout -= fee;
        }

        mm.updateBalance(-payout);
    }

    public void addTran(Transaction tr)
    {
        this.transactions.add(tr);
    }
    public double getOutcomePrice(int index) {
        double q0 = outcomes[0].getQuantity();
        double q1 = outcomes[1].getQuantity();
        double maxQ = Math.max(q0, q1);
        double exp0 = Math.exp((q0 - maxQ) / b);
        double exp1 = Math.exp((q1 - maxQ) / b);

        if (index == 0) {
            return exp0 / (exp0 + exp1);
        } else {
            return exp1 / (exp0 + exp1);
        }
    }
    private double calculateCost(double q0, double q1) {
        double maxQ = Math.max(q0, q1);
        double exp0 = Math.exp((q0 - maxQ) / b);
        double exp1 = Math.exp((q1 - maxQ) / b);
        return maxQ + b * Math.log(exp0 + exp1);
    }

    public double[] buyShares(int optionIndex, int quantity) {
        double q0 = outcomes[0].getQuantity();
        double q1 = outcomes[1].getQuantity();

        double oldCost = calculateCost(q0, q1);

        double newCost;
        if (optionIndex == 0) {
            newCost = calculateCost(q0 + quantity, q1);
        } else {
            newCost = calculateCost(q0, q1 + quantity);
        }

        double sharesPrice = newCost - oldCost;
        this.totalInvestment[optionIndex] += sharesPrice;
        double commissionAmount = 0.0;

        if (this.commissionType.equalsIgnoreCase("on-purchase")) {
            commissionAmount = sharesPrice * (this.commission / 100.0);
        }

        double totalPaid = sharesPrice + commissionAmount;

        outcomes[optionIndex].addQuantity(quantity); // הוספת המניות לאפשרות
        mm.updateBalance(sharesPrice);
        if (commissionAmount > 0) {
            mm.addFee(commissionAmount);    // שמירת העמלה בקופה
        }

        transactions.add(new Transaction(outcomes[optionIndex].getName(), quantity, totalPaid));

        return new double[]{sharesPrice, commissionAmount, totalPaid};
    }
    public dto.EventDTO toDTO() {
        dto.OutcomeDTO[] dtoOutcomes = new dto.OutcomeDTO[2];
        for (int i = 0; i < 2; i++) {
            dtoOutcomes[i] = new dto.OutcomeDTO(
                    outcomes[i].getName(),
                    outcomes[i].getQuantity(),
                    getOutcomePrice(i) // כאן אנחנו מחשבים את המחיר ושומרים אותו ב-DTO!
            );
        }

        java.util.List<dto.TransactionDTO> dtoTransactions = new java.util.ArrayList<>();
        for (Transaction t : transactions) {
            dtoTransactions.add(new dto.TransactionDTO(t.getOutName(), t.getAmount(), t.getTotal()));
        }

        return new dto.EventDTO(
                id, name, description, commission, commissionType, isActive,
                mm.getBalance(), mm.getTotalFees(),
                dtoOutcomes, dtoTransactions
        );
    }

}