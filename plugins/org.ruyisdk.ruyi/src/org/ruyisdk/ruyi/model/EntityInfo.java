package org.ruyisdk.ruyi.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base model for an entity list output entry ({@code entitylistoutput-v1}).
 *
 * <p>
 * Each entity shares a common envelope with the entity type, entity id, optional display name, and
 * lists of related/reverse references to other entities.
 *
 * <p>
 * Subclasses add type-specific fields extracted from the nested {@code data} object.
 */
public class EntityInfo {

    private final String entityType;
    private final String entityId;
    private final String displayName;
    private final List<String> relatedRefs;
    private final List<String> reverseRefs;

    /**
     * Constructs an {@code EntityInfo}.
     *
     * @param entityType the entity type, e.g. {@code "device"}, {@code "cpu"}, {@code "uarch"}
     * @param entityId the entity id, e.g. {@code "milkv-duo"}, {@code "xuantie-c910"}
     * @param displayName the human-readable display name, may be {@code null}
     * @param relatedRefs list of related entity references, must not be {@code null}
     * @param reverseRefs list of reverse entity references, must not be {@code null}
     */
    public EntityInfo(String entityType, String entityId, String displayName,
            List<String> relatedRefs, List<String> reverseRefs) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.displayName = displayName;
        this.relatedRefs = new ArrayList<>(relatedRefs);
        this.reverseRefs = new ArrayList<>(reverseRefs);
    }

    /**
     * Returns the entity type, e.g. {@code "device"}, {@code "device-variant"}, {@code "cpu"},
     * {@code "uarch"}.
     */
    public String getEntityType() {
        return entityType;
    }

    /**
     * Returns the entity id, e.g. {@code "milkv-duo"}, {@code "starfive-visionfive2"}.
     */
    public String getEntityId() {
        return entityId;
    }

    /**
     * Returns the human-readable display name, or {@code null} if not available.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns an unmodifiable list of related entity references, e.g.
     * {@code ["device-variant:milkv-duo@64m", "device-variant:milkv-duo@256m"]}.
     */
    public List<String> getRelatedRefs() {
        return Collections.unmodifiableList(relatedRefs);
    }

    /**
     * Returns an unmodifiable list of reverse entity references.
     */
    public List<String> getReverseRefs() {
        return Collections.unmodifiableList(reverseRefs);
    }

    /**
     * Returns the display name if available, otherwise the entity id.
     */
    public String getLabel() {
        return displayName != null ? displayName : entityId;
    }
}
