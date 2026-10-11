package ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Everything that is shown to the user is formatted here: money always with two decimals, with a dot. */
public final class Fmt {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private Fmt() {
    }

    public static String money(double value) {
        return String.format(Locale.US, "%.2f", Math.abs(value) < 0.005 ? 0.0 : value);
    }

    /** Like money, but with an explicit plus sign for positive numbers (used for the profit / loss). */
    public static String signed(double value) {
        return Math.abs(value) < 0.005 ? "0.00" : String.format(Locale.US, "%+.2f", value);
    }

    /** A price that does not exist yet (nobody bids, nobody asks) is shown as a dash. */
    public static String priceOrDash(double value) {
        return value > 0 ? money(value) : "-";
    }

    public static String percent(double fraction) {
        return String.format(Locale.US, "%.0f%%", fraction * 100);
    }

    public static String time(long millis) {
        return TIME.format(Instant.ofEpochMilli(millis));
    }

    /** The shares of a user in the options of an event, in the order of the options: "5 / 0 / 3". */
    public static String shares(List<Integer> shares) {
        return shares.stream().map(String::valueOf).collect(Collectors.joining(" / "));
    }

    public static String integer(double value) {
        return String.valueOf((long) value);
    }
}