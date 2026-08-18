package engine;

public class Transaction implements java.io.Serializable {
    private String outName;
    private int amount;
    private double total;

    public Transaction(String outCome,int amount,double total)
    {
        this.outName =  outCome;
        this.amount = amount;
        this.total = total;
    }

    public String getOutName()
    {
        return outName;
    }

    public double getTotal() {
        return total;
    }

    public int getAmount() {
        return amount;
    }
}
