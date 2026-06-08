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
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
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



    public LivingEntityGravityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    // ===== Fall Damage Immunity (LivingEntity overrides) =====

    @Inject(method = "calculateFallDamage", at = @At("HEAD"), cancellable = true)
    private void gravity$computeFallDamage(double fallDistance, float damageMultiplier,
            CallbackInfoReturnable<Integer> cir) {
        if (((GravityChanger) this).hasGravityImmunity()) {
            cir.setReturnValue(0);
        }
    }

    // ===== travelMidAir() getY redirect =====
    // In 1.21.11, travel() was refactored into
    // travelMidAir/travelInFluid/travelGliding etc.
    // travelMidAir has: this.y > this.level().getBottomY()

    @Redirect(method = "travelInAir", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getY()D", ordinal = 0))
    private double gravity$travelMidAir_getY(LivingEntity livingEntity) {
        Direction gravityDirection = GravityHelper.getGravityDirection(livingEntity);
        if (gravityDirection == Direction.DOWN) {
            return livingEntity.getY();
        }
        return RotationUtil.vecWorldToPlayer(livingEntity.position(), gravityDirection).y;
    }

    // ===== travelInFluid() getY redirect =====
    // travelInFluid has: double $$2 = this.y; (passed as y to
    // travelInWater/travelInLava)

    @Redirect(method = "travelInFluid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getY()D", ordinal = 0))
    private double gravity$travelInFluid_getY(LivingEntity livingEntity) {
        Direction gravityDirection = GravityHelper.getGravityDirection(livingEntity);
        if (gravityDirection == Direction.DOWN) {
            return livingEntity.getY();
        }
        return RotationUtil.vecWorldToPlayer(livingEntity.position(), gravityDirection).y;
    }

    // ===== resetVerticalVelocityInFluid() getY redirect =====
    // resetVerticalVelocityInFluid has: doesNotCollide($$1.x, $$1.y + 0.6 -
    // this.y + y, $$1.z)

    @Redirect(method = "jumpOutOfFluid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getY()D", ordinal = 0))
    private double gravity$resetVerticalVelocityInFluid_getY(LivingEntity livingEntity) {
        Direction gravityDirection = GravityHelper.getGravityDirection(livingEntity);
        if (gravityDirection == Direction.DOWN) {
            return livingEntity.getY();
        }
        return RotationUtil.vecWorldToPlayer(livingEntity.position(), gravityDirection).y;
    }

    // ===== calcGlidingVelocity() getRotationVector transform =====
    // In 1.21.11, getRotationVector() moved from travel() to calcGlidingVelocity()

    @ModifyVariable(method = "updateFallFlyingMovement", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;", ordinal = 0), ordinal = 1)
    private Vec3 gravity$calcGliding_rotationVec(Vec3 vec3d) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN) {
            return vec3d;
        }
        return RotationUtil.vecWorldToPlayer(vec3d, gravityDirection);
    }

    // ===== playBlockFallSound =====

    @ModifyArg(method = "playBlockFallSound", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"), index = 0)
    private BlockPos gravity$playBlockFallSound(BlockPos blockPos) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN) {
            return blockPos;
        }
        return BlockPos.containing(
                ((net.minecraft.world.entity.LivingEntity)(Object)this).position().add(RotationUtil.vecPlayerToWorld(0, -0.20000000298023224D, 0, gravityDirection)));
    }

    // ===== canSee =====
    // In 1.21.11, canSee(Entity) delegates to canSee(Entity, ShapeType,
    // FluidHandling, double)
    // which has the Vec3 constructions. Redirect both there.

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

    // ===== getBoundingBox(Pose) =====

    @Inject(method = "getLocalBoundsForPose(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/phys/AABB;", at = @At("RETURN"), cancellable = true)
    private void gravity$getBoundingBox(Pose pose, CallbackInfoReturnable<AABB> cir) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return;

        AABB box = cir.getReturnValue();
        if (gravityDirection.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            box = box.move(0.0D, -1.0E-6D, 0.0D);
        }
        cir.setReturnValue(RotationUtil.boxPlayerToWorld(box, gravityDirection));
    }

    // ===== tick() limb animation =====

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

    // ===== damage() position transforms =====
    // In 1.21.11, damage() uses: source.getPosition().x - this.x for
    // knockback direction
    // We need to transform these for gravity-aware knockback

    @Redirect(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;x()D", ordinal = 0))
    private double gravity$damage_sourceX(Vec3 damageSourcePosition) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN) {
            return damageSourcePosition.x;
        }
        return RotationUtil.vecWorldToPlayer(damageSourcePosition, gravityDirection).x;
    }

    @Redirect(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;z()D", ordinal = 0))
    private double gravity$damage_sourceZ(Vec3 damageSourcePosition) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN) {
            return damageSourcePosition.z;
        }
        return RotationUtil.vecWorldToPlayer(damageSourcePosition, gravityDirection).z;
    }

    @Redirect(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D", ordinal = 0))
    private double gravity$damage_targetX(LivingEntity target) {
        Direction gravityDirection = GravityHelper.getGravityDirection(target);
        if (gravityDirection == Direction.DOWN) {
            return target.getX();
        }
        return RotationUtil.vecWorldToPlayer(target.position(), gravityDirection).x;
    }

    @Redirect(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D", ordinal = 0))
    private double gravity$damage_targetZ(LivingEntity target) {
        Direction gravityDirection = GravityHelper.getGravityDirection(target);
        if (gravityDirection == Direction.DOWN) {
            return target.getZ();
        }
        return RotationUtil.vecWorldToPlayer(target.position(), gravityDirection).z;
    }

    // ===== causeExtraKnockback() — gravity-aware yaw for knockback direction =====
    // causeExtraKnockback uses getYRot() for knockback direction.

    @WrapOperation(method = "causeExtraKnockback", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"))
    private float gravity$knockbackTarget_getYaw(LivingEntity self, Operation<Float> original, Entity target, float knockback, Vec3 oldMovement) {
        Direction gravityDirection = GravityHelper.getGravityDirection(target);
        if (gravityDirection == Direction.DOWN) {
            return original.call(self);
        }
        return RotationUtil.rotWorldToPlayer(original.call(self), self.getXRot(), gravityDirection).x;
    }

    // ===== baseTick eye position =====

    @Redirect(method = "baseTick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;containing(DDD)Lnet/minecraft/core/BlockPos;", ordinal = 0))
    private BlockPos gravity$baseTick_eyePos(double x, double y, double z) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN) {
            return BlockPos.containing(x, y, z);
        }
        return BlockPos.containing(this.getEyePosition());
    }

    // ===== spawnItemParticles =====

    @WrapOperation(method = "spawnItemParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;", ordinal = 0))
    private Vec3 gravity$spawnItemParticles_add(Vec3 vec3d, double x, double y, double z, Operation<Vec3> original) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN) {
            return original.call(vec3d, x, y, z);
        }
        Vec3 rotated = RotationUtil.vecPlayerToWorld(vec3d, gravityDirection);
        return original.call(this.getEyePosition(), rotated.x, rotated.y, rotated.z);
    }

    @ModifyVariable(method = "spawnItemParticles(Lnet/minecraft/world/item/ItemStack;I)V", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/phys/Vec3;yRot(F)Lnet/minecraft/world/phys/Vec3;", ordinal = 0), ordinal = 0)
    private Vec3 gravity$spawnItemParticles_rotateY(Vec3 vec3d) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN) {
            return vec3d;
        }
        return RotationUtil.vecPlayerToWorld(vec3d, gravityDirection);
    }

    // ===== tickStatusEffects particle position =====
    // In 1.21.11, tickStatusEffects uses addParticleClient with
    // getParticleX/getRandomBodyY/getParticleZ
    // These helper methods account for entity dimensions but NOT gravity.
    // TODO: Add gravity-aware particle position for status effects

    // ===== addDeathParticles =====
    // In 1.21.11, addDeathParticles uses addParticleClient with
    // getParticleX/getRandomBodyY/getParticleZ
    // TODO: Add gravity-aware death particle position

    // ===== updateLimbs gravity-aware =====

    @Shadow
    public abstract void calculateEntityAnimation(boolean flutter);

    @Shadow
    protected abstract void updateWalkAnimation(float limbDistance);

    @Inject(method = "calculateEntityAnimation(Z)V", at = @At("HEAD"), cancellable = true)
    private void gravity$updateLimbs(boolean flutter, CallbackInfo ci) {
        Direction gravityDirection = GravityHelper.getGravityDirection(this);
        if (gravityDirection == Direction.DOWN)
            return;

        ci.cancel();

        Vec3 playerPosDelta = RotationUtil.vecWorldToPlayer(
                this.getX() - this.xOld,
                this.getY() - this.yOld,
                this.getZ() - this.zOld,
                gravityDirection);

        float mag = (float) Mth.length(playerPosDelta.x, flutter ? playerPosDelta.y : 0.0D, playerPosDelta.z);
        this.updateWalkAnimation(mag);
    }

    // ===== createItemStackToDrop position + velocity fix =====
    // We fix the position and velocity here.

    @Redirect(method = "createItemStackToDrop", at = @At(value = "NEW", target = "(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;", ordinal = 0))
    private ItemEntity gravity$createItemEntity_newEntity(
            Level world, double x, double y, double z, net.minecraft.world.item.ItemStack stack) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN) {
            return new ItemEntity(world, x, y, z, stack);
        }

        Vec3 vec3d = this.getEyePosition()
                .subtract(RotationUtil.vecPlayerToWorld(0.0D, 0.3D, 0.0D, gravityDirection));

        return new ItemEntity(world, vec3d.x, vec3d.y, vec3d.z, stack);
    }

    @WrapOperation(method = "createItemStackToDrop", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;setDeltaMovement(DDD)V"))
    private void gravity$createItemEntity_setVelocity(ItemEntity itemEntity, double x, double y, double z,
            Operation<Void> original) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN) {
            original.call(itemEntity, x, y, z);
            return;
        }

        Vec3 worldVel = RotationUtil.vecPlayerToWorld(x, y, z, gravityDirection);
        itemEntity.setDeltaMovement(worldVel.x, worldVel.y, worldVel.z);
    }

}
