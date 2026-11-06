package net.adminrunet.h9cluster;

import java.util.Objects;

/**
 * Immutable formatted payload pushed into the main-display widgets.
 *
 * <p>Every field is the exact string a TextView receives. Instances are
 * compared for equality before a RemoteViews update so an idling vehicle
 * does not produce one Binder transaction per second for nothing.</p>
 */
public final class ClusterWidgetContent {
    public final String speedText;
    public final String gearText;
    public final String rpmText;

    public final String fuelLitersText;
    public final String fuelPercentText;
    public final int fuelBarPercent;
    public final boolean fuelLow;

    public final String rangeText;
    public final String coolantText;
    public final String atfText;
    public final TransmissionTemperatureAlert.Level atfLevel;
    public final String outsideText;
    public final String voltageText;

    public final String instantConsumptionText;
    public final String averageConsumptionText;

    public final String odometerText;
    public final String tripText;
    public final String dayText;

    public final String pressureFrontLeftText;
    public final String pressureFrontRightText;
    public final String pressureRearLeftText;
    public final String pressureRearRightText;

    public final String steeringText;
    public final String torqueText;
    public final String driveModeText;
    public final String wheelSpeedsText;

    public ClusterWidgetContent(
            String speedText,
            String gearText,
            String rpmText,
            String fuelLitersText,
            String fuelPercentText,
            int fuelBarPercent,
            boolean fuelLow,
            String rangeText,
            String coolantText,
            String atfText,
            TransmissionTemperatureAlert.Level atfLevel,
            String outsideText,
            String voltageText,
            String instantConsumptionText,
            String averageConsumptionText,
            String odometerText,
            String tripText,
            String dayText,
            String pressureFrontLeftText,
            String pressureFrontRightText,
            String pressureRearLeftText,
            String pressureRearRightText,
            String steeringText,
            String torqueText,
            String driveModeText,
            String wheelSpeedsText) {
        this.speedText = speedText;
        this.gearText = gearText;
        this.rpmText = rpmText;
        this.fuelLitersText = fuelLitersText;
        this.fuelPercentText = fuelPercentText;
        this.fuelBarPercent = fuelBarPercent;
        this.fuelLow = fuelLow;
        this.rangeText = rangeText;
        this.coolantText = coolantText;
        this.atfText = atfText;
        this.atfLevel = atfLevel == null
                ? TransmissionTemperatureAlert.Level.NORMAL
                : atfLevel;
        this.outsideText = outsideText;
        this.voltageText = voltageText;
        this.instantConsumptionText = instantConsumptionText;
        this.averageConsumptionText = averageConsumptionText;
        this.odometerText = odometerText;
        this.tripText = tripText;
        this.dayText = dayText;
        this.pressureFrontLeftText = pressureFrontLeftText;
        this.pressureFrontRightText = pressureFrontRightText;
        this.pressureRearLeftText = pressureRearLeftText;
        this.pressureRearRightText = pressureRearRightText;
        this.steeringText = steeringText;
        this.torqueText = torqueText;
        this.driveModeText = driveModeText;
        this.wheelSpeedsText = wheelSpeedsText;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ClusterWidgetContent)) {
            return false;
        }
        ClusterWidgetContent that = (ClusterWidgetContent) other;
        return fuelBarPercent == that.fuelBarPercent
                && fuelLow == that.fuelLow
                && Objects.equals(speedText, that.speedText)
                && Objects.equals(gearText, that.gearText)
                && Objects.equals(rpmText, that.rpmText)
                && Objects.equals(fuelLitersText, that.fuelLitersText)
                && Objects.equals(fuelPercentText, that.fuelPercentText)
                && Objects.equals(rangeText, that.rangeText)
                && Objects.equals(coolantText, that.coolantText)
                && Objects.equals(atfText, that.atfText)
                && atfLevel == that.atfLevel
                && Objects.equals(outsideText, that.outsideText)
                && Objects.equals(voltageText, that.voltageText)
                && Objects.equals(
                        instantConsumptionText,
                        that.instantConsumptionText)
                && Objects.equals(
                        averageConsumptionText,
                        that.averageConsumptionText)
                && Objects.equals(odometerText, that.odometerText)
                && Objects.equals(tripText, that.tripText)
                && Objects.equals(dayText, that.dayText)
                && Objects.equals(
                        pressureFrontLeftText,
                        that.pressureFrontLeftText)
                && Objects.equals(
                        pressureFrontRightText,
                        that.pressureFrontRightText)
                && Objects.equals(
                        pressureRearLeftText,
                        that.pressureRearLeftText)
                && Objects.equals(
                        pressureRearRightText,
                        that.pressureRearRightText)
                && Objects.equals(steeringText, that.steeringText)
                && Objects.equals(torqueText, that.torqueText)
                && Objects.equals(driveModeText, that.driveModeText)
                && Objects.equals(wheelSpeedsText, that.wheelSpeedsText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                speedText,
                gearText,
                rpmText,
                fuelLitersText,
                fuelPercentText,
                fuelBarPercent,
                fuelLow,
                rangeText,
                coolantText,
                atfText,
                atfLevel,
                outsideText,
                voltageText,
                instantConsumptionText,
                averageConsumptionText,
                odometerText,
                tripText,
                dayText,
                pressureFrontLeftText,
                pressureFrontRightText,
                pressureRearLeftText,
                pressureRearRightText,
                steeringText,
                torqueText,
                driveModeText,
                wheelSpeedsText);
    }
}
