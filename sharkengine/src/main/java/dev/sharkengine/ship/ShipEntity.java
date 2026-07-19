package dev.sharkengine.ship;

import dev.sharkengine.net.ShipBlueprintS2CPayload;
import dev.sharkengine.ship.part.ShipPartAnalyzer;
import dev.sharkengine.ship.part.ShipStats;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ship entity for controllable flying vehicles (air ships).
 * Supports vertical movement, acceleration phases, fuel system,
 * weight-based speed limits, BUG-based direction, and health.
 *
 * <p>Direction rule: The BUG block's FACING property defines the
 * vehicle's forward direction. Player look direction has no effect
 * on movement direction. Thrusters provide thrust only, not direction.</p>
 *
 * @author Shark Engine Team
 * @version 3.0 (Bug-Fix + Health + BuildMode)
 */
public final class ShipEntity extends Entity {
    private static final EntityDataAccessor<Boolean> ANCHORED =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SYNC_FUEL =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SYNC_SPEED =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> SYNC_BLOCK_COUNT =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.INT);
    /**
     * Synced total ship mass (AIR-023), the same value the server uses to
     * determine {@link WeightCategory}/max speed. Without this, a client-side
     * HUD that recomputed WeightCategory from {@link #SYNC_BLOCK_COUNT} alone
     * would disagree with the server for any mixed-mass ship — e.g. a
     * handful of heavy {@code helicopter_engine} blocks reads as "light" by
     * block count but is actually OVERLOADED by mass, so the HUD would show
     * full speed on a ship the server refuses to fly at more than 0 b/s.
     */
    private static final EntityDataAccessor<Integer> SYNC_MASS =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SYNC_ENGINE_OUT =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SYNC_HEALTH =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.INT);
    /**
     * Synced current turn input (FLR-001, docs/plans/flight-bank-roll.md). The
     * server-side {@link #inputTurn} field this mirrors is never itself
     * synced — rendering runs for every observer (including a third party
     * watching someone else's ship), not just the local pilot whose own
     * {@code HelmInputClient} already knows its own last-sent value, so the
     * renderer needs a server-authoritative copy to compute bank angle from.
     */
    private static final EntityDataAccessor<Float> SYNC_TURN =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.FLOAT);
    /**
     * Synced current vertical input (FLP-001, docs/plans/flight-pitch.md) — the
     * direct analog of {@link #SYNC_TURN} for the pitch feature. Mirrors the
     * server-side {@link #inputVertical} field for the same reason: rendering
     * runs for every observer, not just the local pilot.
     */
    private static final EntityDataAccessor<Float> SYNC_VERTICAL =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.FLOAT);

    // ═══════════════════════════════════════════════════════════════════
    // FIELDS
    // ═══════════════════════════════════════════════════════════════════

    private float inputThrottle; // -1..+1 (vertical)
    private float inputTurn;     // -1..+1 (rotation)
    private ShipBlueprint blueprint;
    private UUID pilot;

    /**
     * REQ-009/T07: the currently-mounted copilot's UUID, or {@code null} if the copilot
     * seat is empty — a stored occupant reference, the same shape as {@link #pilot}. The
     * T07 falsifying-test contract's sharpest named risk is an occupancy check that
     * OVERWRITES this reference on a second interact instead of rejecting it outright, so a
     * second player silently displaces the first with no dismount event; {@link
     * #mountCopilot} guards against exactly that — see its javadoc.
     */
    private UUID copilot;

    /**
     * REQ-009/T07 remediation (QA finding: {@code secondPlayerCannotDisplaceFirstCopilot}'s
     * javadoc/fail-messages claimed to rule out "even an internal dismount-and-remount cycle"
     * for the first copilot, but every assertion was a post-hoc end-state check with zero
     * instrumentation on the actual mount/dismount machinery -- a hypothetical internal
     * displace-then-remount that preserved final state would still have passed silently).
     * Counts, per passenger UUID, how many times {@link #addPassenger} has actually fired for
     * them -- i.e. real mount events via ANY path (vanilla's own machinery included), not just
     * the call sites this class happens to control today. Tests use this to assert a specific
     * player's count did NOT increment while a different player's interaction was processed,
     * which a pure end-state check cannot.
     *
     * <p>Bounded growth (non-blocking pattern flagged across T01/T02/T03's own per-player
     * maps -- {@code TutorialService#lastPopupSent}, {@code ShipAssemblyService#lastPreviewSent},
     * {@code TutorialService#lastModeLockedNotice} -- all of which are {@code static}
     * server-lifetime maps keyed by every player who ever connects; this one is materially
     * different in scope, since it is a per-{@code ShipEntity} INSTANCE field, so its lifetime
     * and key cardinality are already bounded by this specific ship's own lifetime and the set
     * of distinct players who actually rode it, not the server's whole population. There is no
     * existing player-disconnect event hook anywhere in this codebase to reuse (checked), and
     * this ship entity is not tracked in any global registry a disconnect listener could use to
     * find and clear it, so "clear on disconnect" would require adding both a new event
     * registration AND a new global live-ship registry -- real new infrastructure, not the "few
     * lines" this cleanup pass is scoped to. {@link #MAX_TRACKED_MOUNT_COUNTS} instead caps this
     * map's size defensively in {@link #addPassenger}: since this map is pure test/inspection
     * instrumentation (never consulted by {@link #mountCopilot}'s actual occupancy guard, which
     * uses {@link #copilot} directly), resetting it past the cap loses no gameplay-relevant
     * state, only long-tail historical mount-count instrumentation for a ship far past any
     * realistic distinct-rider count.</p>
     */
    private static final int MAX_TRACKED_MOUNT_COUNTS = 64;

    private final Map<UUID, Integer> passengerMountCounts = new HashMap<>();

    /** Vehicle class (AIR for MVP) */
    private VehicleClass vehicleClass = VehicleClass.AIR;

    /** Current acceleration phase (1-5) */
    private AccelerationPhase phase = AccelerationPhase.PHASE_1;

    /** Ticks since acceleration started (20 ticks = 1 second) */
    private int accelerationTicks = 0;

    /** Current speed in blocks/sec */
    private float currentSpeed = 0.0f;

    /** Maximum possible speed (based on weight) */
    private float maxSpeed = 30.0f;

    /** Height penalty multiplier (0.4-1.0) */
    private float heightPenalty = 1.0f;

    /** Number of blocks in the ship */
    private int blockCount = 0;

    /**
     * Total ship mass (AIR-023) — sum of every part's mass via
     * {@link ShipPartAnalyzer}, the single source of truth for weight (no
     * second, block-count-based aggregation). Drives {@link #weightCategory}
     * and {@link #maxSpeed}; synced to the client via {@link #SYNC_MASS} so
     * the HUD ({@code FuelHudOverlay}) reads the exact value the server used,
     * instead of recomputing an approximation from block count.
     */
    private int mass = 0;

    /** Current weight category */
    private WeightCategory weightCategory = WeightCategory.LIGHT;

    /** Whether the blueprint contains thruster blocks */
    private boolean hasThrusters = false;

    /** Fuel level in energy units (0-100) */
    private int fuelLevel = 100;

    /** Engine out flag (when fuel depleted) */
    private boolean engineOut = false;

    /** Forward input (0..1, W-key for acceleration) */
    private float inputForward = 0.0f;

    /** Vertical input (-1..+1, Space/Shift) */
    private float inputVertical = 0.0f;

    /** Tick counter for per-second fuel consumption */
    private int fuelConsumptionTick = 0;

    /**
     * Fractional energy not yet subtracted from {@link #fuelLevel} (2026-07-13
     * fuel-duration tuning). {@link dev.sharkengine.ship.part.VehicleBalance#FUEL_CONSUMPTION_RATE}
     * quarters the nominal per-second consumption from {@link ShipPhysics#calculateFuelConsumption},
     * which no longer divides evenly into the integer {@link #fuelLevel} — this
     * accumulator carries the sub-1 remainder across ticks so the average
     * long-run burn rate is still exactly the intended fraction, not rounded
     * away every second.
     */
    private float fuelDebt = 0.0f;

    // ═══════════════════════════════════════════════════════════════════
    // BUG-BASED DIRECTION (replaces thruster direction)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * The forward direction angle in degrees (world-space yaw).
     * Determined exclusively by the BUG block's FACING property at assembly.
     * This is the single source of truth for vehicle orientation.
     */
    private float bugYawDeg = 0.0f;

    // ═══════════════════════════════════════════════════════════════════
    // CLIENT-SIDE POSITION/ROTATION INTERPOLATION
    //
    // Recovered 2026-07-12 from a dangling commit (c240f80, 2026-07-03,
    // "Fix: implement real client-side interpolation for ShipEntity (was a
    // no-op)") orphaned by an earlier history-recovery reset in this same
    // session — found via `git fsck --unreachable` after a live playtest
    // ("ruckelt... wird mit steigender Geschwindigkeit immer doller") that
    // this session's own setOldPosAndRot() fix (a real but insufficient fix
    // for a *different* staleness bug) did not resolve.
    //
    // Root cause (ground-truthed by decompiling this project's own vanilla
    // 1.21.1 jar, per that commit): Entity.lerpTo() — the base class
    // ShipEntity extends directly — is just
    // "this.setPos(x,y,z); this.setRot(yaw,pitch);", an immediate teleport
    // that completely ignores its own "steps" parameter despite the name.
    // Every server position/rotation sync (~20 times/sec) therefore snapped
    // the client straight to the new value with zero smoothing — worse at
    // higher speed because each sync covers proportionally more distance,
    // exactly matching the reported "worse with increasing speed" pattern.
    //
    // Fixed using the same pattern vanilla's own AbstractMinecart uses:
    // lerpTo() now stores the incoming target and a step count instead of
    // teleporting; tick()'s client branch calls the inherited
    // Entity.lerpPositionAndRotationStep() each tick to move 1/lerpSteps of
    // the way there — gradual, multi-tick smoothing instead of instant
    // snapping.
    // ═══════════════════════════════════════════════════════════════════

    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;
    private double lerpYRot, lerpXRot;

    /**
     * Client-only smoothed bank/roll angle (FLR-003,
     * docs/plans/flight-bank-roll.md). Deliberately NOT an EntityData sync
     * field — this is pure rendering-interpolation state, derived every
     * client tick from the already-synced {@link #SYNC_TURN} via
     * {@link ShipTransform#rollFromTurnInput}, the same way vanilla entities
     * keep purely-cosmetic animation state as plain client-side fields
     * rather than syncing it. Read by {@code ShipEntityRenderer}.
     */
    private float clientRoll = 0.0f;

    public float getClientRoll() { return clientRoll; }

    /**
     * Client-only smoothed pitch angle (FLP-003, docs/plans/flight-pitch.md).
     * Direct analog of {@link #clientRoll}: derived every client tick from
     * {@link #SYNC_VERTICAL} via {@link ShipTransform#pitchFromVerticalInput},
     * never itself synced. Read by {@code ShipEntityRenderer}.
     */
    private float clientPitch = 0.0f;

    public float getClientPitch() { return clientPitch; }

    // ═══════════════════════════════════════════════════════════════════
    // FEATURE 5: VEHICLE HEALTH
    // ═══════════════════════════════════════════════════════════════════

    public static final int MAX_HEALTH = 100;
    private static final float COLLISION_SPEED_THRESHOLD = 5.0f; // blocks/sec
    private static final float COLLISION_DAMAGE_MULTIPLIER = 2.0f;
    private static final float EXPLOSION_DAMAGE = 40.0f;
    private static final float ATTACK_DAMAGE_DEFAULT = 5.0f;

    private int health = MAX_HEALTH;
    private int damageCooldownTicks = 0;

    public ShipEntity(EntityType<? extends ShipEntity> type, Level level) {
        super(type, level);
        // P0 hotfix (2026-07-12, live playtest: "durchgehendes feines
        // Zittern/Tearing", ship-specific, not present walking normally):
        // ShipPhysics.checkCollision (tick()) is our own, purpose-built
        // collision system, run BEFORE this.move() every tick. But
        // Entity.move() ALSO runs vanilla's own collision resolution using
        // this entity's small 2.5x1.5 hitbox (ModEntities.java) against
        // world geometry whenever noPhysics is false (the default) —
        // confirmed via javap on the real Entity.class: move() branches on
        // noPhysics right at the top; false takes the full collide()/
        // step-up/edge-backoff pipeline, true does a single unconditional
        // setPos() with no collision math at all. A small hitbox grazing
        // leaves/fences/any partial-collision block while flying makes that
        // vanilla pipeline apply constant tiny corrective nudges — a real
        // per-tick position jitter, independent of and invisible to our own
        // collision system, and not fixable by any render-side
        // interpolation change (which is why the earlier setOldPosAndRot
        // fix had zero effect on it). Disabling it here hands movement
        // entirely to our own already-existing collision check.
        this.noPhysics = true;
    }

    // ═══════════════════════════════════════════════════════════════════
    // BLUEPRINT & PILOT
    // ═══════════════════════════════════════════════════════════════════

    public void setBlueprint(ShipBlueprint blueprint) {
        this.blueprint = blueprint;
        applyBlueprintStats();
    }

    public ShipBlueprint getBlueprint() {
        return this.blueprint;
    }

    public void setPilot(ServerPlayer sp) {
        this.pilot = sp.getUUID();
    }

    public boolean isPilot(Player p) {
        return pilot != null && pilot.equals(p.getUUID());
    }

    /**
     * REQ-009/T07: the copilot seat's server-authoritative occupant reference, or {@code
     * null} if empty. See {@link #copilot}'s javadoc for the falsifying-test contract this
     * exists to satisfy.
     */
    public UUID getCopilot() {
        return copilot;
    }

    public boolean isCopilot(Player p) {
        return copilot != null && copilot.equals(p.getUUID());
    }

    /**
     * Vanilla hook fired by {@code Entity#startRiding} on the vehicle (this ship) whenever
     * ANY passenger actually mounts it -- through {@link #mountCopilot}, the {@link #interact}
     * pilot fallback, or any other path, present or future. Recorded per-UUID in {@link
     * #passengerMountCounts} so tests can assert a specific player's mount count did NOT
     * change while unrelated interactions were processed (see {@link #getMountCount}),
     * catching a hypothetical internal dismount-and-remount cycle that a pure end-state check
     * would miss.
     */
    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        // Defensive size cap (see passengerMountCounts' javadoc): only ever trims when a
        // genuinely NEW rider would push this ship past MAX_TRACKED_MOUNT_COUNTS distinct
        // UUIDs -- an existing rider's own running count is never reset by their own repeat
        // mounts, only by some other, previously-untracked player's arrival once the map is
        // already at the cap.
        if (passengerMountCounts.size() >= MAX_TRACKED_MOUNT_COUNTS
                && !passengerMountCounts.containsKey(passenger.getUUID())) {
            passengerMountCounts.clear();
        }
        passengerMountCounts.merge(passenger.getUUID(), 1, Integer::sum);
    }

    /**
     * REQ-009/T07 remediation: how many times {@code playerId} has actually mounted this ship
     * (real {@link #addPassenger} events, any path) since it was spawned. See {@link
     * #passengerMountCounts}'s javadoc for why this exists.
     */
    public int getMountCount(UUID playerId) {
        return passengerMountCounts.getOrDefault(playerId, 0);
    }

    /**
     * REQ-009/T07: whether this ship's blueprint carries at least one COPILOT-role {@code
     * SeatAnchor} — i.e. whether a {@code copilot_seat} block was actually part of the
     * assembled structure. A copilot mount attempt is only ever honored when this is {@code
     * true}; ships assembled before the copilot seat existed (or without one placed) have no
     * copilot seat to occupy at all.
     */
    private boolean hasCopilotSeat() {
        return blueprint != null && blueprint.seatAnchors().stream()
                .anyMatch(anchor -> anchor.role() == ShipBlueprint.SeatRole.COPILOT);
    }

    /**
     * Mounts {@code player} into this ship's copilot seat (REQ-009/AC-009), or rejects the
     * attempt outright if the seat is already occupied or the ship has no copilot seat at
     * all.
     *
     * <p>REQ-009/AC-009's sharpest named risk (test-plan, "REQ-009 — Craftable copilot
     * seat"): an occupancy check that silently OVERWRITES {@link #copilot} on a second
     * interact instead of rejecting it — so a second player displaces the first with no
     * dismount event, desyncing the first player's client. This method's very first branch
     * is the guard against exactly that: when {@link #copilot} is already set to a player who
     * is still actually riding, this returns {@code false} immediately and never assigns
     * {@link #copilot} or calls {@code startRiding} — the existing occupant is left
     * completely untouched (no dismount-and-remount cycle, not even internally).</p>
     *
     * <p>The one exception is a defensive self-heal: if {@link #copilot} still names a
     * player who is verifiably no longer among {@link #getPassengers()} (e.g. they dismounted
     * via vanilla's own sneak-to-dismount path, which does not go through this method), the
     * stale reference is cleared before re-checking — otherwise a copilot who left would
     * permanently lock the seat for everyone else. This never touches a still-mounted
     * occupant's reference.</p>
     *
     * @return {@code true} if {@code player} was mounted as the new copilot, {@code false} if
     *         the attempt was rejected
     */
    private boolean mountCopilot(Player player) {
        if (copilot != null) {
            boolean stillRiding = getPassengers().stream().anyMatch(p -> p.getUUID().equals(copilot));
            if (stillRiding) {
                return false; // seat genuinely occupied -- reject, no state change whatsoever
            }
            copilot = null; // stale reference to a passenger who already left some other way
        }
        if (!hasCopilotSeat()) {
            return false;
        }
        copilot = player.getUUID();
        player.startRiding(this, true);
        return true;
    }

    // ═══════════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════════

    public VehicleClass getVehicleClass() { return vehicleClass; }
    public AccelerationPhase getPhase() { return phase; }
    public int getAccelerationTicks() { return accelerationTicks; }

    public float getCurrentSpeed() {
        return level().isClientSide ? this.entityData.get(SYNC_SPEED) : currentSpeed;
    }

    public float getMaxSpeed() { return maxSpeed; }
    public float getHeightPenalty() { return heightPenalty; }

    public int getBlockCount() {
        return level().isClientSide ? this.entityData.get(SYNC_BLOCK_COUNT) : blockCount;
    }

    /**
     * Total ship mass (AIR-023). On the client this reads the server-synced
     * value ({@link #SYNC_MASS}) rather than recomputing it locally — see
     * that accessor's javadoc for why a client-local recomputation from
     * block count would be able to disagree with the server for mixed-mass
     * ships.
     */
    public int getMass() {
        return level().isClientSide ? this.entityData.get(SYNC_MASS) : mass;
    }

    public WeightCategory getWeightCategory() {
        return WeightCategory.fromMass(getMass());
    }

    public int getFuelLevel() {
        return level().isClientSide ? this.entityData.get(SYNC_FUEL) : fuelLevel;
    }

    public boolean isEngineOut() {
        return level().isClientSide ? this.entityData.get(SYNC_ENGINE_OUT) : engineOut;
    }

    /**
     * Synced current turn input, -1..+1 (FLR-001). Client-side consumer:
     * {@code ShipEntityRenderer}'s bank/roll rendering (FLR-003) — see that
     * class for the sign convention this value feeds into.
     */
    public float getSyncedTurn() {
        return level().isClientSide ? this.entityData.get(SYNC_TURN) : inputTurn;
    }

    /**
     * Synced current vertical input, -1..+1 (FLP-001). Client-side consumer:
     * {@code ShipEntityRenderer}'s pitch rendering (FLP-003) — see that class
     * for the sign convention this value feeds into.
     */
    public float getSyncedVertical() {
        return level().isClientSide ? this.entityData.get(SYNC_VERTICAL) : inputVertical;
    }

    public boolean hasThrusters() { return hasThrusters; }
    public float getInputForward() { return inputForward; }
    public float getBugYawDeg() { return bugYawDeg; }

    public void setBugYawDeg(float yaw) { this.bugYawDeg = yaw; }

    public void setInputForward(float inputForward) {
        this.inputForward = clamp(inputForward, 0.0f, 1.0f);
    }

    public float getInputVertical() { return inputVertical; }

    public void setInputVertical(float inputVertical) {
        this.inputVertical = clamp(inputVertical, -1.0f, 1.0f);
    }

    // ═══════════════════════════════════════════════════════════════════
    // HEALTH GETTERS (Feature 5)
    // ═══════════════════════════════════════════════════════════════════

    public int getHealth() {
        return level().isClientSide ? this.entityData.get(SYNC_HEALTH) : health;
    }

    public int getMaxHealth() { return MAX_HEALTH; }

    public boolean isDestroyed() { return getHealth() <= 0; }

    // ═══════════════════════════════════════════════════════════════════
    // FUEL REFILL (Bug 3 Fix)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Adds fuel to the ship's fuel tank.
     *
     * @param woodCount Number of wood blocks to convert to fuel
     * @return Actual fuel added (may be capped at maximum)
     */
    public int addFuel(int woodCount) {
        int energyToAdd = FuelSystem.woodToEnergy(woodCount);
        int oldFuel = fuelLevel;

        fuelLevel = Math.min(FuelSystem.MAX_FUEL, fuelLevel + energyToAdd);

        if (fuelLevel > 0) {
            engineOut = false;
        }

        int added = fuelLevel - oldFuel;

        // Sync immediately
        if (!level().isClientSide) {
            this.entityData.set(SYNC_FUEL, fuelLevel);
            this.entityData.set(SYNC_ENGINE_OUT, engineOut);

            for (ServerPlayer sp : ((ServerLevel) level()).players()) {
                if (sp.distanceTo(this) < 32) {
                    sp.sendSystemMessage(Component.translatable(
                            "message.sharkengine.fuel_added",
                            FuelSystem.formatFuelDisplay(fuelLevel, FuelSystem.MAX_FUEL)));
                }
            }
        }

        return added;
    }

    /**
     * Sets the input values for ship control
     */
    public void setInputs(float throttle, float turn, float forward) {
        float clampedThrottle = clamp(throttle, -1.0f, 1.0f);
        this.inputThrottle = clampedThrottle;
        setInputVertical(clampedThrottle);
        this.inputTurn = clamp(turn, -1.0f, 1.0f);
        this.inputForward = clamp(forward, 0.0f, 1.0f);
    }

    public void setInputs(float throttle, float turn) {
        float clampedThrottle = clamp(throttle, -1.0f, 1.0f);
        this.inputThrottle = clampedThrottle;
        setInputVertical(clampedThrottle);
        this.inputTurn = clamp(turn, -1.0f, 1.0f);
    }

    public void setYawDeg(float yaw) {
        this.setYRot(yaw);
        this.setXRot(0);
    }

    public boolean isAnchored() {
        return this.entityData.get(ANCHORED);
    }

    public void toggleAnchor(Player p) {
        boolean next = !isAnchored();
        this.entityData.set(ANCHORED, next);
        if (!level().isClientSide) {
            p.sendSystemMessage(
                    Component.translatable(next ? "message.sharkengine.anchor_on" : "message.sharkengine.anchor_off"));
        }
        if (next) {
            setDeltaMovement(Vec3.ZERO);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // SYNCHED DATA
    // ═══════════════════════════════════════════════════════════════════

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ANCHORED, false);
        builder.define(SYNC_FUEL, 100);
        builder.define(SYNC_SPEED, 0.0f);
        builder.define(SYNC_BLOCK_COUNT, 0);
        builder.define(SYNC_MASS, 0);
        builder.define(SYNC_ENGINE_OUT, false);
        builder.define(SYNC_HEALTH, MAX_HEALTH);
        builder.define(SYNC_TURN, 0.0f);
        builder.define(SYNC_VERTICAL, 0.0f);
    }

    // ═══════════════════════════════════════════════════════════════════
    // NBT PERSISTENCE
    // ═══════════════════════════════════════════════════════════════════

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        // AIR-015: read BugYaw/ThrustYaw BEFORE the blueprint, so it's
        // available as the legacy-save fallback for blueprints written
        // before AssemblyYaw existed (schema v1). Order matters here.
        if (compound.contains("BugYaw")) {
            this.bugYawDeg = compound.getFloat("BugYaw");
        } else if (compound.contains("ThrustYaw")) {
            // Migration: alte Fahrzeuge ohne BugYaw
            this.bugYawDeg = compound.getFloat("ThrustYaw");
        }
        if (compound.contains("Blueprint")) {
            CompoundTag blueprintTag = compound.getCompound("Blueprint");
            ShipBlueprint loaded = ShipBlueprint.fromNbt(blueprintTag, level().registryAccess());
            if (!blueprintTag.contains("AssemblyYaw")) {
                // v1 blueprint NBT: no AssemblyYaw was ever written. Fall
                // back to the entity's own persisted BUG-facing yaw
                // (bugYawDeg, just read above) — NOT the live getYRot(),
                // which drifts as the ship turns and would reintroduce the
                // exact visual-snap bug this schema exists to fix. See
                // ShipBlueprint.withAssemblyYaw's design note.
                loaded = loaded.withAssemblyYaw(bugYawDeg);
            }
            this.blueprint = loaded;
            applyBlueprintStats();
        }
        if (compound.contains("Anchored")) {
            this.entityData.set(ANCHORED, compound.getBoolean("Anchored"));
        }
        if (compound.hasUUID("Pilot")) {
            this.pilot = compound.getUUID("Pilot");
        }
        // REQ-009/T07 remediation (security-reviewer BLOCKING finding, round 2): mirrors
        // the Pilot persistence immediately above exactly. Without this, `copilot` --
        // mountCopilot()'s ONLY in-memory guard against a second simultaneous occupant,
        // since both mount call sites use startRiding(this, true), which bypasses
        // vanilla's own single-passenger canAddPassenger() cap entirely -- resets to null
        // across an entity reload while the real passenger relationship survives via
        // vanilla's independent passenger-persistence path, letting the next non-pilot
        // interact force-mount a second simultaneous passenger past a guard that thinks
        // the seat is empty.
        if (compound.hasUUID("Copilot")) {
            this.copilot = compound.getUUID("Copilot");
        }
        if (compound.contains("FuelLevel")) {
            this.fuelLevel = compound.getInt("FuelLevel");
        }
        if (compound.contains("AccelerationTicks")) {
            this.accelerationTicks = compound.getInt("AccelerationTicks");
        }
        if (compound.contains("CurrentSpeed")) {
            this.currentSpeed = compound.getFloat("CurrentSpeed");
        }
        if (compound.contains("EngineOut")) {
            this.engineOut = compound.getBoolean("EngineOut");
        }
        if (compound.contains("BlockCount")) {
            this.blockCount = compound.getInt("BlockCount");
        }
        if (compound.contains("Health")) {
            this.health = compound.getInt("Health");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        if (blueprint != null) {
            compound.put("Blueprint", blueprint.toNbt());
        }
        compound.putBoolean("Anchored", isAnchored());
        if (pilot != null) {
            compound.putUUID("Pilot", pilot);
        }
        // REQ-009/T07 remediation: see readAdditionalSaveData's "Copilot" comment above --
        // same tag-presence convention as Pilot (only written when actually occupied, so
        // hasUUID("Copilot") correctly reports absent for an empty seat on reload).
        if (copilot != null) {
            compound.putUUID("Copilot", copilot);
        }
        compound.putInt("FuelLevel", fuelLevel);
        compound.putInt("AccelerationTicks", accelerationTicks);
        compound.putFloat("CurrentSpeed", currentSpeed);
        compound.putBoolean("EngineOut", engineOut);
        compound.putInt("BlockCount", blockCount);
        compound.putFloat("BugYaw", bugYawDeg);
        compound.putInt("Health", health);
    }

    // ═══════════════════════════════════════════════════════════════════
    // S2C SYNC
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (blueprint != null) {
            ServerPlayNetworking.send(player, new ShipBlueprintS2CPayload(this.getId(), blueprint));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // INTERACT (Bug 3 Fix: Fuel Refill via Right-Click with Wood)
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide)
            return InteractionResult.SUCCESS;

        // Shift-rightclick: anchor/disassemble – only the pilot may do this
        if (player.isShiftKeyDown()) {
            if (!isPilot(player)) {
                if (player instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(Component.translatable("message.sharkengine.not_pilot"));
                }
                return InteractionResult.CONSUME;
            }
            if (isAnchored()) {
                disassemble();
            } else {
                toggleAnchor(player);
            }
            return InteractionResult.CONSUME;
        }

        // Check if player is holding wood/logs → refuel (pilot only)
        ItemStack heldItem = player.getItemInHand(hand);
        if (!heldItem.isEmpty() && (heldItem.is(ItemTags.LOGS) || heldItem.is(ItemTags.PLANKS))) {
            if (!isPilot(player)) {
                if (player instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(Component.translatable("message.sharkengine.not_pilot"));
                }
                return InteractionResult.CONSUME;
            }
            if (fuelLevel < FuelSystem.MAX_FUEL) {
                int fuelBefore = fuelLevel;
                // Each item = 1 wood unit
                int toConsume = Math.min(heldItem.getCount(),
                    (FuelSystem.MAX_FUEL - fuelLevel + FuelSystem.ENERGY_PER_WOOD - 1) / FuelSystem.ENERGY_PER_WOOD);
                toConsume = Math.max(1, toConsume);

                addFuel(toConsume);

                if (fuelLevel > fuelBefore) {
                    if (!player.getAbilities().instabuild) {
                        heldItem.shrink(toConsume);
                    }
                }
                return InteractionResult.CONSUME;
            } else {
                if (player instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(Component.literal("§eTreibstofftank ist voll!"));
                }
                return InteractionResult.CONSUME;
            }
        }

        // BUG FIX (2026-07-13, live playtest report: "helicopter_engine/rotor_shaft/
        // metal_sheet können nicht angebaut werden, es passiert einfach nichts"):
        // a held placeable BlockItem (a ship_eligible block the player is trying to
        // attach to an already-assembled/launched ship) must NOT be swallowed by the
        // mount fallback below. This entity's hitbox spans the whole blueprint (up to
        // 32-block radius, AIR-0xx assembly constraints), so before this fix *every*
        // right-click anywhere on/near a launched ship — including one holding a block
        // meant for placement — landed here and unconditionally mounted the player (or,
        // if already mounted, silently no-op'd) while returning CONSUME, which prevents
        // vanilla's normal BlockItem-placement path from ever running. Root-caused via
        // /setblock (proves the block itself places fine server-side) plus live
        // runClient repro (mounting happened silently on every attempt). PASS here lets
        // vanilla fall through to normal placement against the actual world block grid,
        // exactly like right-clicking a vanilla boat while holding a block still lets
        // you place against terrain behind/around it.
        //
        // CORRECTED same day (ultrathink-craftsmanship gate caught it before any live
        // report): the first version of this fix checked "any non-empty hand", not just
        // BlockItem — that silently regressed mounting for every OTHER held item too
        // (holding a sword/torch/food and right-clicking the ship stopped mounting you
        // at all, since it's neither empty nor a block). Narrowing to BlockItem restores
        // exactly the pre-fix mount behavior for every non-block item while still fixing
        // the reported bug.
        if (player.getItemInHand(hand).getItem() instanceof BlockItem) {
            return InteractionResult.PASS;
        }

        // Normal rightclick with an empty hand: mount (only one pilot at a time)
        //
        // REMEDIATION (T07, Watcher review-required finding, "stowaway" mount gap): the
        // branch condition used to also require getPassengers().stream().anyMatch(e -> e
        // instanceof Player) as a proxy for "the pilot is currently aboard". That was safe
        // before copilots existed (only the pilot could ever be a Player passenger) but is
        // wrong now: if the assigned pilot dismounts (REQ-011 -- normal/expected, a ship can
        // exist with no pilot aboard) while the copilot seat is still empty, that proxy goes
        // false and the next right-click fell through to the bare player.startRiding(this,
        // true) fallback below -- force-mounting the interacting player with ZERO
        // registration as pilot or copilot (an untracked "stowaway"). A second player
        // arriving afterward could then legitimately claim the copilot seat via
        // mountCopilot() (still null, since the stowaway never touched it), producing two
        // simultaneous Player passengers for a seat AC-009 promises holds "genau ein
        // zusätzlicher Passagier".
        //
        // The fix: {@link #pilot} is already the single persistent, server-authoritative
        // source of truth for who is authorized to hold the pilot seat -- set once at
        // assembly (ShipAssemblyService#tryAssemble) and never cleared on dismount, exactly
        // like isPilot() is already used to gate shift-rightclick disassembly and refuel
        // above. Whether anyone happens to be riding *right now* is irrelevant to that
        // question, so it's dropped from the condition entirely: any player who is not the
        // assigned pilot (and a pilot has in fact been assigned) is routed to the copilot
        // seat, full stop -- whether the pilot is currently riding, dismounted, or was never
        // aboard yet. mountCopilot() itself remains the sole guard against silently
        // displacing an already-mounted copilot (see its javadoc for the falsifying-test
        // contract this closes) and is unchanged.
        if (pilot != null && !isPilot(player)) {
            mountCopilot(player);
            return InteractionResult.CONSUME;
        }
        // Either this player IS the assigned pilot re-entering a vacated seat (REQ-011), or
        // no pilot has ever been assigned yet (pilot == null) -- both are legitimate mounts
        // via the ordinary passenger path.
        player.startRiding(this, true);
        return InteractionResult.CONSUME;
    }

    // ═══════════════════════════════════════════════════════════════════
    // DISASSEMBLY
    // ═══════════════════════════════════════════════════════════════════

    public void disassemble() {
        if (blueprint == null || level().isClientSide)
            return;

        BlockPos base = blockPosition();
        int placed = 0;
        int blocked = 0;

        for (ShipBlueprint.ShipBlock block : blueprint.blocks()) {
            BlockPos target = base.offset(block.dx(), block.dy(), block.dz());
            BlockState existing = level().getBlockState(target);
            if (existing.isAir() || existing.canBeReplaced()) {
                level().setBlock(target, block.state(), 3);
                placed++;
            } else {
                blocked++;
            }
        }

        if (level() instanceof ServerLevel serverLevel) {
            String key;
            Object arg;
            if (blocked == 0) {
                key = "message.sharkengine.disassembly_ok";
                arg = placed;
            } else {
                key = "message.sharkengine.disassembly_partial";
                arg = placed + "/" + (placed + blocked);
            }
            for (ServerPlayer sp : serverLevel.players()) {
                if (sp.distanceTo(this) < 64) {
                    sp.sendSystemMessage(Component.translatable(key, arg));
                }
            }
        }

        this.discard();
    }

    // ═══════════════════════════════════════════════════════════════════
    // DAMAGE SYSTEM (Feature 5)
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isDestroyed()) return false;
        if (damageCooldownTicks > 0) return false;

        // Scale damage based on source
        float damage;
        if (source.getEntity() == null) {
            // Explosion or environmental (no direct attacker)
            damage = EXPLOSION_DAMAGE;
        } else {
            // Direct attack from player/mob
            damage = Math.max(ATTACK_DAMAGE_DEFAULT, amount);
        }

        health = Math.max(0, health - (int) damage);
        damageCooldownTicks = 10; // 0.5 sec cooldown

        // Sync to client
        this.entityData.set(SYNC_HEALTH, health);

        // Notify nearby players
        if (level() instanceof ServerLevel serverLevel) {
            for (ServerPlayer sp : serverLevel.players()) {
                if (sp.distanceTo(this) < 32) {
                    sp.sendSystemMessage(Component.literal(
                            "§cSchiff beschädigt! HP: " + health + "/" + MAX_HEALTH));
                }
            }
        }

        // Destroyed at 0 HP
        if (health <= 0) {
            onShipDestroyed();
        }

        return true;
    }

    /**
     * Applies collision damage based on speed.
     * Called when the ship hits a solid block at speed.
     */
    private void applyCollisionDamage() {
        if (currentSpeed < COLLISION_SPEED_THRESHOLD) return;
        float damage = (currentSpeed - COLLISION_SPEED_THRESHOLD) * COLLISION_DAMAGE_MULTIPLIER;
        if (damage < 1) return;

        health = Math.max(0, health - (int) damage);
        damageCooldownTicks = 20; // 1 sec cooldown after collision

        this.entityData.set(SYNC_HEALTH, health);

        if (level() instanceof ServerLevel serverLevel) {
            for (ServerPlayer sp : serverLevel.players()) {
                if (sp.distanceTo(this) < 32) {
                    sp.sendSystemMessage(Component.literal(
                            "§cKollision! Schaden: " + (int) damage + " | HP: " + health + "/" + MAX_HEALTH));
                }
            }
        }

        if (health <= 0) {
            onShipDestroyed();
        }
    }

    /**
     * Called when ship reaches 0 HP.
     * Disassembles the ship and drops blocks.
     */
    private void onShipDestroyed() {
        if (level().isClientSide) return;

        // Eject passengers
        this.ejectPassengers();

        // Notify players
        if (level() instanceof ServerLevel serverLevel) {
            for (ServerPlayer sp : serverLevel.players()) {
                if (sp.distanceTo(this) < 64) {
                    sp.sendSystemMessage(Component.literal("§4§l⚠ Schiff zerstört!"));
                }
            }
        }

        // Disassemble wreck
        disassemble();
    }

    // ═══════════════════════════════════════════════════════════════════
    // PHYSICS UPDATE (Bug 1+2+4 Fix)
    // ═══════════════════════════════════════════════════════════════════

    private void notifyPilot(Component message) {
        if (level().isClientSide || pilot == null) return;
        if (!(level() instanceof ServerLevel serverLevel)) return;
        ServerPlayer pilotPlayer = serverLevel.getServer().getPlayerList().getPlayer(pilot);
        if (pilotPlayer != null) {
            pilotPlayer.sendSystemMessage(message);
        }
    }

    /**
     * Updates ship physics: acceleration, weight, height penalty, fuel consumption.
     *
     * <p>Direction: Entity yaw is the single source of truth, initialized from
     * the BUG block's FACING at assembly. A/D steering modifies entity yaw.
     * Player look direction has no influence on movement.</p>
     */
    private void updatePhysics() {
        if (level().isClientSide) return;

        // Damaged cooldown
        if (damageCooldownTicks > 0) damageCooldownTicks--;

        // Destroyed check
        if (isDestroyed()) {
            setDeltaMovement(new Vec3(0, -0.3, 0));
            return;
        }

        // ━━━ Acceleration / Deceleration ━━━
        if (inputForward > 0 && !engineOut) {
            accelerationTicks++;
        } else {
            accelerationTicks = Math.max(0, accelerationTicks - 4);
            currentSpeed = Mth.lerp(0.15f, currentSpeed, 0.0f);
            if (currentSpeed < 0.01f) currentSpeed = 0.0f;
        }

        phase = ShipPhysics.calculatePhase(accelerationTicks);

        // ━━━ Weight (AIR-023: mass-based, not block-count-based) ━━━
        maxSpeed = ShipPhysics.calculateMaxSpeed(mass);
        weightCategory = WeightCategory.fromMass(mass);

        // ━━━ Height Penalty ━━━
        heightPenalty = ShipPhysics.calculateHeightPenalty((float) this.getY());

        // ━━━ Speed Calculation ━━━
        if (inputForward > 0 && !engineOut) {
            float targetSpeed = maxSpeed * heightPenalty * (phase.getSpeed() / 30.0f);
            // BUG FIX 4: Smoother acceleration lerp (was 0.1, now 0.08 for less jitter)
            currentSpeed = Mth.lerp(0.08f, currentSpeed, targetSpeed);
        }

        // ━━━ Fuel Consumption (1x per second) ━━━
        if (!engineOut && inputForward > 0) {
            fuelConsumptionTick++;
            if (fuelConsumptionTick >= 20) {
                fuelConsumptionTick = 0;
                int nominalConsumption = ShipPhysics.calculateFuelConsumption(phase);
                fuelDebt += nominalConsumption * dev.sharkengine.ship.part.VehicleBalance.FUEL_CONSUMPTION_RATE;
                int wholeUnits = (int) fuelDebt;
                if (wholeUnits > 0) {
                    fuelDebt -= wholeUnits;
                    fuelLevel -= wholeUnits;

                    if (fuelLevel <= 0) {
                        engineOut = true;
                        fuelLevel = 0;
                        notifyPilot(Component.translatable("message.sharkengine.no_fuel"));
                    }
                }
            }
        } else {
            fuelConsumptionTick = 0;
        }

        // ━━━ Overweight Warnings (every 5 sec) ━━━
        if (weightCategory == WeightCategory.OVERLOADED && tickCount % 100 == 0) {
            notifyPilot(Component.translatable("message.sharkengine.too_heavy"));
        }

        // ━━━ Sync to client ━━━
        this.entityData.set(SYNC_FUEL, fuelLevel);
        this.entityData.set(SYNC_SPEED, currentSpeed);
        this.entityData.set(SYNC_BLOCK_COUNT, blockCount);
        this.entityData.set(SYNC_MASS, mass);
        this.entityData.set(SYNC_ENGINE_OUT, engineOut);
        this.entityData.set(SYNC_HEALTH, health);
        this.entityData.set(SYNC_TURN, inputTurn);
        this.entityData.set(SYNC_VERTICAL, inputVertical);
    }

    // ═══════════════════════════════════════════════════════════════════
    // TICK (Bug 1+2+4 Fix)
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            // Consume the lerp target set by lerpTo() below, 1/lerpSteps of the
            // way per tick — the exact pattern vanilla's own AbstractMinecart.tick()
            // uses (recovered from dangling commit c240f80, see the field-block
            // comment above). Without this, position/rotation updates from the
            // server would just sit in lerpX/Y/Z/lerpYRot/lerpXRot and never
            // actually move the entity, since lerpTo() no longer teleports
            // directly.
            if (this.lerpSteps > 0) {
                this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ,
                        this.lerpYRot, this.lerpXRot);
                this.lerpSteps--;
            }
            // FLR-003: smoothly chase the current bank/roll target every
            // client tick so releasing/reversing turn input rolls back
            // level gradually instead of snapping.
            float targetRoll = ShipTransform.rollFromTurnInput(getSyncedTurn(), dev.sharkengine.ship.part.VehicleBalance.MAX_BANK_DEG);
            this.clientRoll = Mth.lerp(dev.sharkengine.ship.part.VehicleBalance.BANK_SMOOTHING_FACTOR, this.clientRoll, targetRoll);
            // FLP-003: same smoothing pattern as roll above, for pitch.
            float targetPitch = ShipTransform.pitchFromVerticalInput(getSyncedVertical(), dev.sharkengine.ship.part.VehicleBalance.MAX_PITCH_DEG);
            this.clientPitch = Mth.lerp(dev.sharkengine.ship.part.VehicleBalance.PITCH_SMOOTHING_FACTOR, this.clientPitch, targetPitch);
            return;
        }

        // ━━━ Anchor ━━━
        if (isAnchored()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }

        // ━━━ Physics Update ━━━
        updatePhysics();

        // ━━━ Engine Out: drift/fall ━━━
        if (engineOut || isDestroyed()) {
            FluidState fluid = level().getFluidState(blockPosition());
            if (!fluid.isEmpty()) {
                double bobVelocity = (this.getY() % 1.0 > 0.5) ? -0.02 : 0.02;
                setDeltaMovement(new Vec3(0, bobVelocity, 0));
            } else {
                setDeltaMovement(new Vec3(0, -0.15, 0));
            }
            this.move(MoverType.SELF, getDeltaMovement());
            return;
        }

        // ━━━ BUG FIX 1+2: Turn (Rotation) ━━━
        // Steering modifies the entity yaw. The entity yaw IS the forward direction.
        // Smooth turning: 3 deg/tick (was 4, reduced for stability)
        //
        // P0 hotfix (2026-07-12, live playtest: "Lenkung ist invertiert. Links
        // ist rechts und rechts ist links."): proven via forward/right cross
        // product (right = forward x up, with forward=(-sin(yaw),0,cos(yaw)),
        // up=(0,1,0), giving right=(-cos(yaw),0,-sin(yaw))... concretely: at
        // yaw=0 (facing South) the pilot's right side is West, and increasing
        // yaw sweeps facing from South towards West — i.e. increasing yaw
        // turns the ship RIGHT, not left. Both client input sources assumed
        // the opposite: HelmInputClient maps keyLeft("A")->+1 expecting a
        // left turn, and ControllerInput negates the stick specifically "so
        // right stick right = turn right" (turn=-1 for a right push) — both
        // self-consistent internally, both backwards relative to physics.
        // Flipping the sign once here (rather than in either client file)
        // corrects both input paths at their single point of consumption.
        float yaw = this.getYRot() - (inputTurn * 3.0f);
        this.setYRot(yaw);

        // ━━━ Forward Movement ━━━
        // Direction is ALWAYS based on entity yaw (single source of truth).
        // At assembly, yaw is set from BUG block's FACING direction.
        double rad = Math.toRadians(yaw);
        double fx = -Math.sin(rad);
        double fz = Math.cos(rad);

        // BUG FIX 4: Use currentSpeed directly, scale to ticks (÷20)
        // Previously 0.05 was used which doesn't match blocks/sec definition
        double speedPerTick = currentSpeed / 20.0;
        Vec3 moveVec = new Vec3(fx * speedPerTick, 0, fz * speedPerTick);

        // ━━━ Vertical Movement ━━━
        // BUG FIX 4: Smooth vertical speed, scaled properly
        double verticalMotion = inputVertical * 0.3;

        Vec3 vel = new Vec3(moveVec.x, verticalMotion, moveVec.z);

        // ━━━ Drag (Air Resistance) ━━━
        // BUG FIX 4: Gentler drag for smoother movement
        vel = new Vec3(vel.x * 0.95, vel.y * 0.95, vel.z * 0.95);
        this.setDeltaMovement(vel);

        // ━━━ Collision Check ━━━
        // P0 hotfix (2026-07-12): pass the effective yaw (ShipTransform,
        // AIR-010/011) so the probed volume matches the ship's actual
        // rotated shape, not its frozen build-time orientation (B2).
        float effectiveYaw = ShipTransform.effectiveYaw(yaw, blueprint.assemblyYaw());
        if (ShipPhysics.checkCollision(level(), blockPosition(), blueprint, effectiveYaw)) {
            // Feature 5: Collision damage
            applyCollisionDamage();
            // P0 hotfix: only zero the HORIZONTAL component. Zeroing the
            // whole vector (previous behavior) trapped the ship permanently
            // once any collision fired near the ground — it could not even
            // climb away, since vertical input was wiped along with
            // forward motion every single tick the (possibly false)
            // collision persisted. Preserving vertical always leaves an
            // escape route.
            setDeltaMovement(new Vec3(0, vel.y, 0));
            currentSpeed *= 0.3f; // Lose most speed on collision
            accelerationTicks = Math.max(0, accelerationTicks - 40);
        }

        // ━━━ Apply Movement ━━━
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    /**
     * Stores the incoming network position/rotation target for gradual
     * client-side interpolation instead of teleporting to it immediately.
     *
     * <p>The base {@link net.minecraft.world.entity.Entity#lerpTo} does
     * {@code setPos(x,y,z); setRot(yaw,pitch);} — an instant snap, not
     * interpolation, despite the name (confirmed by decompiling vanilla —
     * see the field-block comment above). {@code steps + 2} matches vanilla
     * AbstractMinecart's own convention exactly (a small padding above
     * whatever step count the network layer suggests, guaranteeing a
     * minimum smoothing window).</p>
     */
    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yaw;
        this.lerpXRot = pitch;
        this.lerpSteps = steps + 2;
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        double rad = Math.toRadians(this.getYRot() + 90);
        double offsetX = Math.cos(rad) * 1.5;
        double offsetZ = Math.sin(rad) * 1.5;
        return new Vec3(this.getX() + offsetX, this.getY(), this.getZ() + offsetZ);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    /**
     * P0 hotfix (2026-07-12, live playtest): keeps the pilot's camera loosely
     * locked to the ship's heading, matching the reported expectation ("wie
     * GTA Steuerung" — direction should keep following look direction).
     * Before this fix {@link dev.sharkengine.client.FlightCameraHandler}
     * forced a third-person-back camera type but never touched rotation, so
     * the camera tracked the pilot's own free mouse-look — completely
     * decoupled from the A/D turning that DOES actively rotate the ship
     * (see the "BUG FIX 1+2: Turn" block in {@link #tick()}) — making
     * working steering invisible/disorienting.
     *
     * <p>Reimplements vanilla {@code Boat.onPassengerTurned}/
     * {@code clampRotation} verbatim (decompiled from the pinned MC 1.21.1
     * jar, not guessed) rather than a full hard lock: the body always faces
     * the ship's heading, but the head/camera may swivel up to 105° either
     * side of it — the same proven cone vanilla boats use, so the pilot
     * keeps some free-look instead of being welded to dead-ahead.</p>
     */
    @Override
    public void onPassengerTurned(Entity passenger) {
        passenger.setYBodyRot(this.getYRot());
        float relativeYaw = Mth.wrapDegrees(passenger.getYRot() - this.getYRot());
        float clampedRelativeYaw = Mth.clamp(relativeYaw, -105.0f, 105.0f);
        float delta = clampedRelativeYaw - relativeYaw;
        passenger.yRotO += delta;
        passenger.setYRot(passenger.getYRot() + delta);
        passenger.setYHeadRot(passenger.getYRot());
    }

    private static float clamp(float v, float a, float b) {
        return Math.max(a, Math.min(b, v));
    }

    /**
     * Applies blueprint stats: block count, weight, speed, thruster check.
     *
     * <p>Direction is NOT determined here – it comes exclusively from the
     * BUG block's FACING property, set via {@link #setBugYawDeg(float)}
     * during assembly.</p>
     *
     * <p>Thrusters are decorative thrust indicators only.
     * They are still required for assembly validation but have
     * no directional authority.</p>
     */
    private void applyBlueprintStats() {
        if (blueprint == null) {
            blockCount = 0;
            mass = 0;
            weightCategory = WeightCategory.LIGHT;
            maxSpeed = ShipPhysics.calculateMaxSpeed(0);
            hasThrusters = false;
            return;
        }
        blockCount = blueprint.blockCount();

        // Role-based aggregation (AIR-021/AIR-023, REQ-S1/S2): mass,
        // propulsion, etc. all come from ShipPartAnalyzer in a single pass —
        // no second, block-count-based approximation of weight anywhere.
        // Any future PROPULSION part (e.g. helicopter_engine) correctly
        // lights up thrust effects too, not just the legacy thruster block
        // (this used to be a hardcoded ModBlocks.THRUSTER check, B4).
        List<String> blockIds = blueprint.blocks().stream()
                .map(block -> BuiltInRegistries.BLOCK.getKey(block.state().getBlock()).toString())
                .toList();
        ShipStats stats = ShipPartAnalyzer.analyze(blockIds);
        mass = stats.mass();
        weightCategory = WeightCategory.fromMass(mass);
        maxSpeed = ShipPhysics.calculateMaxSpeed(mass);
        hasThrusters = stats.hasPropulsion();

        // Sync blockCount and mass to client. Syncing mass (not just block
        // count) is what lets the HUD (FuelHudOverlay) show the exact same
        // WeightCategory the server enforces for flight — see SYNC_MASS.
        if (!level().isClientSide) {
            this.entityData.set(SYNC_BLOCK_COUNT, blockCount);
            this.entityData.set(SYNC_MASS, mass);
        }
    }
}
