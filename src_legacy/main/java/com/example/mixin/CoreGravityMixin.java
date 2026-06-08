package com.example.mixin;

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

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
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

    // ===== SynchedEntityData Gravity Storage =====

    @Unique
    private static final EntityDataAccessor<Integer> GRAVITY_DIRECTION = SynchedEntityData.defineId(Entity.class,
            net.minecraft.network.syncher.EntityDataSerializers.INT);

    @Shadow
    public abstract SynchedEntityData getEntityData();

    @WrapOperation(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;defineSynchedData(Lnet/minecraft/network/syncher/SynchedEntityData$Builder;)V"))
    private void gravity$defineSynchedData(Entity self, SynchedEntityData.Builder builder, Operation<Void> original) {
        original.call(self, builder);
        builder.define(GRAVITY_DIRECTION, 0);
    }

    @Unique
    private Gravity prevGravityDirection = Gravity.DOWN;
    @Unique
    private int gravityTransitionTimer = 0;
    @Unique
    private int gravityImmunityTimer = 0;
    @Unique
    private static final int MAX_TRANSITION_TICKS = 20;

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
        float progress = 1.0f - ((float) this.gravityTransitionTimer - tickDelta) / (float) MAX_TRANSITION_TICKS;
        progress = Mth.clamp(progress, 0.0f, 1.0f);
        // Sine easing in-out for butter smooth rotation
        return (float) (1.0 - Math.cos(progress * Math.PI)) / 2.0f;
    }

    public boolean hasGravityImmunity() {
        return this.gravityImmunityTimer > 0;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void gravity$tick(CallbackInfo ci) {
        if (this.gravityTransitionTimer > 0) {
            this.gravityTransitionTimer--;
            if (this.gravityTransitionTimer <= 0) {
                // Animation finished, synchronize prev to current to avoid jumps on next
                // rotation
                this.prevGravityDirection = getGravityDirection();
            }
        }
        if (this.gravityImmunityTimer > 0) {
            this.gravityImmunityTimer--;
            this.fallDistance = 0.0f; // Force zero fall distance while immune
        }
    }

    @Override
    public void setGravity(Gravity gravity) {
        Gravity current = getGravityDirection();
        if (current != gravity) {
            Entity entity = (Entity) (Object) this;
            net.minecraft.world.phys.AABB oldBox = entity.getBoundingBox();

            this.prevGravityDirection = current;
            this.gravityTransitionTimer = MAX_TRANSITION_TICKS;
            this.gravityImmunityTimer = 40; // 2 seconds
            this.fallDistance = 0.0f; // reset fall distance on change
            getEntityData().set(GRAVITY_DIRECTION, gravity.ordinal());

            net.minecraft.world.phys.Vec3 center = oldBox.getCenter();
            float height = entity.getBbHeight();

            double newX = center.x;
            double newY = center.y;
            double newZ = center.z;

            switch (gravity.getDirection()) {
                case UP -> newY = center.y + height / 2.0;
                case DOWN -> newY = center.y - height / 2.0;
                case NORTH -> newZ = center.z - height / 2.0;
                case SOUTH -> newZ = center.z + height / 2.0;
                case WEST -> newX = center.x - height / 2.0;
                case EAST -> newX = center.x + height / 2.0;
            }

            entity.setPos(newX, newY, newZ);
            onGravityChanged();
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
            // This is called on the client when the server updates the gravity direction
            // via SynchedEntityData
            Entity self = (Entity) (Object) this;
            if (self.level().isClientSide()) {
                // Determine the newly synced gravity value
                int newIdx = getEntityData().get(GRAVITY_DIRECTION);
                Gravity[] values = Gravity.values();
                Gravity newGravity = (newIdx >= 0 && newIdx < values.length) ? values[newIdx] : Gravity.DOWN;

                // If it differs from the previously known 'prevGravityDirection', trigger
                // animation
                if (this.prevGravityDirection != newGravity && this.gravityTransitionTimer <= 0) {
                    // We only want to set this if we are not ALREADY transitioning to the same
                    // gravity.
                    // Actually, if we just received a new direction, we set the timer.
                    // The "old" state for the animation is whatever was on the screen, which is
                    // prevGravityDirection.
                    // Wait, no. If we were fully transitioned, prevGravityDirection is the OLD
                    // gravity.
                    // Actually, we should store CURRENT gravity before it changes. But
                    // onSyncedDataUpdated is called AFTER change.
                    // How to know the old gravity here?
                    // Let's use getGravityDirection() vs prevGravityDirection logic better:
                    // In tick(), if transition timer is 0, prevGravityDirection =
                    // currentGravityDirection?
                    // Let's just do:
                    this.gravityTransitionTimer = MAX_TRANSITION_TICKS;
                    this.gravityImmunityTimer = 40;
                    this.fallDistance = 0.0f;
                    // prevGravityDirection remains what it was (the OLD direction),
                    // newGravity is now the CURRENT direction.
                    // But wait, what if we change gravity AGAIN while animating?
                    // To be safe, if transition timer == 0, the OLD direction is the LAST known
                    // target gravity.
                    // Since we're here, newGravity is the NEW target.
                    // Let's ensure prevGravityDirection captures the visual state just before this
                    // update.
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
    }

    @WrapOperation(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V"))
    private void gravity$readCustomData(Entity self, net.minecraft.world.level.storage.ValueInput view, Operation<Void> original) {
        original.call(self, view);
        int idx = view.getIntOr("GravityDirection", 0);
        Gravity[] values = Gravity.values();
        if (idx >= 0 && idx < values.length) {
            getEntityData().set(GRAVITY_DIRECTION, idx);
        }
        this.prevGravityDirection = Gravity.values()[view.getIntOr("PrevGravityDirection", 0)];
        this.gravityTransitionTimer = view.getIntOr("GravityTransitionTimer", 0);
        this.gravityImmunityTimer = view.getIntOr("GravityImmunityTimer", 0);
    }

    // ===== Shadows =====

    @Shadow
    private Vec3 position;
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
    public abstract void push(double x, double y, double z);

    @Shadow
    protected abstract void onBelowWorld();

    @Shadow
    public abstract double getEyeY();

    @Shadow
    public abstract float getYRot(float tickDelta);

    @Shadow
    public abstract float getYRot();

    @Shadow
    public abstract float getXRot();

    @Shadow
    @Final
    protected RandomSource random;
    @Shadow
    public double fallDistance;

    @Shadow
    private static Vec3 collideBoundingBox(Entity entity, Vec3 movement, AABB entityBoundingBox, Level level, 
            List<VoxelShape> collisions) {
        return null;
    }

    // ===== Fall Damage Immunity =====

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void gravity$handleFallDamage(double fallDistance, float damageMultiplier,
            net.minecraft.world.damagesource.DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if (this.hasGravityImmunity()) {
            cir.setReturnValue(false); // Cancel fall damage if immune
        }
    }

    // ===== calculateBoundingBox =====

    @Inject(method = "makeBoundingBox()Lnet/minecraft/world/phys/AABB;", at = @At("RETURN"), cancellable = true)
    private void gravity$calculateBoundingBox(CallbackInfoReturnable<AABB> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Projectile)
            return;

        Direction gravityDirection = GravityHelper.getGravityDirection(self);
        if (gravityDirection == Direction.DOWN)
            return;

        AABB box = cir.getReturnValue().move(this.position.reverse());
        if (gravityDirection.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            box = box.move(0.0D, -1.0E-6D, 0.0D);
        }
        cir.setReturnValue(RotationUtil.boxPlayerToWorld(box, gravityDirection).move(this.position));
    }

    // ===== getRotationVector =====

    @Inject(method = "calculateViewVector(FF)Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"), cancellable = true)
    private void gravity$getRotationVector(CallbackInfoReturnable<Vec3> cir) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return;

        cir.setReturnValue(RotationUtil.vecPlayerToWorld(cir.getReturnValue(), gravityDirection));
    }

    // ===== getVelocityAffectingPos =====

    @Inject(method = "getBlockPosBelowThatAffectsMyMovement()Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
    private void gravity$getVelocityAffectingPos(CallbackInfoReturnable<BlockPos> cir) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return;

        cir.setReturnValue(
                BlockPos.containing(this.position.add(gravityDirection.getUnitVec3().scale(0.5000001D))));
    }

    // ===== getEyePos =====

    @Inject(method = "getEyePosition()Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void gravity$getEyePos(CallbackInfoReturnable<Vec3> cir) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return;

        cir.setReturnValue(
                RotationUtil.vecPlayerToWorld(0.0D, this.eyeHeight, 0.0D, gravityDirection).add(this.position));
    }

    // ===== getCameraPosVec =====

    @Inject(method = "getLightProbePosition(F)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void gravity$getCameraPosVec(float tickDelta, CallbackInfoReturnable<Vec3> cir) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return;

        Vec3 vec3d = RotationUtil.vecPlayerToWorld(0.0D, this.eyeHeight, 0.0D, gravityDirection);
        double d = Mth.lerp((double) tickDelta, this.xOld, this.getX()) + vec3d.x;
        double e = Mth.lerp((double) tickDelta, this.yOld, ((net.minecraft.world.entity.Entity)(Object)this).getY()) + vec3d.y;
        double f = Mth.lerp((double) tickDelta, this.zOld, this.getZ()) + vec3d.z;
        cir.setReturnValue(new Vec3(d, e, f));
    }

    // ===== getBrightnessAtEyes =====

    @Inject(method = "getLightLevelDependentMagicValue()F", at = @At("HEAD"), cancellable = true)
    private void gravity$getBrightnessAtEyes(CallbackInfoReturnable<Float> cir) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return;

        cir.setReturnValue(((net.minecraft.world.entity.Entity)(Object)this).level().hasChunk(((net.minecraft.world.entity.Entity)(Object)this).getBlockX() >> 4, ((net.minecraft.world.entity.Entity)(Object)this).getBlockZ() >> 4)
                ? ((net.minecraft.world.entity.Entity)(Object)this).level().getMaxLocalRawBrightness(BlockPos.containing(((net.minecraft.world.entity.Entity)(Object)this).getEyePosition()))
                : 0.0F);
    }

    // ===== move() - transform velocity from local to world =====

    @ModifyVariable(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private Vec3 gravity$move_toWorld(Vec3 vec3d) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return vec3d;

        return RotationUtil.vecPlayerToWorld(vec3d, gravityDirection);
    }

    // transform argument back to local after collision resolve
    @ModifyVariable(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V", ordinal = 0), ordinal = 0, argsOnly = true)
    private Vec3 gravity$move_toLocal_arg(Vec3 vec3d) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return vec3d;

        return RotationUtil.vecWorldToPlayer(vec3d, gravityDirection);
    }

    // transform collision result to local
    @ModifyVariable(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V", ordinal = 0), ordinal = 1)
    private Vec3 gravity$move_toLocal_result(Vec3 vec3d) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return vec3d;

        return RotationUtil.vecWorldToPlayer(vec3d, gravityDirection);
    }

    // ===== movementInputToVelocity() - transform local input to world velocity
    // =====

    @Inject(method = "getInputVector", at = @At("RETURN"), cancellable = true)
    private static void gravity$movementInputToVelocity(Vec3 movementInput, float speed, float yaw,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Vec3> cir) {
        // We cannot directly get 'this' since it's a static method in Entity, but
        // unfortunately
        // it doesn't take the Entity as a parameter. Wait, we CAN'T use
        // GravityHelper.getGravityDirection(this)
        // in a static method! Let's check how Entity implemented
        // movementInputToVelocity in 1.21.11.
    }

    // ===== getLandingPos =====

    @Inject(method = "getOnPos()Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
    private void gravity$getLandingPos(CallbackInfoReturnable<BlockPos> cir) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return;

        cir.setReturnValue(BlockPos.containing(
                RotationUtil.vecPlayerToWorld(0.0D, -0.20000000298023224D, 0.0D, gravityDirection).add(this.position)));
    }

    // ===== adjustMovementForCollisions(Vec3) - instance method =====

    @ModifyVariable(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/level/Level;getEntityCollisions(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;", ordinal = 0), ordinal = 0, argsOnly = true)
    private Vec3 gravity$adjustMovement_toLocal(Vec3 vec3d) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return vec3d;

        return RotationUtil.vecWorldToPlayer(vec3d, gravityDirection);
    }

    @Inject(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"), cancellable = true)
    private void gravity$adjustMovement_toWorld(CallbackInfoReturnable<Vec3> cir) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return;

        cir.setReturnValue(RotationUtil.vecPlayerToWorld(cir.getReturnValue(), gravityDirection));
    }

    @ModifyArg(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;expandTowards(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/AABB;"), index = 0)
    private Vec3 gravity$adjustMovement_expandTowards(Vec3 vec) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN) return vec;
        return RotationUtil.vecPlayerToWorld(vec, gravityDirection);
    }

    @ModifyArgs(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;move(DDD)Lnet/minecraft/world/phys/AABB;"))
    private void gravity$adjustMovement_offset(Args args) {
        Vec3 rotate = new Vec3(args.get(0), args.get(1), args.get(2));
        rotate = RotationUtil.vecPlayerToWorld(rotate, GravityHelper.getGravityDirection((Entity) (Object) this));
        args.set(0, rotate.x);
        args.set(1, rotate.y);
        args.set(2, rotate.z);
    }

    @ModifyArgs(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;collideBoundingBox(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/level/Level;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;"))
    private void gravity$adjustMovement_innerCall(Args args) {
        Vec3 rotate = args.get(1);
        rotate = RotationUtil.vecPlayerToWorld(rotate, GravityHelper.getGravityDirection((Entity) (Object) this));
        args.set(1, rotate);
    }

    // Redirect collectCandidateStepUpHeights for gravity-aware step
    @Redirect(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;collectCandidateStepUpHeights(Lnet/minecraft/world/phys/AABB;Ljava/util/List;FF)[F"))
    private float[] gravity$collectCandidateStepUpHeights(AABB boundingBox, List<VoxelShape> colliders, float maxStepHeight, float stepHeightToSkip) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN) {
            // Fall through to default (use original static method logic)
            FloatSet floatSet = new FloatArraySet(4);
            double relativeBottom = boundingBox.minY;
            for (VoxelShape voxelShape : colliders) {
                for (double coord : voxelShape.getCoords(Direction.Axis.Y)) {
                    float relativeCoord = (float) (coord - relativeBottom);
                    if (!(relativeCoord < 0.0F) && relativeCoord != stepHeightToSkip) {
                        if (relativeCoord > maxStepHeight) break;
                        floatSet.add(relativeCoord);
                    }
                }
            }
            float[] fs = floatSet.toFloatArray();
            FloatArrays.unstableSort(fs);
            return fs;
        }

        FloatSet floatSet = new FloatArraySet(4);
        double relativeBottom = getRelativeBottom(boundingBox, gravityDirection);

        if (gravityDirection.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
            for (VoxelShape voxelShape : colliders) {
                for (double collisionPoint : voxelShape.getCoords(gravityDirection.getAxis())) {
                    float verticalDist = (float) (collisionPoint - relativeBottom);
                    if (!(verticalDist < 0.0F) && verticalDist != stepHeightToSkip) {
                        if (verticalDist > maxStepHeight) break;
                        floatSet.add(verticalDist);
                    }
                }
            }
        } else {
            for (VoxelShape voxelShape : colliders) {
                for (double collisionPoint : voxelShape.getCoords(gravityDirection.getAxis()).reversed()) {
                    float verticalDist = -(float) (collisionPoint - relativeBottom);
                    if (!(verticalDist < 0.0F) && verticalDist != stepHeightToSkip) {
                        if (verticalDist > maxStepHeight) break;
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

    // ===== isInsideWall =====

    @ModifyArgs(method = "isInWall", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;ofSize(Lnet/minecraft/world/phys/Vec3;DDD)Lnet/minecraft/world/phys/AABB;", ordinal = 0))
    private void gravity$isInsideWall(Args args) {
        Vec3 rotate = new Vec3(args.get(1), args.get(2), args.get(3));
        rotate = RotationUtil.vecPlayerToWorld(rotate, GravityHelper.getGravityDirection((Entity) (Object) this));
        args.set(1, rotate.x);
        args.set(2, rotate.y);
        args.set(3, rotate.z);
    }

    // ===== getHorizontalFacing =====

    @ModifyArg(method = "getDirection", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Direction;fromYRot(D)Lnet/minecraft/core/Direction;"))
    private double gravity$getHorizontalFacing(double rotation) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return rotation;

        return RotationUtil.rotPlayerToWorld((float) rotation, ((net.minecraft.world.entity.Entity)(Object)this).getXRot(), gravityDirection).x;
    }

    // ===== spawnSprintingParticles =====

    @Inject(method = "spawnSprintParticle()V", at = @At("HEAD"), cancellable = true)
    private void gravity$spawnSprintingParticles(CallbackInfo ci) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return;

        ci.cancel();

        Vec3 floorPos = this.position()
                .subtract(RotationUtil.vecPlayerToWorld(0.0D, 0.20000000298023224D, 0.0D, gravityDirection));
        BlockPos blockPos = BlockPos.containing(floorPos);
        BlockState blockState = ((net.minecraft.world.entity.Entity)(Object)this).level().getBlockState(blockPos);
        if (blockState.getRenderShape() != RenderShape.INVISIBLE) {
            Vec3 particlePos = this.position().add(RotationUtil.vecPlayerToWorld(
                    (this.random.nextDouble() - 0.5D) * (double) this.dimensions.width(), 0.1D,
                    (this.random.nextDouble() - 0.5D) * (double) this.dimensions.width(), gravityDirection));
            Vec3 playerVelocity = ((net.minecraft.world.entity.Entity)(Object)this).getDeltaMovement();
            Vec3 particleVelocity = RotationUtil.vecPlayerToWorld(playerVelocity.x * -4.0D, 1.5D,
                    playerVelocity.z * -4.0D, gravityDirection);
            ((net.minecraft.world.entity.Entity)(Object)this).level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, blockState),
                    particlePos.x, particlePos.y, particlePos.z,
                    particleVelocity.x, particleVelocity.y, particleVelocity.z);
        }
    }

    // ===== updateInWaterStateAndDoFluidPushing =====

    //@ModifyVariable...
    private Vec3 gravity$updateInWaterStateAndDoFluidPushing_vel(Vec3 vec3d) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return vec3d;

        return RotationUtil.vecPlayerToWorld(vec3d, gravityDirection);
    }

    //@ModifyArg(method = "updateInWaterStateAndDoFluidPushing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;add(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", ordinal = 1), index = 0)
    private Vec3 gravity$updateInWaterStateAndDoFluidPushing_push(Vec3 vec3d) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return vec3d;

        return RotationUtil.vecWorldToPlayer(vec3d, gravityDirection);
    }

    // ===== pushAwayFrom =====

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void gravity$pushAwayFrom(Entity entity, CallbackInfo ci) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        Direction otherGravityDirection = GravityHelper.getGravityDirection(entity);

        if (gravityDirection == Direction.DOWN && otherGravityDirection == Direction.DOWN)
            return;

        ci.cancel();

        if (!this.isPassengerOfSameVehicle(entity)) {
            if (!entity.noPhysics && !((Entity)(Object)this).noPhysics) {
                Vec3 entityOffset = entity.getBoundingBox().getCenter().subtract(this.getBoundingBox().getCenter());

                // Push self
                {
                    Vec3 playerEntityOffset = RotationUtil.vecWorldToPlayer(entityOffset, gravityDirection);
                    double dx = playerEntityOffset.x;
                    double dz = playerEntityOffset.z;
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
                            ((net.minecraft.world.entity.Entity)(Object)this).addDeltaMovement(new net.minecraft.world.phys.Vec3(-dx, 0.0D, -dz));
                        }
                    }
                }

                // Push other
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
                            entity.addDeltaMovement(new net.minecraft.world.phys.Vec3(dx, 0.0D, dz));
                        }
                    }
                }
            }
        }
    }

    // ===== attemptTickInVoid =====

    @Inject(method = "checkBelowWorld()V", at = @At("HEAD"), cancellable = true)
    private void gravity$attemptTickInVoid(CallbackInfo ci) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);

        if (((net.minecraft.world.entity.Entity)(Object)this).getY() > (double) (((net.minecraft.world.entity.Entity)(Object)this).level().getMaxY() + 256) && gravityDirection == Direction.UP) {
            this.onBelowWorld();
            ci.cancel();
            return;
        }

        if (gravityDirection.getAxis() != Direction.Axis.Y && fallDistance > 1024) {
            this.onBelowWorld();
            ci.cancel();
        }
    }

    // ===== doesNotCollide =====

    @ModifyArgs(method = "isFree(DDD)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;move(DDD)Lnet/minecraft/world/phys/AABB;", ordinal = 0))
    private void gravity$doesNotCollide(Args args) {
        Vec3 rotate = new Vec3(args.get(0), args.get(1), args.get(2));
        rotate = RotationUtil.vecPlayerToWorld(rotate, GravityHelper.getGravityDirection((Entity) (Object) this));
        args.set(0, rotate.x);
        args.set(1, rotate.y);
        args.set(2, rotate.z);
    }

    // ===== updateSubmergedInWaterState =====

    //@ModifyVariable...
    private double gravity$submergedEyeFix(double d) {
        return ((net.minecraft.world.entity.Entity)(Object)this).getEyePosition().y;
    }
    

    //@ModifyVariable...
    private BlockPos gravity$submergedPosFix(BlockPos blockpos) {
        return BlockPos.containing(((net.minecraft.world.entity.Entity)(Object)this).getEyePosition());
    }
    

    // ===== applyGravity direction fix =====

    @Inject(method = "applyGravity", at = @At("HEAD"), cancellable = true)
    private void gravity$applyGravity(CallbackInfo ci) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return;

        Entity self = (Entity) (Object) this;
        double gravity = self.getGravity();
        if (gravity != 0.0) {
            Vec3 vel = self.getDeltaMovement();
            Vec3 gravityVec = RotationUtil.vecPlayerToWorld(0.0, -gravity, 0.0, gravityDirection);
            self.setDeltaMovement(vel.add(gravityVec));
        }
        ci.cancel();
    }
}
