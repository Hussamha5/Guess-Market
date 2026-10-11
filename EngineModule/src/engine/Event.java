package engine;

import dto.EventDTO;
import dto.OrderDTO;
import dto.OutcomeDTO;
import dto.ParticipantDTO;
import dto.ParticipationDTO;
import dto.TransactionDTO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One event of the market (2 to 10 options), traded with LMSR (b != 0) or with an Order Book (b == 0).
 * The user who uploaded the event is its Market Maker. The event has its own account (the "contract").
 */
public class Event {

    public static final String FEE_ON_PURCHASE = "on-purchase";
    public static final String FEE_ON_CLOSE = "on-close";

    private static final int MAX_QUANTITY = 1_000_000;
    private static final double MIN_PRICE_STEP = 0.01;
    private static final double EPSILON = 1e-9;
    private static final double SMALL_AMOUNT = 0.005;
    private static final String ALL_OPTIONS = "All options";

    private final int number;
    private final String name;
    private final String description;
    private final int commission;
    private final String commissionType;
    private String status;
    private final Outcome[] outcomes;
    private final MarketMaker mm;           // the account of the event
    private final User marketMaker;         // the user that uploaded the event
    private int winningIndex = -1;

    private final int b;                    // LMSR: the liquidity
    private final List<Transaction> transactions = new ArrayList<>();

    private final int d;                    // Order Book: the base value
    private final int initial;
    private final boolean allowMint;

    // everybody who took part in the event, in the order in which they joined
    private final Map<String, User> participants = new LinkedHashMap<>();

    /** The resting orders that are used for a mint, with the quantity and the price of the new order. */
    private static class MintPlan {
        final List<Order> restingBids = new ArrayList<>();
        final List<Integer> restingOptions = new ArrayList<>();
        int quantity;
        double incomingPrice;
    }

    public Event(int number, String name, String description, int commission, String commissionType,
                 String[] outcomeN, int b, int d, int initial, boolean allowMint, User marketMaker) {
        this.number = number;
        this.name = name;
        this.description = description;
        this.commission = commission;
        this.commissionType = commissionType;
        this.status = EventDTO.STATUS_NOT_STARTED;
        this.outcomes = new Outcome[outcomeN.length];
        for (int i = 0; i < outcomeN.length; i++) {
            this.outcomes[i] = new Outcome(outcomeN[i]);
        }
        this.mm = new MarketMaker();
        this.marketMaker = marketMaker;
        this.b = b;
        this.d = d;
        this.initial = initial;
        this.allowMint = allowMint;
    }

    // ----- getters -----

