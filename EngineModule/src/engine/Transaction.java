package engine;

import dto.TransactionDTO;

/** One row of the trading history of an event. */
public class Transaction {
    public enum Type { BUY, TRADE, MINT, INITIAL }

    private final int number;
    private final Type type;
    private final String outName;
    private final int amount;
    private final double unitPrice;
    private final double total;
    private final double commission;
    private final String buyer;
    private final String seller;

    public Transaction(int number, Type type, String outName, int amount, double unitPrice, double total,
                       double commission, String buyer, String seller) {
        this.number = number;
        this.type = type;
        this.outName = outName;
        this.amount = amount;
        this.unitPrice = unitPrice;
        this.total = total;
        this.commission = commission;
        this.buyer = buyer;
        this.seller = seller;
    }

    public TransactionDTO toDTO() {
        return new TransactionDTO(number, type.name(), outName, amount, unitPrice, total, commission, buyer, seller);
    }
}