package net.adminrunet.h9cluster;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persistence of the main-screen panel: whether it was on screen and where
 * the user last dragged it. The flag survives reboots so {@code BootReceiver}
 * restores the panel exactly when the user left it.
 */
final class ClusterPanelPreferences {
    private static final String FILE = "cluster_panel";
    private static final String KEY_VISIBLE = "visible";
    private static final String KEY_X = "x";
    private static final String KEY_Y = "y";
    private static final int NO_POSITION = Integer.MIN_VALUE;

    private ClusterPanelPreferences() {
    }

    static boolean isVisible(Context context) {
        return prefs(context).getBoolean(KEY_VISIBLE, false);
    }

    static void setVisible(Context context, boolean visible) {
        prefs(context).edit().putBoolean(KEY_VISIBLE, visible).apply();
    }

    static boolean hasPosition(Context context) {
        return x(context) != NO_POSITION && y(context) != NO_POSITION;
    }

    static int x(Context context) {
        return prefs(context).getInt(KEY_X, NO_POSITION);
    }

    static int y(Context context) {
        return prefs(context).getInt(KEY_Y, NO_POSITION);
    }

    static void storePosition(Context context, int x, int y) {
        prefs(context).edit()
                .putInt(KEY_X, x)
                .putInt(KEY_Y, y)
                .apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}
