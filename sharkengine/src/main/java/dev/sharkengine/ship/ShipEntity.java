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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

import java.util.UUID;

/**
 * Ship entity for controllable flying vehicles (air ships).
 * Supports vertical movement, acceleration phases, fuel system, and weight-based speed limits.
 * 
 * @author Shark Engine Team
 * @version 2.0 (Luftfahrzeug-MVP)
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

    // ═══════════════════════════════════════════════════════════════════
    // EXISTING FIELDS
    // ═══════════════════════════════════════════════════════════════════
    
    private float inputThrottle; // -1..+1 (legacy throttle alias)
    private float inputTurn;     // -1..+1 (rotation)
    private ShipBlueprint blueprint;
    private UUID pilot;

    // ═══════════════════════════════════════════════════════════════════
    // NEW FIELDS FOR LUFTFAHRZEUG-MVP (Task 1.8)
    // ═══════════════════════════════════════════════════════════════════
    
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
    
    /** Vertical input (-1..+1, Leertaste/Shift) */
    private float inputVertical = 0.0f;

    /**
     * Tick counter for per-second fuel consumption (20 ticks = 1 second).
     * FuelSystem consumption values are energy/sec; we only charge once per second.
     */
    private int fuelConsumptionTick = 0;

    public ShipEntity(EntityType<? extends ShipEntity> type, Level level) {
        super(type, level);
    }

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
    // NEW GETTER/SETTER FOR LUFTFAHRZEUG-MVP (Task 1.8)
    // ═══════════════════════════════════════════════════════════════════

    public VehicleClass getVehicleClass() {
        return vehicleClass;
    }

    public AccelerationPhase getPhase() {
        return phase;
    }

    public int getAccelerationTicks() {
        return accelerationTicks;
    }

    public float getCurrentSpeed() {
        return level().isClientSide ? this.entityData.get(SYNC_SPEED) : currentSpeed;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public float getHeightPenalty() {
        return heightPenalty;
    }

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

    public boolean hasThrusters() {
        return hasThrusters;
    }

    public float getInputForward() {
        return inputForward;
    }

    public void setInputForward(float inputForward) {
        this.inputForward = clamp(inputForward, 0.0f, 1.0f);
    }

    public float getInputVertical() {
        return inputVertical;
    }

    public void setInputVertical(float inputVertical) {
        this.inputVertical = clamp(inputVertical, -1.0f, 1.0f);
    }

    // ═══════════════════════════════════════════════════════════════════
    // FUEL REFILL (Task 3.5)
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
        
        // Add fuel, capped at MAX_FUEL (100)
        fuelLevel = Math.min(FuelSystem.MAX_FUEL, fuelLevel + energyToAdd);
        
        // Reset engine out flag if fuel added
        if (fuelLevel > 0) {
            engineOut = false;
        }
        
        int added = fuelLevel - oldFuel;
        
        // Send feedback to nearby players
        if (!level().isClientSide && added > 0) {
            for (ServerPlayer sp : ((ServerLevel) level()).players()) {
                if (sp.distanceTo(this) < 32) {
                    sp.sendSystemMessage(Component.translatable(
                        "message.sharkengine.fuel_added", 
                        FuelSystem.formatFuelDisplay(fuelLevel, FuelSystem.MAX_FUEL)
                    ));
                }
            }
        }
        
        return added;
    }

    /**
     * Sets the input values for ship control
     * 
     * @param throttle Vertical throttle (-1..+1)
     * @param turn Rotation (-1..+1)
     * @param forward Forward acceleration (0..1)
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
            p.sendSystemMessage(Component.translatable(next ? "message.sharkengine.anchor_on" : "message.sharkengine.anchor_off"));
        }
        if (next) {
            setDeltaMovement(Vec3.ZERO);
        }
    }

    // --- NBT Persistence (Step 2) ---

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ANCHORED, false);
        builder.define(SYNC_FUEL, 100);
        builder.define(SYNC_SPEED, 0.0f);
        builder.define(SYNC_BLOCK_COUNT, 0);
        builder.define(SYNC_ENGINE_OUT, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        // EXISTING: Blueprint, Anchored, Pilot
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
        
        // NEW (Task 3.3): Luftfahrzeug-MVP Felder
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
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        // EXISTING: Blueprint, Anchored, Pilot
        if (blueprint != null) {
            compound.put("Blueprint", blueprint.toNbt());
        }
        compound.putBoolean("Anchored", isAnchored());
        if (pilot != null) {
            compound.putUUID("Pilot", pilot);
        }
        
        // NEW (Task 3.3): Luftfahrzeug-MVP Felder
        compound.putInt("FuelLevel", fuelLevel);
        compound.putInt("AccelerationTicks", accelerationTicks);
        compound.putFloat("CurrentSpeed", currentSpeed);
        compound.putBoolean("EngineOut", engineOut);
        compound.putInt("BlockCount", blockCount);
    }

    // --- S2C Sync (Step 3) ---

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (blueprint != null) {
            ServerPlayNetworking.send(player, new ShipBlueprintS2CPayload(this.getId(), blueprint));
        }
    }

    // --- Disassembly (Step 5) ---

    public void disassemble() {
        if (blueprint == null || level().isClientSide) return;

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

        // Notify nearby players
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
    // PHYSICS UPDATE (Task 3.1)
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Sends a message to the pilot player (server-side only).
     * ShipEntity is not a Player, so we must look up the pilot via UUID.
     *
     * @param message The message component to deliver
     */
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
     * Called every tick from tick() method.
     *
     * <p>Fuel is consumed at the documented energy/sec rate (once every 20 ticks),
     * not once per tick. The overload warning is rate-limited to avoid chat spam.</p>
     */
    private void updatePhysics() {
        if (level().isClientSide) return;

        // ━━━ Beschleunigung / Deceleration ━━━
        if (inputForward > 0 && !engineOut) {
            accelerationTicks++;
        } else {
            // Decelerate faster than we accelerated (responsive feel)
            accelerationTicks = Math.max(0, accelerationTicks - 4);
            currentSpeed = Mth.lerp(0.15f, currentSpeed, 0.0f);
            if (currentSpeed < 0.01f) currentSpeed = 0.0f;
        }

        phase = ShipPhysics.calculatePhase(accelerationTicks);

        // ━━━ Gewicht (Block-Anzahl) ━━━
        maxSpeed = ShipPhysics.calculateMaxSpeed(blockCount);
        weightCategory = WeightCategory.fromBlockCount(blockCount);

        // ━━━ Höhen-Penalty ━━━
        heightPenalty = ShipPhysics.calculateHeightPenalty((float) this.getY());

        // ━━━ Aktuelle Geschwindigkeit berechnen ━━━
        if (inputForward > 0 && !engineOut) {
            float targetSpeed = maxSpeed * heightPenalty * (phase.getSpeed() / 30.0f);
            currentSpeed = Mth.lerp(0.1f, currentSpeed, targetSpeed);
        }

        // ━━━ Treibstoff verbrauchen (1x pro Sekunde = alle 20 Ticks) ━━━
        // FuelSystem values are documented as energy/sec; do NOT charge every tick.
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

        // ━━━ Gewicht-Warnungen (alle 100 Ticks = 5 Sekunden, via tickCount) ━━━
        if (weightCategory == WeightCategory.OVERLOADED && tickCount % 100 == 0) {
            notifyPilot(Component.translatable("message.sharkengine.too_heavy"));
        }

        // ━━━ Sync to client for HUD ━━━
        this.entityData.set(SYNC_FUEL, fuelLevel);
        this.entityData.set(SYNC_SPEED, currentSpeed);
        this.entityData.set(SYNC_BLOCK_COUNT, blockCount);
        this.entityData.set(SYNC_ENGINE_OUT, engineOut);
    }

    // --- Tick ---

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) return;

        // ━━━ Anchor-Check ━━━
        if (isAnchored()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }

        // ━━━ Physik-Update (Task 3.1) ━━━
        updatePhysics();

        // ━━━ Engine-Out Check ━━━
        if (engineOut) {
            // Check if ship is in water — apply buoyancy
            FluidState fluid = level().getFluidState(blockPosition());
            if (!fluid.isEmpty()) {
                // Float on water surface: gentle bob, no sinking
                double bobVelocity = (this.getY() % 1.0 > 0.5) ? -0.02 : 0.02;
                setDeltaMovement(new Vec3(0, bobVelocity, 0));
            } else {
                // Fall gently through air (reduced from 0.5 to 0.15)
                setDeltaMovement(new Vec3(0, -0.15, 0));
            }
            this.move(MoverType.SELF, getDeltaMovement());
            return;
        }

        // ━━━ Turn (Rotation) ━━━
        float yaw = this.getYRot() + (inputTurn * 4.0f); // deg/tick
        this.setYRot(yaw);

        // ━━━ Forward Movement ━━━
        double rad = Math.toRadians(yaw);
        double fx = -Math.sin(rad);
        double fz =  Math.cos(rad);

        // Bewegung mit currentSpeed anwenden
        Vec3 moveVec = new Vec3(fx, 0, fz).scale(currentSpeed * 0.05);
        
        // ━━━ Vertical Movement (Leertaste/Shift) ━━━
        double verticalMotion = inputVertical * 0.5; // Aufsteigen/Absteigen
        
        Vec3 vel = new Vec3(moveVec.x, verticalMotion, moveVec.z);

        // ━━━ Drag (Luftwiderstand) ━━━
        vel = new Vec3(vel.x * 0.90, vel.y * 0.98, vel.z * 0.90);
        this.setDeltaMovement(vel);

        // ━━━ Kollision prüfen (Task 3.2) ━━━
        if (ShipPhysics.checkCollision(level(), blockPosition(), blueprint)) {
            setDeltaMovement(Vec3.ZERO);
        }

        // ━━━ Bewegung anwenden ━━━
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        // Always allow dismount by returning position next to the ship
        double rad = Math.toRadians(this.getYRot() + 90);
        double offsetX = Math.cos(rad) * 1.5;
        double offsetZ = Math.sin(rad) * 1.5;
        return new Vec3(this.getX() + offsetX, this.getY(), this.getZ() + offsetZ);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;

        // Shift-rightclick: if anchored, disassemble; otherwise toggle anchor
        if (player.isShiftKeyDown()) {
            if (isAnchored()) {
                disassemble();
            } else {
                toggleAnchor(player);
            }
            return InteractionResult.CONSUME;
        }

        // Normal rightclick mounts
        player.startRiding(this, true);
        return InteractionResult.CONSUME;
    }

    private static float clamp(float v, float a, float b) {
        return Math.max(a, Math.min(b, v));
    }

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
        hasThrusters = blueprint.blocks().stream().anyMatch(block -> block.state().is(ModBlocks.THRUSTER));
        // Sync blockCount to client immediately
        if (!level().isClientSide) {
            this.entityData.set(SYNC_BLOCK_COUNT, blockCount);
        }
    }
}
