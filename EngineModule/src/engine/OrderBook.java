package engine;

import java.io.Serializable;
import java.util.Comparator;
import java.util.PriorityQueue;

public class OrderBook implements Serializable {

    private static class BidComparator implements Comparator<Order>, Serializable {
        @Override
        public int compare(Order o1, Order o2) {
            if (o1.getPrice() == o2.getPrice()) {
                return Long.compare(o1.getTimestamp(), o2.getTimestamp());
            }
            return Double.compare(o2.getPrice(), o1.getPrice());
        }
    }

    private static class AskComparator implements Comparator<Order>, Serializable {
        @Override
        public int compare(Order o1, Order o2) {
            if (o1.getPrice() == o2.getPrice()) {
                return Long.compare(o1.getTimestamp(), o2.getTimestamp());
            }
            return Double.compare(o1.getPrice(), o2.getPrice());
        }
    }

    private PriorityQueue<Order> bids; // תור קונים
    private PriorityQueue<Order> asks; // תור מוכרים

    public OrderBook() {
        this.bids = new PriorityQueue<>(new BidComparator());
        this.asks = new PriorityQueue<>(new AskComparator());
    }

    public void addBid(Order order) { bids.add(order); }
    public void addAsk(Order order) { asks.add(order); }

    public Order peekHighestBid() { return bids.peek(); }
    public Order peekLowestAsk() { return asks.peek(); }

    public Order pollHighestBid() { return bids.poll(); }
    public Order pollLowestAsk() { return asks.poll(); }

    public boolean hasBids() { return !bids.isEmpty(); }
    public boolean hasAsks() { return !asks.isEmpty(); }

    public PriorityQueue<Order> getBids() { return bids; }
    public PriorityQueue<Order> getAsks() { return asks; }
}