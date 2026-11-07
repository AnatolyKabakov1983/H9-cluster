package net.adminrunet.h9cluster;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * Floating cluster panel over the factory main screen.
 *
 * <p>The GWM head unit launcher hosts only the factory "смарт-виджеты" and
 * offers no AppWidget picker for third-party packages, so the widget built
 * for ordinary launchers never appears there. This panel is the head-unit
 * variant of the same widget: an overlay window (SYSTEM_ALERT_WINDOW) that
 * draws the full 4×4 content directly over whatever the launcher shows,
 * including the factory smart-widget page. It needs no launcher support,
 * can be dragged anywhere and is toggled from the service notification.</p>
 *
 * <p>All methods must be called from the main thread; the owning service
 * ticks once per second and refreshes the panel together with the
 * RemoteViews widgets.</p>
 */
final class ClusterMainPanelWindow {
    private static View panelView;
    private static WindowManager.LayoutParams panelParams;

    private ClusterMainPanelWindow() {
    }

    static boolean isVisible() {
        return panelView != null;
    }

    /** True when the platform already granted the overlay permission. */
    static boolean canShow(Context context) {
        return Settings.canDrawOverlays(context);
    }

    /**
     * Shows the panel; returns false when the overlay permission is missing.
     * The panel is created with the latest known snapshot so it never opens
     * empty.
     */
    static boolean show(Context context) {
        if (panelView != null) {
            return true;
        }
        if (!canShow(context)) {
            return false;
        }
        Context app = context.getApplicationContext();
        WindowManager windows = windowManager(app);
        if (windows == null) {
            return false;
        }
        View view = LayoutInflater.from(app)
                .inflate(R.layout.widget_cluster_full, null);
        int screenWidth = app.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = app.getResources().getDisplayMetrics().heightPixels;
        int width = ClusterPanelGeometry.panelWidth(screenWidth);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        if (ClusterPanelPreferences.hasPosition(app)) {
            params.x = ClusterPanelGeometry.clampX(
                    ClusterPanelPreferences.x(app),
                    screenWidth,
                    width);
            params.y = ClusterPanelGeometry.clampY(
                    ClusterPanelPreferences.y(app),
                    screenHeight,
                    0);
        } else {
            params.x = ClusterPanelGeometry.defaultX(screenWidth, width);
            params.y = ClusterPanelGeometry.defaultY(screenHeight);
        }

        attachDragHandler(app, view, params, windows);
        ClusterPanelBinder.bind(
                view,
                ClusterWidgetFormatter.build(
                        ClusterWidgetDataStore.latest(),
                        SkinPreferences.getSpeedDisplayMode(app),
                        SkinPreferences.getFuelTankCapacityLiters(app),
                        BatteryChargeReader.readPercent(app),
                        SystemClock.elapsedRealtime()));
        windows.addView(view, params);
        panelView = view;
        panelParams = params;
        DiagnosticLog.info("cluster_panel", "shown");
        return true;
    }

    /** Removes the panel if it is on screen. */
    static void hide(Context context) {
        if (panelView == null) {
            return;
        }
        WindowManager windows = windowManager(
                context.getApplicationContext());
        if (windows != null) {
            windows.removeView(panelView);
        }
        panelView = null;
        panelParams = null;
        DiagnosticLog.info("cluster_panel", "hidden");
    }

    /** Refreshes the panel contents on the one-second service tick. */
    static void update(ClusterWidgetContent content) {
        if (panelView != null && content != null) {
            ClusterPanelBinder.bind(panelView, content);
        }
    }

    /** Notifies the user when the overlay permission is still missing. */
    static void toastPermissionMissing(Context context) {
        Toast.makeText(
                context,
                context.getString(R.string.panel_permission_missing),
                Toast.LENGTH_LONG)
                .show();
    }

    private static WindowManager windowManager(Context app) {
        return (WindowManager) app.getSystemService(Context.WINDOW_SERVICE);
    }

    /**
     * Any touch moves the panel; the last position is persisted so the next
     * show and every reboot restore the same spot. There is no click action,
     * so dragging cannot collide with navigation.
     */
    private static void attachDragHandler(
            final Context app,
            View view,
            final WindowManager.LayoutParams params,
            final WindowManager windows) {
        final int touchSlop = ViewConfiguration.get(app).getScaledTouchSlop();
        view.setOnTouchListener(new View.OnTouchListener() {
            private float downRawX;
            private float downRawY;
            private boolean dragging;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        dragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - downRawX;
                        float deltaY = event.getRawY() - downRawY;
                        if (!dragging
                                && Math.hypot(deltaX, deltaY) < touchSlop) {
                            return true;
                        }
                        dragging = true;
                        int screenWidth =
                                app.getResources()
                                        .getDisplayMetrics()
                                        .widthPixels;
                        int screenHeight =
                                app.getResources()
                                        .getDisplayMetrics()
                                        .heightPixels;
                        int measuredWidth =
                                v.getMeasuredWidth() > 0
                                        ? v.getMeasuredWidth()
                                        : params.width;
                        params.x = ClusterPanelGeometry.clampX(
                                Math.round(params.x + deltaX),
                                screenWidth,
                                measuredWidth);
                        params.y = ClusterPanelGeometry.clampY(
                                Math.round(params.y + deltaY),
                                screenHeight,
                                v.getMeasuredHeight());
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        try {
                            windows.updateViewLayout(v, params);
                        } catch (RuntimeException ignored) {
                            // The window can vanish mid-drag during shutdown.
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (dragging) {
                            ClusterPanelPreferences.storePosition(
                                    app,
                                    params.x,
                                    params.y);
                        }
                        dragging = false;
                        return true;
                    default:
                        return true;
                }
            }
        });
    }
}
