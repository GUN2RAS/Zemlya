package com.example.mixin.mob;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.RamTarget;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = RamTarget.class, priority = 1001)
public abstract class RamImpactTaskGravityMixin {

    @Shadow private Vec3 direction;

    @WrapOperation(
        method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/animal/goat/Goat;J)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V", ordinal = 0)
    )
    private void gravity$keepRunning_knockback(LivingEntity target, double strength, double x, double z, Operation<Void> original) {
        Direction gd = GravityHelper.getGravityDirection(target);
        if (gd == Direction.DOWN) {
            original.call(target, strength, x, z);
            return;
        }
        Vec3 dir = RotationUtil.vecWorldToPlayer(this.direction, gd);
        original.call(target, strength, dir.x, dir.z);
    }
}
