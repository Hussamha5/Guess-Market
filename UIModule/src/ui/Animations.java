package ui;

import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/** The small optional animations of the "Enable Animations" check box (as in exercise 2). */
public final class Animations {

    private static boolean enabled;

    private Animations() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    /** A short fade-in, played only when the user switched the animations on. */
    public static void fadeIn(Node node) {
        if (!enabled || node == null) {
            return;
        }
        FadeTransition fade = new FadeTransition(Duration.millis(700), node);
        fade.setFromValue(0.15);
        fade.setToValue(1.0);
        fade.play();
    }
}