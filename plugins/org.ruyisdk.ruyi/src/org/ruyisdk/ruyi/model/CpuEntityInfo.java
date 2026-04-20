package org.ruyisdk.ruyi.model;

import java.util.List;

/**
 * Entity info for a CPU/SoC (entity type {@code "cpu"}).
 *
 * <p>
 * Represents a chip such as "SpacemiT Key Stone K1" or "WCH CH32V307". CPU entities link to
 * microarchitecture (uarch) entities via {@link #getRelatedRefs()} and are referenced back by
 * device-variant entities via {@link #getReverseRefs()}.
 */
public class CpuEntityInfo extends EntityInfo {

    /**
     * Constructs a {@code CpuEntityInfo}.
     *
     * @param entityId the CPU entity id, e.g. {@code "spacemit-k1"}
     * @param displayName the human-readable name, e.g. {@code "SpacemiT Key Stone K1"}
     * @param relatedRefs references to related uarch entities
     * @param reverseRefs reverse references from device-variant entities
     */
    public CpuEntityInfo(String entityId, String displayName, List<String> relatedRefs,
            List<String> reverseRefs) {
        super("cpu", entityId, displayName, relatedRefs, reverseRefs);
    }
}