    public int getNumber() { return number; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getCommission() { return commission; }
    public String getCommissionType() { return commissionType; }
    public String getStatus() { return status; }
    public Outcome[] getOutcomes() { return outcomes; }
    public MarketMaker getMarketMaker() { return mm; }
    public User getMarketMakerUser() { return marketMaker; }
    public int getB() { return this.b; }

    public boolean isLmsr() {
        return b != 0;
    }

    public boolean isClosed() {
        return EventDTO.STATUS_CLOSED.equals(status);
    }

    public boolean hasParticipant(User user) {
        return participants.containsKey(user.getName());
    }

    public List<String> getOptions() {
        List<String> opNames = new ArrayList<>();
        for (Outcome o : this.outcomes) {
            opNames.add(o.getName());
        }
        return opNames;
    }

    // ----- life cycle: open and close -----

    /** The Market Maker opens the event: he pays the subsidy (LMSR) or buys the first shares (Order Book). */
    public void activateEvent(User caller) {
        requireMarketMaker(caller, "open");
        if (!EventDTO.STATUS_NOT_STARTED.equals(status)) {
            throw new IllegalStateException("Event '" + name + "' cannot be opened because it is already "
                    + status.toLowerCase(Locale.ROOT) + ".");
        }
        if (isLmsr()) {
            openLmsr(caller);
        } else {
            openOrderBook(caller);
        }
        this.status = EventDTO.STATUS_ACTIVE;
    }

    // the most that the Market Maker can lose: b * ln(number of options)
    private void openLmsr(User mmUser) {
        double subsidy = Money.round(b * Math.log(outcomes.length));
        requireFunds(mmUser, subsidy, "pay the initial subsidy");
        payIntoContract(mmUser, subsidy, LedgerKind.OPENING, "Initial subsidy of LMSR event '" + name + "'");
    }

    // the Market Maker buys initial/d complete sets: one share of every option for every d that he pays
    private void openOrderBook(User mmUser) {
        int sets = initial / d;
        if (sets == 0) {
            return;
        }
        double cost = (double) sets * d;
        requireFunds(mmUser, cost, "buy the initial shares");
        join(mmUser);
        payIntoContract(mmUser, cost, LedgerKind.OPENING, "Initial purchase of " + sets
                + " share(s) of every option of '" + name + "'");
        for (int option = 0; option < outcomes.length; option++) {
            mmUser.addShares(name, option, sets);
            outcomes[option].addQuantity(sets);
        }
        recordTransaction(Transaction.Type.INITIAL, ALL_OPTIONS, sets, d, cost, 0.0, mmUser.getName(), "");
    }

    /** The Market Maker closes the event: the winners are paid, what is left in the account goes to him. */
    public void resolveEvent(User caller, int winningIndex) {
        requireMarketMaker(caller, "close");
        if (!EventDTO.STATUS_ACTIVE.equals(status)) {
            throw new IllegalStateException("Only an active event can be closed, but event '" + name + "' is "
                    + status.toLowerCase(Locale.ROOT) + ".");
        }
        requireOption(winningIndex);
        this.status = EventDTO.STATUS_CLOSED;
        this.winningIndex = winningIndex;
        for (Outcome outcome : outcomes) {
            outcome.getOrderBook().clear();     // the orders that are still waiting are cancelled
        }
        payWinners();
        returnRemainingFunds();
    }

    private double payoutPerShare() {
        return isLmsr() ? 1.0 : d;
    }

    private void payWinners() {
        for (User user : participants.values()) {
            int shares = user.getShares(name, winningIndex);
            if (shares > 0) {
                payWinner(user, shares);
            }
        }
    }

    private void payWinner(User winner, int shares) {
        double gross = Money.round(shares * payoutPerShare());
        mm.updateBalance(-gross);
        winner.updateBalance(gross, LedgerKind.PAYOUT, name, "Payout for " + shares + " winning share(s) of '"
                + outcomes[winningIndex].getName() + "' in '" + name + "'");
        if (FEE_ON_CLOSE.equals(commissionType)) {
            collectFee(winner, gross, "on the winnings");
        }
    }

    // what is left in the account of the event goes back to the Market Maker (a shortfall is paid by him)
    private void returnRemainingFunds() {
        double rest = mm.getBalance();
        mm.updateBalance(-rest);
        if (Math.abs(rest) < SMALL_AMOUNT) {
            return;
        }
        String text = rest > 0
                ? "Remaining funds of event '" + name + "' returned to you"
                : "Shortfall of event '" + name + "' covered by you";
        marketMaker.updateBalance(rest, LedgerKind.REFUND, name, text);
    }

    // ----- trading -----

    /** A buy or a sell in the event. For LMSR the price is ignored and only buying is possible. */
    public String trade(User user, int optionIndex, boolean isBuy, Double price, int quantity) {
        if (!isLmsr()) {
            return placeOrder(user, optionIndex, price, quantity, isBuy);
        }
        if (!isBuy) {
            throw new IllegalArgumentException("Shares cannot be sold in an LMSR event. Only buying is possible.");
        }
        double[] result = buyShares(user, optionIndex, quantity);
        return String.format(Locale.US, "Bought %d share(s) of '%s' for %s plus %s commission (%s in total).",
                quantity, outcomes[optionIndex].getName(), Money.format(result[0]), Money.format(result[1]),
                Money.format(result[2]));
    }

    /** LMSR: buying shares from the account of the event. Returns {price of the shares, commission, total}. */
    public double[] buyShares(User buyer, int optionIndex, int quantity) {
        requireTradable(buyer);
        requireOption(optionIndex);
        requireQuantity(quantity);
        double cost = Money.round(costOfBuying(optionIndex, quantity));
        String optionName = outcomes[optionIndex].getName();

        join(buyer);
        payIntoContract(buyer, cost, LedgerKind.SHARES_PAID, "Bought " + quantity + " share(s) of '"
                + optionName + "' in '" + name + "'");
        double fee = chargePurchaseFee(buyer, cost);
        buyer.addShares(name, optionIndex, quantity);
        outcomes[optionIndex].addQuantity(quantity);
        recordTransaction(Transaction.Type.BUY, optionName, quantity, cost / quantity, cost, fee,
                buyer.getName(), "");
        return new double[]{cost, fee, Money.round(cost + fee)};
    }

    /** Order Book: a buy or a sell order. Returns a message that describes what happened to the order. */
    public String placeOrder(User user, int optionIndex, Double price, int quantity, boolean isBuy) {
        if (isLmsr()) {
            throw new IllegalStateException("Orders can be placed only in an Order Book event.");
        }
        requireTradable(user);
        requireOption(optionIndex);
        requireQuantity(quantity);
        double unitPrice = validatePrice(price);
        if (!isBuy) {
            requireEnoughToSell(user, optionIndex, quantity);
        }
        join(user);
        int waiting = isBuy ? processBuy(user, optionIndex, unitPrice, quantity)
                : processSell(user, optionIndex, unitPrice, quantity);
        return describeResult(isBuy, optionIndex, unitPrice, quantity, waiting);
    }

    // returns the quantity that was not executed and waits in the book
    private int processBuy(User user, int optionIndex, double price, int quantity) {
        OrderBook ob = outcomes[optionIndex].getOrderBook();
        int remainingQty = matchAsks(user, optionIndex, price, quantity);
        if (allowMint) {
            remainingQty = mintShares(user, optionIndex, price, remainingQty);
        }
        if (remainingQty > 0) {
            ob.addBid(new Order(user.getName(), Order.OrderType.BUY, price, remainingQty));
        }
        return remainingQty;
    }

    private int processSell(User user, int optionIndex, double price, int quantity) {
        OrderBook ob = outcomes[optionIndex].getOrderBook();
        int remainingQty = matchBids(user, optionIndex, price, quantity);
        if (remainingQty > 0) {
            ob.addAsk(new Order(user.getName(), Order.OrderType.SELL, price, remainingQty));
        }
        return remainingQty;
    }

    private int matchAsks(User user, int optionIndex, double price, int quantity) {
        OrderBook ob = outcomes[optionIndex].getOrderBook();
        int remainingQty = quantity;
        while (remainingQty > 0 && ob.hasAsks() && ob.peekLowestAsk().getPrice() <= price) {
            Order matchingAsk = ob.peekLowestAsk();
            int tradeQty = Math.min(remainingQty, matchingAsk.getQuantity());

            executeTrade(user, participants.get(matchingAsk.getUsername()), optionIndex, matchingAsk.getPrice(), tradeQty);

            remainingQty -= tradeQty;
            matchingAsk.reduceQuantity(tradeQty);
            if (matchingAsk.getQuantity() == 0) ob.pollLowestAsk();
        }
        return remainingQty;
    }

    private int matchBids(User user, int optionIndex, double price, int quantity) {
        OrderBook ob = outcomes[optionIndex].getOrderBook();
        int remainingQty = quantity;
        while (remainingQty > 0 && ob.hasBids() && ob.peekHighestBid().getPrice() >= price) {
            Order matchingBid = ob.peekHighestBid();
            int tradeQty = Math.min(remainingQty, matchingBid.getQuantity());

            executeTrade(participants.get(matchingBid.getUsername()), user, optionIndex, matchingBid.getPrice(), tradeQty);

            remainingQty -= tradeQty;
            matchingBid.reduceQuantity(tradeQty);
            if (matchingBid.getQuantity() == 0) ob.pollHighestBid();
        }
        return remainingQty;
    }

    private void executeTrade(User buyer, User seller, int optionIndex, double price, int quantity) {
        String optionName = outcomes[optionIndex].getName();
        double totalCost = Money.round(price * quantity);
        String priceText = Money.format(price);

        buyer.updateBalance(-totalCost, LedgerKind.SHARES_PAID, name, "Bought " + quantity + " share(s) of '"
                + optionName + "' at " + priceText + " from " + seller.getName() + " in '" + name + "'");
        seller.updateBalance(totalCost, LedgerKind.SHARES_SOLD, name, "Sold " + quantity + " share(s) of '"
                + optionName + "' at " + priceText + " to " + buyer.getName() + " in '" + name + "'");
        buyer.addShares(name, optionIndex, quantity);
        seller.removeShares(name, optionIndex, quantity);
        double commissionAmount = chargePurchaseFee(buyer, totalCost);

        outcomes[optionIndex].setLastPrice(price);
        recordTransaction(Transaction.Type.TRADE, optionName, quantity, price, totalCost, commissionAmount,
                buyer.getName(), seller.getName());
    }

    // ----- mint: new shares are created when the bids of all the options together reach the base value -----

    private int mintShares(User user, int optionIndex, double price, int quantity) {
        int remainingQty = quantity;
        MintPlan plan = planMint(optionIndex, price, remainingQty);
        while (plan != null) {
            executeMint(user, optionIndex, plan);
            remainingQty -= plan.quantity;
            plan = remainingQty > 0 ? planMint(optionIndex, price, remainingQty) : null;
        }
        return remainingQty;
    }

    // the best bid of every other option; the new order pays what is missing up to the base value
    private MintPlan planMint(int optionIndex, double price, int quantity) {
        MintPlan plan = new MintPlan();
        double sumOfOthers = 0.0;
        plan.quantity = quantity;
        for (int other = 0; other < outcomes.length; other++) {
            if (other == optionIndex) {
                continue;
            }
            Order bid = outcomes[other].getOrderBook().peekHighestBid();
            if (bid == null) {
                return null;
            }
            plan.restingBids.add(bid);
            plan.restingOptions.add(other);
            sumOfOthers += bid.getPrice();
            plan.quantity = Math.min(plan.quantity, bid.getQuantity());
        }
        double complement = Money.round(d - sumOfOthers);
        if (complement < MIN_PRICE_STEP - EPSILON || complement > price + EPSILON) {
            return null;
        }
        plan.incomingPrice = complement;
        return plan;
    }

    private void executeMint(User newBuyer, int optionIndex, MintPlan plan) {
        List<String> buyers = new ArrayList<>();
        double fees = chargeMintPart(newBuyer, optionIndex, plan.incomingPrice, plan.quantity, buyers);
        for (int i = 0; i < plan.restingBids.size(); i++) {
            Order resting = plan.restingBids.get(i);
            int restingOption = plan.restingOptions.get(i);
            fees += chargeMintPart(participants.get(resting.getUsername()), restingOption, resting.getPrice(),
                    plan.quantity, buyers);
            resting.reduceQuantity(plan.quantity);
            if (resting.getQuantity() == 0) {
                outcomes[restingOption].getOrderBook().pollHighestBid();
            }
        }
        for (Outcome outcome : outcomes) {
            outcome.addQuantity(plan.quantity);
        }
        double total = Money.round((double) d * plan.quantity);
        recordTransaction(Transaction.Type.MINT, ALL_OPTIONS, plan.quantity, d, total, fees,
                String.join(" + ", buyers), "");
    }

    // one buyer pays for the new shares of his option; returns the commission that he paid
    private double chargeMintPart(User buyer, int optionIndex, double price, int quantity, List<String> buyers) {
        double cost = Money.round(price * quantity);
        payIntoContract(buyer, cost, LedgerKind.SHARES_PAID, "Bought " + quantity + " newly created share(s) of '"
                + outcomes[optionIndex].getName() + "' at " + Money.format(price) + " in '" + name + "'");
        buyer.addShares(name, optionIndex, quantity);
        buyers.add(buyer.getName());
        return chargePurchaseFee(buyer, cost);
    }

    // ----- commissions -----

    /** On-purchase commission: the buyer pays a percentage of the amount to the Market Maker. */
    private double chargePurchaseFee(User buyer, double amount) {
        if (!FEE_ON_PURCHASE.equals(commissionType)) {
            return 0.0;
        }
        return collectFee(buyer, amount, "on a purchase");
    }

    private double collectFee(User payer, double amount, String reason) {
        double fee = Money.round(amount * commission / 100.0);
        if (fee <= 0) {
            return 0.0;
        }
        payer.updateBalance(-fee, LedgerKind.FEE_PAID, name, "Commission " + commission + "% " + reason
                + " in '" + name + "'");
        marketMaker.updateBalance(fee, LedgerKind.FEE_RECEIVED, name, "Commission received from "
                + payer.getName() + " in '" + name + "'");
        mm.addFee(fee);
        return fee;
    }

    // ----- checks -----

    private void requireMarketMaker(User caller, String action) {
        if (caller != marketMaker) {
            throw new SecurityException("Only the Market Maker of event '" + name + "' ("
                    + marketMaker.getName() + ") can " + action + " it.");
        }
        requireNotBlocked(caller);
    }

    private void requireTradable(User user) {
        if (!EventDTO.STATUS_ACTIVE.equals(status)) {
            throw new IllegalStateException("Trading is not possible: event '" + name + "' is "
                    + status.toLowerCase(Locale.ROOT) + ". Trading is possible only while an event is active.");
        }
        requireNotBlocked(user);
    }

    private void requireNotBlocked(User user) {
        if (user.isBlocked()) {
            throw new SecurityException("Your account is blocked because its balance is negative ("
                    + Money.format(user.getBalance()) + "). Load funds to continue.");
        }
    }

    private void requireOption(int optionIndex) {
        if (optionIndex < 0 || optionIndex >= outcomes.length) {
            throw new IllegalArgumentException("Option number must be between 1 and " + outcomes.length
                    + " for event '" + name + "'.");
        }
    }

    private void requireQuantity(int quantity) {
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("Quantity must be a whole number between 1 and "
                    + MAX_QUANTITY + ".");
        }
    }

