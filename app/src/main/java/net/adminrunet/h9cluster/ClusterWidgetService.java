package net.adminrunet.h9cluster;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
 * Foreground source of vehicle data for the main-display widgets.
 *
 * <p>The service owns its own read-only {@link ClusterDataSource}, keeps the
 * latest {@link ClusterState} in {@link ClusterWidgetDataStore} and pushes a
 * formatted payload into every bound widget once per second. Running in the
 * foreground is what keeps the one-second cadence alive while the launcher is
 * visible; a plain background service would be frozen by Android after a few
 * minutes. When the last widget disappears the system stops the service and
 * vehicle reading stops with it.</p>
 *
 * <p>Demo builds use {@link DemoClusterDataSource}: the widget animates with
 * synthetic telemetry and never touches GWM Adapter, FDBus or TBOX.</p>
 */
public final class ClusterWidgetService extends Service {
    private static final String CHANNEL_ID = "cluster_widget_data";
    private static final int NOTIFICATION_ID = 41;
    private static final long PUBLISH_PERIOD_MS = 1_000L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final TransmissionTemperatureAlert atfAlert =
            new TransmissionTemperatureAlert();
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

    private void publish() {
        long nowMs = SystemClock.elapsedRealtime();
        ClusterState state = ClusterWidgetDataStore.latest();
        TransmissionTemperatureAlert.Level atfLevel = atfAlert.update(
                state.transmissionTemperatureC,
                ClusterWidgetFormatter.hasFreshTransmissionTemperature(
                        state,
                        nowMs));
        ClusterWidgetContent content = ClusterWidgetFormatter.build(
                state,
                SkinPreferences.getSpeedDisplayMode(this),
                SkinPreferences.getFuelTankCapacityLiters(this),
                atfLevel,
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
                .build();
    }

    /** Starts the service only when at least one widget is bound. */
    static void ensureStarted(Context context) {
        if (hasBoundWidgets(context)) {
            context.startForegroundService(
                    new Intent(context, ClusterWidgetService.class));
        }
    }

    /** Stops reading vehicle data when no widget of any size remains. */
    static void stopIfUnused(Context context) {
        if (!hasBoundWidgets(context)) {
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
