package net.adminrunet.h9cluster;

/**
 * Placement of the main-screen panel button on the shared settings canvas.
 * The row holds the support-log button, this button, the exit button and
 * the update button.
 */
public final class SettingsPanelButton {
    public static final float LEFT = 260.0f;
    public static final float TOP = 466.0f;
    public static final float RIGHT = 470.0f;
    public static final float BOTTOM = 510.0f;

    private SettingsPanelButton() {
    }

    public static boolean contains(float x, float y) {
        return x >= LEFT && x <= RIGHT && y >= TOP && y <= BOTTOM;
    }
}
