package net.adminrunet.h9cluster;

import android.content.Context;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Applies the widget payload to the inflated full panel layout.
 *
 * <p>The floating main-screen panel reuses the 4×4 widget layout, so this
 * binder mirrors {@link ClusterWidgetViews} but works on a real view
 * hierarchy instead of RemoteViews — the overlay window needs no launcher
 * support at all, which is exactly what the GWM head unit requires.</p>
 */
final class ClusterPanelBinder {
    private ClusterPanelBinder() {
    }

    static void bind(View root, ClusterWidgetContent content) {
        Context context = root.getContext();
        text(root, R.id.widget_speed, content.speedText);
        text(root, R.id.widget_gear, content.gearText);
        text(root, R.id.widget_rpm, content.rpmText);
        text(root, R.id.widget_drive_mode, content.driveModeText);

        TextView fuelLiters = root.findViewById(R.id.widget_fuel_liters);
        fuelLiters.setText(content.fuelLitersText);
        fuelLiters.setTextColor(content.fuelLow
                ? context.getColor(R.color.widget_critical_red)
                : context.getColor(R.color.widget_text_primary));
        text(root, R.id.widget_fuel_percent, content.fuelPercentText);
        text(root, R.id.widget_range, content.rangeText);

        ProgressBar fuelBar = root.findViewById(R.id.widget_fuel_bar);
        fuelBar.setMax(100);
        fuelBar.setProgress(content.fuelBarPercent);
        fuelBar.setSecondaryProgress(0);

        text(root, R.id.widget_coolant, content.coolantText);

        TextView battery = root.findViewById(R.id.widget_battery);
        battery.setText(content.batteryPercentText);
        battery.setTextColor(content.batteryLow
                ? context.getColor(R.color.widget_critical_red)
                : context.getColor(R.color.widget_text_primary));

        text(root, R.id.widget_outside, content.outsideText);
        text(root, R.id.widget_instant, content.instantConsumptionText);
        text(root, R.id.widget_average, content.averageConsumptionText);
        text(root, R.id.widget_odometer, content.odometerText);
        text(root, R.id.widget_trip, content.tripText);
        text(root, R.id.widget_day, content.dayText);
        text(root, R.id.widget_pressure_fl, content.pressureFrontLeftText);
        text(root, R.id.widget_pressure_fr, content.pressureFrontRightText);
        text(root, R.id.widget_pressure_rl, content.pressureRearLeftText);
        text(root, R.id.widget_pressure_rr, content.pressureRearRightText);
        text(root, R.id.widget_voltage, content.voltageText);
        text(root, R.id.widget_torque, content.torqueText);
        text(root, R.id.widget_steering, content.steeringText);
        text(root, R.id.widget_wheel_speeds, content.wheelSpeedsText);
    }

    private static void text(View root, int id, String value) {
        TextView view = root.findViewById(id);
        if (view != null) {
            view.setText(value);
        }
    }
}
