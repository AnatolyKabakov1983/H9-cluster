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
            int tankCapacity,
            TransmissionTemperatureAlert.Level atfLevel) {
        return ClusterWidgetFormatter.build(state, mode, tankCapacity, atfLevel, NOW_MS);
    }

    @Test
    public void speedFollowsTheSharedDisplayModes() {
        assertEquals("60", build(
                state(60, GearSelector.DRIVE, 3),
                SpeedDisplayMode.REAL,
                80,
                TransmissionTemperatureAlert.Level.NORMAL).speedText);
        assertEquals("63", build(
                state(60, GearSelector.DRIVE, 3),
                SpeedDisplayMode.FACTORY_PLUS_4_5_PERCENT,
                80,
                TransmissionTemperatureAlert.Level.NORMAL).speedText);
        assertEquals("35", build(
                state(30, GearSelector.DRIVE, 3),
                SpeedDisplayMode.FACTORY_PLUS_5_KPH,
                80,
                TransmissionTemperatureAlert.Level.NORMAL).speedText);
    }

    @Test
    public void speedShowsDashWhenNoTrustworthySourceExists() {
        ClusterState unavailable = state(-1, GearSelector.DRIVE, 3);
        assertEquals(DASH, build(
                unavailable,
                SpeedDisplayMode.REAL,
                80,
                TransmissionTemperatureAlert.Level.NORMAL).speedText);
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
                80,
                TransmissionTemperatureAlert.Level.NORMAL).speedText);

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
                80,
                TransmissionTemperatureAlert.Level.NORMAL).speedText);
    }

    @Test
    public void gearLabelsFollowTheSelectorPolicy() {
        assertEquals("P", build(
                state(0, GearSelector.PARK, 0),
                SpeedDisplayMode.REAL,
                80,
                TransmissionTemperatureAlert.Level.NORMAL).gearText);
        assertEquals("D3", build(
                state(60, GearSelector.DRIVE, 3),
                SpeedDisplayMode.REAL,
                80,
                TransmissionTemperatureAlert.Level.NORMAL).gearText);
        assertEquals("M2", build(
                state(40, GearSelector.MANUAL, 2),
                SpeedDisplayMode.REAL,
                80,
                TransmissionTemperatureAlert.Level.NORMAL).gearText);
        assertEquals(DASH, build(
                state(0, GearSelector.UNKNOWN, 0),
                SpeedDisplayMode.REAL,
                80,
                TransmissionTemperatureAlert.Level.NORMAL).gearText);
    }

    @Test
    public void fuelProjectsOntoTheSelectedTankSize() {
        ClusterWidgetContent content = build(
                state(0, GearSelector.PARK, 0),
                SpeedDisplayMode.REAL,
                40,
                TransmissionTemperatureAlert.Level.NORMAL);
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
                80,
                TransmissionTemperatureAlert.Level.NORMAL);
        assertEquals("10 L", content.fuelLitersText);
        assertEquals("13", content.fuelPercentText);
        assertTrue(content.fuelLow);
    }

    @Test
    public void atfShowsDashAfterTheFifteenSecondWindow() {
        ClusterState fresh = new ClusterState(
                0, 800, 0, GearSelector.PARK, 40, 75.4f,
                40.0f, 300, 0.0, 0.0f, 0.0f,
                2.3f, 2.3f, 2.3f, 2.3f,
                Float.NaN, 8.5f, Float.NaN, 12.4f, 18.0f, 0.0f,
                0f, 0f, 0f, 0f, 0f,
                0L, 0L, 0L, 0L, NOW_MS - 1_000L, "ECO");
        assertEquals("75 °C", build(
                fresh,
                SpeedDisplayMode.REAL,
                80,
                TransmissionTemperatureAlert.Level.ELEVATED).atfText);
        assertEquals(
                TransmissionTemperatureAlert.Level.ELEVATED,
                build(
                        fresh,
                        SpeedDisplayMode.REAL,
                        80,
                        TransmissionTemperatureAlert.Level.ELEVATED).atfLevel);

        ClusterState stale = new ClusterState(
                0, 800, 0, GearSelector.PARK, 40, 75.4f,
                40.0f, 300, 0.0, 0.0f, 0.0f,
                2.3f, 2.3f, 2.3f, 2.3f,
                Float.NaN, 8.5f, Float.NaN, 12.4f, 18.0f, 0.0f,
                0f, 0f, 0f, 0f, 0f,
                0L, 0L, 0L, 0L, NOW_MS - 20_000L, "ECO");
        assertEquals(DASH, build(
                stale,
                SpeedDisplayMode.REAL,
                80,
                TransmissionTemperatureAlert.Level.NORMAL).atfText);
    }

    @Test
    public void sensorRangesAndPlaceholdersMatchTheClusterPolicy() {
        ClusterWidgetContent content = build(
                state(60, GearSelector.DRIVE, 3),
                SpeedDisplayMode.REAL,
                80,
                TransmissionTemperatureAlert.Level.NORMAL);
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
                80,
                TransmissionTemperatureAlert.Level.NORMAL).instantConsumptionText);
        assertEquals(DASH, build(
                state(60, GearSelector.DRIVE, 3),
                SpeedDisplayMode.REAL,
                80,
                TransmissionTemperatureAlert.Level.NORMAL).averageConsumptionText);
    }

    @Test
    public void emptySnapshotRendersPlaceholdersWithoutCrashing() {
        ClusterWidgetContent content = ClusterWidgetFormatter.build(
                ClusterState.empty(),
                null,
                FuelTankCapacity.DEFAULT_LITERS,
                null,
                NOW_MS);
        assertEquals("0", content.speedText);
        assertEquals(DASH, content.gearText);
        assertEquals(DASH, content.instantConsumptionText);
        assertEquals(
                TransmissionTemperatureAlert.Level.NORMAL,
                content.atfLevel);
    }

    @Test
    public void equalSnapshotsProduceEqualContent() {
        ClusterState first = state(60, GearSelector.DRIVE, 3);
        ClusterWidgetContent one = build(
                first,
                SpeedDisplayMode.REAL,
                80,
                TransmissionTemperatureAlert.Level.NORMAL);
        ClusterWidgetContent two = build(
                first,
                SpeedDisplayMode.REAL,
                80,
                TransmissionTemperatureAlert.Level.NORMAL);
        assertEquals(one, two);
        assertEquals(one.hashCode(), two.hashCode());

        ClusterWidgetContent otherSpeed = build(
                state(61, GearSelector.DRIVE, 3),
                SpeedDisplayMode.REAL,
                80,
                TransmissionTemperatureAlert.Level.NORMAL);
        assertFalse(one.equals(otherSpeed));
    }
}
