package dto;

/** What every user is allowed to see about the other users: name, balance and whether he is a Market Maker. */
public class UserDTO {
    private final String name;
    private final double balance;
    private final boolean marketMaker;

    public UserDTO(String name, double balance, boolean marketMaker) {
        this.name = name;
        this.balance = balance;
        this.marketMaker = marketMaker;
    }

    public String getName() { return name; }
    public double getBalance() { return balance; }
    public boolean isMarketMaker() { return marketMaker; }

    @Override
    public String toString() {
        return name;
    }
}