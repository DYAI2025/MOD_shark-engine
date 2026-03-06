package dev.sharkengine.ship;

import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.net.ShipBlueprintS2CPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

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
    private static final EntityDataAccessor<Boolean> SYNC_ENGINE_OUT =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SYNC_HEALTH =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.INT);

    // ═══════════════════════════════════════════════════════════════════
    // FIELDS
    // ═══════════════════════════════════════════════════════════════════

    private float inputThrottle; // -1..+1 (vertical)
    private float inputTurn;     // -1..+1 (rotation)
    private ShipBlueprint blueprint;
    private UUID pilot;

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
    // BUG FIX 4: SMOOTH INTERPOLATION
    // ═══════════════════════════════════════════════════════════════════

    /** Previous tick position for client-side interpolation */
    private double prevPosX, prevPosY, prevPosZ;
    private float prevYaw;

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

    public WeightCategory getWeightCategory() {
        return WeightCategory.fromBlockCount(getBlockCount());
    }

    public int getFuelLevel() {
        return level().isClientSide ? this.entityData.get(SYNC_FUEL) : fuelLevel;
    }

    public boolean isEngineOut() {
        return level().isClientSide ? this.entityData.get(SYNC_ENGINE_OUT) : engineOut;
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
        builder.define(SYNC_ENGINE_OUT, false);
        builder.define(SYNC_HEALTH, MAX_HEALTH);
    }

    // ═══════════════════════════════════════════════════════════════════
    // NBT PERSISTENCE
    // ═══════════════════════════════════════════════════════════════════

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.contains("Blueprint")) {
            this.blueprint = ShipBlueprint.fromNbt(
                    compound.getCompound("Blueprint"),
                    level().registryAccess()
            );
            applyBlueprintStats();
        }
        if (compound.contains("Anchored")) {
            this.entityData.set(ANCHORED, compound.getBoolean("Anchored"));
        }
        if (compound.hasUUID("Pilot")) {
            this.pilot = compound.getUUID("Pilot");
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
        if (compound.contains("BugYaw")) {
            this.bugYawDeg = compound.getFloat("BugYaw");
        } else if (compound.contains("ThrustYaw")) {
            // Migration: alte Fahrzeuge ohne BugYaw
            this.bugYawDeg = compound.getFloat("ThrustYaw");
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

        // Shift-rightclick: if anchored, disassemble; otherwise toggle anchor
        if (player.isShiftKeyDown()) {
            if (isAnchored()) {
                disassemble();
            } else {
                toggleAnchor(player);
            }
            return InteractionResult.CONSUME;
        }

        // Check if player is holding wood/logs → refuel
        ItemStack heldItem = player.getItemInHand(hand);
        if (!heldItem.isEmpty() && (heldItem.is(ItemTags.LOGS) || heldItem.is(ItemTags.PLANKS))) {
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

        // Normal rightclick: mount
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

        // ━━━ Weight ━━━
        maxSpeed = ShipPhysics.calculateMaxSpeed(blockCount);
        weightCategory = WeightCategory.fromBlockCount(blockCount);

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
                int consumption = ShipPhysics.calculateFuelConsumption(phase);
                fuelLevel -= consumption;

                if (fuelLevel <= 0) {
                    engineOut = true;
                    fuelLevel = 0;
                    notifyPilot(Component.translatable("message.sharkengine.no_fuel"));
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
        this.entityData.set(SYNC_ENGINE_OUT, engineOut);
        this.entityData.set(SYNC_HEALTH, health);
    }

    // ═══════════════════════════════════════════════════════════════════
    // TICK (Bug 1+2+4 Fix)
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public void tick() {
        // BUG FIX 4: Store previous position for interpolation
        this.prevPosX = this.getX();
        this.prevPosY = this.getY();
        this.prevPosZ = this.getZ();
        this.prevYaw = this.getYRot();

        super.tick();

        if (level().isClientSide)
            return;

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
        float yaw = this.getYRot() + (inputTurn * 3.0f);
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
        if (ShipPhysics.checkCollision(level(), blockPosition(), blueprint)) {
            // Feature 5: Collision damage
            applyCollisionDamage();
            setDeltaMovement(Vec3.ZERO);
            currentSpeed *= 0.3f; // Lose most speed on collision
            accelerationTicks = Math.max(0, accelerationTicks - 40);
        }

        // ━━━ Apply Movement ━━━
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    // ═══════════════════════════════════════════════════════════════════
    // BUG FIX 4: CLIENT INTERPOLATION
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Provides smooth client-side position interpolation.
     * Overrides default to use our stored previous positions.
     */
    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
        // Use shorter lerp steps for smoother movement at high speed
        super.lerpTo(x, y, z, yaw, pitch, Math.min(steps, 3));
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
            weightCategory = WeightCategory.LIGHT;
            maxSpeed = ShipPhysics.calculateMaxSpeed(0);
            hasThrusters = false;
            return;
        }
        blockCount = blueprint.blockCount();
        weightCategory = WeightCategory.fromBlockCount(blockCount);
        maxSpeed = ShipPhysics.calculateMaxSpeed(blockCount);

        // Check for thrusters (required for assembly, decorative for direction)
        hasThrusters = blueprint.blocks().stream()
                .anyMatch(block -> block.state().is(ModBlocks.THRUSTER));

        // Sync blockCount to client
        if (!level().isClientSide) {
            this.entityData.set(SYNC_BLOCK_COUNT, blockCount);
        }
    }
}
