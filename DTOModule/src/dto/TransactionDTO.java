package dto;

public class TransactionDTO {
    private final String outcomeName;
    private final int quantity;
    private final double totalPaid;

    public TransactionDTO(String outcomeName, int quantity, double totalPaid) {
        this.outcomeName = outcomeName;
        this.quantity = quantity;
        this.totalPaid = totalPaid;
    }

    public String getOutcomeName() { return outcomeName; }
    public int getQuantity() { return quantity; }
    public double getTotalPaid() { return totalPaid; }
}