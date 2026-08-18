package engine;

public class Outcome implements java.io.Serializable {
    private String name;
    private int quantity;

    public Outcome(String name) {
        this.name = name;
        this.quantity = 0;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }


    public void addQuantity(int amount) {
        this.quantity += amount;
    }
}