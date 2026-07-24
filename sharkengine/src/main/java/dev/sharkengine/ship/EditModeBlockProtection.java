package dev.sharkengine.ship;

import dev.sharkengine.content.ModEntities;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * REQ-014/T14 remediation round 3 (security-review BLOCKER): once {@link
 * ShipAssemblyService#materializeForEdit} places a ship's blueprint into the world as real blocks,
 * those blocks were, until this class existed, ordinary UNPROTECTED world state for the entire
 * (currently untimed) Edit Mode window — ANY nearby player, not just the editing pilot, could
 * break or place blocks in that footprint. That is exactly the RISK-004 "Blöcke duplizieren oder
 * verlieren" failure REQ-014 exists to close, reachable via a third party: a bystander's foreign
 * block could get silently folded into the eventual commit, or a block a bystander broke could go
 * unnoticed; on a rejected commit, {@link ShipAssemblyService#commitEdit}'s rollback clears the
 * WHOLE footprint to air with NO item drops — destroying anything a bystander (or the pilot,
 * experimenting) placed there, with zero compensation.
 *
 * <p><b>The fix:</b> while a ship's {@link ShipEntity#isEditModeActive()} is {@code true}, any
 * block-break or block-place attempt landing on (or, for placement, immediately touching — see
 * below) a position in that ship's OWN materialized footprint ({@link
 * ShipAssemblyService#materializedFootprint}, reused verbatim, not a second independently-computed
 * definition) is cancelled UNLESS the acting player {@link ShipEntity#isPilot} for that specific
 * ship. The pilot's own break/place within their own ship's footprint is deliberately let through
 * completely unchanged — this class never overrides or duplicates any existing authorization
 * logic, it only adds a new precondition in front of vanilla block mutation.</p>
 *
 * <p><b>Mechanism (investigated fresh — this codebase had zero precedent for block-break/place
 * protection before this task; verified against Fabric API's own {@code
 * ServerPlayerInteractionManagerMixin} in {@code fabric-events-interaction-v0}, not assumed):</b></p>
 * <ul>
 *   <li>{@link PlayerBlockBreakEvents#BEFORE} — the mixin injects immediately before {@code
 *   Block.playerWillDestroy}/{@code Block.destroy} inside {@code ServerPlayerGameMode#destroyBlock}
 *   (survival AND creative both route through this one method) and short-circuits the whole call
 *   the moment this listener returns {@code false}: the target block is left completely untouched
 *   (not merely "un-dropped" — {@code level.removeBlock} itself never runs).</li>
 *   <li>{@link UseBlockCallback#EVENT} — the mixin injects at the HEAD of {@code
 *   ServerPlayerGameMode#useItemOn} (the method a block-item right-click placement flows through)
 *   and cancels the ENTIRE call, returning this listener's result directly, the instant it returns
 *   anything other than {@link InteractionResult#PASS} — vanilla's own placement logic inside that
 *   method never runs at all.</li>
 * </ul>
 *
 * <p><b>Placement is guarded against the footprint's own 6-neighbor shell, not just exact
 * membership (closes the cross-ship BFS bridging gap as far as a live event listener can, see
 * {@code ShipAssemblyService} class docs / this task's own findings for the full reasoning):</b>
 * {@link ShipAssemblyService#scanStructure}'s BFS connects any two {@code ship_eligible} blocks
 * that are simply adjacent — so a new block need not overwrite/replace an existing footprint
 * position to make the footprint BFS-reachable from outside it; sitting immediately next to one
 * (approached from any angle, using an unrelated reference block as the placement anchor, not
 * necessarily the footprint block's own face) is already enough to create that connectivity at the
 * next commit's scan. Checking only "is the new/clicked position an EXACT footprint member" would
 * miss that route entirely; expanding the guarded zone to the footprint's own immediate neighbor
 * shell for placement (not for breaking, which only ever targets a real block's own exact position)
 * closes it for any bridge attempted while the target ship's Edit Mode is active — see this
 * class's own package-level reasoning in the T14 remediation round 3 summary for the ONE scenario
 * this still cannot close: a bridge built before either ship's session was ever opened, when there
 * was nothing yet to protect.</p>
 *
 * <p><b>REQ-014/T14 remediation round 4 (security-review BLOCKER — fixed an overreach introduced
 * by round 3's own fix):</b> round 3's {@code onUseBlock} guarded BOTH the CLICKED block position
 * and the computed PLACED position with the adjacent-shell check. That double guard was wrong:
 * {@link UseBlockCallback#EVENT} fires at the HEAD of {@code ServerPlayerGameMode}'s single
 * top-level right-click-on-block dispatch, which covers EVERY block-use interaction in the game
 * (chests, furnaces, doors, buttons — not just placement attempts), and cancelling based on
 * CLICKED-position proximity meant any bystander right-clicking their own, entirely unrelated,
 * pre-existing block within one tile of an active edit session's footprint got that interaction
 * silently cancelled — opening a chest, pressing a button, or using a door near a ship mid-edit
 * became impossible, for every non-pilot player, for as long as that session stayed open (no
 * timeout). {@code onUseBlock} now guards ONLY the position a new block would actually occupy,
 * computed via the real {@link BlockPlaceContext}/{@link UseOnContext} vanilla classes (not a
 * re-derived approximation — see {@code onUseBlock}'s own inline comments for the decompiled
 * {@code ServerPlayerGameMode#useItemOn} trace this was verified against), gated on the held item
 * actually being a {@link BlockItem} in the first place. A pure "use the existing block"
 * interaction is never touched by this class again, regardless of proximity to any footprint.</p>
 *
 * <p><b>REQ-014/T14 remediation round 5 (2nd occurrence of the same bug class — fixed a false
 * positive introduced by round 4's own fix):</b> round 4 correctly stopped guarding the CLICKED
 * position, but {@code onUseBlock} still unconditionally computed a hypothetical PLACED position
 * via {@link BlockPlaceContext} and guarded on it whenever the held item was a {@link BlockItem} —
 * even when the CLICKED block itself has use-behavior that vanilla's real {@code
 * ServerPlayerGameMode#useItemOn} dispatch always lets consume the interaction FIRST, before
 * placement is ever attempted (a chest opens, a door toggles, a button is pressed — {@code
 * BlockState#useWithoutItem} gets first refusal there, and only an unconsumed result falls through
 * to {@code ItemStack#useOn}/placement). Guarding a placement position vanilla would never actually
 * have attempted meant a bystander right-clicking their OWN, entirely unrelated, pre-existing
 * interactive block (chest, door, button, …) while merely HOLDING a placeable item — not trying to
 * place one — got the whole interaction wrongly cancelled if that block's face happened to point
 * toward an active edit session's footprint. Fully replicating vanilla's per-block
 * interaction-dispatch chain to predict this exactly is fragile (there is no side-effect-free way to
 * ask an arbitrary block whether it would consume a click without invoking it — round 4's own full
 * {@code BlockPlaceContext} simulation already tried the closest thing and still missed this case),
 * so {@code onUseBlock} now special-cases the specific interactive categories named in the original
 * finding instead: {@link BlockState#getMenuProvider} (generically catches chests, furnaces, and any
 * other container/GUI block with no enumeration needed) plus cheap {@code instanceof} checks for
 * {@link DoorBlock}, {@link ButtonBlock}, {@link LeverBlock}, {@link PressurePlateBlock}, and {@link
 * TrapDoorBlock} — the other common interactive-without-menu vanilla block families. When the
 * CLICKED block matches one of these (and the player is not using vanilla's own sneak-bypass, see
 * {@code clickedBlockConsumesInteractionFirst}), {@code onUseBlock} returns {@link
 * InteractionResult#PASS} immediately, without ever computing or guarding a placement position — see
 * the "known, accepted limitations" list below for the narrow residual gap this heuristic
 * deliberately accepts instead of chasing full dispatch prediction.</p>
 *
 * <p><b>Known, ACCEPTED limitations for Release 1 (explicit user decision, not an oversight —
 * matches the treatment already given the "no timeout" and "pre-existing bridge" gaps disclosed
 * above):</b> this class protects only ordinary PLAYER block-break and block-place interactions
 * (the two Fabric events above). It does NOT, and for Release 1 deliberately will NOT, protect a
 * materialized footprint against:</p>
 * <ul>
 *   <li><b>TNT / explosions</b> — {@code Level#explode()}'s block removal never routes through
 *   {@link PlayerBlockBreakEvents} or {@link UseBlockCallback} at all (it calls
 *   {@code level.removeBlock}/{@code Block.wasExploded} directly), so a primed TNT detonated
 *   inside or against a materialized footprint during an active edit session is completely
 *   unguarded by this class.</li>
 *   <li><b>Pistons</b> — a piston pushing or pulling blocks into, out of, or within a materialized
 *   footprint mutates block state directly through {@code PistonBaseBlock}'s own movement logic,
 *   which is not a player-attributed break/place event either and is likewise unguarded.</li>
 *   <li><b>Fire spread</b> — {@code FireBlock}'s own tick-driven spread/burn mutates neighboring
 *   blocks with no player actor and no break/place event at all, so it is unguarded by this class
 *   for the same reason.</li>
 *   <li><b>Exotic/modded interactive blocks not in the round-5 category list</b> — {@code
 *   onUseBlock}'s "does the clicked block consume the interaction first" check is a deliberately
 *   NARROW heuristic (menu-provider blocks, plus {@code DoorBlock}/{@code ButtonBlock}/{@code
 *   LeverBlock}/{@code PressurePlateBlock}/{@code TrapDoorBlock}), not an exhaustive simulation of
 *   every vanilla or modded block's {@code useWithoutItem}. A block type with real use-behavior
 *   that falls outside this list (e.g. a modded block, or a vanilla block with bespoke
 *   click-handling this list does not name) can still be incorrectly guarded — its use wrongly
 *   cancelled — if a hypothetical placement against it would land in an active session's footprint
 *   or shell. This is an accepted, disclosed residual gap, not an oversight: building a fully
 *   exhaustive list (or re-deriving vanilla's per-block dispatch some other way) is exactly the
 *   fragile full-prediction trap this round's fix was written to avoid.</li>
 * </ul>
 * <p>This is a scoped Release 1 decision: player-interaction protection (break/place, including
 * the scaffold-bridging closure above) is accepted as sufficient for Release 1, and TNT/pistons/
 * fire/exotic-interactive-blocks bypassing (or being wrongly caught by) the materialized footprint
 * guard are accepted as a known, disclosed residual risk — not something this class silently fails
 * to mention. Closing these fully would require separate, block-level (not player-event-level)
 * hooks, or a complete re-simulation of vanilla's per-block dispatch, and is out of this task's
 * scope.</p>
 */
public final class EditModeBlockProtection {

    private EditModeBlockProtection() {}

    /** Registers this class's two listeners. Call exactly once, from {@code SharkEngineMod#init}. */
    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register(EditModeBlockProtection::onBreak);
        UseBlockCallback.EVENT.register(EditModeBlockProtection::onUseBlock);
    }

    /**
     * @return {@code false} (cancel the break) iff {@code pos} is an exact member of some OTHER
     *         ship's currently-active-Edit-Mode footprint than {@code player} pilots.
     */
    private static boolean onBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return true;
        }
        if (isGuarded(serverLevel, pos, player, false)) {
            notifyBlocked(player);
            return false;
        }
        return true;
    }

    /**
     * @return {@link InteractionResult#FAIL} (cancel the entire interaction) iff this interaction
     *         would actually PLACE a new block, and the position that new block would land at is
     *         within (or immediately touching) some OTHER ship's currently-active-Edit-Mode
     *         footprint than {@code player} pilots. A pure "use the existing block" interaction
     *         (open a chest, press a button, swing a sword at nothing in particular) is never
     *         cancelled by this method, no matter how close the clicked block is to a footprint —
     *         see the round-3→4 remediation note on the class javadoc for why the CLICKED position
     *         is deliberately never guarded here, only the position a new block would occupy — and
     *         the round-5 note for why the clicked block's OWN use-behavior (chest/door/button/…)
     *         is checked BEFORE a hypothetical placement position is ever computed or guarded.
     */
    private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty() || !(held.getItem() instanceof BlockItem)) {
            // Held item cannot place a block at all (verified against vanilla's own
            // ServerPlayerGameMode#useItemOn dispatch by decompiling it for this fix: item
            // placement is only ever attempted via ItemStack#useOn as the LAST step of that
            // method, after both BlockState#useItemOn and BlockState#useWithoutItem have already
            // had first refusal — a chest's/button's/door's own use always resolves independently
            // of what, if anything, is in hand). So this can only ever be a pure "use the existing
            // block" interaction, never a placement — there is nothing here for a footprint guard
            // to legitimately intercept.
            return InteractionResult.PASS;
        }
        BlockPos clickedPos = hitResult.getBlockPos();
        BlockState clickedState = serverLevel.getBlockState(clickedPos);
        if (clickedBlockConsumesInteractionFirst(player, hand, serverLevel, clickedPos, clickedState)) {
            // REQ-014/T14 round 5: the clicked block is a known-interactive type (menu provider, or
            // door/button/lever/pressure-plate/trapdoor) that vanilla's real useItemOn dispatch
            // always lets consume the click BEFORE placement is ever attempted (BlockState#
            // useWithoutItem gets first refusal, and only an unconsumed result falls through to
            // item placement). Since that use will happen instead of any placement, there is no
            // hypothetical placement position to guard here at all — computing one via
            // BlockPlaceContext (as below) and guarding it would cancel an interaction vanilla
            // would never actually have routed through placement in the first place. See the class
            // javadoc's round-5 remediation note and its "known, accepted limitations" entry for the
            // narrow residual gap this heuristic (not a full dispatch simulation) deliberately
            // accepts.
            return InteractionResult.PASS;
        }
        // Reuses vanilla's OWN BlockPlaceContext(UseOnContext) construction (real vanilla classes,
        // not a re-derivation of their logic) to compute exactly where a new block would land:
        // BlockPlaceContext#getClickedPos() resolves to the clicked position itself when that
        // block is replaceable (BlockState#canBeReplaced — tall grass, snow layers, or an
        // in-progress edit-mode gap left as air) or to the position one step off the clicked face
        // otherwise. Getting this right (rather than always assuming "one step off the clicked
        // face") is what lets the CLICKED-position guard be removed instead of merely relocated —
        // when the clicked block IS itself the placement target (the replaceable case), guarding
        // only "placed" already covers it; when it is not, the clicked block's own face is never a
        // placement target regardless of the shell, exactly as intended.
        BlockPos placed = new BlockPlaceContext(new UseOnContext(player, hand, hitResult)).getClickedPos();
        if (isGuarded(serverLevel, placed, player, true)) {
            notifyBlocked(player);
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    /**
     * Whether the block at {@code clickedPos} is a type vanilla's real {@code
     * ServerPlayerGameMode#useItemOn} dispatch always lets consume the click BEFORE placement is
     * ever attempted — see the class javadoc's round-5 remediation note. This is a narrow,
     * block-family heuristic (menu-provider blocks, plus a short list of common
     * interactive-without-menu types), deliberately NOT an exhaustive simulation of every block's
     * {@code useWithoutItem} — see the "known, accepted limitations" list on the class javadoc.
     *
     * <p>Mirrors two exact, fixed, block-independent gates decompiled straight out of the real
     * {@code ServerPlayerGameMode#useItemOn} bytecode for this fix (not a per-block prediction, so
     * replicating them does not reintroduce the round-4 dispatch-simulation trap — they only refine
     * WHICH blocks this heuristic should even bother checking):</p>
     * <ul>
     *   <li>{@code BlockState#useWithoutItem} (the chest/door/button/… "default block interaction"
     *   step) is only ever invoked by vanilla when acting with the {@link
     *   InteractionHand#MAIN_HAND} — an off-hand right-click skips it entirely regardless of the
     *   clicked block, so this method returns {@code false} outright for {@link
     *   InteractionHand#OFF_HAND}.</li>
     *   <li>A player who is sneaking ({@link Player#isSecondaryUseActive()}) while holding a
     *   non-empty item in either hand bypasses ALL block-use behavior for that click and goes
     *   straight to item placement instead.</li>
     * </ul>
     */
    private static boolean clickedBlockConsumesInteractionFirst(Player player, InteractionHand hand,
            ServerLevel level, BlockPos clickedPos, BlockState clickedState) {
        if (hand != InteractionHand.MAIN_HAND) {
            return false;
        }
        boolean sneakBypassesBlockUse = player.isSecondaryUseActive()
                && (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty());
        if (sneakBypassesBlockUse) {
            return false;
        }
        return isKnownInteractiveBlock(level, clickedPos, clickedState);
    }

    /**
     * @return {@code true} if {@code state} is one of the block families this class recognizes as
     *         always consuming a right-click before placement could ever be attempted: any block
     *         with a {@link BlockState#getMenuProvider} (generically covers chests, furnaces,
     *         shulker boxes, and any other container/GUI block — no per-block enumeration needed),
     *         or one of the common interactive-without-menu vanilla families named in the original
     *         finding — doors, buttons, levers, pressure plates, trapdoors.
     */
    private static boolean isKnownInteractiveBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getMenuProvider(level, pos) != null) {
            return true;
        }
        return state.getBlock() instanceof DoorBlock
                || state.getBlock() instanceof ButtonBlock
                || state.getBlock() instanceof LeverBlock
                || state.getBlock() instanceof PressurePlateBlock
                || state.getBlock() instanceof TrapDoorBlock;
    }

    /** Feedback so a blocked attempt is never a silent, unexplained no-op. */
    private static void notifyBlocked(Player player) {
        if (player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.translatable("message.sharkengine.edit_footprint_protected"));
        }
    }

    /**
     * Whether {@code pos} falls inside a protected zone that {@code actor} is not exempt from.
     * Searches every {@link ShipEntity} within {@link ShipAssemblyService#MAX_RADIUS} + 1 of
     * {@code pos} whose Edit Mode is currently active (a ship whose session is closed protects
     * nothing), skips any ship {@code actor} personally pilots (their own edits are unaffected),
     * and treats a match as: {@code pos} is an exact member of that ship's footprint, OR — only
     * when {@code includeAdjacentShell} is {@code true} (placement checks; never breaking, see
     * {@link #onBreak}) — {@code pos} is one of a footprint member's 6 cardinal/vertical
     * neighbors.
     */
    private static boolean isGuarded(ServerLevel level, BlockPos pos, Player actor, boolean includeAdjacentShell) {
        List<ShipEntity> candidates = level.getEntities(ModEntities.SHIP,
                new AABB(pos).inflate(ShipAssemblyService.MAX_RADIUS + 1),
                ShipEntity::isEditModeActive);
        for (ShipEntity ship : candidates) {
            if (ship.isPilot(actor)) {
                continue;
            }
            Set<BlockPos> footprint = new HashSet<>(ShipAssemblyService.materializedFootprint(ship));
            if (footprint.contains(pos)) {
                return true;
            }
            if (includeAdjacentShell) {
                for (Direction d : Direction.values()) {
                    if (footprint.contains(pos.relative(d))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
