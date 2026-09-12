package engine;

import java.util.ArrayList;
import java.util.List;

public class Event implements java.io.Serializable {
    private String id;
    private String name;
    private String description;
    private int commission;
    private String commissionType;
    private String status;
    private double mmBalance;
    private double totalFees;
    private Outcome[] outcomes;
    private MarketMaker mm;
    private double[] totalInvestment;

    private int b;
    private List<Transaction> transactions;

    private int d;
    private int initial;
    private boolean allowMint;

    public Event(String id, String name, String description, int commission, String commissionType, String[] outcomeN, int b, int d, int initial, boolean allowMint) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commission = commission;
        this.commissionType = commissionType;

        this.status = "Not Started";
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

        // אתחול משתני Order Book
        this.d = d;
        this.initial = initial;
        this.allowMint = allowMint;
    }

    public void placeOrder(User user, int optionIndex, double price, int quantity, boolean isBuy, java.util.List<User> allUsers) {
        if (b != 0) return; // רלוונטי רק ל-Order Book

        if (!this.status.equals("Active")) {
            throw new IllegalStateException("Cannot trade. Event is not active.");
        }
        if (user.isBlocked()) {
            throw new IllegalStateException("User is blocked due to negative balance and cannot perform actions.");
        }
        if (price >= d) {
            throw new IllegalArgumentException("Price must be less than the base value (" + d + ")");
        }

        Outcome outcome = outcomes[optionIndex];
        OrderBook ob = outcome.getOrderBook();
        int remainingQty = quantity;

        if (isBuy) {
            while (remainingQty > 0 && ob.hasAsks() && ob.peekLowestAsk().getPrice() <= price) {
                Order matchingAsk = ob.peekLowestAsk();
                int tradeQty = Math.min(remainingQty, matchingAsk.getQuantity());

                executeTrade(user, getUserByName(matchingAsk.getUsername(), allUsers), optionIndex, matchingAsk.getPrice(), tradeQty);

                remainingQty -= tradeQty;
                matchingAsk.reduceQuantity(tradeQty);
                if (matchingAsk.getQuantity() == 0) ob.pollLowestAsk();
            }

            if (allowMint && remainingQty > 0) {
                int oppositeIndex = 1 - optionIndex;
                OrderBook oppositeOb = outcomes[oppositeIndex].getOrderBook();

                while (remainingQty > 0 && oppositeOb.hasBids() && (price + oppositeOb.peekHighestBid().getPrice() >= d)) {
                    Order oppositeBid = oppositeOb.peekHighestBid();
                    int mintQty = Math.min(remainingQty, oppositeBid.getQuantity());

                    // חישוב המחיר המשלים (לפי דרישות ה-Mint)
                    double complementaryPrice = this.d - oppositeBid.getPrice();

                    executeMint(user, getUserByName(oppositeBid.getUsername(), allUsers), optionIndex, oppositeIndex, complementaryPrice, oppositeBid.getPrice(), mintQty);

                    remainingQty -= mintQty;
                    oppositeBid.reduceQuantity(mintQty);
                    if (oppositeBid.getQuantity() == 0) oppositeOb.pollHighestBid();
                }
            }

            if (remainingQty > 0) {
                ob.addBid(new Order(user.getName(), Order.OrderType.BUY, price, remainingQty));
            }

        } else {
            if (user.getShares(this.id, optionIndex) < quantity) {
                throw new IllegalArgumentException("Not enough shares to sell!");
            }

            while (remainingQty > 0 && ob.hasBids() && ob.peekHighestBid().getPrice() >= price) {
                Order matchingBid = ob.peekHighestBid();
                int tradeQty = Math.min(remainingQty, matchingBid.getQuantity());

                executeTrade(getUserByName(matchingBid.getUsername(), allUsers), user, optionIndex, matchingBid.getPrice(), tradeQty);

                remainingQty -= tradeQty;
                matchingBid.reduceQuantity(tradeQty);
                if (matchingBid.getQuantity() == 0) ob.pollHighestBid();
            }

            if (remainingQty > 0) {
                ob.addAsk(new Order(user.getName(), Order.OrderType.SELL, price, remainingQty));
            }
        }
    }

    private void executeTrade(User buyer, User seller, int optionIndex, double price, int quantity) {
        double totalCost = price * quantity;
        double commissionAmount = 0.0;

        if (this.commissionType.equalsIgnoreCase("on-purchase")) {
            commissionAmount = totalCost * (this.commission / 100.0);
        }

        double totalPaid = totalCost + commissionAmount;

        buyer.updateBalance(-totalPaid);
        buyer.addShares(this.id, optionIndex, quantity);
        // תיעוד הוצאות ועמלות לקונה
        buyer.addInvestment(this.id, totalCost);
        buyer.addCommission(this.id, commissionAmount);

        seller.updateBalance(totalCost);
        seller.removeShares(this.id, optionIndex, quantity);
        // תיעוד הכנסות למוכר (מקטין את ההשקעה נטו)
        seller.addInvestment(this.id, -totalCost);

        if (commissionAmount > 0) {
            this.mm.addFee(commissionAmount);
        }

        this.transactions.add(new Transaction(outcomes[optionIndex].getName(), quantity, price));
    }

    private void executeMint(User buyer1, User buyer2, int option1Index, int option2Index, double price1, double price2, int quantity) {
        double cost1 = price1 * quantity;
        double cost2 = price2 * quantity;

        double comm1 = 0.0, comm2 = 0.0;
        if (this.commissionType.equalsIgnoreCase("on-purchase")) {
            comm1 = cost1 * (this.commission / 100.0);
            comm2 = cost2 * (this.commission / 100.0);
        }

        buyer1.updateBalance(-(cost1 + comm1));
        buyer1.addShares(this.id, option1Index, quantity);
        buyer1.addInvestment(this.id, cost1);
        buyer1.addCommission(this.id, comm1);

        buyer2.updateBalance(-(cost2 + comm2));
        buyer2.addShares(this.id, option2Index, quantity);
        buyer2.addInvestment(this.id, cost2);
        buyer2.addCommission(this.id, comm2);

        this.mm.updateBalance(cost1 + cost2);
        this.mm.addFee(comm1 + comm2);

        this.transactions.add(new Transaction("MINT (Both)", quantity, cost1 + cost2));
    }

    // פונקציית עזר למציאת משתמש לפי שם מתוך הרשימה
    private User getUserByName(String username, java.util.List<User> allUsers) {
        for (User u : allUsers) {
            if (u.getName().equals(username)) return u;
        }
        return null;
    }
    // אתחול האירוע - הפקעת הכסף מעושה השוק (MM) והענקת מניות ראשוניות
    public void activateEvent(User mmUser) {
        if (!this.status.equals("Not Started")) {
            throw new IllegalStateException("Event already started or closed.");
        }

        if (b != 0) {
            // LMSR Initialization
            double subsidy = b * Math.log(2.0);
            if (mmUser.getBalance() < subsidy) {
                throw new IllegalStateException("Market Maker does not have enough balance for subsidy.");
            }
            mmUser.updateBalance(-subsidy);
            this.mm.updateBalance(subsidy);
        } else {
            // Order Book Initialization
            if (this.initial > 0 && this.d > 0) {
                if (mmUser.getBalance() < this.initial) {
                    throw new IllegalStateException("Market Maker does not have enough balance to open this event.");
                }
                mmUser.updateBalance(-this.initial);
                this.mm.updateBalance(this.initial);

                int sharesToGive = this.initial / this.d;
                mmUser.addShares(this.id, 0, sharesToGive);
                mmUser.addShares(this.id, 1, sharesToGive);

                this.transactions.add(new Transaction("Initial Allocation", sharesToGive, this.initial));
            }
        }
        this.status = "Active"; // עדכון הסטטוס לפעיל
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



    public double getMmBalance() {
        return mmBalance;
    }

    public double getTotalFees() {
        return totalFees;
    }
    public String getStatus() { return status; }

    public Outcome[] getOutcomes()
    {
        return outcomes;
    }
    public MarketMaker getMarketMaker()
    {
        return mm;
    }
    public List<Transaction> getTransactions() { return transactions; }
    public int getB() {
        return this.b; // או איך שלא קראת למשתנה b אצלך במחלקה
    }


    public List<String> getOptions() {
        List<String> opNames = new ArrayList<>();
        if (this.outcomes != null) {
            for (Outcome o : this.outcomes) {
                // אנחנו שולפים את השם של כל תוצאה ומוסיפים לרשימה
                opNames.add(o.getName());
            }
        }
        return opNames;
    }

    public String getCommissionType()
    {
        return commissionType;
    }

    // === סגירת אירוע וחלוקת כספים ===
    public void resolveEvent(int winningIndex, java.util.List<User> allUsers) {
        if (!this.status.equals("Active")) {
            throw new IllegalStateException("Only active events can be closed.");
        }
        this.status = "Closed";

        double totalPayoutFromMM = 0.0;
        double payoutPerShare = (this.b != 0) ? 1.0 : this.d;

        if (allUsers != null) {
            for (User u : allUsers) {
                int userShares = u.getShares(this.id, winningIndex);
                if (userShares > 0) {
                    double userPayout = userShares * payoutPerShare;
                    double fee = 0.0;

                    if (this.commissionType.equalsIgnoreCase("on-close")) {
                        fee = userPayout * (this.commission / 100.0);
                        this.mm.addFee(fee);
                        u.addCommission(this.id, fee); // תיעוד עמלת סגירה
                    }

                    double finalUserPayout = userPayout - fee;
                    u.updateBalance(finalUserPayout);
                    u.addPayout(this.id, finalUserPayout); // תיעוד הזכייה לחישוב רווח
                    totalPayoutFromMM += finalUserPayout;
                }
            }
        }
        this.mm.updateBalance(-totalPayoutFromMM);
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
            double highestBid = 0.0;
            double lowestAsk = 0.0;
            double lastPrice = 0.0;

            if (b == 0) { // סימן ב-Order Book
                OrderBook ob = outcomes[i].getOrderBook();
                if (ob.hasBids()) highestBid = ob.peekHighestBid().getPrice();
                if (ob.hasAsks()) lowestAsk = ob.peekLowestAsk().getPrice();

                for (int j = transactions.size() - 1; j >= 0; j--) {
                    Transaction t = transactions.get(j);
                    if (t.getOutName().equals(outcomes[i].getName()) && !t.getOutName().startsWith("MINT")) {
                        lastPrice = t.getTotal();
                        break;
                    }
                }
            }

            dtoOutcomes[i] = new dto.OutcomeDTO(
                    outcomes[i].getName(),
                    outcomes[i].getQuantity(),
                    getOutcomePrice(i),
                    lastPrice, highestBid, lowestAsk
            );
        }

        java.util.List<dto.TransactionDTO> dtoTransactions = new java.util.ArrayList<>();
        for (Transaction t : transactions) {
            dtoTransactions.add(new dto.TransactionDTO(t.getOutName(), t.getAmount(), t.getTotal()));
        }

        String methodName = (b == 0) ? "Order Book" : "LMSR";

        return new dto.EventDTO(
                id, name, description, commission, commissionType, this.status,
                mm.getBalance(), mm.getTotalFees(),
                dtoOutcomes, dtoTransactions,
                methodName, getOptions()
        );
    }

}