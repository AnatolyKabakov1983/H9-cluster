package net.adminrunet.h9cluster;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;

import androidx.core.app.NotificationCompat;

import net.adminrunet.h9cluster.skins.SpeedDisplayMode;

/**
 * Foreground source of vehicle data for the main display.
 *
 * <p>The service owns its own read-only {@link ClusterDataSource}, keeps the
 * latest {@link ClusterState} in {@link ClusterWidgetDataStore} and pushes a
 * formatted payload once per second into every bound AppWidget and into the
 * floating main-screen panel. Running in the foreground is what keeps the
 * one-second cadence alive while the launcher is visible; a plain background
 * service would be frozen by Android after a few minutes. When the last
 * widget disappears and the panel is off, the system stops the service and
 * vehicle reading stops with it.</p>
 *
 * <p>Demo builds use {@link DemoClusterDataSource}: the widget animates with
 * synthetic telemetry and never touches GWM Adapter, FDBus or TBOX.</p>
 */
public final class ClusterWidgetService extends Service {
    private static final String CHANNEL_ID = "cluster_widget_data";
    private static final int NOTIFICATION_ID = 41;
    private static final long PUBLISH_PERIOD_MS = 1_000L;

    /** Toggles the floating panel over the factory main screen. */
    public static final String ACTION_SHOW_PANEL =
            "net.adminrunet.h9cluster.action.SHOW_PANEL";
    public static final String ACTION_HIDE_PANEL =
            "net.adminrunet.h9cluster.action.HIDE_PANEL";

    private static final int REQUEST_SHOW_PANEL = 1;
    private static final int REQUEST_HIDE_PANEL = 2;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ClusterDataSource dataSource;
    private boolean dataSourceStarted;
    private ClusterWidgetContent lastPublishedContent;
    private long lastPublishAtMs;

    private final Runnable publishTick = new Runnable() {
        @Override
        public void run() {
            publish();
            mainHandler.postDelayed(this, PUBLISH_PERIOD_MS);
        }
    };

    private final ClusterDataSource.Listener dataSourceListener =
            new ClusterDataSource.Listener() {
                @Override
                public void onClusterState(ClusterState state) {
                    ClusterWidgetDataStore.update(state);
                    publish();
                }
            };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        dataSource = BuildConfig.DEMO_MODE
                ? new DemoClusterDataSource(this)
                : new GwmClusterDataSource(this);
        DiagnosticLog.info("cluster_widget", "service_created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification());
        String action = intent == null ? null : intent.getAction();
        if (ACTION_HIDE_PANEL.equals(action)) {
            hidePanel();
        } else if (ACTION_SHOW_PANEL.equals(action)) {
            showPanel();
        }
        if (!dataSourceStarted) {
            dataSourceStarted = true;
            dataSource.start(dataSourceListener);
            mainHandler.removeCallbacks(publishTick);
            mainHandler.post(publishTick);
            DiagnosticLog.info(
                    "cluster_widget",
                    "source_started",
                    "demo_mode", String.valueOf(BuildConfig.DEMO_MODE));
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        mainHandler.removeCallbacks(publishTick);
        if (dataSourceStarted) {
            dataSourceStarted = false;
            dataSource.stop();
        }
        DiagnosticLog.info("cluster_widget", "service_destroyed");
        super.onDestroy();
    }

    private void showPanel() {
        if (ClusterMainPanelWindow.show(this)) {
            ClusterPanelPreferences.setVisible(this, true);
        } else {
            ClusterMainPanelWindow.toastPermissionMissing(this);
            ClusterPanelPreferences.setVisible(this, false);
            // The service may have been started only for this request; do
            // not keep reading vehicle data when nothing displays it.
            stopIfUnused(this);
        }
    }

    private void hidePanel() {
        ClusterMainPanelWindow.hide(this);
        ClusterPanelPreferences.setVisible(this, false);
        stopIfUnused(this);
    }

    private void publish() {
        long nowMs = SystemClock.elapsedRealtime();
        ClusterState state = ClusterWidgetDataStore.latest();
        ClusterWidgetContent content = ClusterWidgetFormatter.build(
                state,
                SkinPreferences.getSpeedDisplayMode(this),
                SkinPreferences.getFuelTankCapacityLiters(this),
                BatteryChargeReader.readPercent(this),
                nowMs);
        if (!ClusterWidgetUpdatePolicy.shouldPublish(
                nowMs,
                lastPublishAtMs,
                !content.equals(lastPublishedContent))) {
            return;
        }
        lastPublishAtMs = nowMs;
        lastPublishedContent = content;
        ClusterWidgetViews.pushAll(this, content);
        ClusterMainPanelWindow.update(content);
    }

    private void createNotificationChannel() {
        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.widget_service_channel_name),
                NotificationManager.IMPORTANCE_MIN);
        channel.setDescription(
                getString(R.string.widget_service_channel_description));
        channel.setShowBadge(false);
        manager.createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_dashboard)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.widget_service_notification))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setShowWhen(false)
                .addAction(0, getString(R.string.panel_notification_show),
                        panelPendingIntent(ACTION_SHOW_PANEL, REQUEST_SHOW_PANEL))
                .addAction(0, getString(R.string.panel_notification_hide),
                        panelPendingIntent(ACTION_HIDE_PANEL, REQUEST_HIDE_PANEL))
                .build();
    }

    private PendingIntent panelPendingIntent(String action, int requestCode) {
        Intent intent = new Intent(this, ClusterWidgetService.class);
        intent.setAction(action);
        return PendingIntent.getService(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Starts the service only when at least one widget is bound. */
    static void ensureStarted(Context context) {
        if (hasBoundWidgets(context)) {
            context.startForegroundService(
                    new Intent(context, ClusterWidgetService.class));
        }
    }

    /**
     * Restores the panel after boot when it was on screen before shutdown.
     * Called from {@link BootReceiver} next to the widget restore.
     */
    static void restorePanelAfterBoot(Context context) {
        if (ClusterPanelPreferences.isVisible(context)) {
            Intent intent = new Intent(context, ClusterWidgetService.class);
            intent.setAction(ACTION_SHOW_PANEL);
            context.startForegroundService(intent);
        }
    }

    /** Stops reading vehicle data when no widget and no panel remains. */
    static void stopIfUnused(Context context) {
        if (!hasBoundWidgets(context)
                && !ClusterPanelPreferences.isVisible(context)
                && !ClusterMainPanelWindow.isVisible()) {
            context.stopService(
                    new Intent(context, ClusterWidgetService.class));
        }
    }

    static boolean hasBoundWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        if (manager == null) {
            return false;
        }
        return manager.getAppWidgetIds(
                new ComponentName(context, CompactClusterWidgetProvider.class))
                .length > 0
                || manager.getAppWidgetIds(
                        new ComponentName(
                                context,
                                FullClusterWidgetProvider.class))
                        .length > 0;
    }
}
