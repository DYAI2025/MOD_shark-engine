package dev.sharkengine.ship;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class ShipEntity extends Entity {
    private static final EntityDataAccessor<Boolean> ANCHORED =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.BOOLEAN);

    private float inputThrottle; // -1..+1
    private float inputTurn;     // -1..+1
    private ShipBlueprint blueprint; // server-only in this base
    private UUID pilot;

    public ShipEntity(EntityType<? extends ShipEntity> type, Level level) {
        super(type, level);
    }

    public void setBlueprint(ShipBlueprint blueprint) {
        this.blueprint = blueprint;
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

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ANCHORED, false);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        // MVP base: no persistence yet
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        // MVP base: no persistence yet
    }

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
    protected InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;

        // Shift-rightclick toggles anchor
        if (player.isShiftKeyDown()) {
            toggleAnchor(player);
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
