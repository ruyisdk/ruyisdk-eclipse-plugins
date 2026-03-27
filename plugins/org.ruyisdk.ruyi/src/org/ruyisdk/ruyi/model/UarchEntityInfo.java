package org.ruyisdk.ruyi.model;

import java.util.List;

/**
 * Entity info for a microarchitecture (entity type {@code "uarch"}).
 *
 * <p>
 * Represents a CPU microarchitecture such as "Xuantie C910" or "SiFive U74". Uarch entities carry
 * ISA (Instruction Set Architecture) information and link to an architecture entity (e.g.
 * {@code "arch:riscv64"}) via {@link #getRelatedRefs()}.
 */
public class UarchEntityInfo extends EntityInfo {

    private final String arch;
    private final String isa;

    /**
     * Constructs a {@code UarchEntityInfo}.
     *
     * @param entityId the uarch entity id, e.g. {@code "xuantie-c910"}
     * @param displayName the human-readable name, e.g. {@code "Xuantie C910"}
     * @param relatedRefs references to arch entities
     * @param reverseRefs reverse references from CPU entities
     * @param arch the architecture, e.g. {@code "riscv64"}, {@code "riscv32"}
     * @param isa the full ISA string, e.g. {@code "rv64imafdc_zicntr_zicsr_..."}; may be {@code null}
     *        for non-RISC-V architectures
     */
    public UarchEntityInfo(String entityId, String displayName, List<String> relatedRefs, List<String> reverseRefs,
                    String arch, String isa) {
        super("uarch", entityId, displayName, relatedRefs, reverseRefs);
        this.arch = arch;
        this.isa = isa;
    }

    /**
     * Returns the architecture, e.g. {@code "riscv64"} or {@code "riscv32"}.
     */
    public String getArch() {
        return arch;
    }

    /**
     * Returns the full ISA string, e.g.
     * {@code "rv64imafdc_zicntr_zicsr_zifencei_zihpm_zfh_xtheadba..."}, or {@code null} if not
     * available.
     */
    public String getIsa() {
        return isa;
    }
}
