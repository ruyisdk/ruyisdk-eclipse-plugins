package org.ruyisdk.ruyi.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity info for a device (entity type {@code "device"}).
 *
 * <p>
 * Represents a development board such as "Milk-V Duo" or "StarFive VisionFive2". Device entities
 * link to one or more device-variant entities via {@link #getRelatedRefs()}.
 */
public class DeviceEntityInfo extends EntityInfo {

    /**
     * Constructs a {@code DeviceEntityInfo}.
     *
     * @param entityId the device entity id, e.g. {@code "milkv-duo"}
     * @param displayName the human-readable name, e.g. {@code "Milk-V Duo"}
     * @param relatedRefs references to related device-variant entities
     * @param reverseRefs reverse references from other entities
     */
    public DeviceEntityInfo(String entityId, String displayName, List<String> relatedRefs, List<String> reverseRefs) {
        super("device", entityId, displayName, relatedRefs, reverseRefs);
    }

    /**
     * Extracts variant names from the related refs by taking the part after {@code '@'}.
     *
     * <p>
     * For example, a ref like {@code "device-variant:milkv-duo@256m"} yields {@code "256m"}.
     *
     * @return a list of variant name strings (may be empty)
     */
    public List<String> getVariantNames() {
        final var refs = getRelatedRefs();
        final var variantNames = new ArrayList<String>();
        for (final var ref : refs) {
            final var at = ref.indexOf('@');
            if (at >= 0) {
                variantNames.add(ref.substring(at + 1));
            }
        }
        return variantNames;
    }
}
