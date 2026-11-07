package net.adminrunet.h9cluster;

/**
 * Pure placement math for the main-screen panel window.
 *
 * <p>The panel floats over the factory launcher and must always stay fully
 * on screen, both when the user drags it and when it is restored on the
 * next boot. Keeping the clamping here makes the policy testable without
 * Android.</p>
 */
public final class ClusterPanelGeometry {
    private ClusterPanelGeometry() {
    }

    /** Horizontal position clamped so the panel stays inside the screen. */
    public static int clampX(int x, int screenWidth, int panelWidth) {
        int max = Math.max(0, screenWidth - panelWidth);
        return Math.max(0, Math.min(x, max));
    }

    /** Vertical position clamped so the panel stays inside the screen. */
    public static int clampY(int y, int screenHeight, int panelHeight) {
        int max = Math.max(0, screenHeight - panelHeight);
        return Math.max(0, Math.min(y, max));
    }

    /**
     * Default anchor: the right part of the wide head-unit screen, a little
     * below the top edge, clear of the factory status column.
     */
    public static int defaultX(int screenWidth, int panelWidth) {
        return clampX(Math.round(screenWidth * 0.56f), screenWidth, panelWidth);
    }

    /** Default anchor: a little below the top edge of the main screen. */
    public static int defaultY(int screenHeight) {
        return Math.max(0, Math.round(screenHeight * 0.08f));
    }

    /** Panel width: about 42 percent of the main screen width. */
    public static int panelWidth(int screenWidth) {
        return Math.round(screenWidth * 0.42f);
    }
}
