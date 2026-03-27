package org.ruyisdk.packages.viewmodel;

import java.util.List;
import org.ruyisdk.ruyi.model.DeviceEntityInfo;

/**
 * ViewModel for the device-selection dialog.
 *
 * <p>
 * Receives an immutable snapshot of the device list and tracks the user's selection. The dialog
 * reads properties from this ViewModel and forwards user-selection changes back into it; it never
 * touches the main {@link PackageExplorerViewModel} directly.
 */
public class DeviceSelectionViewModel extends BaseViewModel {

    public static final String PROP_SELECTED_DEVICE = "selectedDevice";
    public static final String PROP_STATUS_TEXT = "statusText";

    private final List<DeviceEntityInfo> devices;
    private final String errorMessage;
    private DeviceEntityInfo selectedDevice;

    /**
     * Creates a new device-selection ViewModel.
     *
     * <p>
     * This ViewModel is always used from the UI thread (dialog interaction), so it uses a synchronous
     * executor ({@code Runnable::run}).
     *
     * @param devices the cached device list (snapshot)
     * @param currentDevice the device currently chosen in the main view (may be {@code null})
     * @param errorMessage the error message from the last device-load attempt (may be {@code null})
     */
    public DeviceSelectionViewModel(List<DeviceEntityInfo> devices, DeviceEntityInfo currentDevice,
                    String errorMessage) {
        super(Runnable::run);
        this.devices = devices;
        this.selectedDevice = currentDevice;
        this.errorMessage = errorMessage;
    }

    public List<DeviceEntityInfo> getDevices() {
        return devices;
    }

    public DeviceEntityInfo getSelectedDevice() {
        return selectedDevice;
    }

    /** Update the user's selection and recompute the status text. */
    public void setSelectedDevice(DeviceEntityInfo device) {
        final var old = this.selectedDevice;
        this.selectedDevice = device;
        firePropertyChange(PROP_SELECTED_DEVICE, old, device);
        firePropertyChange(PROP_STATUS_TEXT, null, getStatusText());
    }

    /** Clear the selection. */
    public void clearSelection() {
        setSelectedDevice(null);
    }

    /** Whether devices are available. */
    public boolean hasDevices() {
        return !devices.isEmpty();
    }

    /** Build the status text from the current state. */
    public String getStatusText() {
        final var sb = new StringBuilder();
        if (errorMessage != null) {
            sb.append("Error loading devices: ").append(errorMessage);
        } else if (devices.isEmpty()) {
            sb.append("0 device(s) found. Please check your Ruyi installation.");
        } else {
            sb.append(devices.size()).append(" device(s) found.");
            if (selectedDevice != null) {
                sb.append("\nSelected: ").append(selectedDevice.getLabel());
            }
        }
        return sb.toString();
    }
}
