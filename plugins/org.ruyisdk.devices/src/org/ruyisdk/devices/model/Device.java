package org.ruyisdk.devices.model;

/**
 * Represents a RISC-V development device configuration.
 */
public class Device {
    private String name;
    private String chip;
    private String vendor;
    private String version;
    private boolean isDefault;

    /**
     * Constructs a new Device with specified parameters.
     *
     * @param name device name
     * @param chip chip model
     * @param vendor vendor name
     * @param version firmware version
     * @param isDefault whether this is the default device
     */
    public Device(String name, String chip, String vendor, String version, boolean isDefault) {
        this.name = name;
        this.chip = chip;
        this.vendor = vendor;
        this.version = version;
        this.isDefault = isDefault;
    }

    /**
     * Gets the device name.
     *
     * @return the device name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the device name.
     *
     * @param name the device name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the chip model.
     *
     * @return the chip model
     */
    public String getChip() {
        return chip;
    }

    /**
     * Sets the chip model.
     *
     * @param chip the chip model
     */
    public void setChip(String chip) {
        this.chip = chip;
    }

    /**
     * Gets the vendor name.
     *
     * @return the vendor name
     */
    public String getVendor() {
        return vendor;
    }

    /**
     * Sets the vendor name.
     *
     * @param vendor the vendor name
     */
    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    /**
     * Gets the firmware version.
     *
     * @return the firmware version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the firmware version.
     *
     * @param version the firmware version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Checks if this is the default device.
     *
     * @return true if default, false otherwise
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * Sets whether this is the default device.
     *
     * @param isDefault true to set as default
     */
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }



    // @Override
    // public boolean equals(Object obj) {
    // if (this == obj) return true;
    // if (obj == null || getClass() != obj.getClass()) return false;
    // Board board = (Board) obj;
    // return name.equals(board.name) &&
    // chip.equals(board.chip) &&
    // vendor.equals(board.vendor) &&
    // version.equals(board.version);
    // }
}
