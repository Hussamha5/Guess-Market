package ui;

import javafx.scene.Scene;

import java.net.URL;
import java.util.List;

/** The three looks of exercise 2: the default one, the dark one and the blue one. */
public final class Themes {

    public static final String DEFAULT = "Default Theme";
    public static final String DARK = "Dark Mode";
    public static final String BLUE = "Blue Mode";
    public static final List<String> NAMES = List.of(DEFAULT, DARK, BLUE);

    private static String current = DEFAULT;

    private Themes() {
    }

    public static String current() {
        return current;
    }

    public static void apply(Scene scene, String theme) {
        current = theme == null ? DEFAULT : theme;
        String file = switch (current) {
            case DARK -> "/dark-theme.css";
            case BLUE -> "/blue-theme.css";
            default -> "/default-theme.css";
        };
        scene.getStylesheets().clear();
        URL url = Themes.class.getResource(file);
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        }
    }
}