    private void requireFunds(User user, double amount, String purpose) {
        if (user.getBalance() < amount) {
            throw new IllegalArgumentException("Not enough funds to " + purpose + ": " + Money.format(amount)
                    + " are needed but your balance is " + Money.format(user.getBalance()) + ".");
        }
    }

    // the price must be between 0.01 and d - 0.01, with at most 2 digits after the decimal point
    private double validatePrice(Double price) {
        if (price == null) {
            throw new IllegalArgumentException("A price is required for an Order Book event.");
        }
        if (Double.isNaN(price) || Double.isInfinite(price) || price <= 0) {
            throw new IllegalArgumentException("Price must be a number greater than 0.");
        }
        if (!Money.hasAtMostTwoDecimals(price)) {
            throw new IllegalArgumentException("Price can have at most 2 digits after the decimal point.");
        }
        double highest = d - MIN_PRICE_STEP;
        if (price > highest + EPSILON) {
            throw new IllegalArgumentException("Price cannot be higher than " + Money.format(highest)
                    + " (the base value " + d + " minus 0.01).");
        }
        return Money.round(price);
    }

    // shares that are already offered for sale cannot be offered again
    private void requireEnoughToSell(User user, int optionIndex, int quantity) {
        int offered = outcomes[optionIndex].getOrderBook().sharesOfferedBy(user.getName());
        int owned = user.getShares(name, optionIndex);
        int available = owned - offered;
        if (available < quantity) {
            throw new IllegalArgumentException("You cannot sell " + quantity + " share(s) of '"
                    + outcomes[optionIndex].getName() + "': you have " + owned + " and " + offered
                    + " of them are already offered for sale (" + Math.max(available, 0) + " available).");
        }
    }

