package engine;

import dto.AccountDTO;
import dto.EventDTO;
import dto.LedgerEntryDTO;
import dto.ParticipationDTO;
import dto.UserDTO;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The market: all the users and all the events. Users and events are found by name (not case sensitive).
 * This class is NOT thread safe - the server must synchronize the calls to it.
 */
public class Market implements Engine {

    public static final int MAX_USER_NAME_LENGTH = 30;
    public static final double MAX_FUNDS_LOAD = 1_000_000;

    private static final Pattern VALID_USER_NAME = Pattern.compile("[A-Za-z0-9 _.-]+");

    private final Map<String, User> users = new LinkedHashMap<>();
    private final Map<String, Event> events = new LinkedHashMap<>();
    private long version = 1;

    @Override
    public long getVersion() {
        return version;
    }

    // ----- users -----

    /** Checks a user name and returns it without the spaces at its start and end. */
    public static String validateUserName(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Please enter a user name.");
        }
        if (name.length() > MAX_USER_NAME_LENGTH) {
            throw new IllegalArgumentException("The user name can have at most " + MAX_USER_NAME_LENGTH
                    + " characters.");
        }
        if (!VALID_USER_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("The user name can contain only English letters, digits, spaces, "
                    + "'_', '-' and '.'.");
        }
        return name;
    }

    /** Names are not case sensitive, so "Dana" and "dana" are the same user. */
    public static String keyOf(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    @Override
    public String getOrCreateUser(String rawName) {
        String name = validateUserName(rawName);
        User existing = users.get(keyOf(name));
        if (existing != null) {
            return existing.getName();
        }
        users.put(keyOf(name), new User(name));
        version++;
        return name;
    }

    @Override
    public void loadFunds(String userName, double amount) {
        User user = requireUser(userName);
        if (Double.isNaN(amount) || amount <= 0 || amount > MAX_FUNDS_LOAD) {
            throw new IllegalArgumentException("The amount to load must be greater than 0 and at most "
                    + Money.format(MAX_FUNDS_LOAD) + ".");
        }
        if (!Money.hasAtMostTwoDecimals(amount)) {
            throw new IllegalArgumentException("The amount can have at most 2 digits after the decimal point.");
        }
        user.updateBalance(amount, LedgerKind.LOAD, null, "Funds loaded to the account");
        version++;
    }

    // ----- events -----

    /** Nothing is added unless the whole file is valid. */
    @Override
    public List<String> uploadEvents(String userName, String fileName, InputStream xmlContent) {
        User uploader = requireUser(userName);
        if (uploader.isBlocked()) {
            throw new SecurityException("Your account is blocked because its balance is negative ("
                    + Money.format(uploader.getBalance()) + "). Load funds to continue.");
        }
        if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".xml")) {
            throw new IllegalArgumentException("The file must be an XML file (its name must end with .xml).");
        }
        Set<String> existingNames = new HashSet<>(events.keySet());
        List<Event> newEvents = XmlParser.parse(xmlContent, uploader, events.size() + 1, existingNames);
        List<String> names = new ArrayList<>();
        for (Event event : newEvents) {
            events.put(keyOf(event.getName()), event);
            names.add(event.getName());
        }
        version++;
        return names;
    }

    @Override
    public String activateEvent(String userName, String eventName) {
        User user = requireUser(userName);
        Event event = requireEvent(eventName);
        event.activateEvent(user);
        version++;
        return withWarning(user, "Event '" + event.getName() + "' is now active.");
    }

    @Override
    public String closeEvent(String userName, String eventName, int option) {
        User user = requireUser(userName);
        Event event = requireEvent(eventName);
        event.resolveEvent(user, option - 1);
        version++;
        return withWarning(user, "Event '" + event.getName() + "' is closed. The winning option is number "
                + option + ", and the winners were paid.");
    }

    @Override
    public String trade(String userName, String eventName, int option, boolean buy, Double price, int quantity) {
        User user = requireUser(userName);
        Event event = requireEvent(eventName);
        String message = event.trade(user, option - 1, buy, price, quantity);
        version++;
        return withWarning(user, message);
    }

    // ----- data for the clients -----

    @Override
    public List<EventDTO> getEvents() {
        List<EventDTO> dtos = new ArrayList<>();
        for (Event event : events.values()) {
            dtos.add(event.toDTO());
        }
        return dtos;
    }

    @Override
    public List<UserDTO> getUsers() {
        List<UserDTO> dtos = new ArrayList<>();
        for (User user : users.values()) {
            dtos.add(new UserDTO(user.getName(), user.getBalance(), isMarketMaker(user)));
        }
        return dtos;
    }

    @Override
    public AccountDTO getAccount(String userName, int lastSerialSeen) {
        User user = requireUser(userName);
        List<ParticipationDTO> participations = new ArrayList<>();
        for (Event event : events.values()) {
            if (event.getMarketMakerUser() == user || event.hasParticipant(user)) {
                participations.add(event.participationOf(user));
            }
        }
        List<LedgerEntryDTO> ledger = new ArrayList<>();
        for (LedgerEntry entry : user.ledgerAfter(lastSerialSeen)) {
            ledger.add(entry.toDTO());
        }
        return new AccountDTO(user.getName(), user.getBalance(), user.isBlocked(), participations, ledger);
    }

    // ----- internal -----

    private boolean isMarketMaker(User user) {
        for (Event event : events.values()) {
            if (event.getMarketMakerUser() == user) {
                return true;
            }
        }
        return false;
    }

    private User requireUser(String name) {
        User user = name == null ? null : users.get(keyOf(name.trim()));
        if (user == null) {
            throw new NoSuchElementException("There is no user named '" + name + "'.");
        }
        return user;
    }

    private Event requireEvent(String name) {
        Event event = name == null ? null : events.get(keyOf(name.trim()));
        if (event == null) {
            throw new NoSuchElementException("There is no event named '" + name + "'.");
        }
        return event;
    }

    // a user whose balance became negative is told so, in the answer to the action that caused it
    private static String withWarning(User user, String message) {
        if (user.isBlocked()) {
            return message + " WARNING: your balance is now negative (" + Money.format(user.getBalance())
                    + "). Your account is blocked until you load enough funds.";
        }
        return message;
    }
}