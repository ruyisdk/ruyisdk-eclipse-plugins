package org.ruyisdk.ruyi.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.assertj.core.api.Assertions;
import org.ruyisdk.core.ruyi.model.RuyiVersion;
import org.junit.Test;

/**
 * Unit tests for {@link RuyiCliVersionSupport}.
 */
public class RuyiCliVersionSupportTest {

    @Test
    public void parseVersionTextParsesRuyiBanner() {
        String sample = """
                        Ruyi 0.47.2

                        Running on linux/x86_64.
                        """;

        RuyiVersion version = RuyiCliVersionSupport.parseVersionText(sample);
        assertNotNull(version);
        assertEquals(0, version.getMajor());
        assertEquals(47, version.getMinor());
        assertEquals(2, version.getPatch());
    }

    @Test
    public void parseVersionTextReturnsNullWhenBannerMissing() {
        String sample = """
                        ruyi version: unknown
                        """;

        RuyiVersion version = RuyiCliVersionSupport.parseVersionText(sample);
        assertNull(version);
    }

    @Test
    public void isSupportedVersionAccepts047BugfixRelease() {
        assertTrue(RuyiCliVersionSupport.isSupportedVersion(new RuyiVersion(0, 47, 9)));
    }

    @Test
    public void isSupportedVersionAcceptsNewerMinorAndMajor() {
        assertFalse(RuyiCliVersionSupport.isSupportedVersion(new RuyiVersion(0, 46, 9)));
        assertTrue(RuyiCliVersionSupport.isSupportedVersion(new RuyiVersion(0, 47, 0)));
        assertTrue(RuyiCliVersionSupport.isSupportedVersion(new RuyiVersion(0, 48, 0)));
        assertTrue(RuyiCliVersionSupport.isSupportedVersion(new RuyiVersion(1, 0, 0)));
    }

    @Test
    public void ensureSupportedVersionRejectsUnsupportedMinor() {
        boolean threw = false;
        try {
            RuyiCliVersionSupport.ensureSupportedVersion(new RuyiVersion(0, 46, 3));
        } catch (RuyiCliException e) {
            threw = true;
            Assertions.assertThat(e.getMessage()).isEqualTo(String.format(
                            "Installed ruyi version %s is unsupported. Minimum required version is %s",
                            new RuyiVersion(0, 46, 3), new RuyiVersion(0, 47, 0)));
        }
        assertTrue(threw);
    }
}
