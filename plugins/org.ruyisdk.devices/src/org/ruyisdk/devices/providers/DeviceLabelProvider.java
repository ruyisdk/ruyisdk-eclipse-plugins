package org.ruyisdk.devices.providers;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.ruyisdk.devices.model.Device;

public class DeviceLabelProvider extends LabelProvider implements ITableLabelProvider {
  @Override
  public String getColumnText(Object element, int columnIndex) {
    Device device = (Device) element;
    switch (columnIndex) {
      case 0:
        return device.getName();
      case 1:
        return device.getChip();
      case 2:
        return device.getVendor();
      case 3:
        return device.getVersion();
      case 4:
        return device.isDefault() ? "Default" : "";
      default:
        return "";
    }
  }

  @Override
  public Image getColumnImage(Object element, int columnIndex) {
    return null;
  }
}
