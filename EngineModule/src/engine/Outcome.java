package engine;

import java.io.Serializable;

public class Outcome implements Serializable {
    private String name;
    private int quantity;

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
}