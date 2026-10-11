package engine;

import dto.LedgerEntryDTO;

/** One row in the account of a user. eventName is null for rows that do not belong to an event. */
public class LedgerEntry {
    private final int serial;
    private final long timeMillis;
    private final String description;
    private final double amount;
    private final double balanceAfter;
    private final LedgerKind kind;
    private final String eventName;

    public LedgerEntry(int serial, long timeMillis, String description, double amount, double balanceAfter,
                       LedgerKind kind, String eventName) {
        this.serial = serial;
        this.timeMillis = timeMillis;
        this.description = description;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.kind = kind;
        this.eventName = eventName;
    }

    public double getAmount() { return amount; }
    public LedgerKind getKind() { return kind; }
    public String getEventName() { return eventName; }

    public LedgerEntryDTO toDTO() {
        return new LedgerEntryDTO(serial, timeMillis, description, amount, balanceAfter);
    }
}