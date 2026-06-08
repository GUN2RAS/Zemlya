package com.example.mixin;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BehaviorUtils.class)
public class LookTargetUtilGravityMixin {

    @WrapOperation(
        method = "throwItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;F)V",
        at = @At(value = "NEW", target = "(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;")
    )
    private static ItemEntity gravity$give_newItem(
        Level level, double posX, double posY, double posZ, ItemStack itemStack,
        Operation<ItemEntity> operation,
        @Local(argsOnly = true) float yOffset, @Local(argsOnly = true) LivingEntity entity
    ) {
        Direction gd = GravityHelper.getGravityDirection(entity);
        if (gd == Direction.DOWN) {
            return operation.call(level, posX, posY, posZ, itemStack);
        }
        // Compute eye offset in world space
        Vec3 eyeOffset = RotationUtil.vecPlayerToWorld(0.0D, entity.getEyeHeight(), 0.0D, gd);
        Vec3 offset = eyeOffset.normalize().scale(yOffset);
        Vec3 itemPos = entity.position().add(eyeOffset).subtract(offset);
        return operation.call(level, itemPos.x, itemPos.y, itemPos.z, itemStack);
    }

    @WrapOperation(
        method = "throwItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;F)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V")
    )
    private static void gravity$give_setVelocity(
        ItemEntity itemEntity, Vec3 deltaMovement, Operation<Void> operation,
        @Local(argsOnly = true) LivingEntity entity
    ) {
        Direction gd = GravityHelper.getGravityDirection(entity);
        if (gd == Direction.DOWN) {
            operation.call(itemEntity, deltaMovement);
            return;
        }
        // Transform velocity from player space to world space
        Vec3 worldVel = RotationUtil.vecPlayerToWorld(deltaMovement, gd);
        operation.call(itemEntity, worldVel);
    }
}
