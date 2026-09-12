package dto;

public class OutcomeDTO {
    private final String name;
    private final int quantity;
    private final double price;

    // נתונים סטטיסטיים עבור ספר פקודות (Order Book)
    private final double lastPrice;
    private final double highestBid;
    private final double lowestAsk;

    public OutcomeDTO(String name, int quantity, double price, double lastPrice, double highestBid, double lowestAsk) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.lastPrice = lastPrice;
        this.highestBid = highestBid;
        this.lowestAsk = lowestAsk;
    }

    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }

    public double getLastPrice() { return lastPrice; }
    public double getHighestBid() { return highestBid; }
    public double getLowestAsk() { return lowestAsk; }

    // חישוב אמצע (Mid) והפרש (Spread) ישירות ב-DTO
    public double getMidPrice() {
        if (highestBid > 0 && lowestAsk > 0) return (highestBid + lowestAsk) / 2.0;
        return 0.0;
    }

    public double getSpread() {
        if (highestBid > 0 && lowestAsk > 0) return lowestAsk - highestBid;
        return 0.0;
    }
}