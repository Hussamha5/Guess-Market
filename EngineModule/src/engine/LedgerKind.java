package engine;

/** The reason for a change in the balance of a user. */
public enum LedgerKind {
    LOAD,
    SHARES_PAID,
    SHARES_SOLD,
    OPENING,
    FEE_PAID,
    FEE_RECEIVED,
    PAYOUT,
    REFUND
}