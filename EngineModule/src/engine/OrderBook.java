package engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class OrderBook {

    // the highest price first; the same price: the order that arrived first
    private static final Comparator<Order> BID_ORDER = (o1, o2) -> {
        int byPrice = Double.compare(o2.getPrice(), o1.getPrice());
        return byPrice != 0 ? byPrice : Long.compare(o1.getSequence(), o2.getSequence());
    };

    // the lowest price first; the same price: the order that arrived first
    private static final Comparator<Order> ASK_ORDER = (o1, o2) -> {
        int byPrice = Double.compare(o1.getPrice(), o2.getPrice());
        return byPrice != 0 ? byPrice : Long.compare(o1.getSequence(), o2.getSequence());
    };

    private final PriorityQueue<Order> bids = new PriorityQueue<>(BID_ORDER); // buyers
    private final PriorityQueue<Order> asks = new PriorityQueue<>(ASK_ORDER); // sellers

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

    /** All the waiting buy orders, the best one first. */
    public List<Order> sortedBids() {
        List<Order> copy = new ArrayList<>(bids);
        copy.sort(BID_ORDER);
        return copy;
    }

    /** All the waiting sell orders, the best one first. */
    public List<Order> sortedAsks() {
        List<Order> copy = new ArrayList<>(asks);
        copy.sort(ASK_ORDER);
        return copy;
    }

    /** The number of shares that this user already offers for sale in this book. */
    public int sharesOfferedBy(String username) {
        int total = 0;
        for (Order order : asks) {
            if (order.getUsername().equals(username)) {
                total += order.getQuantity();
            }
        }
        return total;
    }

    /** The number of waiting orders (buy and sell) of this user. */
    public int ordersOf(String username) {
        int count = 0;
        for (Order order : bids) {
            if (order.getUsername().equals(username)) {
                count++;
            }
        }
        for (Order order : asks) {
            if (order.getUsername().equals(username)) {
                count++;
            }
        }
        return count;
    }

    public void clear() {
        bids.clear();
        asks.clear();
    }
}