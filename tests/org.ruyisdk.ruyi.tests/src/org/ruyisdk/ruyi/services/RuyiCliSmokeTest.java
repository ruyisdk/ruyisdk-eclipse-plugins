package org.ruyisdk.ruyi.services;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.ruyisdk.ruyi.services.RuyiCli;

public class RuyiCliSmokeTest {

    @Ignore("Disabled by default: depends on local Ruyi CLI environment")
    @Test
    public void smoke() {
        System.out.println("Running RuyiCli smoke test...");

        List<RuyiCli.ProfileInfo> ps = RuyiCli.listProfiles();
        System.out.println("Profiles: " + (ps == null ? "null" : ps.size()));
        if (ps != null) {
            for (RuyiCli.ProfileInfo p : ps) {
                System.out.println(" - " + p.getName() + " quirks=" + p.getQuirks());
            }
        }

        List<RuyiCli.ToolchainInfo> tcs = RuyiCli.listToolchains();
        System.out.println("Toolchains: " + (tcs == null ? "null" : tcs.size()));
        if (tcs != null) {
            for (RuyiCli.ToolchainInfo t : tcs) {
                System.out.println(" - " + t.getName() + " versions=" + t.getVersions());
            }
        }

        List<RuyiCli.EmulatorInfo> ems = RuyiCli.listEmulators();
        System.out.println("Emulators: " + (ems == null ? "null" : ems.size()));
        if (ems != null) {
            for (RuyiCli.EmulatorInfo e : ems) {
                System.out.println(" - " + e.getName() + " versions=" + e.getVersions());
            }
        }

        List<RuyiCli.VenvInfo> vs = RuyiCli.listVenvs();
        System.out.println("Venvs: " + (vs == null ? "null" : vs.size()));
        if (vs != null) {
            for (RuyiCli.VenvInfo v : vs) {
                System.out.println(" - " + v.getPath() + " profile=" + v.getProfile() + " sysroot=" + v.getSysroot()
                                + " activated=" + v.getActivated());
            }
        }
    }
}
