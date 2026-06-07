package com.example.mixin;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;

import net.minecraft.world.phys.AABB;

import com.example.api.Gravity;
import com.example.api.GravityChanger;
import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import it.unimi.dsi.fastutil.floats.FloatArraySet;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.floats.FloatSet;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.Pose;


import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;

import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.Level;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.List;

@Mixin(value = Entity.class, priority = 1100)
public abstract class CoreGravityMixin implements GravityChanger {



    @Unique
    private static final EntityDataAccessor<Integer> GRAVITY_DIRECTION = SynchedEntityData.defineId(Entity.class,
            EntityDataSerializers.INT);
    @Unique
    private static final EntityDataAccessor<Float> GRAVITY_ANOMALY = SynchedEntityData.defineId(Entity.class,
            EntityDataSerializers.FLOAT);

    @Shadow
    public abstract SynchedEntityData getEntityData();

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;defineSynchedData(Lnet/minecraft/network/syncher/SynchedEntityData$Builder;)V"))
    private void gravity$initSynchedEntityData(Entity self, SynchedEntityData.Builder builder, Operation<Void> original) {
        original.call(self, builder);
        builder.define(GRAVITY_DIRECTION, 0);
        builder.define(GRAVITY_ANOMALY, 0.0f);
    }

    @Unique
    private Gravity prevGravityDirection = Gravity.DOWN;
    @Unique
    private int gravityTransitionTimer = 0;
    @Unique
    private int gravityImmunityTimer = 0;
    @Unique
    private int maxTransitionTicks = 20;

    @Unique
    private boolean isInfected = false;
    @Unique
    private int infectionTimer = 0;
    @Unique
    private Gravity infectedGravity = Gravity.DOWN;
    @Unique
    private Gravity baseGravity = Gravity.DOWN;
    @Unique
    private float prevAnomalyStrength = 0.0f;

    @Override
    public boolean isInfected() {
        return this.isInfected;
    }

    @Override
    public int getInfectionTimer() {
        return this.infectionTimer;
    }

    @Override
    public Gravity getInfectedGravity() {
        return this.infectedGravity;
    }

    @Override
    public Gravity getBaseGravity() {
        return this.baseGravity;
    }

    @Override
    public void infect(Gravity gravity, int durationTicks) {
        if (!this.isInfected) {
            this.baseGravity = getGravityDirection();
            this.isInfected = true;
        }
        this.infectedGravity = gravity;
        this.infectionTimer = durationTicks;
        this.setGravity(gravity);
    }

    @Override
    public void clearInfection() {
        if (this.isInfected) {
            this.isInfected = false;
            this.infectionTimer = 0;
            this.setGravity(this.baseGravity);
        }
    }

    @Override
    public float getGravityAnomalyStrength() {
        return getEntityData().get(GRAVITY_ANOMALY);
    }

    @Override
    public void setGravityAnomalyStrength(float strength) {
        getEntityData().set(GRAVITY_ANOMALY, net.minecraft.util.Mth.clamp(strength, 0.0f, 1.0f));
    }

    @Override
    public float getPrevGravityAnomalyStrength() {
        return this.prevAnomalyStrength;
    }

    @Override
    public void setGravityInstant(Gravity gravity) {
        Gravity current = getGravityDirection();
        Entity entity = (Entity) (Object) this;
        this.prevGravityDirection = gravity;
        this.gravityTransitionTimer = 0;
        this.gravityImmunityTimer = 0;
        this.needsLandingShake = false;
        getEntityData().set(GRAVITY_DIRECTION, gravity.ordinal());
        
        net.minecraft.world.phys.AABB oldBox = entity.getBoundingBox();
        net.minecraft.world.phys.Vec3 center = oldBox.getCenter();
        double newX = center.x;
        double newY = center.y;
        double newZ = center.z;

        switch (gravity.getDirection()) {
            case UP -> newY = oldBox.maxY;
            case DOWN -> newY = oldBox.minY;
            case NORTH -> newZ = oldBox.minZ;
            case SOUTH -> newZ = oldBox.maxZ;
            case WEST -> newX = oldBox.minX;
            case EAST -> newX = oldBox.maxX;
        }

        entity.setPos(newX, newY, newZ);
        onGravityChanged();

        Entity vehicle = entity.getVehicle();
        if (vehicle != null && vehicle instanceof GravityChanger vehicleGc) {
            if (vehicleGc.getGravityDirection() != gravity) {
                vehicleGc.setGravityInstant(gravity);
            }
        }
        for (Entity passenger : entity.getPassengers()) {
            if (passenger instanceof GravityChanger passengerGc) {
                if (passengerGc.getGravityDirection() != gravity) {
                    passengerGc.setGravityInstant(gravity);
                }
            }
        }
    }

