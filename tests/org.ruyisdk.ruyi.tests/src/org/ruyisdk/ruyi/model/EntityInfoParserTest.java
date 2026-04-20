package org.ruyisdk.ruyi.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.json.JSONException;
import org.junit.Test;

/**
 * Unit tests for the entity model classes and {@link EntityInfoParser}.
 */
public class EntityInfoParserTest {

    // ------------------------------------------------------------------
    // EntityInfo (base class)
    // ------------------------------------------------------------------

    @Test
    public void entityInfoBasicGetters() {
        EntityInfo info = new EntityInfo("device", "milkv-duo", "Milk-V Duo",
                        List.of("device-variant:milkv-duo@64m"), List.of());

        assertEquals("device", info.getEntityType());
        assertEquals("milkv-duo", info.getEntityId());
        assertEquals("Milk-V Duo", info.getDisplayName());
        assertEquals(1, info.getRelatedRefs().size());
        assertEquals("device-variant:milkv-duo@64m", info.getRelatedRefs().get(0));
        assertTrue(info.getReverseRefs().isEmpty());
    }

    @Test
    public void entityInfoLabelFallsBackToId() {
        EntityInfo withName = new EntityInfo("device", "milkv-duo", "Milk-V Duo",
                        List.of(), List.of());
        assertEquals("Milk-V Duo", withName.getLabel());

        EntityInfo withoutName = new EntityInfo("device", "milkv-duo", null,
                        List.of(), List.of());
        assertEquals("milkv-duo", withoutName.getLabel());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void relatedRefsIsUnmodifiable() {
        EntityInfo info = new EntityInfo("device", "id", null, List.of("a"), List.of());
        info.getRelatedRefs().add("should-fail");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void reverseRefsIsUnmodifiable() {
        EntityInfo info = new EntityInfo("device", "id", null, List.of(), List.of("b"));
        info.getReverseRefs().add("should-fail");
    }

    // ------------------------------------------------------------------
    // DeviceEntityInfo
    // ------------------------------------------------------------------

    @Test
    public void deviceEntityInfoSetsType() {
        DeviceEntityInfo device = new DeviceEntityInfo("milkv-duo", "Milk-V Duo",
                        List.of("device-variant:milkv-duo@64m", "device-variant:milkv-duo@256m"),
                        List.of());

        assertEquals("device", device.getEntityType());
        assertEquals("milkv-duo", device.getEntityId());
        assertEquals("Milk-V Duo", device.getLabel());
        assertEquals(2, device.getRelatedRefs().size());
    }

    // ------------------------------------------------------------------
    // CpuEntityInfo
    // ------------------------------------------------------------------

    @Test
    public void cpuEntityInfoSetsType() {
        CpuEntityInfo cpu = new CpuEntityInfo("spacemit-k1", "SpacemiT Key Stone K1",
                        List.of("uarch:spacemit-x60"),
                        List.of("device-variant:bananapi-bpi-f3@sd"));

        assertEquals("cpu", cpu.getEntityType());
        assertEquals("spacemit-k1", cpu.getEntityId());
        assertEquals("SpacemiT Key Stone K1", cpu.getLabel());
        assertEquals(1, cpu.getRelatedRefs().size());
        assertEquals(1, cpu.getReverseRefs().size());
    }

    // ------------------------------------------------------------------
    // DeviceVariantInfo
    // ------------------------------------------------------------------

    @Test
    public void deviceVariantInfoFields() {
        DeviceVariantInfo dv = new DeviceVariantInfo("sipeed-lc4a@8g", null,
                        List.of("cpu:xuantie-th1520"),
                        List.of("device:sipeed-lc4a", "image-combo:revyos-sipeed-lc4a@8g"),
                        "8g", "8G RAM");

        assertEquals("device-variant", dv.getEntityType());
        assertEquals("sipeed-lc4a@8g", dv.getEntityId());
        assertNull(dv.getDisplayName());
        assertEquals("8g", dv.getVariantId());
        assertEquals("8G RAM", dv.getVariantName());
    }

    @Test
    public void deviceVariantLabelPrefersVariantName() {
        DeviceVariantInfo withName = new DeviceVariantInfo("id@v", null,
                        List.of(), List.of(), "v", "Variant Name");
        assertEquals("Variant Name", withName.getLabel());

        DeviceVariantInfo withoutName = new DeviceVariantInfo("id@v", "DisplayName",
                        List.of(), List.of(), "v", null);
        // Falls through to super.getLabel() which uses displayName
        assertEquals("DisplayName", withoutName.getLabel());

        DeviceVariantInfo bare = new DeviceVariantInfo("id@v", null,
                        List.of(), List.of(), "v", null);
        // Falls through to entityId
        assertEquals("id@v", bare.getLabel());
    }

    // ------------------------------------------------------------------
    // UarchEntityInfo
    // ------------------------------------------------------------------

    @Test
    public void uarchEntityInfoFields() {
        UarchEntityInfo uarch = new UarchEntityInfo("xuantie-c910", "Xuantie C910",
                        List.of("arch:riscv64"),
                        List.of("cpu:xuantie-th1520"),
                        "riscv64",
                        "rv64imafdc_zicntr_zicsr_zifencei");

        assertEquals("uarch", uarch.getEntityType());
        assertEquals("xuantie-c910", uarch.getEntityId());
        assertEquals("riscv64", uarch.getArch());
        assertEquals("rv64imafdc_zicntr_zicsr_zifencei", uarch.getIsa());
        assertEquals("Xuantie C910", uarch.getLabel());
    }

    @Test
    public void uarchEntityInfoNullIsaAllowed() {
        UarchEntityInfo uarch = new UarchEntityInfo("test", "Test", List.of(), List.of(),
                        "riscv64", null);
        assertNull(uarch.getIsa());
    }

    // ------------------------------------------------------------------
    // EntityInfoParser — parseAll
    // ------------------------------------------------------------------

    @Test
    public void parseAllEmptyInput() {
        assertTrue(EntityInfoParser.parseAll("").isEmpty());
        assertTrue(EntityInfoParser.parseAll(null).isEmpty());
        assertTrue(EntityInfoParser.parseAll("   ").isEmpty());
    }

    @Test
    public void parseAllSkipsNonEntityListOutput() {
        String json = """
                        {"ty":"something-else","entity_type":"device","entity_id":"x"}
                        """;
        assertTrue(EntityInfoParser.parseAll(json).isEmpty());
    }

    @Test
    public void parseAllDispatchesToDeviceSubclass() {
        String json = """
                        {"ty":"entitylistoutput-v1","entity_type":"device","entity_id":"milkv-duo","display_name":"Milk-V Duo","related_refs":["device-variant:milkv-duo@64m","device-variant:milkv-duo@256m"],"reverse_refs":[]}
                        """;
        List<EntityInfo> result = EntityInfoParser.parseAll(json);
        assertEquals(1, result.size());
        assertTrue(result.get(0) instanceof DeviceEntityInfo);
        DeviceEntityInfo device = (DeviceEntityInfo) result.get(0);
        assertEquals("milkv-duo", device.getEntityId());
        assertEquals("Milk-V Duo", device.getDisplayName());
        assertEquals(2, device.getRelatedRefs().size());
    }

    @Test
    public void parseAllDispatchesToCpuSubclass() {
        String json = """
                        {"ty":"entitylistoutput-v1","entity_type":"cpu","entity_id":"spacemit-k1","display_name":"SpacemiT Key Stone K1","related_refs":["uarch:spacemit-x60"],"reverse_refs":["device-variant:bananapi-bpi-f3@sd"]}
                        """;
        List<EntityInfo> result = EntityInfoParser.parseAll(json);
        assertEquals(1, result.size());
        assertTrue(result.get(0) instanceof CpuEntityInfo);
    }

    @Test
    public void parseAllDispatchesToDeviceVariantSubclass() {
        String json = """
                        {"ty":"entitylistoutput-v1","entity_type":"device-variant","entity_id":"sipeed-lc4a@8g","display_name":null,"data":{"device-variant":{"id":"8g","variant_name":"8G RAM"}},"related_refs":["cpu:xuantie-th1520"],"reverse_refs":["device:sipeed-lc4a"]}
                        """;
        List<EntityInfo> result = EntityInfoParser.parseAll(json);
        assertEquals(1, result.size());
        assertTrue(result.get(0) instanceof DeviceVariantInfo);
        DeviceVariantInfo dv = (DeviceVariantInfo) result.get(0);
        assertEquals("8g", dv.getVariantId());
        assertEquals("8G RAM", dv.getVariantName());
    }

    @Test
    public void parseAllDispatchesToUarchSubclass() {
        String json = """
                        {"ty":"entitylistoutput-v1","entity_type":"uarch","entity_id":"xuantie-c910","display_name":"Xuantie C910","data":{"uarch":{"id":"xuantie-c910","display_name":"Xuantie C910","arch":"riscv64","riscv":{"isa":"rv64imafdc"}}},"related_refs":["arch:riscv64"],"reverse_refs":["cpu:xuantie-th1520"]}
                        """;
        List<EntityInfo> result = EntityInfoParser.parseAll(json);
        assertEquals(1, result.size());
        assertTrue(result.get(0) instanceof UarchEntityInfo);
        UarchEntityInfo uarch = (UarchEntityInfo) result.get(0);
        assertEquals("riscv64", uarch.getArch());
        assertEquals("rv64imafdc", uarch.getIsa());
    }

    @Test
    public void parseAllUnknownEntityTypeReturnBaseClass() {
        String json = """
                        {"ty":"entitylistoutput-v1","entity_type":"something-new","entity_id":"foo","display_name":"Foo","related_refs":[],"reverse_refs":[]}
                        """;
        List<EntityInfo> result = EntityInfoParser.parseAll(json);
        assertEquals(1, result.size());
        assertEquals("something-new", result.get(0).getEntityType());
        // Not a subclass — plain EntityInfo
        assertEquals(EntityInfo.class, result.get(0).getClass());
    }

    // ------------------------------------------------------------------
    // EntityInfoParser — concatenated objects
    // ------------------------------------------------------------------

    @Test
    public void parseAllHandlesConcatenatedObjects() {
        String json = """
                        {"ty":"entitylistoutput-v1","entity_type":"device","entity_id":"board-a","display_name":"Board A","related_refs":[],"reverse_refs":[]}{"ty":"entitylistoutput-v1","entity_type":"device","entity_id":"board-b","display_name":"Board B","related_refs":[],"reverse_refs":[]}
                        """;
        List<EntityInfo> result = EntityInfoParser.parseAll(json);
        assertEquals(2, result.size());
        assertEquals("board-a", result.get(0).getEntityId());
        assertEquals("board-b", result.get(1).getEntityId());
    }

    @Test
    public void parseAllHandlesJsonArray() {
        String json = """
                        [{"ty":"entitylistoutput-v1","entity_type":"device","entity_id":"board-a","display_name":"Board A","related_refs":[],"reverse_refs":[]},{"ty":"entitylistoutput-v1","entity_type":"cpu","entity_id":"cpu-a","display_name":"CPU A","related_refs":[],"reverse_refs":[]}]
                        """;
        List<EntityInfo> result = EntityInfoParser.parseAll(json);
        assertEquals(2, result.size());
        assertTrue(result.get(0) instanceof DeviceEntityInfo);
        assertTrue(result.get(1) instanceof CpuEntityInfo);
    }

    @Test
    public void parseAllThrowsOnTruncatedJson() {
        // Second object is truncated — JSONTokener throws on malformed input
        String json = """
                        {"ty":"entitylistoutput-v1","entity_type":"device","entity_id":"good","display_name":"Good","related_refs":[],"reverse_refs":[]}{"ty":"entitylistoutput-v1","entity_type":"device","entity_id":"bad","displ
                        """;
        JSONException ex = null;
        try {
            EntityInfoParser.parseAll(json);
        } catch (JSONException e) {
            ex = e;
        }
        assertNotNull("Expected JSONException for truncated JSON", ex);
        assertTrue("Message should mention unterminated string: " + ex.getMessage(),
                        ex.getMessage().contains("Unterminated string"));
    }

    // ------------------------------------------------------------------
    // EntityInfoParser — type-specific convenience methods
    // ------------------------------------------------------------------

    @Test
    public void parseDeviceEntitiesFiltersCorrectly() {
        String json = """
                        {"ty":"entitylistoutput-v1","entity_type":"device","entity_id":"d1","display_name":"D1","related_refs":[],"reverse_refs":[]}{"ty":"entitylistoutput-v1","entity_type":"cpu","entity_id":"c1","display_name":"C1","related_refs":[],"reverse_refs":[]}{"ty":"entitylistoutput-v1","entity_type":"device","entity_id":"d2","display_name":"D2","related_refs":[],"reverse_refs":[]}
                        """;
        List<DeviceEntityInfo> devs = EntityInfoParser.parseDeviceEntities(json);
        assertEquals(2, devs.size());
        assertEquals("d1", devs.get(0).getEntityId());
        assertEquals("d2", devs.get(1).getEntityId());
    }

    @Test
    public void parseCpuEntitiesFiltersCorrectly() {
        String json = """
                        {"ty":"entitylistoutput-v1","entity_type":"cpu","entity_id":"c1","display_name":"C1","related_refs":[],"reverse_refs":[]}{"ty":"entitylistoutput-v1","entity_type":"device","entity_id":"d1","display_name":"D1","related_refs":[],"reverse_refs":[]}
                        """;
        List<CpuEntityInfo> cpus = EntityInfoParser.parseCpuEntities(json);
        assertEquals(1, cpus.size());
        assertEquals("c1", cpus.get(0).getEntityId());
    }

    @Test
    public void parseDeviceVariantsFiltersCorrectly() {
        String json = """
                        {"ty":"entitylistoutput-v1","entity_type":"device-variant","entity_id":"dv@1","display_name":null,"data":{"device-variant":{"id":"1","variant_name":"V1"}},"related_refs":[],"reverse_refs":[]}{"ty":"entitylistoutput-v1","entity_type":"device","entity_id":"d1","display_name":"D1","related_refs":[],"reverse_refs":[]}
                        """;
        List<DeviceVariantInfo> dvs = EntityInfoParser.parseDeviceVariants(json);
        assertEquals(1, dvs.size());
        assertEquals("dv@1", dvs.get(0).getEntityId());
        assertEquals("V1", dvs.get(0).getVariantName());
    }

    @Test
    public void parseUarchEntitiesFiltersCorrectly() {
        String json = """
                        {"ty":"entitylistoutput-v1","entity_type":"uarch","entity_id":"u1","display_name":"U1","data":{"uarch":{"arch":"riscv64","riscv":{"isa":"rv64gc"}}},"related_refs":["arch:riscv64"],"reverse_refs":[]}{"ty":"entitylistoutput-v1","entity_type":"device","entity_id":"d1","display_name":"D1","related_refs":[],"reverse_refs":[]}
                        """;
        List<UarchEntityInfo> uarchs = EntityInfoParser.parseUarchEntities(json);
        assertEquals(1, uarchs.size());
        assertEquals("riscv64", uarchs.get(0).getArch());
        assertEquals("rv64gc", uarchs.get(0).getIsa());
    }

    // ------------------------------------------------------------------
    // EntityInfoParser — edge cases for data extraction
    // ------------------------------------------------------------------

    @Test
    public void deviceVariantGenericVariant() {
        String json = """
                        {"ty":"entitylistoutput-v1","entity_type":"device-variant","entity_id":"milkv-mars@generic","display_name":null,"data":{"device-variant":{"id":"generic","variant_name":"generic variant"}},"related_refs":["cpu:starfive-jh7110"],"reverse_refs":["device:milkv-mars","image-combo:revyos-milkv-mars@generic"]}
                        """;
        List<DeviceVariantInfo> dvs = EntityInfoParser.parseDeviceVariants(json);
        assertEquals(1, dvs.size());
        DeviceVariantInfo dv = dvs.get(0);
        assertEquals("milkv-mars@generic", dv.getEntityId());
        assertNull(dv.getDisplayName());
        assertEquals("generic", dv.getVariantId());
        assertEquals("generic variant", dv.getVariantName());
        // getLabel() prefers variantName over displayName/entityId
        assertEquals("generic variant", dv.getLabel());
        assertEquals(1, dv.getRelatedRefs().size());
        assertEquals("cpu:starfive-jh7110", dv.getRelatedRefs().get(0));
        assertEquals(2, dv.getReverseRefs().size());
    }

    @Test
    public void deviceVariantWithNoDataBlock() {
        String json = """
                        {"ty":"entitylistoutput-v1","entity_type":"device-variant","entity_id":"x@y","display_name":null,"related_refs":[],"reverse_refs":[]}
                        """;
        List<DeviceVariantInfo> dvs = EntityInfoParser.parseDeviceVariants(json);
        assertEquals(1, dvs.size());
        assertNull(dvs.get(0).getVariantId());
        assertNull(dvs.get(0).getVariantName());
    }

    @Test
    public void uarchWithNoDataBlock() {
        String json = """
                        {"ty":"entitylistoutput-v1","entity_type":"uarch","entity_id":"u-test","display_name":"Test","related_refs":[],"reverse_refs":[]}
                        """;
        List<UarchEntityInfo> us = EntityInfoParser.parseUarchEntities(json);
        assertEquals(1, us.size());
        assertNull(us.get(0).getArch());
        assertNull(us.get(0).getIsa());
    }

    @Test
    public void uarchWithNoRiscvBlock() {
        String json = """
                        {"ty":"entitylistoutput-v1","entity_type":"uarch","entity_id":"u-test","display_name":"Test","data":{"uarch":{"arch":"riscv64"}},"related_refs":[],"reverse_refs":[]}
                        """;
        List<UarchEntityInfo> us = EntityInfoParser.parseUarchEntities(json);
        assertEquals(1, us.size());
        assertEquals("riscv64", us.get(0).getArch());
        assertNull(us.get(0).getIsa());
    }

    @Test
    public void missingRelatedRefsDefaultsToEmpty() {
        String json = """
                        {"ty":"entitylistoutput-v1","entity_type":"device","entity_id":"d1","display_name":"D1"}
                        """;
        List<DeviceEntityInfo> devs = EntityInfoParser.parseDeviceEntities(json);
        assertEquals(1, devs.size());
        assertTrue(devs.get(0).getRelatedRefs().isEmpty());
        assertTrue(devs.get(0).getReverseRefs().isEmpty());
    }
}
