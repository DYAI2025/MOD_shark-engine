package dev.sharkengine.ship;

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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class ShipEntity extends Entity {
    private static final EntityDataAccessor<Boolean> ANCHORED =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.BOOLEAN);

    private float inputThrottle; // -1..+1
    private float inputTurn;     // -1..+1
    private ShipBlueprint blueprint;
    private UUID pilot;

    public ShipEntity(EntityType<? extends ShipEntity> type, Level level) {
        super(type, level);
    }

    public void setBlueprint(ShipBlueprint blueprint) {
        this.blueprint = blueprint;
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

    public void setInputs(float throttle, float turn) {
        this.inputThrottle = clamp(throttle, -1f, 1f);
        this.inputTurn = clamp(turn, -1f, 1f);
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
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.contains("Blueprint")) {
            this.blueprint = ShipBlueprint.fromNbt(
                    compound.getCompound("Blueprint"),
                    level().registryAccess()
            );
        }
        if (compound.contains("Anchored")) {
            this.entityData.set(ANCHORED, compound.getBoolean("Anchored"));
        }
        if (compound.hasUUID("Pilot")) {
            this.pilot = compound.getUUID("Pilot");
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

    // --- Tick ---

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) return;

        if (isAnchored()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }

        // Turn
        float yaw = this.getYRot() + (inputTurn * 4.0f); // deg/tick
        this.setYRot(yaw);

        // Forward accel
        double rad = Math.toRadians(yaw);
        double fx = -Math.sin(rad);
        double fz =  Math.cos(rad);

        Vec3 accel = new Vec3(fx, 0, fz).scale(inputThrottle * 0.05);
        Vec3 vel = this.getDeltaMovement().add(accel);

        // Drag
        vel = new Vec3(vel.x * 0.90, vel.y * 0.98, vel.z * 0.90);
        this.setDeltaMovement(vel);

        // Move (vanilla collision against world, with our entity AABB)
        this.move(MoverType.SELF, this.getDeltaMovement());
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
}