    @Override
    public Gravity getGravityDirection() {
        int idx = getEntityData().get(GRAVITY_DIRECTION);
        Gravity[] values = Gravity.values();
        return (idx >= 0 && idx < values.length) ? values[idx] : Gravity.DOWN;
    }

    @Override
    public Gravity getPrevGravityDirection() {
        return this.prevGravityDirection;
    }

    @Override
    public int getGravityTransitionTimer() {
        return this.gravityTransitionTimer;
    }

    @Override
    public void setGravityTransitionTimer(int ticks) {
        this.gravityTransitionTimer = ticks;
    }

    @Override
    public float getGravityTransitionProgress(float tickDelta) {
        if (this.gravityTransitionTimer <= 0)
            return 1.0f;
        float t = 1.0f - ((float) this.gravityTransitionTimer - tickDelta) / (float) this.maxTransitionTicks;
        t = Mth.clamp(t, 0.0f, 1.0f);
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        return 1.0f + c3 * (float) Math.pow(t - 1.0f, 3) + c1 * (float) Math.pow(t - 1.0f, 2);
    }

    public boolean hasGravityImmunity() {
        return this.gravityImmunityTimer > 0;
    }

    @Unique
    private boolean needsLandingShake = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void gravity$tick(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        
        if (this.gravityTransitionTimer > 0) {
            this.gravityTransitionTimer--;
            if (this.gravityTransitionTimer <= 0) {
                this.prevGravityDirection = getGravityDirection();
            }
        }
        this.prevAnomalyStrength = getGravityAnomalyStrength();
        
        if (this.needsLandingShake && this.gravityTransitionTimer <= 0 && self.level().isClientSide() && self instanceof net.minecraft.world.entity.player.Player player && player.isLocalPlayer()) {
            if (self.onGround()) {
                com.example.util.CameraShakeTracker.addShake(8.0f);
                this.needsLandingShake = false;
            }
        }
        if (this.gravityImmunityTimer > 0) {
            this.gravityImmunityTimer--;
            this.fallDistance = 0.0f;
        }
        if (this.isInfected) {
            if (this.infectionTimer > 0) {
                this.infectionTimer--;
                if (this.infectionTimer <= 0) {
                    this.clearInfection();
                }
            }
        }
    }

    @Override
    public void setGravity(Gravity gravity) {
        Gravity current = getGravityDirection();
        if (current != gravity) {
            Entity entity = (Entity) (Object) this;
            AABB oldBox = entity.getBoundingBox();

            this.prevGravityDirection = current;
            this.maxTransitionTicks = (current.getDirection().getOpposite() == gravity.getDirection()) ? 25 : 15;
            this.gravityTransitionTimer = this.maxTransitionTicks;
            this.gravityImmunityTimer = 40;
            this.fallDistance = 0.0f;
            this.needsLandingShake = true;
            getEntityData().set(GRAVITY_DIRECTION, gravity.ordinal());

            if (entity.level().isClientSide() && entity instanceof net.minecraft.world.entity.player.Player player && player.isLocalPlayer()) {
                com.example.util.CameraShakeTracker.addShake(15.0f);
                entity.level().playLocalSound(entity.getX(), entity.getY(), entity.getZ(), net.minecraft.sounds.SoundEvents.ENDER_DRAGON_FLAP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.5F, false);
                entity.level().playLocalSound(entity.getX(), entity.getY(), entity.getZ(), net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT, net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, 1.5F, false);
            }

            Vec3 center = oldBox.getCenter();
            float height = entity.getBbHeight();

            double newX = center.x;
            double newY = center.y;
            double newZ = center.z;

            switch (gravity.getDirection()) {
                case UP -> newY = oldBox.maxY;
                case DOWN -> newY = oldBox.minY;
                case NORTH -> newZ = oldBox.minZ;
                case SOUTH -> newZ = oldBox.maxZ;
                case WEST -> newX = oldBox.minX;
                case EAST -> newX = oldBox.maxX;
            }

            entity.setPos(newX, newY, newZ);
            onGravityChanged();

            Entity vehicle = entity.getVehicle();
            if (vehicle != null && vehicle instanceof GravityChanger vehicleGc) {
                if (vehicleGc.getGravityDirection() != gravity) {
                    vehicleGc.setGravity(gravity);
                }
            }
            for (Entity passenger : entity.getPassengers()) {
                if (passenger instanceof GravityChanger passengerGc) {
                    if (passengerGc.getGravityDirection() != gravity) {
                        passengerGc.setGravity(gravity);
                    }
                }
            }
        }
    }

    @Override
    public void onGravityChanged() {
        Entity self = (Entity) (Object) this;
        self.refreshDimensions();
    }

