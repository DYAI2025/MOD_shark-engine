package dev.sharkengine.ship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * REQ-023/AC-023 (T23): the looping maneuver is documented as a POST-Release-1 backlog entry
 * carrying the user-confirmed design notes — and nothing more. Resource-contract pattern
 * (like {@code ResourceValidationTest}): the doc's existence and required content are a
 * testable contract, so the backlog entry cannot silently vanish in a docs cleanup while
 * REQ-023 claims it exists.
 *
 * <p>The companion negative guard ({@code AccelerationPhaseTest.noLoopRelatedPhaseIntroduced})
 * covers the opposite failure: looping code sneaking INTO Release 1.</p>
 */
@DisplayName("REQ-023/T23: looping backlog entry exists with the confirmed design notes")
class LoopingBacklogDocumentationTest {

    private static final Path BACKLOG = Path.of("docs/BACKLOG.md");

    @Test
    @DisplayName("docs/BACKLOG.md exists")
    void backlogDocumentExists() {
        assertTrue(Files.exists(BACKLOG),
                "docs/BACKLOG.md missing — REQ-023 requires looping to be documented as a "
                        + "post-release backlog entry");
    }

    @Test
    @DisplayName("The looping entry carries the confirmed design notes and the not-in-Release-1 boundary")
    void loopingEntryCarriesConfirmedNotes() throws IOException {
        String content = Files.readString(BACKLOG);
        String lower = content.toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("looping"), "backlog has no looping entry");
        assertTrue(content.contains("REQ-023"),
                "looping entry must reference REQ-023 (traceability)");
        assertTrue(content.contains("VIS-010") && content.contains("CAN-013"),
                "looping entry must reference the vision/canvas boundaries that keep it post-release");
        assertTrue(lower.contains("eintrittsbedingungen") || lower.contains("entry condition"),
                "confirmed design note missing: entry conditions gate the maneuver");
        assertTrue(lower.contains("crash"),
                "confirmed design note missing: failed loops have fatal crash consequences");
        assertTrue(lower.contains("halbautomatisch") || lower.contains("semi-automatic"),
                "confirmed design note missing: manual or semi-automatically overridden maneuver");
        assertTrue(lower.contains("not a release-1") || lower.contains("kein release-1"),
                "the entry must state explicitly that looping is NOT a Release-1 gate");
    }
}
