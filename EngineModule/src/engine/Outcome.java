package engine;

public class Outcome {
    private String name;
    private int quantity;       // the number of shares of this option that exist
    private double lastPrice;   // the price of the last trade (order book events)

    private OrderBook orderBook;

    public Outcome(String name) {
        this.name = name;
        this.quantity = 0;
        this.orderBook = new OrderBook();
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public OrderBook getOrderBook() {
        return orderBook;
    }

    public void addQuantity(int amount) {
        this.quantity += amount;
    }

    public double getLastPrice() {
        return lastPrice;
    }

    public void setLastPrice(double lastPrice) {
        this.lastPrice = lastPrice;
    }
}