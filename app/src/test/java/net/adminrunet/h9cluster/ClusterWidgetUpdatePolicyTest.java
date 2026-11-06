package net.adminrunet.h9cluster;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Publish cadence of the main-display widgets. */
public final class ClusterWidgetUpdatePolicyTest {
    @Test
    public void firstPublishIsImmediate() {
        assertTrue(ClusterWidgetUpdatePolicy.shouldPublish(1000L, 0L, false));
    }

    @Test
    public void changedContentPassesAfterTheShortInterval() {
        assertTrue(ClusterWidgetUpdatePolicy.shouldPublish(
                1150L, 1000L, true));
        assertFalse(ClusterWidgetUpdatePolicy.shouldPublish(
                1149L, 1000L, true));
    }

    @Test
    public void unchangedContentOnlyRepublishesOncePerSecond() {
        assertFalse(ClusterWidgetUpdatePolicy.shouldPublish(
                1999L, 1000L, false));
        assertTrue(ClusterWidgetUpdatePolicy.shouldPublish(
                2000L, 1000L, false));
    }

    @Test
    public void changedContentAlsoRespectsTheSecondCeiling() {
        // A change within the one-second window does not bypass the ceiling
        // unless 150 ms have passed since the previous publish.
        assertFalse(ClusterWidgetUpdatePolicy.shouldPublish(
                1150L, 1100L, true));
        assertTrue(ClusterWidgetUpdatePolicy.shouldPublish(
                1300L, 1100L, true));
    }

    @Test
    public void clockRewindIsTreatedAsDue() {
        assertTrue(ClusterWidgetUpdatePolicy.shouldPublish(500L, 1000L, false));
    }
}
