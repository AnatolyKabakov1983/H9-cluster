package net.adminrunet.h9cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.adminrunet.h9cluster.skins.SpeedDisplayMode;

import org.junit.Test;

/** Formatting policy of the main-display widget payloads. */
public final class ClusterWidgetFormatterTest {
    private static final long NOW_MS = 5000L;
    private static final String DASH = "\u2014";

    private static ClusterState state() {
        return new ClusterState(
                0,
                800,
                0,
                GearSelector.PARK,
                40,
                Float.NaN,
                40.0f,
                300,
                123456.7,
                12.3f,
                45.6f,
                2.3f,
                2.3f,
                2.3f,
                2.3f,
                Float.NaN,
                8.5f,
                Float.NaN,
                12.4f,
                18.0f,
                -12.0f,
                0.0f,
                1.4f,
                Float.NaN,
                -1.0f,
                120.0f,
                0L,
                0L,
                0L,
                0L,
                0L,
                "ECO");
    }

    private static ClusterState state(
            int speedKph,
            String gearSelector,
            int currentGear) {
        return new ClusterState(
                speedKph,
                1850,
                currentGear,
                gearSelector,
                92,
                Float.NaN,
                40.0f,
                300,
                123456.7,
                12.3f,
                45.6f,
                2.3f,
                2.3f,
                2.3f,
                2.3f,
                Float.NaN,
                8.5f,
                Float.NaN,
                12.4f,
                18.0f,
                -12.0f,
                0.0f,
                1.4f,
                Float.NaN,
                -1.0f,
                120.0f,
                0L,
                0L,
                0L,
                0L,
                0L,
                "ECO");
    }

    private static ClusterWidgetContent build(
            ClusterState state,
            SpeedDisplayMode mode,
            int tankCapacity) {
        return ClusterWidgetFormatter.build(state, mode, tankCapacity, 72, NOW_MS);
    }

    private static ClusterWidgetContent build(
            ClusterState state,
            SpeedDisplayMode mode,
            int tankCapacity,
            int batteryPercent) {
        return ClusterWidgetFormatter.build(
                state,
                mode,
                tankCapacity,
                batteryPercent,
                NOW_MS);
    }

    @Test
    public void speedFollowsTheSharedDisplayModes() {
        assertEquals("60", build(
                state(60, GearSelector.DRIVE, 3),
                SpeedDisplayMode.REAL,
                80).speedText);
        assertEquals("63", build(
                state(60, GearSelector.DRIVE, 3),
                SpeedDisplayMode.FACTORY_PLUS_4_5_PERCENT,
                80).speedText);
        assertEquals("35", build(
                state(30, GearSelector.DRIVE, 3),
                SpeedDisplayMode.FACTORY_PLUS_5_KPH,
                80).speedText);
    }

    @Test
    public void speedShowsDashWhenNoTrustworthySourceExists() {
        ClusterState unavailable = state(-1, GearSelector.DRIVE, 3);
        assertEquals(DASH, build(
                unavailable,
                SpeedDisplayMode.REAL,
                80).speedText);
    }

    @Test
    public void speedShowsZeroOnlyWithFreshRunningInParkEvidence() {
        ClusterState remoteStart = state(-1, GearSelector.DRIVE, 3)
                .withTboxVehicleState(
                        ClusterState.ENGINE_STATE_RUNNING,
                        NOW_MS,
                        true,
                        true,
                        NOW_MS);
        assertEquals("0", build(
                remoteStart,
                SpeedDisplayMode.REAL,
                80).speedText);

        ClusterState staleEvidence = state(-1, GearSelector.DRIVE, 3)
                .withTboxVehicleState(
                        ClusterState.ENGINE_STATE_RUNNING,
                        NOW_MS - StationarySpeedDisplayFallback.VEHICLE_STATE_FRESH_MS - 1L,
                        true,
                        true,
                        NOW_MS - StationarySpeedDisplayFallback.VEHICLE_STATE_FRESH_MS - 1L);
        assertEquals(DASH, build(
                staleEvidence,
                SpeedDisplayMode.REAL,
                80).speedText);
    }

    @Test
    public void gearLabelsFollowTheSelectorPolicy() {
        assertEquals("P", build(
                state(0, GearSelector.PARK, 0),
                SpeedDisplayMode.REAL,
                80).gearText);
        assertEquals("D3", build(
                state(60, GearSelector.DRIVE, 3),
                SpeedDisplayMode.REAL,
                80).gearText);
        assertEquals("M2", build(
                state(40, GearSelector.MANUAL, 2),
                SpeedDisplayMode.REAL,
                80).gearText);
        assertEquals(DASH, build(
                state(0, GearSelector.UNKNOWN, 0),
                SpeedDisplayMode.REAL,
                80).gearText);
    }

    @Test
    public void fuelProjectsOntoTheSelectedTankSize() {
        ClusterWidgetContent content = build(
                state(0, GearSelector.PARK, 0),
                SpeedDisplayMode.REAL,
                40);
        // The source snapshot carries 40 L on the 80-liter basis; a 40-liter
        // tank keeps the same percentage but halves the displayed liters.
        assertEquals("20 L", content.fuelLitersText);
        assertEquals("50", content.fuelPercentText);
        assertEquals(50, content.fuelBarPercent);
        assertFalse(content.fuelLow);
    }

