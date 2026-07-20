package dev.sharkengine.gametest;

import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.content.ModEntities;
import dev.sharkengine.content.block.BugBlock;
import dev.sharkengine.net.BuilderPreviewS2CPayload;
import dev.sharkengine.ship.ShipAssemblyService;
import dev.sharkengine.ship.ShipBlueprint;
import dev.sharkengine.ship.ShipEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * REQ-013/AC-013 (T13) falsifying-test contract (test-plan
 * {@code docs/plans/2026-07-18-shark-engine-air-release-1-test-plan.md}, "REQ-013 — Builder
 * reopen"): the counter-thesis is a false positive where the builder "opens" (session/edit-mode
 * state transitions correctly, per T12) but shows a STALE or generic/blank blueprint instead of
 * the vehicle's actual CURRENT structure — e.g. a cached snapshot from before the vehicle's last
 * committed edit, rather than a live re-scan. A test that only checks "edit mode == active" would
 * never notice the payload's content is wrong or old. This class proves the reopened builder
 * preview's block list matches the ship's LIVE blueprint after a structural change, not a
 * snapshot captured before that change.
 *
 * <p><b>T14 scoping note (honestly documented per this task's own instructions, not fabricated):
 * </b> REQ-014's atomic validate-then-commit edit-reassembly mechanism does not exist yet (T14 has
 * not run) — there is no real in-game path today for a player to grow an already-launched ship's
 * structure by one block. To still exercise the test-plan's DoD ("modify the vehicle's structure
 * via one prior edit-session commit, block count N -> N+1"), this test stands in for that future
 * commit using {@link ShipEntity#setBlueprint} directly — the SAME production mutator {@link
 * ShipAssemblyService#tryAssemble} and NBT load already use as the ship's sole way to replace "this
 * is now the ship's live structure", and the ONLY such mutator that exists in production code
 * today (any real REQ-014 commit handler will necessarily call this exact method too). The
 * replacement (N+1) blueprint fed into it is not a fabricated/synthetic block list either: it is
 * produced by the real, unmodified {@link ShipAssemblyService#scanStructure} BFS + validation
 * pipeline against a second, genuinely-placed N+1-block structure elsewhere in the test world — a
 * real, validly-assemblable structure's blueprint, not an arbitrary offset list. What this test
 * does NOT do is fabricate a bypass of assembly validation, and it does not block on T14 existing
 * first.</p>
 *
 * <p><b>REQ-013 remediation (Watcher review-required + security-review confirmed, "edit-mode
 * reopen is unreachable by any player action"):</b> this test previously drove the reopen step
 * through {@link ShipAssemblyService#openEditMode} directly — a real production method, but one
 * with zero call sites anywhere a player action reaches (checked: no {@code interact()}/keybind/
 * C2S-handler wired it up). {@link #reopenedBuilderPreviewMatchesLiveStructureNotStaleAssemblyTimeSnapshot}
 * now drives the SAME reopen step through {@link ShipEntity#interact} — the actual production
 * right-click entry point every other GameTest in this package already uses to simulate a player
 * action — so this test proves a real player gesture (an empty-hand, non-sneak right-click by the
 * already-mounted pilot, see {@code ShipEntity#interact}'s own javadoc for why that gesture was the
 * one genuinely unclaimed slot in its existing hand/sneak-state vocabulary) can reach this code
 * path, not merely that the Java method behaves correctly when called directly.</p>
 */
public final class BuilderReopenGameTest implements FabricGameTest {

    private static final BlockPos WHEEL_POS = new BlockPos(3, 1, 3);
    /**
     * A second, independent structure elsewhere in the test world, used only to produce a real,
     * validly-scanned N+1-block blueprint (see class javadoc) — never itself assembled into its
     * own ship entity, just scanned.
     */
    private static final BlockPos SCRATCH_WHEEL_POS = new BlockPos(3, 1, 20);

    private static void placeStructure(GameTestHelper helper, BlockPos wheelPos, boolean extraHullBlock) {
        helper.setBlock(wheelPos, ModBlocks.STEERING_WHEEL);
        helper.setBlock(wheelPos.north(), Blocks.OAK_PLANKS);
        helper.setBlock(wheelPos.south(), ModBlocks.PILOT_SEAT);
        helper.setBlock(wheelPos.east(), Blocks.OAK_PLANKS);
        helper.setBlock(wheelPos.west(), Blocks.OAK_PLANKS);
        helper.setBlock(wheelPos.above(), ModBlocks.THRUSTER);
        BlockState bug = ModBlocks.BUG.defaultBlockState().setValue(BugBlock.FACING, Direction.SOUTH);
        helper.setBlock(wheelPos.north().north(), bug);
        if (extraHullBlock) {
            // The "+1" block a hypothetical REQ-014 edit-session commit would have added.
            helper.setBlock(wheelPos.east().east(), Blocks.OAK_PLANKS);
        }
    }

    private record AssembledShip(ShipEntity ship, ServerPlayer pilot) {}

    /**
     * Places the N-block structure, assembles for real via the same production entry point every
     * other REQ-012/013-adjacent GameTest in this package uses, and returns the spawned {@link
     * ShipEntity} (pilot already mounted by {@code tryAssemble} itself) paired with that pilot — or
     * {@code null} if a precondition failed (in which case {@code helper.fail} was already called
     * and the caller must return immediately).
     */
    private static AssembledShip assembleShip(GameTestHelper helper) {
        placeStructure(helper, WHEEL_POS, false);
        BlockPos wheelWorldPos = helper.absolutePos(WHEEL_POS);

        ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
        pilot.setPos(wheelWorldPos.getX() + 0.5, wheelWorldPos.getY(), wheelWorldPos.getZ() + 0.5);
        ShipAssemblyService.AssembleResult result =
                ShipAssemblyService.tryAssemble(helper.getLevel(), wheelWorldPos, pilot);
        if (!result.isSuccess()) {
            helper.fail("test precondition: expected assembly to succeed, got " + result.translationKey());
            return null;
        }

        List<ShipEntity> ships = helper.getLevel().getEntities(
                ModEntities.SHIP, new AABB(wheelWorldPos).inflate(8), e -> true);
        if (ships.size() != 1) {
            helper.fail("test precondition: expected exactly one spawned ShipEntity, got " + ships.size());
            return null;
        }
        return new AssembledShip(ships.get(0), pilot);
    }

    /**
     * Produces a real, validly-scanned (N+1)-block blueprint using the SAME production BFS +
     * validation pipeline {@link ShipAssemblyService#tryAssemble} itself uses — see class javadoc
     * for why a real scan (rather than a hand-built block list) is used here.
     */
    private static ShipBlueprint scanRealExpandedBlueprint(GameTestHelper helper) {
        placeStructure(helper, SCRATCH_WHEEL_POS, true);
        BlockPos scratchWorldPos = helper.absolutePos(SCRATCH_WHEEL_POS);
        ShipAssemblyService.StructureScan scan =
                ShipAssemblyService.scanStructure(helper.getLevel(), scratchWorldPos);
        return scan.toBlueprint();
    }

    private static void placePilotAtOffset(ShipEntity ship, ServerPlayer pilot, double dx, double dy, double dz) {
        pilot.setPos(ship.getX() + dx, ship.getY() + dy, ship.getZ() + dz);
    }

    /** The set of a blueprint's block positions, expressed as its own local (dx, dy, dz) offsets — position-independent, so this correctly compares two blueprints regardless of their world origin. */
    private static Set<BlockPos> localOffsets(ShipBlueprint blueprint) {
        Set<BlockPos> offsets = new HashSet<>();
        for (ShipBlueprint.ShipBlock block : blueprint.blocks()) {
            offsets.add(new BlockPos(block.dx(), block.dy(), block.dz()));
        }
        return offsets;
    }

    /**
     * REQ-013/AC-013 core contract: after the ship's live structure grows from N to N+1 blocks
     * (via {@link ShipEntity#setBlueprint}, standing in for T14's not-yet-built edit-commit — see
     * class javadoc), a LATER, SEPARATE Edit Mode request — driven through the real {@link
     * ShipEntity#interact} player gesture, which internally calls {@link
     * ShipAssemblyService#openEditMode} — must open a builder preview whose block list is EXACTLY
     * the current N+1 blueprint — never the N-block blueprint the ship was originally assembled
     * with.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void reopenedBuilderPreviewMatchesLiveStructureNotStaleAssemblyTimeSnapshot(GameTestHelper helper) {
        AssembledShip assembled = assembleShip(helper);
        if (assembled == null) {
            return;
        }
        ShipEntity ship = assembled.ship();
        ServerPlayer pilot = assembled.pilot();

        int originalBlockCount = ship.getBlockCount();
        ShipBlueprint originalBlueprint = ship.getBlueprint();

        // Real, independently-scanned N+1 blueprint (see scanRealExpandedBlueprint's javadoc).
        ShipBlueprint expandedBlueprint = scanRealExpandedBlueprint(helper);
        if (expandedBlueprint.blockCount() != originalBlockCount + 1) {
            helper.fail("test precondition: expected the scratch structure to scan to exactly "
                    + (originalBlockCount + 1) + " blocks (N+1), got " + expandedBlueprint.blockCount());
            return;
        }

        // Stand-in for T14's not-yet-built atomic edit-commit (see class javadoc): the ONE prior
        // edit-session commit the test-plan's DoD calls for, applied via the real, only-existing
        // production mutator for "this is now the ship's live structure".
        ship.setBlueprint(expandedBlueprint);

        if (ship.getBlockCount() != originalBlockCount + 1) {
            helper.fail("test precondition: expected ship.getBlockCount() to be N+1 ("
                    + (originalBlockCount + 1) + ") after the stand-in commit, got " + ship.getBlockCount());
            return;
        }

        // Later, SEPARATE interaction: the player re-requests Edit Mode against the NOW-(N+1)
        // ship — via the REAL production entry point (REQ-013 remediation, see class javadoc),
        // not by calling ShipAssemblyService.openEditMode directly. The pilot has been mounted
        // continuously since assembleShip() (never dismounted), so player.getVehicle() == ship
        // already holds; placePilotAtOffset keeps them within EditModeDistanceGate's 5-block
        // Euclidean range, and the explicit empty-hand set matches this gesture's exact
        // precondition (heldItem.isEmpty()) in ShipEntity#interact.
        placePilotAtOffset(ship, pilot, 0, 0, 0);
        pilot.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        if (pilot.getVehicle() != ship) {
            helper.fail("test precondition: expected the pilot to still be mounted on the ship "
                    + "(this gesture requires player.getVehicle() == ship), got " + pilot.getVehicle());
            return;
        }
        InteractionResult interactResult = ship.interact(pilot, InteractionHand.MAIN_HAND);
        if (interactResult != InteractionResult.CONSUME) {
            helper.fail("expected CONSUME from the real interact() Edit Mode gesture, got " + interactResult);
            return;
        }
        if (!ship.isEditModeActive()) {
            helper.fail("expected Edit Mode to be active on the live N+1 ship after the real "
                    + "interact() gesture, got isEditModeActive()=false");
            return;
        }

        BuilderPreviewS2CPayload sent = ShipAssemblyService.lastPreviewSent(pilot.getUUID());
        if (sent == null) {
            helper.fail("expected a builder preview payload to be sent on Edit Mode open — got none");
            return;
        }
        if (sent.blueprintNbt() == null) {
            helper.fail("expected the sent preview payload to carry a real blueprint, got null blueprintNbt");
            return;
        }

        ShipBlueprint decoded = ShipBlueprint.fromNbt(sent.blueprintNbt(), helper.getLevel().registryAccess());

        // The falsifying assertions: compare against a FRESH, live read of the ship's blueprint at
        // assertion time (not a value captured before the setBlueprint() commit above) -- and
        // explicitly against the ORIGINAL (N-block) blueprint too, so a stale-snapshot regression
        // would be caught here, not just an "N+1 happens to equal N+1" tautology.
        Set<BlockPos> decodedOffsets = localOffsets(decoded);
        Set<BlockPos> liveOffsets = localOffsets(ship.getBlueprint());
        Set<BlockPos> staleOffsets = localOffsets(originalBlueprint);

        if (decoded.blockCount() != originalBlockCount + 1) {
            helper.fail("expected reopened builder preview block COUNT to be N+1 ("
                    + (originalBlockCount + 1) + "), got " + decoded.blockCount()
                    + " -- looks like a stale snapshot from before the structural change");
            return;
        }
        if (!decodedOffsets.equals(liveOffsets)) {
            helper.fail("expected reopened builder preview block list to exactly match the ship's "
                    + "LIVE current blueprint, got a mismatch (decoded=" + decodedOffsets
                    + ", live=" + liveOffsets + ")");
            return;
        }
        if (decodedOffsets.equals(staleOffsets)) {
            helper.fail("reopened builder preview block list matches the STALE pre-edit (N-block) "
                    + "blueprint instead of the current (N+1) one -- exactly the false positive "
                    + "this test exists to catch");
            return;
        }

        helper.succeed();
    }

    // ═══════════════════════════════════════════════════════════════════
    // REQ-013/T13 REMEDIATION ROUND 2 (user decision, code-review finding): a dismounted-but-
    // nearby pilot's own real entry point (ShipEntity#interact's new isAnchored()-gated branch,
    // see that method's javadoc for the full rationale). Unlike every test above (and
    // EditModeDistanceGameTest, which drives ShipEntity#tryEnterEditMode DIRECTLY), the two
    // methods below drive the SAME real, production ShipEntity#interact() gesture every other
    // GameTest in this package uses -- with the pilot GENUINELY dismounted
    // (player.getVehicle() != ship, via a real pilot.stopRiding() call, not merely never having
    // mounted) and the ship GENUINELY anchored (via a real ship.toggleAnchor(pilot) call) -- so
    // this is the first proof anywhere in this codebase that EditModeDistanceGate's Euclidean
    // 5-block boundary is reachable by an actual player action, not just a direct/GameTest call
    // to tryEnterEditMode(). Offsets (3,3,0)/(3,4,1) are reused verbatim from
    // EditModeDistanceGameTest's own accept/reject cases -- the same genuinely-diagonal,
    // non-axis-aligned pair the test-plan names as the sharpest Euclidean-vs-Manhattan
    // discriminator (Euclidean ~4.24 accepts, ~5.099 just past the <=5 boundary rejects).
    // ═══════════════════════════════════════════════════════════════════

    /** @return {@code false} if the dismount precondition failed (a {@code helper.fail} was already issued; the caller must return immediately). */
    private static boolean placePilotAtOffsetAndDismount(GameTestHelper helper, ShipEntity ship, ServerPlayer pilot,
                                                           double dx, double dy, double dz) {
        pilot.stopRiding();
        if (pilot.getVehicle() == ship || ship.getPassengers().contains(pilot)) {
            helper.fail("test precondition: expected the pilot to have actually dismounted");
            return false;
        }
        placePilotAtOffset(ship, pilot, dx, dy, dz);
        pilot.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return true;
    }

    /**
     * REQ-012/AC-012 exercised through the REAL REQ-013 dismounted-pilot entry point: a pilot who
     * has genuinely dismounted (real {@code stopRiding()}, not merely never-mounted), standing at
     * the (3,3,0) diagonal offset (Euclidean ~4.24, within the <=5 boundary) from the Control
     * Anchor, on a ship the pilot has explicitly anchored (the gesture's own disambiguator, see
     * {@code ShipEntity#interact}'s javadoc), empty-hand right-clicks the ship entity -- the exact
     * same {@code ship.interact(pilot, InteractionHand.MAIN_HAND)} call every other GameTest in
     * this package uses to simulate a player action. Must ACCEPT: Edit Mode opens, and the pilot
     * is NOT mounted as a side effect (this gesture never falls through to the mount path).
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void dismountedNearbyPilotOpensEditModeThroughRealInteractEntryPoint(GameTestHelper helper) {
        AssembledShip assembled = assembleShip(helper);
        if (assembled == null) {
            return;
        }
        ShipEntity ship = assembled.ship();
        ServerPlayer pilot = assembled.pilot();

        ship.toggleAnchor(pilot);
        if (!ship.isAnchored()) {
            helper.fail("test precondition: expected the ship to be anchored after toggleAnchor()");
            return;
        }

        if (!placePilotAtOffsetAndDismount(helper, ship, pilot, 3, 3, 0)) {
            return;
        }

        InteractionResult result = ship.interact(pilot, InteractionHand.MAIN_HAND);

        if (result != InteractionResult.CONSUME) {
            helper.fail("expected CONSUME from the real dismounted-pilot Edit Mode gesture at "
                    + "offset (3,3,0), got " + result);
            return;
        }
        if (!ship.isEditModeActive()) {
            helper.fail("expected Edit Mode to be ACCEPTED (isEditModeActive()=true) for a "
                    + "dismounted pilot at Euclidean distance ~4.24 (offset 3,3,0), well within "
                    + "the <=5 boundary, through the real interact() entry point");
            return;
        }
        if (pilot.getVehicle() == ship) {
            helper.fail("expected the dismounted-pilot Edit Mode gesture to NOT also mount the "
                    + "pilot as a side effect -- got pilot mounted after an ACCEPTED edit-mode "
                    + "open");
            return;
        }
        helper.succeed();
    }

    /**
     * REQ-012/AC-012's distance boundary exercised through the SAME real entry point, just past
     * it: offset (3,4,1), Euclidean ~5.099. Must REJECT with {@code REJECTED_TOO_FAR} -- the
     * concrete proof this remediation exists to establish: that the distance gate is no longer
     * dead code for a real, dismounted player, not merely for direct/GameTest calls to
     * tryEnterEditMode(). Per this gesture's own "don't fall through to mount on rejection"
     * design (see {@code ShipEntity#interact}'s javadoc), the pilot must end this test still
     * genuinely dismounted -- a bug that silently mounted them instead of honestly reporting
     * the rejection would pass a shallower assertion that only checked isEditModeActive().
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void dismountedPilotBeyondFiveBlocksRejectedThroughRealInteractEntryPoint(GameTestHelper helper) {
        AssembledShip assembled = assembleShip(helper);
        if (assembled == null) {
            return;
        }
        ShipEntity ship = assembled.ship();
        ServerPlayer pilot = assembled.pilot();

        ship.toggleAnchor(pilot);
        if (!ship.isAnchored()) {
            helper.fail("test precondition: expected the ship to be anchored after toggleAnchor()");
            return;
        }

        if (!placePilotAtOffsetAndDismount(helper, ship, pilot, 3, 4, 1)) {
            return;
        }

        InteractionResult result = ship.interact(pilot, InteractionHand.MAIN_HAND);

        if (result != InteractionResult.CONSUME) {
            helper.fail("expected CONSUME (the gesture still handles/consumes the click even on "
                    + "rejection) at offset (3,4,1), got " + result);
            return;
        }
        if (ship.isEditModeActive()) {
            helper.fail("expected Edit Mode to be REJECTED (isEditModeActive() must remain "
                    + "false) for a dismounted pilot at Euclidean distance ~5.099 (offset "
                    + "3,4,1), just past the <=5 boundary, through the real interact() entry "
                    + "point -- AC-012: 'erfolgt keine Zustandsänderung'");
            return;
        }
        if (pilot.getVehicle() == ship) {
            helper.fail("expected the rejected dismounted-pilot Edit Mode gesture to NOT fall "
                    + "through and silently mount the pilot instead -- got pilot mounted after a "
                    + "rejection, which would mask the rejection from a real player");
            return;
        }
        helper.succeed();
    }
}
