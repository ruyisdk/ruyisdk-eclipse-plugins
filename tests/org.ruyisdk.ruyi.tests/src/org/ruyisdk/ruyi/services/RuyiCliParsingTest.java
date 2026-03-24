package org.ruyisdk.ruyi.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

/**
 * Unit tests for {@link RuyiCli} porcelain parsing helpers.
 */
public class RuyiCliParsingTest {

    /**
     * Parses concatenated toolchain list objects.
     */
    @Test
    public void parseToolchainsMultiVersion() {
        String sample = """
                        {"ty":"pkglistoutput-v1","category":"toolchain","name":"tc-a","vers":[{"semver":"1.0.0"},{"semver":"1.1.0"}]}{"ty":"pkglistoutput-v1","category":"toolchain","name":"tc-b","vers":[{"semver":"2.0.0"}]}
                        """;

        List<RuyiCli.ToolchainInfo> tcs = RuyiCli.parseToolchainsFromString(sample);
        assertNotNull(tcs);
        assertEquals(2, tcs.size());
        assertEquals("tc-a", tcs.get(0).getName());
        assertEquals(2, tcs.get(0).getVersions().size());
        assertEquals("1.0.0", tcs.get(0).getVersions().get(0));
        assertEquals("1.1.0", tcs.get(0).getVersions().get(1));
        assertEquals("tc-b", tcs.get(1).getName());
        assertEquals(1, tcs.get(1).getVersions().size());
        assertEquals("2.0.0", tcs.get(1).getVersions().get(0));
    }

    /**
     * Parses concatenated emulator list objects.
     */
    @Test
    public void parseEmulatorsMultiVersion() {
        String sample = """
                        {"ty":"pkglistoutput-v1","category":"emulator","name":"emu-x","vers":[{"semver":"0.9.0"},{"semver":"1.0.0"}]}{"ty":"pkglistoutput-v1","category":"emulator","name":"emu-y","vers":[{"semver":"2.1.0"}]}
                        """;

        List<RuyiCli.EmulatorInfo> ems = RuyiCli.parseEmulatorsFromString(sample);
        assertNotNull(ems);
        assertEquals(2, ems.size());
        assertEquals("emu-x", ems.get(0).getName());
        assertEquals(2, ems.get(0).getVersions().size());
        assertEquals("0.9.0", ems.get(0).getVersions().get(0));
        assertEquals("1.0.0", ems.get(0).getVersions().get(1));
        assertEquals("emu-y", ems.get(1).getName());
        assertEquals(1, ems.get(1).getVersions().size());
        assertEquals("2.1.0", ems.get(1).getVersions().get(0));
    }

    /**
     * Ensures garbage and truncated JSON do not crash parsing.
     */
    @Test
    public void parseWithInterleavedGarbageAndTruncated() {
        String sample = """
                        {"ty":"pkglistoutput-v1","category":"toolchain","name":"tc-good","vers":[{"semver":"1.2.3"}]}gibberish-not-json{"ty":"pkglistoutput-v1","category":"toolchain","name":"tc-c","vers":[{"semver":"3.0.0"}]}{"ty":"pkglistoutput-v1","category":"toolchain","name":"tc-d","vers":[{"semver":"4.0.0"}]
                        """;

        List<RuyiCli.ToolchainInfo> tcs = RuyiCli.parseToolchainsFromString(sample);
        assertNotNull(tcs);

        boolean hasGood = false;
        boolean hasC = false;
        boolean hasD = false;
        for (RuyiCli.ToolchainInfo t : tcs) {
            if ("tc-good".equals(t.getName())) {
                hasGood = true;
            }
            if ("tc-c".equals(t.getName())) {
                hasC = true;
            }
            if ("tc-d".equals(t.getName())) {
                hasD = true;
            }
        }

        assertTrue(hasGood);
        assertTrue(hasC);
        assertFalse(hasD);
    }

    /**
     * Parses concatenated news list objects and ignores unrelated types.
     */
    @Test
    public void parseNewsListConcatenatedObjects() {
        Locale old = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("zh-CN"));

            String sample = """
                            {"ty":"newsitem-v1","id":"2024-01-14-ruyi-news","ord":1,"is_read":false,"langs":[{"lang":"zh_CN","display_title":"\u65b0\u95fb\u4e2d\u6587","content":"\u5185\u5bb9"},{"lang":"en_US","display_title":"Ruyi News","content":"Hello"}]}
                            {"ty":"other-v1","id":"ignore-me"}
                            {"ty":"newsitem-v1","id":"2024-01-21-ruyi-news","ord":2,"is_read":true,"langs":[{"lang":"en_US","display_title":"Second","content":"World"}]}
                            """;

            List<RuyiCli.NewsListItemInfo> items = RuyiCli.parseNewsListFromString(sample);
            assertNotNull(items);
            assertEquals(2, items.size());

