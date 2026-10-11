package engine;

/**
 * The account of one event (the "contract"): the money that was paid into the event and the total of
 * the commissions that were collected. The commissions themselves are paid to the user who is the
 * Market Maker of the event (see Event).
 */
public class MarketMaker {
    private double balance;
    private double totalFees;

    public MarketMaker()
    {
        this.balance = 0.0;
        this.totalFees = 0.0;
    }

    public double getBalance()
    {
        return this.balance;
    }

    public double getTotalFees()
    {
        return this.totalFees;
    }

    public void addFee(double fee)
    {
        this.totalFees = Money.round(this.totalFees + fee);
    }

    public void updateBalance(double sum)
    {
        this.balance = Money.round(this.balance + sum);
    }
}