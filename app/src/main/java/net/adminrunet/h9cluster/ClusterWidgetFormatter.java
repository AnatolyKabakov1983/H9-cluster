package net.adminrunet.h9cluster;

import net.adminrunet.h9cluster.skins.FuelConsumptionFormatter;
import net.adminrunet.h9cluster.skins.SpeedDisplayMode;

import java.util.Locale;

/**
 * Turns one {@link ClusterState} snapshot into the widget text payload.
 *
 * <p>The class is intentionally free of Android imports: the same formatting
 * policy is covered by plain JUnit tests and shared by the service ticker and
 * the widget providers. Units and placeholder glyphs follow the cluster
 * conventions: an em dash for an unavailable reading, Latin units like
 * {@code km}, {@code L} and {@code V} exactly as the skins draw them.</p>
 */
public final class ClusterWidgetFormatter {
    /** Matches the 15-second ATF card policy shared by all skins. */
    public static final long TRANSMISSION_TEMPERATURE_FRESH_MS = 15_000L;

    /** Fuel percentage at or below which the liters readout turns red. */
    public static final int FUEL_LOW_PERCENT = 15;

    private static final String DASH = "\u2014";
    private static final int COOLANT_MIN_C = 0;
    private static final int COOLANT_MAX_C = 130;
    private static final int OUTSIDE_MIN_C = -60;
    private static final int OUTSIDE_MAX_C = 60;
    private static final float MAX_FLYWHEEL_TORQUE = 2000.0f;

    private ClusterWidgetFormatter() {
    }

    /**
     * Formats the complete widget payload.
     *
     * @param state raw source snapshot; fuel is projected onto the selected
     *              tank size here so every caller sees identical liters
     * @param mode  shared speedometer indication mode
     * @param atfLevel warning band of the transmission temperature; the
     *                 hysteresis lives with the owning service
     * @param nowMs elapsedRealtime() reference for staleness decisions
     */
    public static ClusterWidgetContent build(
            ClusterState state,
            SpeedDisplayMode mode,
            int fuelTankCapacityLiters,
            TransmissionTemperatureAlert.Level atfLevel,
            long nowMs) {
        ClusterState normalized = state == null
                ? ClusterState.empty()
                : state.withFuelTankCapacity(
                        FuelTankCapacity.normalize(fuelTankCapacityLiters));
        SpeedDisplayMode safeMode = mode == null
                ? SpeedDisplayMode.REAL
                : mode;
        return new ClusterWidgetContent(
                speed(normalized, safeMode, nowMs),
                gear(normalized),
                rpm(normalized),
                fuelLiters(normalized),
                fuelPercent(normalized),
                fuelBarPercent(normalized),
                fuelLow(normalized),
                range(normalized),
                coolant(normalized),
                transmissionTemperature(normalized, nowMs),
                atfLevel == null
                        ? TransmissionTemperatureAlert.Level.NORMAL
                        : atfLevel,
                outsideTemperature(normalized),
                voltage(normalized),
                FuelConsumptionFormatter.instant(normalized),
                FuelConsumptionFormatter.average(
                        normalized.journeyAverageFuelConsumption),
                odometer(normalized),
                trip(normalized.tripKm),
                trip(normalized.dayKm),
                pressure(normalized.tyreFrontLeftBar),
                pressure(normalized.tyreFrontRightBar),
                pressure(normalized.tyreRearLeftBar),
                pressure(normalized.tyreRearRightBar),
                steering(normalized),
                torque(normalized),
                driveMode(normalized),
                wheelSpeeds(normalized));
    }

    /** Same freshness rule the service applies to the warning band. */
    public static boolean hasFreshTransmissionTemperature(
            ClusterState state,
            long nowMs) {
        return state.hasTransmissionTemperature()
                && state.transmissionTemperatureUpdatedAtMs > 0L
                && nowMs >= state.transmissionTemperatureUpdatedAtMs
                && nowMs - state.transmissionTemperatureUpdatedAtMs
                        <= TRANSMISSION_TEMPERATURE_FRESH_MS;
    }

    private static String speed(
            ClusterState state,
            SpeedDisplayMode mode,
            long nowMs) {
        int displayKph = StationarySpeedDisplayFallback.resolveSpeedKph(
                state,
                nowMs);
        if (displayKph < 0) {
            return DASH;
        }
        return String.valueOf(Math.round(mode.indicatedKph(displayKph)));
    }

