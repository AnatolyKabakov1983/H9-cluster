package net.adminrunet.h9cluster;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

/**
 * Charge level of the vehicle 12 V battery as reported by the platform.
 *
 * <p>The GWM adapter exposes only the measured battery voltage, so the
 * percent level comes from the head unit's own battery service:
 * first {@link BatteryManager#BATTERY_PROPERTY_CAPACITY}, then the sticky
 * {@code ACTION_BATTERY_CHANGED} broadcast. Car head units without a
 * battery service return no usable value; the widget then shows the dash
 * placeholder instead of an invented number.</p>
 */
public final class BatteryChargeReader {
    private BatteryChargeReader() {
    }

    /** Charge level in 0..100 percent, or -1 when the platform reports none. */
    public static int readPercent(Context context) {
        BatteryManager manager = (BatteryManager)
                context.getSystemService(Context.BATTERY_SERVICE);
        if (manager != null) {
            int percent = manager.getIntProperty(
                    BatteryManager.BATTERY_PROPERTY_CAPACITY);
            if (isValid(percent)) {
                return percent;
            }
        }
        Intent sticky = context.registerReceiver(
                null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (sticky != null) {
            int level = sticky.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = sticky.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level >= 0 && scale > 0 && level <= scale) {
                int percent = Math.round(level * 100.0f / scale);
                if (isValid(percent)) {
                    return percent;
                }
            }
        }
        return -1;
    }

    private static boolean isValid(int percent) {
        return percent >= 0 && percent <= 100;
    }
}