    @Inject(method = "onSyncedDataUpdated(Lnet/minecraft/network/syncher/EntityDataAccessor;)V", at = @At("HEAD"))
    private void gravity$onSyncedDataUpdated(EntityDataAccessor<?> data, CallbackInfo ci) {
        if (GRAVITY_DIRECTION.equals(data)) {
            Entity self = (Entity) (Object) this;
            if (self.level().isClientSide()) {
                int newIdx = getEntityData().get(GRAVITY_DIRECTION);
                Gravity[] values = Gravity.values();
                Gravity newGravity = (newIdx >= 0 && newIdx < values.length) ? values[newIdx] : Gravity.DOWN;

                if (this.prevGravityDirection != newGravity && this.gravityTransitionTimer <= 0) {
                    float currentAnomaly = getGravityAnomalyStrength();
                    if (currentAnomaly > 0.5f || this.prevAnomalyStrength > 0.5f) {
                        this.prevGravityDirection = newGravity;
                        this.gravityImmunityTimer = 40;
                        this.fallDistance = 0.0f;
                        onGravityChanged();
                        return;
                    }
                    net.minecraft.world.phys.AABB oldBox = self.getBoundingBox();
                    
                    this.maxTransitionTicks = (this.prevGravityDirection.getDirection().getOpposite() == newGravity.getDirection()) ? 25 : 15;
                    this.gravityTransitionTimer = this.maxTransitionTicks;
                    this.gravityImmunityTimer = 40;
                    this.fallDistance = 0.0f;
                    this.needsLandingShake = true;

                    if (self.level().isClientSide() && self instanceof net.minecraft.world.entity.player.Player player && player.isLocalPlayer()) {
                        com.example.util.CameraShakeTracker.addShake(15.0f);
                        self.level().playLocalSound(self.getX(), self.getY(), self.getZ(), net.minecraft.sounds.SoundEvents.ENDER_DRAGON_FLAP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.5F, false);
                        self.level().playLocalSound(self.getX(), self.getY(), self.getZ(), net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT, net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, 1.5F, false);
                    }

                    net.minecraft.world.phys.Vec3 center = oldBox.getCenter();
                    double newX = center.x;
                    double newY = center.y;
                    double newZ = center.z;

                    switch (newGravity.getDirection()) {
                        case UP -> newY = oldBox.maxY;
                        case DOWN -> newY = oldBox.minY;
                        case NORTH -> newZ = oldBox.minZ;
                        case SOUTH -> newZ = oldBox.maxZ;
                        case WEST -> newX = oldBox.minX;
                        case EAST -> newX = oldBox.maxX;
                    }

                    self.setPos(newX, newY, newZ);
                    onGravityChanged();
                }
            }
        }
    }

    @WrapOperation(method = "saveWithoutId", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;addAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueOutput;)V"))
    private void gravity$writeCustomData(Entity self, net.minecraft.world.level.storage.ValueOutput view, Operation<Void> original) {
        original.call(self, view);
        view.putInt("GravityDirection", getGravityDirection().ordinal());
        view.putInt("PrevGravityDirection", this.prevGravityDirection.ordinal());
        view.putInt("GravityTransitionTimer", this.gravityTransitionTimer);
        view.putInt("GravityImmunityTimer", this.gravityImmunityTimer);
        view.putInt("IsInfected", this.isInfected ? 1 : 0);
        view.putInt("InfectionTimer", this.infectionTimer);
        view.putInt("InfectedGravity", this.infectedGravity.ordinal());
        view.putInt("BaseGravity", this.baseGravity.ordinal());
        view.putInt("GravityAnomalyStrength", (int)(getGravityAnomalyStrength() * 1000));
    }

    @WrapOperation(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V"))
    private void gravity$readCustomData(Entity self, net.minecraft.world.level.storage.ValueInput view, Operation<Void> original) {
        original.call(self, view);
        int idx = view.getInt("GravityDirection").orElse(0);
        Gravity[] values = Gravity.values();
        if (idx >= 0 && idx < values.length) {
            getEntityData().set(GRAVITY_DIRECTION, idx);
        }
        this.prevGravityDirection = Gravity.values()[view.getInt("PrevGravityDirection").orElse(0)];
        this.gravityTransitionTimer = view.getInt("GravityTransitionTimer").orElse(0);
        this.gravityImmunityTimer = view.getInt("GravityImmunityTimer").orElse(0);
        this.isInfected = view.getInt("IsInfected").orElse(0) != 0;
        this.infectionTimer = view.getInt("InfectionTimer").orElse(0);
        int infIdx = view.getInt("InfectedGravity").orElse(0);
        this.infectedGravity = (infIdx >= 0 && infIdx < values.length) ? values[infIdx] : Gravity.DOWN;
        int baseIdx = view.getInt("BaseGravity").orElse(0);
        this.baseGravity = (baseIdx >= 0 && baseIdx < values.length) ? values[baseIdx] : Gravity.DOWN;
        setGravityAnomalyStrength(view.getInt("GravityAnomalyStrength").orElse(0) / 1000.0f);
    }



    
    @Shadow
    public abstract Pose getPose();
    @Shadow
    public abstract EntityDimensions getDimensions(Pose pose);
    @Shadow
    protected abstract Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale);

