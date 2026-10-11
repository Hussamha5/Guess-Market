package engine;

import java.util.concurrent.atomic.AtomicLong;

public class Order {
    public enum OrderType { BUY, SELL }

    private static final AtomicLong SEQUENCE = new AtomicLong();

    private final String username;
    private final OrderType type;
    private final double price;
    private int quantity;
    private final long sequence; // keeps the order of arrival (FIFO) - two orders can arrive in the same millisecond

    public Order(String username, OrderType type, double price, int quantity) {
        this.username = username;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.sequence = SEQUENCE.incrementAndGet();
    }

    public String getUsername() { return username; }
    public OrderType getType() { return type; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public long getSequence() { return sequence; }

    // after a partial match the quantity of the order is reduced
    public void reduceQuantity(int amount) {
        if (amount <= this.quantity) {
            this.quantity -= amount;
        }
    }
}