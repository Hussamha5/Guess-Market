package ui.model;

import com.google.gson.Gson;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Brings an ObservableList (the one a table shows) to the content of a fresh list from the server,
 * touching only what really changed. This is what prevents the flickering and keeps the selection of the user:
 * <ul>
 *   <li>rows that are not in the fresh list are removed,</li>
 *   <li>rows that are equal (same content, see {@link #sameContent}) are not touched at all,</li>
 *   <li>rows that changed are replaced in place, new rows are inserted.</li>
 * </ul>
 * Must be called on the JavaFX Application Thread.
 */
public final class ListSync {

    private static final Gson GSON = new Gson();

    private ListSync() {
    }

    /**
     * The DTOs have no equals method, so two DTOs have the same content when their JSON texts are the same.
     * (The JSON is what came from the server in the first place.)
     */
    public static boolean sameContent(Object first, Object second) {
        if (first == second) {
            return true;
        }
        if (first == null || second == null) {
            return false;
        }
        return GSON.toJson(first).equals(GSON.toJson(second));
    }

    public static <T, K> void sync(ObservableList<T> shown, List<T> fresh, Function<T, K> keyOf) {
        // 1. remove the rows that do not exist any more
        for (int i = shown.size() - 1; i >= 0; i--) {
            K key = keyOf.apply(shown.get(i));
            if (indexOfKey(fresh, key, keyOf, 0) < 0) {
                shown.remove(i);
            }
        }
        // 2. walk through the fresh list and fix position after position
        for (int i = 0; i < fresh.size(); i++) {
            T wanted = fresh.get(i);
            K wantedKey = keyOf.apply(wanted);
            if (i < shown.size() && Objects.equals(keyOf.apply(shown.get(i)), wantedKey)) {
                if (!sameContent(shown.get(i), wanted)) {
                    shown.set(i, wanted);
                }
                continue;
            }
            int existing = indexOfKey(shown, wantedKey, keyOf, i + 1);
            if (existing >= 0) {
                shown.remove(existing); // the row moved: take it from its old place
            }
            shown.add(Math.min(i, shown.size()), wanted);
        }
        if (shown.size() > fresh.size()) {
            shown.remove(fresh.size(), shown.size());
        }
    }

    /** For lists without a key (like the orders of an order book): compares row by row. */
    public static <T> void syncByPosition(ObservableList<T> shown, List<T> fresh) {
        int common = Math.min(shown.size(), fresh.size());
        for (int i = 0; i < common; i++) {
            if (!sameContent(shown.get(i), fresh.get(i))) {
                shown.set(i, fresh.get(i));
            }
        }
        if (shown.size() > fresh.size()) {
            shown.remove(fresh.size(), shown.size());
        } else if (fresh.size() > shown.size()) {
            shown.addAll(fresh.subList(shown.size(), fresh.size()));
        }
    }

    private static <T, K> int indexOfKey(List<T> list, K key, Function<T, K> keyOf, int from) {
        for (int i = from; i < list.size(); i++) {
            if (Objects.equals(keyOf.apply(list.get(i)), key)) {
                return i;
            }
        }
        return -1;
    }
}