    @Shadow
    private EntityDimensions dimensions;
    @Shadow
    private float eyeHeight;
    @Shadow
    public double xOld;
    @Shadow
    public double yOld;
    @Shadow
    public double zOld;

    @Shadow
    public abstract double getX();

    @Shadow
    public abstract Vec3 getEyePosition();

    @Shadow
    public abstract double getY();

    @Shadow
    public abstract double getZ();

    @Shadow
    private Level level;

    @Shadow
    public abstract int getBlockX();

    @Shadow
    public abstract int getBlockZ();

    @Shadow
    public boolean noPhysics;

    @Shadow
    public abstract Vec3 getDeltaMovement();

    @Shadow
    public abstract boolean isVehicle();

    @Shadow
    public abstract AABB getBoundingBox();

    @Shadow
    public abstract Vec3 position();

    @Shadow
    public abstract boolean isPassengerOfSameVehicle(Entity entity);

    @Shadow
    public abstract void addDeltaMovement(Vec3 vec);

    @Shadow
    protected abstract void onBelowWorld();

    @Shadow
    public abstract double getEyeY();

    @Shadow
    public abstract float getViewYRot(float tickDelta);

    @Shadow
    public abstract float getYRot();

    @Shadow
    public abstract float getXRot();

    @Shadow
    public abstract boolean onGround();

    @Shadow
    public abstract float maxUpStep();

    @Shadow
    @Final
    protected net.minecraft.util.RandomSource random;
    @Shadow
    public double fallDistance;

    @Shadow
    private static Vec3 collideWithShapes(Vec3 movement, AABB entityBoundingBox,
            List<VoxelShape> collisions) {
        return null;
    }

    @Shadow
    private static List<VoxelShape> collectColliders(Entity source, Level level, List<VoxelShape> entityColliders, AABB boundingBox) {
        return null;
    }



    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void gravity$causeFallDamage(double fallDistance, float damageMultiplier,
            DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if (this.hasGravityImmunity()) {
            cir.setReturnValue(false);
        }
    }



    @Inject(method = "makeBoundingBox(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/AABB;", at = @At("RETURN"), cancellable = true)
    private void gravity$makeBoundingBox(Vec3 pos, CallbackInfoReturnable<AABB> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof net.minecraft.world.entity.projectile.Projectile)
            return;

        Direction gravityDirection = GravityHelper.getGravityDirection(self);
        if (gravityDirection == Direction.DOWN)
            return;

