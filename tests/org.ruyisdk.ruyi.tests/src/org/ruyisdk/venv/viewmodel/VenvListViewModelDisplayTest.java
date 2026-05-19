package org.ruyisdk.venv.viewmodel;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;
import org.ruyisdk.venv.model.Venv;

/**
 * Unit tests for venv table display formatting.
 */
public class VenvListViewModelDisplayTest {

    @Test
    public void duplicateProjectNamesStillShowBaseNameOnly() {
        final var first =
                Venv.createForProject("/workspace/project-a/.venv", "p", "s", "/workspace/demo");
        final var second =
                Venv.createForProject("/workspace/project-b/.venv", "p", "s", "/opt/demo");

        assertEquals("demo", VenvListViewModel.toDisplayProjectName(first));
        assertEquals("demo", VenvListViewModel.toDisplayProjectName(second));
    }

    @Test
    public void uniqueProjectNameShowsBaseNameOnly() {
        final var venv = Venv.createForProject("/workspace/project-a/.venv", "p", "s",
                "/workspace/riscv-app");

        assertEquals("riscv-app", VenvListViewModel.toDisplayProjectName(venv));
    }

    @Test
    public void toDisplayProjectNameNullVenvReturnsEmptyString() {
        assertEquals("", VenvListViewModel.toDisplayProjectName(null));
    }

    @Test
    public void relativePathInsideProjectStartsWithDotSlash() {
        final var venv = Venv.createForProject("/workspace/riscv-app/.venv/debug", "p", "s",
                "/workspace/riscv-app");

        assertEquals("./.venv/debug", VenvListViewModel.toDisplayRelativePath(venv));
    }

    @Test
    public void relativePathOutsideProjectUsesDotDotSegments() {
        final var venv =
                Venv.createForProject("/workspace/venv", "p", "s", "/workspace/project-a");

        assertEquals("../venv", VenvListViewModel.toDisplayRelativePath(venv));
    }

    @Test
    public void toDisplayRelativePathNullVenvReturnsEmptyString() {
        assertEquals("", VenvListViewModel.toDisplayRelativePath(null));
    }

    @Test
    public void toDisplayRelativePathNullOrBlankPathReturnsEmptyString() {
        final var nullPathVenv =
                Venv.createForProject("/workspace/project-a/.venv", "p", "s", "/workspace/demo");
        nullPathVenv.setPath(null);

        final var blankPathVenv =
                Venv.createForProject("/workspace/project-b/.venv", "p", "s", "/workspace/demo");
        blankPathVenv.setPath("   ");

        assertEquals("", VenvListViewModel.toDisplayRelativePath(nullPathVenv));
        assertEquals("", VenvListViewModel.toDisplayRelativePath(blankPathVenv));
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingProjectPathIsRejected() {
        final var venv = Venv.createStandalone("/workspace/project-a/.venv", "p", "s", List.of());

        VenvListViewModel.toDisplayRelativePath(venv);
    }
}
