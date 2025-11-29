package org.ruyisdk.devices.providers;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.ruyisdk.devices.model.Device;

/**
 * Label provider for displaying device information in table format.
 */
public class DeviceLabelProvider extends LabelProvider implements ITableLabelProvider {
    @Override
    public String getColumnText(Object element, int columnIndex) {
        Device device = (Device) element;
        return switch (columnIndex) {
            case 0 -> device.getName();
            case 1 -> device.getChip();
            case 2 -> device.getVendor();
            case 3 -> device.getVersion();
            case 4 -> device.isDefault() ? "Default" : "";
            default -> "";
        };
    }

    @Override
    public Image getColumnImage(Object element, int columnIndex) {
        return null;
    }
}
