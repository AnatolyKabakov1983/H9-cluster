package net.adminrunet.h9cluster;

/**
 * Process-wide holder of the latest vehicle snapshot for the widgets.
 *
 * <p>The service writes on every push from the data source; providers read
 * the same snapshot to render immediately when the launcher re-binds a widget
 * without waiting for the next one-second tick. Values keep the raw source
 * units; formatting happens in {@link ClusterWidgetFormatter}.</p>
 */
public final class ClusterWidgetDataStore {
    private static volatile ClusterState latestState = ClusterState.empty();

    private ClusterWidgetDataStore() {
    }

    /** Replaces the shared snapshot. Thread-safe, never null. */
    public static void update(ClusterState state) {
        latestState = state == null ? ClusterState.empty() : state;
    }

    /** Latest known snapshot; {@link ClusterState#empty()} before any data. */
    public static ClusterState latest() {
        return latestState;
    }
}