        cir.setReturnValue(GravityHelper.buildGravityAwareBox(cir.getReturnValue(), pos, self.getDimensions(self.getPose()), gravityDirection));
    }



    @Inject(method = "calculateViewVector(FF)Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"), cancellable = true)
    private void gravity$calculateViewVector(CallbackInfoReturnable<Vec3> cir) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return;

        cir.setReturnValue(RotationUtil.vecPlayerToWorld(cir.getReturnValue(), gravityDirection));
    }



    @Inject(method = "getBlockPosBelowThatAffectsMyMovement", at = @At("HEAD"), cancellable = true)
    private void gravity$getBlockPosBelowThatAffectsMyMovement(CallbackInfoReturnable<BlockPos> cir) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return;

        cir.setReturnValue(
                BlockPos.containing(this.position().add(new Vec3(gravityDirection.getStepX(), gravityDirection.getStepY(), gravityDirection.getStepZ()).scale(0.5000001D))));
    }



    @Inject(method = "getEyeY()D", at = @At("HEAD"), cancellable = true)
    private void gravity$getEyeY(CallbackInfoReturnable<Double> cir) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN) return;
        
        double yOff = switch (gravityDirection) {
            case UP -> -this.eyeHeight;
            case NORTH, SOUTH, WEST, EAST -> 0.0D;
            default -> this.eyeHeight;
        };
        cir.setReturnValue(this.getY() + yOff);
    }

    @Inject(method = "getEyePosition()Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void gravity$getEyePosition(CallbackInfoReturnable<Vec3> cir) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return;

        cir.setReturnValue(
                RotationUtil.vecPlayerToWorld(0.0D, this.eyeHeight, 0.0D, gravityDirection).add(this.position()));
    }



    @Inject(method = "getEyePosition(F)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void gravity$getEyePositionFloat(float tickDelta, CallbackInfoReturnable<Vec3> cir) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return;

        Vec3 vec3d = RotationUtil.vecPlayerToWorld(0.0D, this.eyeHeight, 0.0D, gravityDirection);
        double d = Mth.lerp((double) tickDelta, this.xOld, this.getX()) + vec3d.x;
        double e = Mth.lerp((double) tickDelta, this.yOld, this.getY()) + vec3d.y;
        double f = Mth.lerp((double) tickDelta, this.zOld, this.getZ()) + vec3d.z;
        cir.setReturnValue(new Vec3(d, e, f));
    }



    @Inject(method = "getLightLevelDependentMagicValue()F", at = @At("HEAD"), cancellable = true)
    private void gravity$getBrightnessAtEyes(CallbackInfoReturnable<Float> cir) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return;

        cir.setReturnValue(((Entity)(Object)this).level().hasChunkAt(((Entity)(Object)this).blockPosition().getX(), ((Entity)(Object)this).blockPosition().getZ())
                ? ((Entity)(Object)this).level().getMaxLocalRawBrightness(BlockPos.containing(this.getEyePosition()))
                : 0.0F);
    }







    @Inject(method = "getOnPosLegacy()Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
    private void gravity$getOnPosLegacy(CallbackInfoReturnable<BlockPos> cir) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return;

        cir.setReturnValue(BlockPos.containing(
                RotationUtil.vecPlayerToWorld(0.0D, -0.2F, 0.0D, gravityDirection).add(this.position())));
    }



    @Redirect(
        method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;collectCandidateStepUpHeights(Lnet/minecraft/world/phys/AABB;Ljava/util/List;FF)[F", ordinal = 0)
    )
    private float[] gravity$collectStepHeights(AABB boxSnappedToGround, List<VoxelShape> allCollisions, float stepHeight, float distToGround) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN) {
            FloatSet floatSet = new FloatArraySet(4);
            double relativeBottom = boxSnappedToGround.minY;
            for (VoxelShape voxelShape : allCollisions) {
                for (double collisionPoint : voxelShape.getCoords(Direction.Axis.Y)) {
                    float verticalDist = (float) (collisionPoint - relativeBottom);
                    if (!(verticalDist < 0.0F) && verticalDist != distToGround) {
                        if (verticalDist > stepHeight)
                            break;
                        floatSet.add(verticalDist);
                    }
                }
            }
            float[] fs = floatSet.toFloatArray();
            FloatArrays.unstableSort(fs);
            return fs;
        }

        FloatSet floatSet = new FloatArraySet(4);
        double relativeBottom = getRelativeBottom(boxSnappedToGround, gravityDirection);

        if (gravityDirection.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
            for (VoxelShape voxelShape : allCollisions) {
                for (double collisionPoint : voxelShape.getCoords(gravityDirection.getAxis())) {
                    float verticalDist = (float) (collisionPoint - relativeBottom);
                    if (!(verticalDist < 0.0F) && verticalDist != distToGround) {
                        if (verticalDist > stepHeight)
                            break;
                        floatSet.add(verticalDist);
                    }
                }
            }
        } else {
            for (VoxelShape voxelShape : allCollisions) {
                for (double collisionPoint : voxelShape.getCoords(gravityDirection.getAxis()).reversed()) {
                    float verticalDist = -(float) (collisionPoint - relativeBottom);
                    if (!(verticalDist < 0.0F) && verticalDist != distToGround) {
                        if (verticalDist > stepHeight)
                            break;
                        floatSet.add(verticalDist);
                    }
                }
            }
        }

        float[] fs = floatSet.toFloatArray();
        FloatArrays.unstableSort(fs);
        return fs;
    }

    @Unique
    private static double getRelativeBottom(AABB box, Direction gravityDirection) {
        return switch (gravityDirection) {
            case DOWN -> box.minY;
            case UP -> box.maxY;
            case NORTH -> box.minZ;
            case SOUTH -> box.maxZ;
            case WEST -> box.minX;
            case EAST -> box.maxX;
        };
    }

    @Redirect(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;collideWithShapes(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;", ordinal = 0))
    private Vec3 gravity$redirect_innerCollide(Vec3 movement, AABB entityBoundingBox, List<VoxelShape> collisions) {
        return gravity$redirection(movement, entityBoundingBox, collisions, (Entity) (Object) this);
    }



    @Inject(
            method = "collideBoundingBox(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/level/Level;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void gravity$onCollideBoundingBox(Entity source, Vec3 movement, AABB boundingBox, Level level, List<VoxelShape> entityColliders, CallbackInfoReturnable<Vec3> cir) {
        if (source != null) {
            List<VoxelShape> colliders = collectColliders(source, level, entityColliders, boundingBox.expandTowards(movement));
            Vec3 result = com.example.api.physics.GravityMovement.calculateCollision(movement, boundingBox, colliders, source);
            if (result != null) {
                cir.setReturnValue(result);
            }
        }
    }

    @Unique
    private static Vec3 gravity$redirection(Vec3 movement, AABB entityBoundingBox, List<VoxelShape> collisions,
            Entity entity) {
        Vec3 result = com.example.api.physics.GravityMovement.calculateCollision(movement, entityBoundingBox, collisions, entity);
        if (result != null) {
            return result;
        }
        return collideWithShapes(movement, entityBoundingBox, collisions);
    }



    @ModifyArgs(method = "isInWall()Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;ofSize(Lnet/minecraft/world/phys/Vec3;DDD)Lnet/minecraft/world/phys/AABB;", ordinal = 0))
    private void gravity$isInsideWall(Args args) {
        Vec3 rotate = new Vec3(args.get(1), args.get(2), args.get(3));
        rotate = RotationUtil.vecPlayerToWorld(rotate, GravityHelper.getGravityDirection((Entity) (Object) this));
        args.set(1, rotate.x);
        args.set(2, rotate.y);
        args.set(3, rotate.z);
    }



    @ModifyArg(method = "getDirection", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Direction;fromYRot(D)Lnet/minecraft/core/Direction;"))
    private double gravity$getHorizontalFacing(double rotation) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return rotation;

        return RotationUtil.rotPlayerToWorld((float) rotation, ((Entity)(Object)this).getXRot(), gravityDirection).x;
    }



    @Inject(method = "spawnSprintParticle()V", at = @At("HEAD"), cancellable = true)
    private void gravity$spawnSprintParticle(CallbackInfo ci) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return;

        ci.cancel();

        Vec3 floorPos = this.position()
                .subtract(RotationUtil.vecPlayerToWorld(0.0D, 0.20000000298023224D, 0.0D, gravityDirection));
        BlockPos blockPos = BlockPos.containing(floorPos);
        BlockState blockState = ((Entity)(Object)this).level().getBlockState(blockPos);
        if (blockState.getRenderShape() != RenderShape.INVISIBLE) {
            Vec3 particlePos = this.position().add(RotationUtil.vecPlayerToWorld(
                    (this.random.nextDouble() - 0.5D) * (double) this.dimensions.width(), 0.1D,
                    (this.random.nextDouble() - 0.5D) * (double) this.dimensions.width(), gravityDirection));
            Vec3 playerVelocity = ((Entity)(Object)this).getDeltaMovement();
            Vec3 particleVelocity = RotationUtil.vecPlayerToWorld(playerVelocity.x * -4.0D, 1.5D,
                    playerVelocity.z * -4.0D, gravityDirection);
            ((Entity)(Object)this).level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, blockState),
                    particlePos.x, particlePos.y, particlePos.z,
                    particleVelocity.x, particleVelocity.y, particleVelocity.z);
        }
    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void gravity$push(Entity entity, CallbackInfo ci) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        Direction otherGravityDirection = GravityHelper.getGravityDirection(entity);

        if (gravityDirection == Direction.DOWN && otherGravityDirection == Direction.DOWN)
            return;

        ci.cancel();

        if (!this.isPassengerOfSameVehicle(entity)) {
            if (!entity.noPhysics && !((Entity)(Object)this).noPhysics) {
                Vec3 entityOffset = entity.getBoundingBox().getCenter().subtract(this.getBoundingBox().getCenter());

                {
                    Vec3 PlayerOffset = RotationUtil.vecWorldToPlayer(entityOffset, gravityDirection);
                    double dx = PlayerOffset.x;
                    double dz = PlayerOffset.z;
                    double f = Mth.absMax(dx, dz);
                    if (f >= 0.01F) {
                        f = Math.sqrt(f);
                        dx /= f;
                        dz /= f;
                        double g = 1.0D / f;
                        if (g > 1.0D)
                            g = 1.0D;
                        dx *= g;
                        dz *= g;
                        dx *= 0.05F;
                        dz *= 0.05F;
                        if (!this.isVehicle()) {
                            ((Entity)(Object)this).addDeltaMovement(new Vec3(-dx, 0.0D, -dz));
                        }
                    }
                }

                {
                    Vec3 entityEntityOffset = RotationUtil.vecWorldToPlayer(entityOffset, otherGravityDirection);
                    double dx = entityEntityOffset.x;
                    double dz = entityEntityOffset.z;
                    double f = Mth.absMax(dx, dz);
                    if (f >= 0.01F) {
                        f = Math.sqrt(f);
                        dx /= f;
                        dz /= f;
                        double g = 1.0D / f;
                        if (g > 1.0D)
                            g = 1.0D;
                        dx *= g;
                        dz *= g;
                        dx *= 0.05F;
                        dz *= 0.05F;
                        if (!entity.isVehicle()) {
                            entity.addDeltaMovement(new Vec3(dx, 0.0D, dz));
                        }
                    }
                }
            }
        }
    }



    @Inject(method = "checkBelowWorld()V", at = @At("HEAD"), cancellable = true)
    private void gravity$checkBelowWorld(CallbackInfo ci) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);

        if (this.getY() > (double) (((Entity)(Object)this).level().getMaxY() + 256) && gravityDirection == Direction.UP) {
            this.onBelowWorld();
            ci.cancel();
            return;
        }

        if (gravityDirection.getAxis() != Direction.Axis.Y && fallDistance > 1024) {
            this.onBelowWorld();
            ci.cancel();
        }
    }



    @ModifyArgs(method = "isFree(DDD)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;move(DDD)Lnet/minecraft/world/phys/AABB;", ordinal = 0))
    private void gravity$isFree(Args args) {
        Vec3 rotate = new Vec3(args.get(0), args.get(1), args.get(2));
        rotate = RotationUtil.vecPlayerToWorld(rotate, GravityHelper.getGravityDirection((Entity) (Object) this));
        args.set(0, rotate.x);
        args.set(1, rotate.y);
        args.set(2, rotate.z);
    }

    @Inject(method = "applyGravity", at = @At("HEAD"), cancellable = true)
    private void gravity$applyGravity(CallbackInfo ci) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        float anomaly = this.getGravityAnomalyStrength();

        if (gravityDirection == Direction.DOWN && anomaly > 0.0f) {
            Entity self = (Entity) (Object) this;
            double gravity = self.getGravity();
            if (gravity != 0.0) {
                double factor = 1.0 - 2.0 * anomaly;
                Vec3 vel = self.getDeltaMovement();
                self.setDeltaMovement(vel.add(0.0, -gravity * factor, 0.0));
            }
            ci.cancel();
            return;
        }

        if (gravityDirection == Direction.DOWN)
            return;

        Entity self = (Entity) (Object) this;
        double gravity = self.getGravity();
        if (gravity != 0.0) {
            Vec3 vel = self.getDeltaMovement();
            self.setDeltaMovement(vel.add(0.0, -gravity, 0.0));
        }
        ci.cancel();
    }

    @ModifyVariable(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private Vec3 gravity$move_toWorld(Vec3 vec3d) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return vec3d;

        return RotationUtil.vecPlayerToWorld(vec3d, gravityDirection);
    }

    @ModifyVariable(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V", ordinal = 0), ordinal = 0, argsOnly = true)
    private Vec3 gravity$move_toLocal_arg(Vec3 vec3d) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return vec3d;

        return RotationUtil.vecWorldToPlayer(vec3d, gravityDirection);
    }

    @ModifyVariable(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V", ordinal = 0), ordinal = 1)
    private Vec3 gravity$move_toLocal_result(Vec3 vec3d) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return vec3d;

        return RotationUtil.vecWorldToPlayer(vec3d, gravityDirection);
    }

    @Redirect(
        method = "positionRider(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity$MoveFunction;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity$MoveFunction;accept(Lnet/minecraft/world/entity/Entity;DDD)V")
    )
    private void gravity$positionRider_redirect(Entity.MoveFunction moveFunction, Entity passenger, double x, double y, double z) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN) {
            moveFunction.accept(passenger, x, y, z);
            return;
        }
        Entity vehicle = (Entity) (Object) this;
        Vec3 localPassenger = this.getPassengerAttachmentPoint(passenger, vehicle.getDimensions(this.getPose()), 1.0F);
        Vec3 localVehicle = passenger.getVehicleAttachmentPoint(vehicle);
        Vec3 worldPassenger = RotationUtil.vecPlayerToWorld(localPassenger, gravityDirection);
        Vec3 worldVehicle = RotationUtil.vecPlayerToWorld(localVehicle, gravityDirection);
        Vec3 finalPos = vehicle.position().add(worldPassenger).subtract(worldVehicle);
        moveFunction.accept(passenger, finalPos.x, finalPos.y, finalPos.z);
    }

    @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z", at = @At("RETURN"))
    private void gravity$startRiding(Entity entityToRide, boolean force, boolean sendEventAndTriggers, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            Entity self = (Entity) (Object) this;
            Gravity passengerGravity = ((GravityChanger) self).getGravityDirection();
            if (entityToRide instanceof GravityChanger vehicleGc) {
                if (vehicleGc.getGravityDirection() != passengerGravity) {
                    vehicleGc.setGravityInstant(passengerGravity);
                }
            }
        }
    }

    @Inject(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void gravity$onCollide(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;
        Direction gravityDirection = GravityHelper.getGravityDirection(self);
        if (gravityDirection != Direction.DOWN) {
            cir.setReturnValue(gravity$calculateGravityCollide(movement, gravityDirection));
        }
    }

    @Unique
    private Vec3 gravity$calculateGravityCollide(Vec3 movement, Direction gravityDirection) {
        Entity self = (Entity) (Object) this;
        AABB aabb = self.getBoundingBox();
        List<VoxelShape> entityColliders = self.level().getEntityCollisions(self, aabb.expandTowards(movement));
        List<VoxelShape> colliders = collectColliders(self, self.level(), entityColliders, aabb.expandTowards(movement));
        Vec3 movementStep = movement.lengthSqr() == 0.0 ? movement : com.example.api.physics.GravityMovement.calculateCollision(movement, aabb, colliders, self);
        float maxUpStep = self.maxUpStep();
        if (maxUpStep > 0.0F) {
            Vec3 playerMovement = RotationUtil.vecWorldToPlayer(movement, gravityDirection);
            Vec3 playerMovementStep = RotationUtil.vecWorldToPlayer(movementStep, gravityDirection);
            boolean xCollision = playerMovement.x != playerMovementStep.x;
            boolean zCollision = playerMovement.z != playerMovementStep.z;
            boolean yCollision = playerMovement.y != playerMovementStep.y;
            boolean onGroundAfterCollision = yCollision && playerMovement.y < 0.0;
            if ((onGroundAfterCollision || self.onGround()) && (xCollision || zCollision)) {
                AABB groundedAABB = onGroundAfterCollision ? aabb.move(RotationUtil.vecPlayerToWorld(0.0, playerMovementStep.y, 0.0, gravityDirection)) : aabb;
                Vec3 expansionVec = RotationUtil.vecPlayerToWorld(playerMovement.x, maxUpStep, playerMovement.z, gravityDirection);
                AABB stepUpAABB = groundedAABB.expandTowards(expansionVec);
                if (!onGroundAfterCollision) {
                    Vec3 downExpansion = RotationUtil.vecPlayerToWorld(0.0, -9.999999747378752E-6, 0.0, gravityDirection);
                    stepUpAABB = stepUpAABB.expandTowards(downExpansion);
                }
                List<VoxelShape> stepUpColliders = collectColliders(self, self.level(), entityColliders, stepUpAABB);
                float stepHeightToSkip = (float) playerMovementStep.y;
                FloatSet floatSet = new FloatArraySet(4);
                double relativeBottom = getRelativeBottom(groundedAABB, gravityDirection);
                double distToGround = playerMovement.y - playerMovementStep.y;
                if (gravityDirection.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
                    for (VoxelShape voxelShape : stepUpColliders) {
                        for (double collisionPoint : voxelShape.getCoords(gravityDirection.getAxis())) {
                            float verticalDist = (float) (collisionPoint - relativeBottom);
                            if (!(verticalDist < 0.0F) && verticalDist != distToGround) {
                                if (verticalDist > maxUpStep)
                                    break;
                                floatSet.add(verticalDist);
                            }
                        }
                    }
                } else {
                    for (VoxelShape voxelShape : stepUpColliders) {
                        for (double collisionPoint : voxelShape.getCoords(gravityDirection.getAxis()).reversed()) {
                            float verticalDist = -(float) (collisionPoint - relativeBottom);
                            if (!(verticalDist < 0.0F) && verticalDist != distToGround) {
                                if (verticalDist > maxUpStep)
                                    break;
                                floatSet.add(verticalDist);
                            }
                        }
                    }
                }
                float[] candidateStepUpHeights = floatSet.toFloatArray();
                FloatArrays.unstableSort(candidateStepUpHeights);
                for (float candidateStepUpHeight : candidateStepUpHeights) {
                    Vec3 stepAttemptPlayer = new Vec3(playerMovement.x, candidateStepUpHeight, playerMovement.z);
                    Vec3 stepAttemptWorld = RotationUtil.vecPlayerToWorld(stepAttemptPlayer, gravityDirection);
                    Vec3 stepFromGround = com.example.api.physics.GravityMovement.calculateCollision(stepAttemptWorld, groundedAABB, stepUpColliders, self);
                    Vec3 stepFromGroundPlayer = RotationUtil.vecWorldToPlayer(stepFromGround, gravityDirection);
                    double stepDistSqr = stepFromGroundPlayer.x * stepFromGroundPlayer.x + stepFromGroundPlayer.z * stepFromGroundPlayer.z;
                    double currentDistSqr = playerMovementStep.x * playerMovementStep.x + playerMovementStep.z * playerMovementStep.z;
                    if (stepDistSqr > currentDistSqr) {
                        double distanceToGround = getRelativeBottom(aabb, gravityDirection) - getRelativeBottom(groundedAABB, gravityDirection);
                        Vec3 offset = RotationUtil.vecPlayerToWorld(0.0, distanceToGround, 0.0, gravityDirection);
                        return stepFromGround.subtract(offset);
                    }
                }
            }
        }
        return movementStep;
    }

}