    // ----- helpers -----

    private void join(User user) {
        participants.putIfAbsent(user.getName(), user);
        user.joinEvent(name);
    }

    // moves money from a user into the account of the event
    private void payIntoContract(User payer, double amount, LedgerKind kind, String text) {
        payer.updateBalance(-amount, kind, name, text);
        mm.updateBalance(amount);
    }

    private void recordTransaction(Transaction.Type type, String outcomeName, int quantity, double unitPrice,
                                   double totalPaid, double fee, String buyer, String seller) {
        transactions.add(new Transaction(transactions.size() + 1, type, outcomeName, quantity, unitPrice,
                totalPaid, fee, buyer, seller));
    }

    private String describeResult(boolean isBuy, int optionIndex, double price, int quantity, int waiting) {
        String text = String.format(Locale.US, "%s order of %d share(s) of '%s' at %s: %d executed",
                isBuy ? "Buy" : "Sell", quantity, outcomes[optionIndex].getName(), Money.format(price),
                quantity - waiting);
        return waiting > 0 ? text + ", " + waiting + " waiting in the order book." : text + ".";
    }

    // ----- LMSR mathematics: C(q) = b * ln( sum of e^(q_i / b) ) -----

    private double[] quantities() {
        double[] q = new double[outcomes.length];
        for (int i = 0; i < q.length; i++) {
            q[i] = outcomes[i].getQuantity();
        }
        return q;
    }

