package dto;

import java.util.List;


public class AccountDTO {
    private final String name;
    private final double balance;
    private final boolean blocked;
    private final List<ParticipationDTO> participations;
    private final List<LedgerEntryDTO> ledger;

    public AccountDTO(String name, double balance, boolean blocked, List<ParticipationDTO> participations,
                      List<LedgerEntryDTO> ledger) {
        this.name = name;
        this.balance = balance;
        this.blocked = blocked;
        this.participations = participations;
        this.ledger = ledger;
    }

    public String getName() { return name; }
    public double getBalance() { return balance; }
    public boolean isBlocked() { return blocked; }
    public List<ParticipationDTO> getParticipations() { return participations; }
    public List<LedgerEntryDTO> getLedger() { return ledger; }
}