package com.example.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class RiftLightningEntity extends Entity {
    private static final EntityDataAccessor<Float> TARGET_X = SynchedEntityData.defineId(RiftLightningEntity.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET_Y = SynchedEntityData.defineId(RiftLightningEntity.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET_Z = SynchedEntityData.defineId(RiftLightningEntity.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    
    public long seed;

    public RiftLightningEntity(EntityType<? extends Entity> entityType, Level world) {
        super(entityType, world);
        this.noPhysics = true;
        this.seed = this.random.nextLong();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TARGET_X, 0.0f);
        builder.define(TARGET_Y, 0.0f);
        builder.define(TARGET_Z, 0.0f);
    }

    public void setTarget(Vec3 target) {
        this.entityData.set(TARGET_X, (float) target.x);
        this.entityData.set(TARGET_Y, (float) target.y);
        this.entityData.set(TARGET_Z, (float) target.z);
    }

    public Vec3 getTarget() {
        return new Vec3(
            this.entityData.get(TARGET_X),
            this.entityData.get(TARGET_Y),
            this.entityData.get(TARGET_Z)
        );
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 64.0 * 64.0; // Render if within 64 blocks
    }

    @Override
    public void tick() {
        super.tick();
        System.out.println("LIGHTNING TICK! Age: " + this.tickCount + " Client: " + this.level().isClientSide());
        if (!this.level().isClientSide()) {
            if (this.tickCount > 30) { // Lives for 30 ticks
                this.discard();
            }
        }
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel world, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        // Not needed to save to disk
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        // Not needed to save to disk
    }
}