    private static double largest(double[] values) {
        double largest = values[0];
        for (double value : values) {
            largest = Math.max(largest, value);
        }
        return largest;
    }

    // calculated in a way that does not overflow
    private double calculateCost(double[] q) {
        double maxQ = largest(q);
        double sum = 0.0;
        for (double value : q) {
            sum += Math.exp((value - maxQ) / b);
        }
        return maxQ + b * Math.log(sum);
    }

    private double costOfBuying(int optionIndex, int quantity) {
        double[] q = quantities();
        double oldCost = calculateCost(q);
        q[optionIndex] += quantity;
        return calculateCost(q) - oldCost;
    }

    /** The LMSR price of an option: a number between 0 and 1. */
    public double getOutcomePrice(int index) {
        double[] q = quantities();
        double maxQ = largest(q);
        double sum = 0.0;
        for (double value : q) {
            sum += Math.exp((value - maxQ) / b);
        }
        return Math.exp((q[index] - maxQ) / b) / sum;
    }

    // the price that is used for valuing the shares: LMSR price, last trade, or the middle price
    private double referencePrice(int index) {
        if (isLmsr()) {
            return getOutcomePrice(index);
        }
        Outcome outcome = outcomes[index];
        if (outcome.getLastPrice() > 0) {
            return outcome.getLastPrice();
        }
        OrderBook ob = outcome.getOrderBook();
        if (ob.hasBids() && ob.hasAsks()) {
            return (ob.peekHighestBid().getPrice() + ob.peekLowestAsk().getPrice()) / 2.0;
        }
        return 0.0;
    }