    @Test
    public void fuelMarksLowReserve() {
        ClusterState low = new ClusterState(
                0, 800, 0, GearSelector.PARK, 40, Float.NaN,
                10.0f, 90, 0.0, 0.0f, 0.0f,
                2.3f, 2.3f, 2.3f, 2.3f,
                Float.NaN, 8.5f, Float.NaN, 12.4f, 18.0f, 0.0f,
                0f, 0f, 0f, 0f, 0f,
                0L, 0L, 0L, 0L, 0L, "ECO");
        ClusterWidgetContent content = build(
                low,
                SpeedDisplayMode.REAL,
                80);
        assertEquals("10 L", content.fuelLitersText);
        assertEquals("13", content.fuelPercentText);
        assertTrue(content.fuelLow);
    }

    @Test
    public void batteryShowsThePercentDigits() {
        assertEquals("72", build(
                state(0, GearSelector.PARK, 0),
                SpeedDisplayMode.REAL,
                80,
                72).batteryPercentText);
        assertEquals("0", build(
                state(0, GearSelector.PARK, 0),
                SpeedDisplayMode.REAL,
                80,
                0).batteryPercentText);
        assertEquals("100", build(
                state(0, GearSelector.PARK, 0),
                SpeedDisplayMode.REAL,
                80,
                100).batteryPercentText);
    }

    @Test
    public void batteryShowsDashWhenThePlatformReportsNoLevel() {
        assertEquals(DASH, build(
                state(0, GearSelector.PARK, 0),
                SpeedDisplayMode.REAL,
                80,
                -1).batteryPercentText);
        assertEquals(DASH, build(
                state(0, GearSelector.PARK, 0),
                SpeedDisplayMode.REAL,
                80,
                101).batteryPercentText);
    }

    @Test
    public void batteryMarksLowCharge() {
        assertTrue(build(
                state(0, GearSelector.PARK, 0),
                SpeedDisplayMode.REAL,
                80,
                20).batteryLow);
        assertFalse(build(
                state(0, GearSelector.PARK, 0),
                SpeedDisplayMode.REAL,
                80,
                21).batteryLow);
        assertFalse(build(
                state(0, GearSelector.PARK, 0),
                SpeedDisplayMode.REAL,
                80,
                -1).batteryLow);
    }

    @Test
    public void sensorRangesAndPlaceholdersMatchTheClusterPolicy() {
        ClusterWidgetContent content = build(
                state(60, GearSelector.DRIVE, 3),
                SpeedDisplayMode.REAL,
                80);
        assertEquals("92 °C", content.coolantText);
        assertEquals("18 °C", content.outsideText);
        assertEquals("12.4 V", content.voltageText);
        assertEquals("300 km", content.rangeText);
        assertEquals("123457 km", content.odometerText);
        assertEquals("45.6 km", content.tripText);
        assertEquals("12.3 km", content.dayText);
        assertEquals("2.3", content.pressureFrontLeftText);
        assertEquals("2.3", content.pressureFrontRightText);
        assertEquals("2.3", content.pressureRearLeftText);
        assertEquals("2.3", content.pressureRearRightText);
        assertEquals("-12°", content.steeringText);
        assertEquals("120 Nm", content.torqueText);
        assertEquals("ECO", content.driveModeText);
        assertEquals("0 / 1 / " + DASH + " / " + DASH, content.wheelSpeedsText);
        assertEquals("1850", content.rpmText);
    }

    @Test
    public void consumptionReusesTheClusterFormatter() {
        assertEquals(DASH, build(
                state(60, GearSelector.DRIVE, 3),
                SpeedDisplayMode.REAL,
                80).instantConsumptionText);
        assertEquals(DASH, build(
                state(60, GearSelector.DRIVE, 3),
                SpeedDisplayMode.REAL,
                80).averageConsumptionText);
    }

    @Test
    public void emptySnapshotRendersPlaceholdersWithoutCrashing() {
        ClusterWidgetContent content = ClusterWidgetFormatter.build(
                ClusterState.empty(),
                null,
                FuelTankCapacity.DEFAULT_LITERS,
                -1,
                NOW_MS);
        assertEquals("0", content.speedText);
        assertEquals(DASH, content.gearText);
        assertEquals(DASH, content.instantConsumptionText);
        assertEquals(DASH, content.batteryPercentText);
        assertFalse(content.batteryLow);
    }

    @Test
    public void equalSnapshotsProduceEqualContent() {
        ClusterState first = state(60, GearSelector.DRIVE, 3);
        ClusterWidgetContent one = build(
                first,
                SpeedDisplayMode.REAL,
                80);
        ClusterWidgetContent two = build(
                first,
                SpeedDisplayMode.REAL,
                80);
        assertEquals(one, two);
        assertEquals(one.hashCode(), two.hashCode());

        ClusterWidgetContent otherSpeed = build(
                state(61, GearSelector.DRIVE, 3),
                SpeedDisplayMode.REAL,
                80);
        assertFalse(one.equals(otherSpeed));

        ClusterWidgetContent otherBattery = build(
                first,
                SpeedDisplayMode.REAL,
                80,
                21);
        assertFalse(one.equals(otherBattery));
    }
}