            assertEquals("2024-01-14-ruyi-news", items.get(0).getId());
            assertEquals(Integer.valueOf(1), items.get(0).getOrd());
            assertEquals(Boolean.FALSE, items.get(0).isRead());
            assertEquals("Ruyi News", items.get(0).getTitle());

            assertEquals("2024-01-21-ruyi-news", items.get(1).getId());
            assertEquals(Boolean.TRUE, items.get(1).isRead());
            assertEquals("Second", items.get(1).getTitle());
        } finally {
            Locale.setDefault(old);
        }
    }

    /**
     * Leaves the ID as null when the porcelain object is missing an id.
     */
    @Test
    public void parseNewsListKeepsNullIdWhenIdMissing() {
        String sample = """
                        {"ty":"newsitem-v1","ord":7,"is_read":false,"langs":[{"lang":"en_US","display_title":"T","content":"C"}]}
                        """;

        List<RuyiCli.NewsListItemInfo> items = RuyiCli.parseNewsListFromString(sample);
        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals(null, items.get(0).getId());
        assertEquals(Integer.valueOf(7), items.get(0).getOrd());
    }

    /**
     * Prefers en_US over the system locale when choosing a news language.
     */
    @Test
    public void parseNewsReadChoosesBestLang() {
        Locale old = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("zh-CN"));

            String sample = """
                            {"ty":"newsitem-v1","id":"2024-01-14-ruyi-news","ord":1,"is_read":true,"langs":[{"lang":"zh_CN","display_title":"\u65b0\u95fb\u4e2d\u6587","content":"\u4e2d\u6587\u5185\u5bb9"},{"lang":"en_US","display_title":"English Title","content":"English Content"}]}
                            """;

            RuyiCli.NewsReadResult r = RuyiCli.parseNewsReadFromString(sample);
            assertNotNull(r);
            assertEquals("2024-01-14-ruyi-news", r.getId());
            assertEquals(Integer.valueOf(1), r.getOrd());
            assertEquals(Boolean.TRUE, r.isRead());
            assertEquals("English Title", r.getTitle());
            assertEquals("English Content", r.getContent());
        } finally {
            Locale.setDefault(old);
        }
    }

    @Test
    public void parseToolchainsExtractsQuirksFromMetadata() {
        String sample = """
                        {"ty":"pkglistoutput-v1","category":"toolchain","name":"tc-a","vers":[{"semver":"1.0.0","pm":{"toolchain":{"quirks":["xthead"]}}}]}
                        """;

        List<RuyiCli.ToolchainInfo> tcs = RuyiCli.parseToolchainsFromString(sample);
        assertEquals(1, tcs.size());
        assertEquals(List.of("xthead"), tcs.get(0).getQuirks());
    }

    @Test
    public void parseToolchainsExtractsFlavorsAsFallback() {
        String sample = """
                        {"ty":"pkglistoutput-v1","category":"toolchain","name":"tc-old","vers":[{"semver":"1.0.0","pm":{"toolchain":{"flavors":["wch"]}}}]}
                        """;

        List<RuyiCli.ToolchainInfo> tcs = RuyiCli.parseToolchainsFromString(sample);
        assertEquals(1, tcs.size());
        assertEquals(List.of("wch"), tcs.get(0).getQuirks());
    }

    @Test
    public void parseToolchainsMergesQuirksAndFlavors() {
        String sample = """
                        {"ty":"pkglistoutput-v1","category":"toolchain","name":"tc-merged","vers":[{"semver":"1.0.0","pm":{"toolchain":{"quirks":["xthead"],"flavors":["wch"]}}}]}
                        """;

        List<RuyiCli.ToolchainInfo> tcs = RuyiCli.parseToolchainsFromString(sample);
        assertEquals(1, tcs.size());
        assertEquals(List.of("xthead", "wch"), tcs.get(0).getQuirks());
    }

    @Test
    public void parseToolchainsDeduplicatesQuirksAndFlavors() {
        String sample = """
                        {"ty":"pkglistoutput-v1","category":"toolchain","name":"tc-dup","vers":[{"semver":"1.0.0","pm":{"toolchain":{"quirks":["xthead","wch"],"flavors":["xthead"]}}}]}
                        """;

        List<RuyiCli.ToolchainInfo> tcs = RuyiCli.parseToolchainsFromString(sample);
        assertEquals(1, tcs.size());
        assertEquals(List.of("xthead", "wch"), tcs.get(0).getQuirks());
    }

    @Test
    public void parseToolchainsNoQuirksWhenNoPmMetadata() {
        String sample = """
                        {"ty":"pkglistoutput-v1","category":"toolchain","name":"tc-plain","vers":[{"semver":"1.0.0"}]}
                        """;

        List<RuyiCli.ToolchainInfo> tcs = RuyiCli.parseToolchainsFromString(sample);
        assertEquals(1, tcs.size());
        assertTrue(tcs.get(0).getQuirks().isEmpty());
    }

    @Test
    public void parseToolchainsUsesFirstVersionForQuirks() {
        String sample = """
                        {"ty":"pkglistoutput-v1","category":"toolchain","name":"tc-multi","vers":[{"semver":"1.0.0","pm":{"toolchain":{"quirks":["first"]}}},{"semver":"2.0.0","pm":{"toolchain":{"quirks":["second"]}}}]}
                        """;

        List<RuyiCli.ToolchainInfo> tcs = RuyiCli.parseToolchainsFromString(sample);
        assertEquals(1, tcs.size());
        assertEquals(List.of("first"), tcs.get(0).getQuirks());
    }

    @Test
    public void parseEmulatorsExtractsQuirksFromMetadata() {
        String sample = """
                        {"ty":"pkglistoutput-v1","category":"emulator","name":"emu-a","vers":[{"semver":"1.0.0","pm":{"emulator":{"quirks":["wch"]}}}]}
                        """;

        List<RuyiCli.EmulatorInfo> ems = RuyiCli.parseEmulatorsFromString(sample);
        assertEquals(1, ems.size());
        assertEquals(List.of("wch"), ems.get(0).getQuirks());
    }

    @Test
    public void parseEmulatorsExtractsFlavorsAsFallback() {
        String sample = """
                        {"ty":"pkglistoutput-v1","category":"emulator","name":"emu-old","vers":[{"semver":"1.0.0","pm":{"emulator":{"flavors":["xthead"]}}}]}
                        """;

        List<RuyiCli.EmulatorInfo> ems = RuyiCli.parseEmulatorsFromString(sample);
        assertEquals(1, ems.size());
        assertEquals(List.of("xthead"), ems.get(0).getQuirks());
    }

    @Test
    public void parseEmulatorsNoQuirksWhenNoPmMetadata() {
        String sample = """
                        {"ty":"pkglistoutput-v1","category":"emulator","name":"emu-plain","vers":[{"semver":"1.0.0"}]}
                        """;

        List<RuyiCli.EmulatorInfo> ems = RuyiCli.parseEmulatorsFromString(sample);
        assertEquals(1, ems.size());
        assertTrue(ems.get(0).getQuirks().isEmpty());
    }

    @Test
    public void profileInfoStoresQuirksList() {
        var pi = new RuyiCli.ProfileInfo("test-profile", List.of("wch", "xthead"));
        assertEquals("test-profile", pi.getName());
        assertEquals(List.of("wch", "xthead"), pi.getQuirks());
    }

    @Test
    public void profileInfoNullQuirksBecomesEmpty() {
        var pi = new RuyiCli.ProfileInfo("null-quirks", null);
        assertNotNull(pi.getQuirks());
        assertTrue(pi.getQuirks().isEmpty());
    }

    @Test
    public void profileInfoQuirksListIsUnmodifiable() {
        var pi = new RuyiCli.ProfileInfo("immutable", List.of("a"));
        boolean threw = false;
        try {
            pi.getQuirks().add("b");
        } catch (UnsupportedOperationException e) {
            threw = true;
        }
        assertTrue(threw);
    }

    @Test
    public void profileInfoEmptyQuirksForNoQuirksProfile() {
        var pi = new RuyiCli.ProfileInfo("plain-profile", Collections.emptyList());
        assertTrue(pi.getQuirks().isEmpty());
    }

    @Test
    public void toolchainInfoQuirksListIsUnmodifiable() {
        var ti = new RuyiCli.ToolchainInfo("tc", List.of("1.0"), List.of("q"));
        boolean threw = false;
        try {
            ti.getQuirks().add("x");
        } catch (UnsupportedOperationException e) {
            threw = true;
        }
        assertTrue(threw);
    }

    @Test
    public void toolchainInfoNullQuirksBecomesEmpty() {
        var ti = new RuyiCli.ToolchainInfo("tc", List.of("1.0"), null);
        assertNotNull(ti.getQuirks());
        assertTrue(ti.getQuirks().isEmpty());
    }

    @Test
    public void emulatorInfoQuirksListIsUnmodifiable() {
        var ei = new RuyiCli.EmulatorInfo("emu", List.of("1.0"), List.of("q"));
        boolean threw = false;
        try {
            ei.getQuirks().add("x");
        } catch (UnsupportedOperationException e) {
            threw = true;
        }
        assertTrue(threw);
    }

    @Test
    public void emulatorInfoNullQuirksBecomesEmpty() {
        var ei = new RuyiCli.EmulatorInfo("emu", List.of("1.0"), null);
        assertNotNull(ei.getQuirks());
        assertTrue(ei.getQuirks().isEmpty());
    }
}