    // ----- data for the clients -----

    public EventDTO toDTO() {
        OutcomeDTO[] dtoOutcomes = new OutcomeDTO[outcomes.length];
        for (int i = 0; i < outcomes.length; i++) {
            dtoOutcomes[i] = outcomeDto(i);
        }
        List<TransactionDTO> dtoTransactions = new ArrayList<>();
        for (Transaction t : transactions) {
            dtoTransactions.add(t.toDTO());
        }
        String methodName = isLmsr() ? EventDTO.METHOD_LMSR : EventDTO.METHOD_ORDER_BOOK;
        String winner = winningIndex < 0 ? "" : outcomes[winningIndex].getName();
        return new EventDTO(number, name, description, commission, commissionType, status, methodName,
                marketMaker.getName(), getOptions(), dtoOutcomes, mm.getBalance(), mm.getTotalFees(), winner,
                b, d, initial, allowMint, dtoTransactions, participantDtos());
    }

    private OutcomeDTO outcomeDto(int i) {
        Outcome outcome = outcomes[i];
        OrderBook ob = outcome.getOrderBook();
        double highestBid = ob.hasBids() ? ob.peekHighestBid().getPrice() : 0.0;
        double lowestAsk = ob.hasAsks() ? ob.peekLowestAsk().getPrice() : 0.0;
        return new OutcomeDTO(outcome.getName(), outcome.getQuantity(), referencePrice(i), outcome.getLastPrice(),
                highestBid, lowestAsk, orderDtos(ob.sortedBids()), orderDtos(ob.sortedAsks()));
    }

