package engine;

import java.util.Locale;

/** Helpers for money amounts, which are always kept with at most 2 digits after the decimal point. */
public final class Money {

    private static final double CENTS = 100.0;
    private static final double TOLERANCE = 1e-6;

    private Money() {
    }

    public static double round(double value) {
        return Math.round(value * CENTS) / CENTS;
    }

    public static boolean hasAtMostTwoDecimals(double value) {
        double scaled = value * CENTS;
        return Math.abs(scaled - Math.rint(scaled)) < TOLERANCE;
    }

    public static String format(double value) {
        return String.format(Locale.US, "$%.2f", value);
    }
}