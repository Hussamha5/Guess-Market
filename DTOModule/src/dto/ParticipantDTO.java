package dto;

import java.util.List;

public class ParticipantDTO {
    private final String userName;
    private final List<Integer> shares;
    private final double value;
    private final int openOrders;

    public ParticipantDTO(String userName, List<Integer> shares, double value, int openOrders) {
        this.userName = userName;
        this.shares = shares;
        this.value = value;
        this.openOrders = openOrders;
    }

    public String getUserName() { return userName; }
    public List<Integer> getShares() { return shares; }
    public double getValue() { return value; }
    public int getOpenOrders() { return openOrders; }
}