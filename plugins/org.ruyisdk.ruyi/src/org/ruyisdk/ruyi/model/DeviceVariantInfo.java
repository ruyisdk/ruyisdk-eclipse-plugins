package org.ruyisdk.ruyi.model;

import java.util.List;

/**
 * Entity info for a device variant (entity type {@code "device-variant"}).
 *
 * <p>
 * Represents a specific variant/configuration of a device, such as a RAM size or chip SKU. Device
 * variant entities link to CPU entities via {@link #getRelatedRefs()} and are referenced back by
 * device entities and image-combo entities via {@link #getReverseRefs()}.
 */
public class DeviceVariantInfo extends EntityInfo {

    private final String variantId;
    private final String variantName;

    /**
     * Constructs a {@code DeviceVariantInfo}.
     *
     * @param entityId the full entity id, e.g. {@code "sipeed-lc4a@8g"}
     * @param displayName the display name, may be {@code null}
     * @param relatedRefs references to related CPU entities
     * @param reverseRefs reverse references (devices and image-combos)
     * @param variantId the short variant id, e.g. {@code "8g"}
     * @param variantName the human-readable variant name, e.g. {@code "8G RAM"}
     */
    public DeviceVariantInfo(String entityId, String displayName, List<String> relatedRefs,
            List<String> reverseRefs, String variantId, String variantName) {
        super("device-variant", entityId, displayName, relatedRefs, reverseRefs);
        this.variantId = variantId;
        this.variantName = variantName;
    }

    /**
     * Returns the short variant id, e.g. {@code "8g"}, {@code "64m"}, {@code "sd"},
     * {@code "generic"}.
     */
    public String getVariantId() {
        return variantId;
    }

    /**
     * Returns the human-readable variant name, e.g. {@code "8G RAM"}, {@code "CH32V203F6P6"}.
     */
    public String getVariantName() {
        return variantName;
    }

    @Override
    public String getLabel() {
        if (variantName != null) {
            return variantName;
        }
        return super.getLabel();
    }
}
