package net.adminrunet.h9cluster;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 * Builds and pushes the RemoteViews payloads for both widget sizes.
 *
 * <p>RemoteViews layouts only accept framework views, so every value is a
 * TextView string and the single gauge is a horizontal ProgressBar. Colors
 * follow the dark cluster style: white numbers, muted labels, orange accent,
 * low battery and low fuel in red.</p>
 */
final class ClusterWidgetViews {
    private ClusterWidgetViews() {
    }

    /** Pushes the payload into every bound widget of both sizes. */
    static void pushAll(Context context, ClusterWidgetContent content) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        push(context, manager, CompactClusterWidgetProvider.class, content);
        push(context, manager, FullClusterWidgetProvider.class, content);
    }

    private static void push(
            Context context,
            AppWidgetManager manager,
            Class<? extends ClusterWidgetProvider> providerClass,
            ClusterWidgetContent content) {
        ComponentName component = new ComponentName(context, providerClass);
        int[] widgetIds = manager.getAppWidgetIds(component);
        if (widgetIds == null || widgetIds.length == 0) {
            return;
        }
        manager.updateAppWidget(
                widgetIds,
                build(context, providerClass, content));
    }

    static RemoteViews build(
            Context context,
            Class<? extends ClusterWidgetProvider> providerClass,
            ClusterWidgetContent content) {
        boolean full = FullClusterWidgetProvider.class.equals(providerClass);
        RemoteViews views = new RemoteViews(
                context.getPackageName(),
                full
                        ? R.layout.widget_cluster_full
                        : R.layout.widget_cluster_compact);
        views.setOnClickPendingIntent(
                R.id.widget_root,
                settingsPendingIntent(context));

        views.setTextViewText(R.id.widget_speed, content.speedText);
        views.setTextViewText(R.id.widget_gear, content.gearText);
        views.setTextViewText(R.id.widget_rpm, content.rpmText);
        views.setTextViewText(R.id.widget_fuel_liters, content.fuelLitersText);
        views.setTextViewText(
                R.id.widget_fuel_percent,
                content.fuelPercentText);
        // RemoteViews.setProgress(int, int, int) exists only from API 31;
        // Android 9 in the head unit needs the reflection-based setters.
        views.setInt(R.id.widget_fuel_bar, "setMax", 100);
        views.setInt(
                R.id.widget_fuel_bar,
                "setProgress",
                content.fuelBarPercent);
        views.setInt(R.id.widget_fuel_bar, "setSecondaryProgress", 0);
        views.setTextViewText(R.id.widget_range, content.rangeText);
        views.setTextViewText(R.id.widget_coolant, content.coolantText);
        views.setTextViewText(
                R.id.widget_battery,
                content.batteryPercentText);
        views.setTextColor(
                R.id.widget_battery,
                content.batteryLow
                        ? context.getColor(R.color.widget_critical_red)
                        : context.getColor(R.color.widget_text_primary));
        views.setTextViewText(R.id.widget_voltage, content.voltageText);
        views.setTextColor(
                R.id.widget_fuel_liters,
                content.fuelLow
                        ? context.getColor(R.color.widget_critical_red)
                        : context.getColor(R.color.widget_text_primary));

        if (full) {
            views.setTextViewText(
                    R.id.widget_outside,
                    content.outsideText);
            views.setTextViewText(
                    R.id.widget_instant,
                    content.instantConsumptionText);
            views.setTextViewText(
                    R.id.widget_average,
                    content.averageConsumptionText);
            views.setTextViewText(
                    R.id.widget_odometer,
                    content.odometerText);
            views.setTextViewText(R.id.widget_trip, content.tripText);
            views.setTextViewText(R.id.widget_day, content.dayText);
            views.setTextViewText(
                    R.id.widget_pressure_fl,
                    content.pressureFrontLeftText);
            views.setTextViewText(
                    R.id.widget_pressure_fr,
                    content.pressureFrontRightText);
            views.setTextViewText(
                    R.id.widget_pressure_rl,
                    content.pressureRearLeftText);
            views.setTextViewText(
                    R.id.widget_pressure_rr,
                    content.pressureRearRightText);
            views.setTextViewText(
                    R.id.widget_steering,
                    content.steeringText);
            views.setTextViewText(R.id.widget_torque, content.torqueText);
            views.setTextViewText(
                    R.id.widget_drive_mode,
                    content.driveModeText);
            views.setTextViewText(
                    R.id.widget_wheel_speeds,
                    content.wheelSpeedsText);
        }
        return views;
    }

    private static PendingIntent settingsPendingIntent(Context context) {
        Intent intent = new Intent(context, SettingsActivity.class);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);
    }
}
