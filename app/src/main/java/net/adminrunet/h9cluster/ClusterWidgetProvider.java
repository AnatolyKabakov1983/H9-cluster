package net.adminrunet.h9cluster;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.os.SystemClock;
import android.widget.RemoteViews;

/**
 * Shared lifecycle of the main-display cluster widgets.
 *
 * <p>Launcher callbacks keep the data service alive exactly while at least
 * one widget of either size is bound: the first instance starts it, the last
 * removal stops it and vehicle reading ends. Re-binding (launcher restart,
 * package replace) renders immediately from {@link ClusterWidgetDataStore}
 * without waiting for the next one-second tick.</p>
 */
abstract class ClusterWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(
            Context context,
            AppWidgetManager manager,
            int[] appWidgetIds) {
        if (appWidgetIds == null || appWidgetIds.length == 0) {
            return;
        }
        manager.updateAppWidget(appWidgetIds, currentViews(context));
        ClusterWidgetService.ensureStarted(context);
    }

    @Override
    public void onEnabled(Context context) {
        ClusterWidgetService.ensureStarted(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        ClusterWidgetService.stopIfUnused(context);
    }

    /**
     * Fires for this size when its last instance is removed; both sizes are
     * checked so the shared service survives while any widget remains.
     */
    @Override
    public void onDisabled(Context context) {
        ClusterWidgetService.stopIfUnused(context);
    }

    /** The concrete provider class this size is bound through. */
    abstract Class<? extends ClusterWidgetProvider> providerClass();

    private RemoteViews currentViews(Context context) {
        long nowMs = SystemClock.elapsedRealtime();
        ClusterWidgetContent content = ClusterWidgetFormatter.build(
                ClusterWidgetDataStore.latest(),
                SkinPreferences.getSpeedDisplayMode(context),
                SkinPreferences.getFuelTankCapacityLiters(context),
                BatteryChargeReader.readPercent(context),
                nowMs);
        return ClusterWidgetViews.build(context, providerClass(), content);
    }
}
