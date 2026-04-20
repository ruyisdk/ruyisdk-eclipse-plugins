package org.ruyisdk.packages.model;

import java.util.Comparator;
import java.util.List;
import org.ruyisdk.ruyi.model.DeviceEntityInfo;
import org.ruyisdk.ruyi.model.EntityInfoParser;
import org.ruyisdk.ruyi.services.RuyiCli;

/**
 * Synchronous data-access object for device entities. All methods block until the result is
 * available; the caller is responsible for running them off the UI thread.
 */
public class DeviceList {

    /**
     * Loads all device entities from the Ruyi CLI, sorted case-insensitively by label.
     *
     * @return sorted list of device entities
     */
    public static List<DeviceEntityInfo> loadDevices() {
        final var output = RuyiCli.listEntitiesByType("device");
        final var devices = EntityInfoParser.parseDeviceEntities(output);
        devices.sort(
                Comparator.comparing(DeviceEntityInfo::getLabel, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(devices);
    }
}
