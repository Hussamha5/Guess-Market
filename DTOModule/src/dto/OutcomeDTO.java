package dto;

import java.util.List;

/**
 * The state of one option of an event.
 * LMSR: price is the current LMSR price (0..1) and the order lists are empty.
 * Order Book: price is the reference price (last trade, or the middle price when nothing was traded yet).
 */
public class OutcomeDTO {
    private final String name;
    private final int quantity;
    private final double price;

    // statistics of the order book
    private final double lastPrice;
    private final double highestBid;
    private final double lowestAsk;
    private final List<OrderDTO> bids;
    private final List<OrderDTO> asks;

    public OutcomeDTO(String name, int quantity, double price, double lastPrice, double highestBid,
                      double lowestAsk, List<OrderDTO> bids, List<OrderDTO> asks) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.lastPrice = lastPrice;
        this.highestBid = highestBid;
        this.lowestAsk = lowestAsk;
        this.bids = bids;
        this.asks = asks;
    }

    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public double getLastPrice() { return lastPrice; }
    public double getHighestBid() { return highestBid; }
    public double getLowestAsk() { return lowestAsk; }
    public List<OrderDTO> getBids() { return bids; }
    public List<OrderDTO> getAsks() { return asks; }

    // the middle price (Mid) and the Spread are calculated from the two values above
    public double getMidPrice() {
        if (highestBid > 0 && lowestAsk > 0) return (highestBid + lowestAsk) / 2.0;
        return 0.0;
    }

    public double getSpread() {
        if (highestBid > 0 && lowestAsk > 0) return lowestAsk - highestBid;
        return 0.0;
    }
}