package dev.sharkengine.ship.part;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AssemblyIssue} — the structured, per-code replacement for the single
 * translated chat message {@code ShipAssemblyService.tryAssemble} used to pick (REQ-S3).
 *
 * <p>Plain JUnit, no Fabric bootstrap: {@link AssemblyIssue} deliberately carries no
 * {@code net.minecraft.network}/{@code net.minecraft.nbt} dependency (see that class's javadoc
 * for why — this repo's {@code test} source set cannot compile against
 * {@code net.minecraft.network.*} at all, confirmed empirically). Wire roundtrip coverage for
 * the codes this class carries lives in a GameTest instead (see
 * {@code dev.sharkengine.gametest.BuilderPreviewPayloadGameTest}), since encoding requires
 * {@code RegistryFriendlyByteBuf}/{@code FriendlyByteBuf}.</p>
 */
@DisplayName("AssemblyIssue Tests")
class AssemblyIssueTest {

    // ─── per-code translation key (one assertion per AssemblyIssue.Code, per plan) ──────────

    @Test
    @DisplayName("EMPTY_STRUCTURE translation key")
    void emptyStructureTranslationKey() {
        assertEquals("assembly_issue.sharkengine.empty_structure",
                AssemblyIssue.of(AssemblyIssue.Code.EMPTY_STRUCTURE).translationKey());
    }

    @Test
    @DisplayName("INVALID_ATTACHMENTS translation key")
    void invalidAttachmentsTranslationKey() {
        assertEquals("assembly_issue.sharkengine.invalid_attachments",
                AssemblyIssue.of(AssemblyIssue.Code.INVALID_ATTACHMENTS, 2).translationKey());
    }

    @Test
    @DisplayName("TERRAIN_CONTACT translation key")
    void terrainContactTranslationKey() {
        assertEquals("assembly_issue.sharkengine.terrain_contact",
                AssemblyIssue.of(AssemblyIssue.Code.TERRAIN_CONTACT, 1).translationKey());
    }

    @Test
    @DisplayName("NO_PROPULSION translation key")
    void noPropulsionTranslationKey() {
        assertEquals("assembly_issue.sharkengine.no_propulsion",
                AssemblyIssue.of(AssemblyIssue.Code.NO_PROPULSION).translationKey());
    }

    @Test
    @DisplayName("TOO_FEW_CORE_NEIGHBORS translation key")
    void tooFewCoreNeighborsTranslationKey() {
        assertEquals("assembly_issue.sharkengine.too_few_core_neighbors",
                AssemblyIssue.of(AssemblyIssue.Code.TOO_FEW_CORE_NEIGHBORS, 3).translationKey());
    }

    @Test
    @DisplayName("NO_BUG translation key")
    void noBugTranslationKey() {
        assertEquals("assembly_issue.sharkengine.no_bug",
                AssemblyIssue.of(AssemblyIssue.Code.NO_BUG).translationKey());
    }

    @Test
    @DisplayName("MULTI_BUG translation key")
    void multiBugTranslationKey() {
        assertEquals("assembly_issue.sharkengine.multi_bug",
                AssemblyIssue.of(AssemblyIssue.Code.MULTI_BUG, 2).translationKey());
    }

    @Test
    @DisplayName("BUG_INSIDE translation key")
    void bugInsideTranslationKey() {
        assertEquals("assembly_issue.sharkengine.bug_inside",
                AssemblyIssue.of(AssemblyIssue.Code.BUG_INSIDE).translationKey());
    }

    // ─── factory / normalization behavior ────────────────────────────────────────────────────

    @Test
    @DisplayName("of(code): no pos, no args")
    void ofCodeOnly() {
        AssemblyIssue issue = AssemblyIssue.of(AssemblyIssue.Code.NO_BUG);
        assertNull(issue.pos());
        assertEquals(List.of(), issue.args());
    }

    @Test
    @DisplayName("of(code, arg): single-arg list")
    void ofCodeWithArg() {
        AssemblyIssue issue = AssemblyIssue.of(AssemblyIssue.Code.TERRAIN_CONTACT, 5);
        assertEquals(List.of(5), issue.args());
        assertArrayEquals(new Object[] {5}, issue.translationArgs());
    }

    @Test
    @DisplayName("of(code, pos): carries position, empty args")
    void ofCodeWithPos() {
        BlockPos pos = new BlockPos(1, 2, 3);
        AssemblyIssue issue = AssemblyIssue.of(AssemblyIssue.Code.BUG_INSIDE, pos);
        assertEquals(pos, issue.pos());
        assertEquals(List.of(), issue.args());
    }

    @Test
    @DisplayName("null args normalizes to empty list, never null")
    void nullArgsNormalizesToEmptyList() {
        AssemblyIssue issue = new AssemblyIssue(AssemblyIssue.Code.NO_BUG, null, null);
        assertEquals(List.of(), issue.args());
    }

    @Test
    @DisplayName("args list is immutable (defensive copy)")
    void argsListIsImmutable() {
        AssemblyIssue issue = AssemblyIssue.of(AssemblyIssue.Code.TERRAIN_CONTACT, 1);
        assertThrows(UnsupportedOperationException.class, () -> issue.args().add(99));
    }

    @Test
    @DisplayName("null code throws NullPointerException")
    void nullCodeThrows() {
        assertThrows(NullPointerException.class, () -> new AssemblyIssue(null, null, List.of()));
    }

    @Test
    @DisplayName("equals/hashCode: structurally equal issues are equal")
    void structuralEquality() {
        AssemblyIssue a = AssemblyIssue.of(AssemblyIssue.Code.TOO_FEW_CORE_NEIGHBORS, 3);
        AssemblyIssue b = AssemblyIssue.of(AssemblyIssue.Code.TOO_FEW_CORE_NEIGHBORS, 3);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
