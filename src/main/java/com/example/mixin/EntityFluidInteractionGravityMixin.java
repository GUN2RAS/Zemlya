package com.example.mixin;

import com.example.util.GravityHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityFluidInteraction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(EntityFluidInteraction.class)
public class EntityFluidInteractionGravityMixin {

    @WrapOperation(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getBlockX()I"))
    private int gravity$fluidInteraction_getBlockX(Entity entity, Operation<Integer> original) {
        Direction gravityDirection = GravityHelper.getGravityDirection(entity);
        if (gravityDirection == Direction.DOWN) {
            return original.call(entity);
        }
        return net.minecraft.util.Mth.floor(entity.getEyePosition().x);
    }

    @WrapOperation(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getEyeY()D"))
    private double gravity$fluidInteraction_getEyeY(Entity entity, Operation<Double> original) {
        Direction gravityDirection = GravityHelper.getGravityDirection(entity);
        if (gravityDirection == Direction.DOWN) {
            return original.call(entity);
        }
        return entity.getEyePosition().y;
    }

    @WrapOperation(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getBlockZ()I"))
    private int gravity$fluidInteraction_getBlockZ(Entity entity, Operation<Integer> original) {
        Direction gravityDirection = GravityHelper.getGravityDirection(entity);
        if (gravityDirection == Direction.DOWN) {
            return original.call(entity);
        }
        return net.minecraft.util.Mth.floor(entity.getEyePosition().z);
    }
}
