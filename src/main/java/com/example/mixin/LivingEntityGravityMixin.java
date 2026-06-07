package com.example.mixin;

import com.example.api.GravityChanger;
import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;
import net.minecraft.core.*;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, priority = 1100)
public abstract class LivingEntityGravityMixin extends Entity {
    @Shadow
    public abstract EntityDimensions getDimensions(Pose pose);



    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "wouldNotSuffocateAtTargetPose", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityDimensions;makeBoundingBox(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/AABB;"))
    private AABB gravity$wouldNotSuffocateAtTargetPose(EntityDimensions dimensions, net.minecraft.world.phys.Vec3 pos, com.llamalad7.mixinextras.injector.wrapoperation.Operation<AABB> original) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN) {
            return original.call(dimensions, pos);
        }

        return GravityHelper.buildGravityAwareBox(original.call(dimensions, pos), pos, dimensions, gravityDirection);
    }

    @Shadow
    public abstract float getViewYRot(float tickDelta);

    public LivingEntityGravityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }





    @Inject(method = "calculateFallDamage", at = @At("HEAD"), cancellable = true)
    private void gravity$computeFallDamage(double fallDistance, float damageMultiplier,
            CallbackInfoReturnable<Integer> cir) {
        if (((GravityChanger) this).hasGravityImmunity()) {
            cir.setReturnValue(0);
        }
    }



    @Inject(method = "jumpOutOfFluid", at = @At("HEAD"), cancellable = true)
    private void gravity$jumpOutOfFluid(double oldWorldY, CallbackInfo ci) {
        if (com.example.api.physics.GravityFluid.handleJumpOutOfFluid((LivingEntity) (Object) this, oldWorldY)) {
            ci.cancel();
        }
    }

    @Inject(method = "getFluidFallingAdjustedMovement", at = @At("HEAD"), cancellable = true)
    private void gravity$getFluidFallingAdjustedMovement(double baseGravity, boolean isFalling, Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        Vec3 newMovement = com.example.api.physics.GravityFluid.handleFluidFallingAdjustedMovement((LivingEntity) (Object) this, baseGravity, isFalling, movement);
        if (newMovement != null) {
            cir.setReturnValue(newMovement);
        }
    }





    @ModifyArg(method = "playBlockFallSound", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"), index = 0)
    private BlockPos gravity$playBlockFallSound(BlockPos blockPos) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN) {
            return blockPos;
        }
        return BlockPos.containing(
                this.position().add(RotationUtil.vecPlayerToWorld(0, -0.20000000298023224D, 0, gravityDirection)));
    }



    @Redirect(method = "hasLineOfSight(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/ClipContext$Block;Lnet/minecraft/world/level/ClipContext$Fluid;D)Z", at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/world/phys/Vec3;", ordinal = 0))
    private Vec3 gravity$canSee_self(double x, double y, double z) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN) {
            return new Vec3(x, y, z);
        }
        return this.getEyePosition();
    }

    @Redirect(method = "hasLineOfSight(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/ClipContext$Block;Lnet/minecraft/world/level/ClipContext$Fluid;D)Z", at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/world/phys/Vec3;", ordinal = 1))
    private Vec3 gravity$canSee_target(double x, double y, double z, Entity entity) {
        Direction gravityDirection = GravityHelper.getGravityDirection(entity);
        if (gravityDirection == Direction.DOWN) {
            return new Vec3(x, y, z);
        }
        return entity.getEyePosition();
    }





    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D", ordinal = 0))
    private double gravity$tick_getX(LivingEntity livingEntity, Operation<Double> original) {
        Direction gravityDirection = GravityHelper.getGravityDirection(livingEntity);
        if (gravityDirection == Direction.DOWN) {
            return original.call(livingEntity);
        }
        return RotationUtil.vecWorldToPlayer(
                original.call(livingEntity) - livingEntity.xOld,
                livingEntity.getY() - livingEntity.yOld,
                livingEntity.getZ() - livingEntity.zOld,
                gravityDirection).x + livingEntity.xOld;
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D", ordinal = 0))
    private double gravity$tick_getZ(LivingEntity livingEntity, Operation<Double> original) {
        Direction gravityDirection = GravityHelper.getGravityDirection(livingEntity);
        if (gravityDirection == Direction.DOWN) {
            return original.call(livingEntity);
        }
        return RotationUtil.vecWorldToPlayer(
                livingEntity.getX() - livingEntity.xOld,
                livingEntity.getY() - livingEntity.yOld,
                original.call(livingEntity) - livingEntity.zOld,
                gravityDirection).z + livingEntity.zOld;
    }



    @Redirect(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;x()D", ordinal = 0))
    private double gravity$damage_sourceX(Vec3 damageSourcePosition) {
        return com.example.api.physics.GravityCombat.transformDamageSourceX((Entity) (Object) this, damageSourcePosition);
    }

    @Redirect(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;z()D", ordinal = 0))
    private double gravity$damage_sourceZ(Vec3 damageSourcePosition) {
        return com.example.api.physics.GravityCombat.transformDamageSourceZ((Entity) (Object) this, damageSourcePosition);
    }

    @Redirect(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D", ordinal = 0))
    private double gravity$damage_targetX(LivingEntity target) {
        return com.example.api.physics.GravityCombat.transformTargetX(target);
    }

    @Redirect(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D", ordinal = 0))
    private double gravity$damage_targetZ(LivingEntity target) {
        return com.example.api.physics.GravityCombat.transformTargetZ(target);
    }







    @Redirect(method = "baseTick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;containing(DDD)Lnet/minecraft/core/BlockPos;", ordinal = 0))
    private BlockPos gravity$baseTick_eyePos(double x, double y, double z) {
        return com.example.api.physics.GravityVision.calculateEyeBlockPos((Entity) (Object) this, x, y, z);
    }



    @WrapOperation(method = "spawnItemParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;", ordinal = 0))
    private Vec3 gravity$spawnItemParticles_add(Vec3 vec3d, double x, double y, double z, Operation<Vec3> original) {
        return com.example.api.physics.GravityVision.transformParticleSpawn_Add((Entity) (Object) this, vec3d, x, y, z, original);
    }

    @ModifyVariable(method = "spawnItemParticles", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/phys/Vec3;yRot(F)Lnet/minecraft/world/phys/Vec3;", ordinal = 0), ordinal = 0)
    private Vec3 gravity$spawnItemParticles_rotateY(Vec3 vec3d) {
        return com.example.api.physics.GravityVision.transformParticleSpawn_RotateY((Entity) (Object) this, vec3d);
    }







    @ModifyVariable(method = "updateWalkAnimation(F)V", at = @At("HEAD"), argsOnly = true)
    private float gravity$updateWalkAnimation(float originalMag) {
        Direction gravityDirection = GravityHelper.getGravityDirection(this);
        if (gravityDirection == Direction.DOWN) {
            return originalMag;
        }

        Vec3 playerPosDelta = RotationUtil.vecWorldToPlayer(
                this.getX() - this.xOld,
                this.getY() - this.yOld,
                this.getZ() - this.zOld,
                gravityDirection);


        float mag = (float) Mth.length(playerPosDelta.x, 0.0D, playerPosDelta.z);
        return mag;
    }

    @Redirect(
        method = "updateFallFlyingMovement",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getXRot()F"
        )
    )
    private float gravity$elytra_redirectXRot(LivingEntity instance) {
        Direction gravityDirection = GravityHelper.getGravityDirection(instance);
        if (gravityDirection == Direction.DOWN) {
            return instance.getXRot();
        }
        Vec3 lookAngle = instance.getLookAngle();
        Vec3 localLook = RotationUtil.vecWorldToPlayer(lookAngle, gravityDirection);
        double horizontalDistance = Math.sqrt(localLook.x * localLook.x + localLook.z * localLook.z);
        return (float) (-(Math.atan2(localLook.y, horizontalDistance) * 180.0 / Math.PI));
    }

    @Redirect(
        method = "updateFallFlyingMovement",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private Vec3 gravity$elytra_redirectLookAngle(LivingEntity instance) {
        Vec3 lookAngle = instance.getLookAngle();
        Direction gravityDirection = GravityHelper.getGravityDirection(instance);
        if (gravityDirection != Direction.DOWN) {
            return RotationUtil.vecWorldToPlayer(lookAngle, gravityDirection);
        }
        return lookAngle;
    }

}
