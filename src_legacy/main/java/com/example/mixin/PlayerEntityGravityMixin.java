package com.example.mixin;

import com.example.api.GravityChanger;
import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(value = Player.class, priority = 1001)
public abstract class PlayerEntityGravityMixin extends LivingEntity {
    protected PlayerEntityGravityMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    // ===== Fall Damage Immunity (Player overrides) =====

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void gravity$causeFallDamage(double fallDistance, float damageMultiplier,
            net.minecraft.world.damagesource.DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if (((GravityChanger) this).hasGravityImmunity()) {
            cir.setReturnValue(false);
        }
    }

    // ===== travel() getRotationVector wrap =====

    @WrapOperation(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 gravity$travel_rotationVector(Player playerEntity, Operation<Vec3> original) {
        Direction gravityDirection = GravityHelper.getGravityDirection(playerEntity);
        if (gravityDirection == Direction.DOWN) {
            return original.call(playerEntity);
        }
        return RotationUtil.vecWorldToPlayer(original.call(playerEntity), gravityDirection);
    }

    // ===== travel() BlockPos.ofFloored gravity fix =====

    @ModifyArgs(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;containing(DDD)Lnet/minecraft/core/BlockPos;"))
    private void gravity$travel_blockPos(Args args) {
        Vec3 rotate = new Vec3(0.0D, 1.0D - 0.1D, 0.0D);
        rotate = RotationUtil.vecPlayerToWorld(rotate, GravityHelper.getGravityDirection(this));
        args.set(0, (double) args.get(0) - rotate.x);
        args.set(1, (double) args.get(1) - rotate.y + (1.0D - 0.1D));
        args.set(2, (double) args.get(2) - rotate.z);
    }

    // ===== adjustMovementForSneaking =====

    @ModifyVariable(method = "maybeBackOffFromEdge", at = @At(value = "HEAD"), argsOnly = true)
    private Vec3 gravity$sneaking_toLocal(Vec3 movement) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        return RotationUtil.vecWorldToPlayer(movement, gravityDirection);
    }

    @Inject(method = "maybeBackOffFromEdge", at = @At(value = "RETURN", ordinal = 1), cancellable = true)
    private void gravity$sneaking_returnIf(CallbackInfoReturnable<Vec3> cir,
            @Local(argsOnly = true) Vec3 movement,
            @Local(ordinal = 0) double d,
            @Local(ordinal = 1) double e) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        cir.setReturnValue(RotationUtil.vecPlayerToWorld(d, movement.y, e, gravityDirection));
    }

    @Inject(method = "maybeBackOffFromEdge", at = @At(value = "RETURN", ordinal = 0), cancellable = true)
    private void gravity$sneaking_returnElse(CallbackInfoReturnable<Vec3> cir,
            @Local(argsOnly = true) Vec3 movement) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        cir.setReturnValue(RotationUtil.vecPlayerToWorld(movement, gravityDirection));
    }

    // ===== isSpaceAroundPlayerEmpty =====
    // 1.21.11 signature: isSpaceAroundPlayerEmpty(double offsetX, double offsetZ,
    // double stepHeight)
    // Creates AABB checking below the player's bounding box. For non-DOWN gravity,
    // we need to check in the gravity direction instead.

    @Inject(method = "canFallAtLeast", at = @At("HEAD"), cancellable = true)
    private void gravity$isSpaceAroundPlayerEmpty(double offsetX, double offsetZ, double stepHeight,
            CallbackInfoReturnable<Boolean> cir) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return;

        double margin = 1.0E-7;
        AABB box = this.getBoundingBox();

        // Transform offsets: vanilla applies offsetX to X, offsetZ to Z, and
        // -stepHeight to Y (below feet)
        // In gravity-rotated space, these need to go in the correct directions
        Vec3 worldOffsets = RotationUtil.vecPlayerToWorld(offsetX, 0.0, offsetZ, gravityDirection);
        Vec3 downDir = RotationUtil.vecPlayerToWorld(0.0, -stepHeight - margin, 0.0, gravityDirection);

        // Build the check box: start from bounding box, apply horizontal offset, then
        // extend in gravity "down" direction
        AABB checkBox = new AABB(
                box.minX + margin + worldOffsets.x + Math.min(0, downDir.x),
                box.minY + margin + worldOffsets.y + Math.min(0, downDir.y),
                box.minZ + margin + worldOffsets.z + Math.min(0, downDir.z),
                box.maxX - margin + worldOffsets.x + Math.max(0, downDir.x),
                box.maxY - margin + worldOffsets.y + Math.max(0, downDir.y),
                box.maxZ - margin + worldOffsets.z + Math.max(0, downDir.z));

        cir.setReturnValue(this.level().noCollision((Entity) (Object) this, checkBox));
    }



    // ===== doSweepingAttack() — gravity-aware sweep direction =====

    @WrapOperation(method = "doSweepAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"))
    private float gravity$doSweepingAttack_getYaw(Player self, Operation<Float> original) {
        Direction gravityDirection = GravityHelper.getGravityDirection(self);
        if (gravityDirection == Direction.DOWN) {
            return original.call(self);
        }
        return RotationUtil.rotPlayerToWorld(original.call(self), self.getXRot(), gravityDirection).x;
    }

    // ===== tickMovement expand =====

    @ModifyArgs(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;inflate(DDD)Lnet/minecraft/world/phys/AABB;"))
    private void gravity$tickMovement_expand(Args args) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN)
            return;

        Vec3 vec3d = RotationUtil.maskPlayerToWorld((double) args.get(0), (double) args.get(1), (double) args.get(2),
                gravityDirection);
        args.set(0, vec3d.x);
        args.set(1, vec3d.y);
        args.set(2, vec3d.z);
    }

    // ===== canChangeIntoPose =====

    @WrapOperation(method = "canPlayerFitWithinBlocksAndEntitiesWhen", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityDimensions;makeBoundingBox(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/AABB;"))
    private AABB gravity$canChangeIntoPose(EntityDimensions dimensions, Vec3 pos, Operation<AABB> original) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN) {
            return original.call(dimensions, pos);
        }

        AABB box = dimensions.makeBoundingBox(0, 0, 0);
        if (gravityDirection.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            box = box.move(0.0D, -1.0E-6D, 0.0D);
        }
        return RotationUtil.boxPlayerToWorld(box, gravityDirection).move(pos);
    }
}
