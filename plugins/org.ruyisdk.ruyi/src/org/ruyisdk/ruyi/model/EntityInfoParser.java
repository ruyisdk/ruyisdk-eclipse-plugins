package org.ruyisdk.ruyi.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Parses concatenated JSON entity list output into typed {@link EntityInfo} model objects.
 */
public final class EntityInfoParser {

    private EntityInfoParser() {
        // utility class
    }

    /**
     * Parses raw CLI output into a list of {@link EntityInfo} objects, dispatching to the
     * appropriate subclass based on {@code entity_type}.
     *
     * @param rawOutput the raw string output from the CLI command
     * @return list of parsed entity info objects; never {@code null}
     */
    public static List<EntityInfo> parseAll(String rawOutput) {
        final var objects = parseConcatenatedJsonObjects(rawOutput);
        final var result = new ArrayList<EntityInfo>();
        for (final var obj : objects) {
            final var ty = obj.optString("ty", "");
            if (!"entitylistoutput-v1".equals(ty)) {
                continue;
            }
            final var entity = parseEntity(obj);
            if (entity != null) {
                result.add(entity);
            }
        }
        return result;
    }

    /**
     * Parses raw CLI output and returns only {@link DeviceEntityInfo} entries.
     *
     * @param rawOutput the raw string output from the CLI command
     * @return list of device entities; never {@code null}
     */
    public static List<DeviceEntityInfo> parseDeviceEntities(String rawOutput) {
        return filterByType(parseAll(rawOutput), DeviceEntityInfo.class);
    }

    /**
     * Parses raw CLI output and returns only {@link DeviceVariantInfo} entries.
     *
     * @param rawOutput the raw string output from the CLI command
     * @return list of device variant entities; never {@code null}
     */
    public static List<DeviceVariantInfo> parseDeviceVariants(String rawOutput) {
        return filterByType(parseAll(rawOutput), DeviceVariantInfo.class);
    }

    /**
     * Parses raw CLI output and returns only {@link CpuEntityInfo} entries.
     *
     * @param rawOutput the raw string output from the CLI command
     * @return list of CPU entities; never {@code null}
     */
    public static List<CpuEntityInfo> parseCpuEntities(String rawOutput) {
        return filterByType(parseAll(rawOutput), CpuEntityInfo.class);
    }

    /**
     * Parses raw CLI output and returns only {@link UarchEntityInfo} entries.
     *
     * @param rawOutput the raw string output from the CLI command
     * @return list of uarch entities; never {@code null}
     */
    public static List<UarchEntityInfo> parseUarchEntities(String rawOutput) {
        return filterByType(parseAll(rawOutput), UarchEntityInfo.class);
    }

    private static EntityInfo parseEntity(JSONObject obj) {
        final var entityType = obj.optString("entity_type", "");
        final var entityId = obj.optString("entity_id", "");
        final var displayName = obj.optString("display_name", null);
        final var relatedRefs = toStringList(obj.optJSONArray("related_refs"));
        final var reverseRefs = toStringList(obj.optJSONArray("reverse_refs"));
        final var data = obj.optJSONObject("data");

        return switch (entityType) {
            case "device" -> new DeviceEntityInfo(entityId, displayName, relatedRefs, reverseRefs);
            case "device-variant" -> parseDeviceVariant(entityId, displayName, relatedRefs,
                    reverseRefs, data);
            case "cpu" -> new CpuEntityInfo(entityId, displayName, relatedRefs, reverseRefs);
            case "uarch" -> parseUarch(entityId, displayName, relatedRefs, reverseRefs, data);
            default -> new EntityInfo(entityType, entityId, displayName, relatedRefs, reverseRefs);
        };
    }

    private static DeviceVariantInfo parseDeviceVariant(String entityId, String displayName,
            List<String> relatedRefs, List<String> reverseRefs, JSONObject data) {
        String variantId = null;
        String variantName = null;
        if (data != null) {
            final var dvObj = data.optJSONObject("device-variant");
            if (dvObj != null) {
                variantId = dvObj.optString("id", null);
                variantName = dvObj.optString("variant_name", null);
            }
        }
        return new DeviceVariantInfo(entityId, displayName, relatedRefs, reverseRefs, variantId,
                variantName);
    }

    private static UarchEntityInfo parseUarch(String entityId, String displayName,
            List<String> relatedRefs, List<String> reverseRefs, JSONObject data) {
        String arch = null;
        String isa = null;
        if (data != null) {
            final var uarchObj = data.optJSONObject("uarch");
            if (uarchObj != null) {
                arch = uarchObj.optString("arch", null);
                final var riscvObj = uarchObj.optJSONObject("riscv");
                if (riscvObj != null) {
                    isa = riscvObj.optString("isa", null);
                }
            }
        }
        return new UarchEntityInfo(entityId, displayName, relatedRefs, reverseRefs, arch, isa);
    }

    private static <T> List<T> filterByType(List<EntityInfo> entities, Class<T> type) {
        final var result = new ArrayList<T>();
        for (final var entity : entities) {
            if (type.isInstance(entity)) {
                result.add(type.cast(entity));
            }
        }
        return result;
    }

    private static List<String> toStringList(JSONArray arr) {
        if (arr == null) {
            return Collections.emptyList();
        }
        final var list = new ArrayList<String>(arr.length());
        for (int i = 0; i < arr.length(); i++) {
            final var s = arr.optString(i, null);
            if (s != null) {
                list.add(s);
            }
        }
        return list;
    }

    /**
     * Parses a string of concatenated JSON objects into individual {@link JSONObject} instances.
     * Handles both raw concatenated objects and arrays.
     */
    private static List<JSONObject> parseConcatenatedJsonObjects(String input) {
        final var out = new ArrayList<JSONObject>();
        if (input == null || input.isBlank()) {
            return out;
        }
        final var t = new JSONTokener(input);
        while (true) {
            final var c = t.nextClean();
            if (c == 0) {
                break;
            }
            t.back();
            final var v = t.nextValue();
            if (v instanceof JSONObject) {
                out.add((JSONObject) v);
            } else if (v instanceof JSONArray arr) {
                for (int i = 0; i < arr.length(); i++) {
                    final var el = arr.get(i);
                    if (el instanceof JSONObject) {
                        out.add((JSONObject) el);
                    }
                }
            }
        }
        return out;
    }
}
