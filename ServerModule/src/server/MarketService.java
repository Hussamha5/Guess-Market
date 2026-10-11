package server;

import dto.AccountDTO;
import dto.EventDTO;
import dto.UserDTO;
import engine.Engine;
import engine.Market;
import engine.Money;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * The single entry point of the servlets to the engine.
 * The engine itself is not thread safe, so every call is guarded here (synchronization "high", at the servlet level):
 * requests that only read data (the frequent polling) share a read lock, requests that change data
 * take the write lock and run alone.
 * It also knows which user name is held by which HTTP session.
 */
public class MarketService {

    public static final String CONTEXT_ATTRIBUTE = "marketService";
    private static final long NO_VERSION = -1;

    private final Engine market = new Market();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, String> sessionByUserKey = new HashMap<>();

    // ----- login -----

    /**
     * Connects a user name to an HTTP session. The name of a user that is connected right now is refused;
     * a name that nobody holds is either a new user or a returning user (who keeps his account).
     */
    public String login(String rawName, String sessionId) {
        return write(() -> {
            String name = Market.validateUserName(rawName);
            String key = Market.keyOf(name);
            String holder = sessionByUserKey.get(key);
            if (holder != null && !holder.equals(sessionId)) {
                throw new IllegalStateException("The user name '" + name + "' is already in use by another "
                        + "connected user. Please choose a different name.");
            }
            String canonicalName = market.getOrCreateUser(name);
            sessionByUserKey.put(key, sessionId);
            return canonicalName;
        });
    }

    /** Frees the user name of this session. The account stays in the market. */
    public void logout(String sessionId) {
        write(() -> {
            sessionByUserKey.values().removeIf(sessionId::equals);
            return null;
        });
    }

    // ----- actions -----

    public List<String> upload(String user, String fileName, InputStream content) {
        return write(() -> market.uploadEvents(user, fileName, content));
    }

    public String activate(String user, String eventName) {
        return write(() -> market.activateEvent(user, eventName));
    }

    public String close(String user, String eventName, int option) {
        return write(() -> market.closeEvent(user, eventName, option));
    }

    public String trade(String user, String eventName, int option, boolean buy, Double price, int quantity) {
        return write(() -> market.trade(user, eventName, option, buy, price, quantity));
    }

    public String loadFunds(String user, double amount) {
        return write(() -> {
            market.loadFunds(user, amount);
            return "Loaded " + Money.format(amount) + " to your account.";
        });
    }

    // ----- reading (answers "nothing changed" when the client already has the current version) -----

    public Versioned<List<EventDTO>> events(long sinceVersion) {
        return read(() -> versioned(sinceVersion, market::getEvents));
    }

    public Versioned<List<UserDTO>> users(long sinceVersion) {
        return read(() -> versioned(sinceVersion, market::getUsers));
    }

    public Versioned<AccountDTO> account(String user, long sinceVersion, int ledgerSince) {
        return read(() -> versioned(sinceVersion, () -> market.getAccount(user, ledgerSince)));
    }

    private <T> Versioned<T> versioned(long sinceVersion, Supplier<T> data) {
        long current = market.getVersion();
        if (sinceVersion != NO_VERSION && sinceVersion == current) {
            return new Versioned<>(current, null);
        }
        return new Versioned<>(current, data.get());
    }

    // ----- locking -----

    private <T> T write(Supplier<T> action) {
        lock.writeLock().lock();
        try {
            return action.get();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private <T> T read(Supplier<T> action) {
        lock.readLock().lock();
        try {
            return action.get();
        } finally {
            lock.readLock().unlock();
        }
    }
}