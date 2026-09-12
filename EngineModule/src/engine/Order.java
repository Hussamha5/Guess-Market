package engine;

import java.io.Serializable;

public class Order implements Serializable {
    public enum OrderType { BUY, SELL }

    private final String username;
    private final OrderType type;
    private final double price;
    private int quantity;
    private final long timestamp; // עוזר לנו לשמור על סדר הגעת הפקודות (FIFO)

    public Order(String username, OrderType type, double price, int quantity) {
        this.username = username;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = System.currentTimeMillis();
    }

    public String getUsername() { return username; }
    public OrderType getType() { return type; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public long getTimestamp() { return timestamp; }

    // כשיש התאמה חלקית (Matching), נצטרך להפחית את הכמות
    public void reduceQuantity(int amount) {
        if (amount <= this.quantity) {
            this.quantity -= amount;
        }
    }
}