    private static String gear(ClusterState state) {
        String label = GearSelector.label(
                state.gearSelector,
                state.currentGear);
        return label == null || label.isEmpty() ? DASH : label;
    }

    private static String rpm(ClusterState state) {
        return state.rpm >= 0 ? Integer.toString(state.rpm) : DASH;
    }

    private static String fuelLiters(ClusterState state) {
        return state.fuelLiters >= 0.0f
                ? Math.round(state.fuelLiters) + " L"
                : DASH;
    }

    private static int fuelBarPercent(ClusterState state) {
        int percent = fuelPercentValue(state);
        return percent < 0 ? 0 : percent;
    }

    private static String fuelPercent(ClusterState state) {
        int percent = fuelPercentValue(state);
        return percent < 0 ? DASH : Integer.toString(percent);
    }

    private static boolean fuelLow(ClusterState state) {
        int percent = fuelPercentValue(state);
        return percent >= 0 && percent <= FUEL_LOW_PERCENT;
    }

    /** -1 when fuel or tank capacity is unavailable. */
    private static int fuelPercentValue(ClusterState state) {
        if (state.fuelLiters < 0.0f || state.fuelTankCapacityLiters <= 0) {
            return -1;
        }
        int percent = Math.round(
                state.fuelLiters * 100.0f / state.fuelTankCapacityLiters);
        return Math.max(0, Math.min(100, percent));
    }

    private static String range(ClusterState state) {
        return state.rangeKm >= 0 ? state.rangeKm + " km" : DASH;
    }

    private static String coolant(ClusterState state) {
        int temperatureC = state.coolantC;
        return temperatureC >= COOLANT_MIN_C && temperatureC <= COOLANT_MAX_C
                ? temperatureC + " °C"
                : DASH;
    }

    private static String transmissionTemperature(
            ClusterState state,
            long nowMs) {
        return hasFreshTransmissionTemperature(state, nowMs)
                ? Math.round(state.transmissionTemperatureC) + " °C"
                : DASH;
    }

    private static String outsideTemperature(ClusterState state) {
        float temperatureC = state.outsideTemperatureC;
        return Float.isFinite(temperatureC)
                && temperatureC >= OUTSIDE_MIN_C
                && temperatureC <= OUTSIDE_MAX_C
                ? Math.round(temperatureC) + " °C"
                : DASH;
    }

    private static String voltage(ClusterState state) {
        return Float.isFinite(state.voltage) && state.voltage > 0.0f
                ? String.format(Locale.US, "%.1f V", state.voltage)
                : DASH;
    }

    private static String odometer(ClusterState state) {
        return state.odometerKm >= 0.0
                ? String.format(Locale.US, "%.0f km", state.odometerKm)
                : DASH;
    }

    private static String trip(float kilometers) {
        return Float.isFinite(kilometers) && kilometers >= 0.0f
                ? String.format(Locale.US, "%.1f km", kilometers)
                : DASH;
    }

    private static String pressure(float pressureBar) {
        return Float.isFinite(pressureBar) && pressureBar > 0.0f
                ? String.format(Locale.US, "%.1f", pressureBar)
                : DASH;
    }

    private static String steering(ClusterState state) {
        return Float.isFinite(state.steeringAngleDeg)
                ? String.format(Locale.US, "%.0f°", state.steeringAngleDeg)
                : DASH;
    }

    private static String torque(ClusterState state) {
        return Float.isFinite(state.engineFlywheelTorque)
                && state.engineFlywheelTorque >= 0.0f
                && state.engineFlywheelTorque <= MAX_FLYWHEEL_TORQUE
                ? String.format(
                        Locale.US,
                        "%.0f Nm",
                        state.engineFlywheelTorque)
                : DASH;
    }

    private static String driveMode(ClusterState state) {
        String mode = state.driveMode;
        return mode == null || mode.trim().isEmpty() ? DASH : mode.trim();
    }

    private static String wheelSpeeds(ClusterState state) {
        return wheel(state.wheelFrontLeftKph)
                + " / " + wheel(state.wheelFrontRightKph)
                + " / " + wheel(state.wheelRearLeftKph)
                + " / " + wheel(state.wheelRearRightKph);
    }

    private static String wheel(float speedKph) {
        return Float.isFinite(speedKph) && speedKph >= 0.0f
                ? String.format(Locale.US, "%.0f", speedKph)
                : DASH;
    }
}