    private static List<OrderDTO> orderDtos(List<Order> orders) {
        List<OrderDTO> dtos = new ArrayList<>();
        for (Order order : orders) {
            dtos.add(new OrderDTO(order.getUsername(), order.getQuantity(), order.getPrice()));
        }
        return dtos;
    }

    private List<ParticipantDTO> participantDtos() {
        List<ParticipantDTO> dtos = new ArrayList<>();
        for (User user : participants.values()) {
            List<Integer> shares = sharesOf(user);
            dtos.add(new ParticipantDTO(user.getName(), shares, holdingsValue(shares), openOrdersOf(user)));
        }
        return dtos;
    }

    private List<Integer> sharesOf(User user) {
        List<Integer> shares = new ArrayList<>();
        for (int i = 0; i < outcomes.length; i++) {
            shares.add(user.getShares(name, i));
        }
        return shares;
    }

    private int openOrdersOf(User user) {
        int count = 0;
        for (Outcome outcome : outcomes) {
            count += outcome.getOrderBook().ordersOf(user.getName());
        }
        return count;
    }

    private double holdingsValue(List<Integer> shares) {
        double value = 0.0;
        for (int i = 0; i < shares.size(); i++) {
            value += shares.get(i) * valuePerShare(i);
        }
        return Money.round(value);
    }

    private double valuePerShare(int option) {
        if (isClosed()) {
            return option == winningIndex ? payoutPerShare() : 0.0;
        }
        return referencePrice(option);
    }

    /** How the given user is involved in this event (as the Market Maker and/or as a trader). */
    public ParticipationDTO participationOf(User user) {
        Map<LedgerKind, Double> totals = user.totalsOfEvent(name);
        double invested = -(amount(totals, LedgerKind.SHARES_PAID) + amount(totals, LedgerKind.SHARES_SOLD)
                + amount(totals, LedgerKind.OPENING));
        double payout = amount(totals, LedgerKind.PAYOUT) + amount(totals, LedgerKind.REFUND);
        double result = 0.0;
        if (isClosed()) {
            for (double value : totals.values()) {
                result += value;
            }
        }
        String role = user == marketMaker ? ParticipationDTO.ROLE_MARKET_MAKER : ParticipationDTO.ROLE_PARTICIPANT;
        return new ParticipationDTO(name, role, status, getOptions(), sharesOf(user), Money.round(invested),
                Money.round(-amount(totals, LedgerKind.FEE_PAID)), Money.round(amount(totals, LedgerKind.FEE_RECEIVED)),
                Money.round(payout), Money.round(result));
    }

    private static double amount(Map<LedgerKind, Double> totals, LedgerKind kind) {
        return totals.getOrDefault(kind, 0.0);
    }
}