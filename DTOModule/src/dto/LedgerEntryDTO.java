package dto;

public class LedgerEntryDTO {
    private final int serial;
    private final long timeMillis;
    private final String description;
    private final double amount;
    private final double balanceAfter;

    public LedgerEntryDTO(int serial, long timeMillis, String description, double amount, double balanceAfter) {
        this.serial = serial;
        this.timeMillis = timeMillis;
        this.description = description;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }

    public int getSerial() { return serial; }
    public long getTimeMillis() { return timeMillis; }
    public String getDescription() { return description; }
    public double getAmount() { return amount; }
    public double getBalanceAfter() { return balanceAfter; }
}