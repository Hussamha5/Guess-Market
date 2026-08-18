package engine;

public class MarketMaker implements java.io.Serializable
{
    private double balance;
    private double totalFees;

    public MarketMaker()
    {
        this.balance = 0.0;
        this .totalFees = 0.0;
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
        this.totalFees += fee;
        this.balance += fee;
    }

    public void updateBalance(double sum)
    {
        this.balance+=sum;
    }
}
