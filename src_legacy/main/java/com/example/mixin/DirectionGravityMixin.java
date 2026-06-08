package com.example.mixin;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Direction.class, priority = 1001)
public abstract class DirectionGravityMixin {

    @WrapOperation(
        method = "orderedByNearest",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F",
            ordinal = 0
        )
    )
    private static float gravity$facingOrder_yaw(Entity entity, float tickDelta, Operation<Float> original) {
        Direction gravityDirection = GravityHelper.getGravityDirection(entity);
        if (gravityDirection == Direction.DOWN) {
            return original.call(entity, tickDelta);
        }
        return RotationUtil.rotPlayerToWorld(original.call(entity, tickDelta), entity.getXRot(tickDelta), gravityDirection).x;
    }

    @WrapOperation(
        method = "orderedByNearest",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F",
            ordinal = 0
        )
    )
    private static float gravity$facingOrder_pitch(Entity entity, float tickDelta, Operation<Float> original) {
        Direction gravityDirection = GravityHelper.getGravityDirection(entity);
        if (gravityDirection == Direction.DOWN) {
            return original.call(entity, tickDelta);
        }
        return RotationUtil.rotPlayerToWorld(entity.getYRot(tickDelta), original.call(entity, tickDelta), gravityDirection).y;
    }

    @WrapOperation(
        method = "getFacingAxis",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F",
            ordinal = 0
        )
    )
    private static float gravity$lookAxis_yaw0(Entity entity, float tickDelta, Operation<Float> original) {
        Direction gravityDirection = GravityHelper.getGravityDirection(entity);
        if (gravityDirection == Direction.DOWN) {
            return original.call(entity, tickDelta);
        }
        return RotationUtil.rotPlayerToWorld(original.call(entity, tickDelta), entity.getXRot(tickDelta), gravityDirection).x;
    }

    @WrapOperation(
        method = "getFacingAxis",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F",
            ordinal = 1
        )
    )
    private static float gravity$lookAxis_yaw1(Entity entity, float tickDelta, Operation<Float> original) {
        Direction gravityDirection = GravityHelper.getGravityDirection(entity);
        if (gravityDirection == Direction.DOWN) {
            return original.call(entity, tickDelta);
        }
        return RotationUtil.rotPlayerToWorld(original.call(entity, tickDelta), entity.getXRot(tickDelta), gravityDirection).x;
    }

    @WrapOperation(
        method = "getFacingAxis",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F",
            ordinal = 0
        )
    )
    private static float gravity$lookAxis_pitch(Entity entity, float tickDelta, Operation<Float> original) {
        Direction gravityDirection = GravityHelper.getGravityDirection(entity);
        if (gravityDirection == Direction.DOWN) {
            return original.call(entity, tickDelta);
        }
        return RotationUtil.rotPlayerToWorld(entity.getYRot(tickDelta), original.call(entity, tickDelta), gravityDirection).y;
    }
}