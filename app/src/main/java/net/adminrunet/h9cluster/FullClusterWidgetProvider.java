package net.adminrunet.h9cluster;

/**
 * Full 4×4 widget with every supported reading: speed, gear, RPM, drive mode,
 * fuel with tank percentage and range, coolant, battery charge level, outside
 * temperature, instant and average consumption, odometer, trip and day
 * meters, four tire pressures, battery voltage, flywheel torque, steering
 * angle and wheel speeds.
 */
public final class FullClusterWidgetProvider extends ClusterWidgetProvider {
    @Override
    Class<? extends ClusterWidgetProvider> providerClass() {
        return FullClusterWidgetProvider.class;
    }
}
