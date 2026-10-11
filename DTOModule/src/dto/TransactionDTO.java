package dto;

/**
 * One row of the trading history of an event.
 * type is one of: BUY (an LMSR purchase), TRADE (an order book trade), MINT, INITIAL.
 * seller is empty for LMSR purchases (the other side is the event account).
 */
public class TransactionDTO {
    private final int number;
    private final String type;
    private final String outcomeName;
    private final int quantity;
    private final double unitPrice;
    private final double totalPaid;
    private final double commission;
    private final String buyer;
    private final String seller;

    public TransactionDTO(int number, String type, String outcomeName, int quantity, double unitPrice,
                          double totalPaid, double commission, String buyer, String seller) {
        this.number = number;
        this.type = type;
        this.outcomeName = outcomeName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPaid = totalPaid;
        this.commission = commission;
        this.buyer = buyer;
        this.seller = seller;
    }

    public int getNumber() { return number; }
    public String getType() { return type; }
    public String getOutcomeName() { return outcomeName; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public double getTotalPaid() { return totalPaid; }
    public double getCommission() { return commission; }
    public String getBuyer() { return buyer; }
    public String getSeller() { return seller; }
}