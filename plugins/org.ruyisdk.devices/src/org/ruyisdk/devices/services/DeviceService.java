package org.ruyisdk.devices.services;

import java.util.ArrayList;
import java.util.List;
import org.ruyisdk.devices.model.Device;
import org.ruyisdk.devices.services.PropertiesService;

/**
 * Service for managing device configurations.
 */
public class DeviceService {
    private List<Device> devices = new ArrayList<>();
    private PropertiesService propertiesService = new PropertiesService();

    /**
     * Constructs a new DeviceService instance.
     */
    public DeviceService() {
        devices = propertiesService.loadDevices();
    }

    /**
     * Retrieves all configured devices.
     *
     * @return list of devices
     */
    public List<Device> getDevices() {
        return devices;
    }

    /**
     * Adds a new device to the configuration.
     *
     * @param device the device to add
     */
    public void addDevice(Device device) {
        devices.add(device);
    }

    /**
     * Updates an existing device configuration.
     *
     * @param oldDevice the device to update
     * @param newDevice the new device data
     */
    public void updateDevice(Device oldDevice, Device newDevice) {
        int index = devices.indexOf(oldDevice);
        if (index != -1) {
            devices.set(index, newDevice);
        }
    }

    /**
     * Deletes a device from the configuration.
     *
     * @param device the device to delete
     */
    public void deleteDevice(Device device) {
        devices.remove(device);
    }

    /**
     * Sets a device as the default.
     *
     * @param device the device to set as default
     */
    public void setDefaultDevice(Device device) {
        for (Device b : devices) {
            b.setDefault(false);
        }
        device.setDefault(true);
    }

    /**
     * Persists all devices to storage.
     */
    public void saveDevices() {
        propertiesService.saveDevices(devices);
    }
}
