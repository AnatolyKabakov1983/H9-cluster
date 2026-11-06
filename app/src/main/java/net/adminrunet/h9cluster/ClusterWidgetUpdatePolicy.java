package net.adminrunet.h9cluster;

/**
 * Decides when the widget payload is worth a RemoteViews transaction.
 *
 * <p>Vehicle sources publish far more often than once per second, and a
 * standing vehicle changes nothing at all. The policy keeps the display
 * fresh at the chosen 1 Hz cadence, lets an actual change through slightly
 * earlier, and drops everything in between.</p>
 */
public final class ClusterWidgetUpdatePolicy {
    /** Hard ceiling for widget refresh transactions. */
    public static final long MAX_PUBLISH_INTERVAL_MS = 1_000L;
    /** A genuinely new payload may pass a bit sooner than the ceiling. */
    public static final long CHANGED_CONTENT_INTERVAL_MS = 150L;

    private ClusterWidgetUpdatePolicy() {
    }

    /**
     * @param nowMs elapsedRealtime() reference
     * @param lastPublishAtMs previous successful publish, 0 when none
     * @param contentChanged formatted payload differs from the last one
     */
    public static boolean shouldPublish(
            long nowMs,
            long lastPublishAtMs,
            boolean contentChanged) {
        if (lastPublishAtMs <= 0L) {
            return true;
        }
        if (nowMs < lastPublishAtMs) {
            // elapsedRealtime() never rewinds, but stay defensive.
            return true;
        }
        long sincePublishMs = nowMs - lastPublishAtMs;
        return contentChanged
                ? sincePublishMs >= CHANGED_CONTENT_INTERVAL_MS
                : sincePublishMs >= MAX_PUBLISH_INTERVAL_MS;
    }
}
