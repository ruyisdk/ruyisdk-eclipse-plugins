package org.ruyisdk.core.ruyi.model;

import java.util.Locale;

/**
 * Represents system architecture information and provides detection utilities.
 *
 * <p>This class helps identify the current system's CPU architecture and generates platform-specific
 * identifiers for RuyiSDK operations.
 */
public class SystemInfo {

    /**
     * Enumeration of supported CPU architectures.
     */
    public enum Architecture {
        /** x86-64/AMD64 architecture. */
        X86_64("amd64"),

        /** ARM64/AArch64 architecture. */
        ARM("arm64"),

        /** RISC-V 64-bit architecture. */
        RISCV("riscv64"),

        /** Unknown or unsupported architecture. */
        UNKNOWN("unknown");

        /** Platform-specific suffix used in file paths and identifiers. */
        private final String suffix;

        /**
         * Creates an architecture enum value.
         *
         * @param suffix the platform-specific suffix
         */
        Architecture(String suffix) {
            this.suffix = suffix;
        }

        /**
         * Gets the platform suffix for this architecture.
         *
         * @return the architecture suffix
         */
        public String getSuffix() {
            return suffix;
        }
    }

    /**
     * Detects the current system's CPU architecture.
     *
     * @return detected Architecture enum value
     */
    public static Architecture detectArchitecture() {
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);

        if (arch.contains("x86_64") || arch.contains("amd64")) {
            return Architecture.X86_64;
        } else if (arch.contains("aarch64") || arch.contains("arm64")) {
            return Architecture.ARM;
        } else if (arch.contains("riscv")) {
            return Architecture.RISCV;
        } else {
            return Architecture.UNKNOWN;
        }
    }

    /**
     * Generates a platform key string in "OS/ARCH" format.
     *
     * <p>The returned string follows RuyiSDK's platform identification convention and can be used for
     * downloading architecture-specific packages.
     *
     * @return platform key string (e.g., "linux/x86_64")
     */
    public static String getPlatformKey() {
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);

        if (arch.contains("x86_64") || arch.contains("amd64")) {
            return "linux/x86_64";
        } else if (arch.contains("aarch64") || arch.contains("arm64")) {
            return "linux/aarch64";
        } else if (arch.contains("riscv")) {
            return "linux/riscv64";
        } else {
            return "linux/" + arch;
        }
    }
}
