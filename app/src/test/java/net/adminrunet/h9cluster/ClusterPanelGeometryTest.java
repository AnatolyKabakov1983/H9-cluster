package net.adminrunet.h9cluster;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** On-screen clamping of the floating main-screen panel. */
public final class ClusterPanelGeometryTest {
    private static final int SCREEN_W = 1920;
    private static final int SCREEN_H = 720;
    private static final int PANEL_W = 800;

    @Test
    public void panelWidthFollowsTheScreenShare() {
        assertEquals(806, ClusterPanelGeometry.panelWidth(SCREEN_W));
        assertEquals(42, ClusterPanelGeometry.panelWidth(100));
    }

    @Test
    public void horizontalPositionStaysInsideTheScreen() {
        assertEquals(0, ClusterPanelGeometry.clampX(-50, SCREEN_W, PANEL_W));
        assertEquals(
                SCREEN_W - PANEL_W,
                ClusterPanelGeometry.clampX(5000, SCREEN_W, PANEL_W));
        assertEquals(
                400,
                ClusterPanelGeometry.clampX(400, SCREEN_W, PANEL_W));
    }

    @Test
    public void verticalPositionStaysInsideTheScreen() {
        assertEquals(0, ClusterPanelGeometry.clampY(-50, SCREEN_H, 600));
        assertEquals(
                120,
                ClusterPanelGeometry.clampY(120, SCREEN_H, 600));
        assertEquals(
                SCREEN_H - 600,
                ClusterPanelGeometry.clampY(5000, SCREEN_H, 600));
    }

    @Test
    public void clampingSurvivesPanelsWiderThanTheScreen() {
        assertEquals(0, ClusterPanelGeometry.clampX(300, 500, 900));
        assertEquals(0, ClusterPanelGeometry.clampY(300, 200, 600));
    }

    @Test
    public void defaultAnchorSitsInTheRightPartBelowTheTopEdge() {
        int defaultX = ClusterPanelGeometry.defaultX(SCREEN_W, PANEL_W);
        int defaultY = ClusterPanelGeometry.defaultY(SCREEN_H);
        // Right of the vertical middle but still fully on screen.
        assertEquals(1075, defaultX);
        assertEquals(58, defaultY);
        assertEquals(20, ClusterPanelGeometry.clampY(defaultY, SCREEN_H, 700));
    }
}
