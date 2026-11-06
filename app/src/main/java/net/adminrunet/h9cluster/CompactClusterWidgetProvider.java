package net.adminrunet.h9cluster;

/** Compact 4×2 widget: speed, gear, RPM, fuel, range, coolant, ATF, voltage. */
public final class CompactClusterWidgetProvider extends ClusterWidgetProvider {
    @Override
    Class<? extends ClusterWidgetProvider> providerClass() {
        return CompactClusterWidgetProvider.class;
    }
}
