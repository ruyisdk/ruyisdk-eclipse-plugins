package org.ruyisdk.core.ruyi.model;
import java.util.Locale;
/**
 * 系统信息模型
 */
public class SystemInfo {
    public enum Architecture {
        X86_64("amd64"),
        ARM("arm64"),
        RISCV("riscv64"),
        UNKNOWN("unknown");

        private final String suffix;

        Architecture(String suffix) {
            this.suffix = suffix;
        }

        public String getSuffix() {
            return suffix;
        }
    }

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