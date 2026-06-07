package com.example.entity;

import com.example.api.Gravity;
import com.example.api.GravityChanger;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

public class GravityFieldEntity extends Entity {
    public static final EntityDataAccessor<Integer> FIELD_GRAVITY = SynchedEntityData.defineId(
            GravityFieldEntity.class, EntityDataSerializers.INT
    );
    public static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(
            GravityFieldEntity.class, EntityDataSerializers.INT
    );
    public static final EntityDataAccessor<Integer> LANDING_DIRECTION = SynchedEntityData.defineId(
            GravityFieldEntity.class, EntityDataSerializers.INT
    );

    public boolean hasPlayedClientStartSound = false;
    public boolean hasPlayedClientEndSound = false;

    public GravityFieldEntity(EntityType<? extends Entity> entityType, Level world) {
        super(entityType, world);
        this.noPhysics = true;
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel world, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FIELD_GRAVITY, 0);
        builder.define(LIFETIME, 220);
        builder.define(LANDING_DIRECTION, 1);
    }

    public void setFieldGravity(Gravity gravity) {
        this.entityData.set(FIELD_GRAVITY, gravity.ordinal());
    }

    public Gravity getFieldGravity() {
        int idx = this.entityData.get(FIELD_GRAVITY);
        Gravity[] values = Gravity.values();
        return (idx >= 0 && idx < values.length) ? values[idx] : Gravity.UP;
    }

    public void setLandingDirection(net.minecraft.core.Direction direction) {
        this.entityData.set(LANDING_DIRECTION, direction.ordinal());
    }

    public net.minecraft.core.Direction getLandingDirection() {
        int idx = this.entityData.get(LANDING_DIRECTION);
        net.minecraft.core.Direction[] values = net.minecraft.core.Direction.values();
        return (idx >= 0 && idx < values.length) ? values[idx] : net.minecraft.core.Direction.UP;
    }

    public void setLifetime(int ticks) {
        this.entityData.set(LIFETIME, ticks);
    }

    public int getLifetime() {
        return this.entityData.get(LIFETIME);
    }

    @Override
    public void tick() {
        super.tick();

        net.minecraft.core.Direction dir = this.getLandingDirection();
        double minX = this.getX() - 4.0; double maxX = this.getX() + 4.0;
        double minY = this.getY() - 4.0; double maxY = this.getY() + 4.0;
        double minZ = this.getZ() - 4.0; double maxZ = this.getZ() + 4.0;

        if (dir.getAxis() == net.minecraft.core.Direction.Axis.X) {
            minX = this.getX() - 1.0; maxX = this.getX() + 1.0;
        } else if (dir.getAxis() == net.minecraft.core.Direction.Axis.Y) {
            minY = this.getY() - 1.0; maxY = this.getY() + 1.0;
        } else if (dir.getAxis() == net.minecraft.core.Direction.Axis.Z) {
            minZ = this.getZ() - 1.0; maxZ = this.getZ() + 1.0;
        }
        
        AABB fieldBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        this.setBoundingBox(fieldBox);

        if (this.level().isClientSide()) {
            int remaining = getLifetime();
            if (this.tickCount == 1) {
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, 1.2F, 0.5F, false);
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(),
                        SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2.0F, 0.6F, false);
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(),
                        SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.5F, 0.5F, false);
            } else if (this.tickCount <= 25 && this.tickCount % 3 == 0) {
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(),
                        SoundEvents.DEEPSLATE_BREAK, SoundSource.BLOCKS, 1.5F, 0.5F + (this.tickCount * 0.01F), false);
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ROOTED_DIRT_BREAK, SoundSource.BLOCKS, 1.2F, 0.6F, false);
            }

            if (this.tickCount == remaining - 25) {
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(),
                        SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 2.0F, 0.6F, false);
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(),
                        SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1.5F, 0.5F, false);
            } else if (this.tickCount >= remaining - 25 && this.tickCount % 3 == 0) {
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(),
                        SoundEvents.DEEPSLATE_BREAK, SoundSource.BLOCKS, 1.5F, 0.5F + ((remaining - this.tickCount) * 0.01F), false);
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ROOTED_DIRT_BREAK, SoundSource.BLOCKS, 1.2F, 0.6F, false);
            }

            if (this.tickCount < remaining - 20 && this.tickCount % 20 == 0) {
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.5F, 0.7F, false);
            }
        } else {
            int remaining = getLifetime();
            if (this.tickCount >= remaining) {
                this.discard();
                return;
            }

            if (this.tickCount < remaining - 20 && this.tickCount % 10 == 0) {
                List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, fieldBox);
                Gravity gravity = getFieldGravity();

                for (LivingEntity target : targets) {
                    if (target instanceof GravityChanger changer) {
                        changer.infect(gravity, 210);
                    }
                }
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        int gravIdx = input.getInt("FieldGravity").orElse(0);
        this.setFieldGravity(Gravity.values()[gravIdx]);
        int ticks = input.getInt("Lifetime").orElse(200);
        this.setLifetime(ticks);
        int landIdx = input.getInt("LandingDirection").orElse(1);
        this.setLandingDirection(net.minecraft.core.Direction.values()[landIdx]);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("FieldGravity", getFieldGravity().ordinal());
        output.putInt("Lifetime", getLifetime());
        output.putInt("LandingDirection", getLandingDirection().ordinal());
    }
}
