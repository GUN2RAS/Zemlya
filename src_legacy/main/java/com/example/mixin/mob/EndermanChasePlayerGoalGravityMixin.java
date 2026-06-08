package com.example.mixin.mob;

import com.example.util.GravityHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.entity.monster.EnderMan$ChasePlayerGoal")
public abstract class EndermanChasePlayerGoalGravityMixin {

    @Redirect(method = "tick()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getEyeY()D", ordinal = 0))
    private double gravity$tick_getEyeY(LivingEntity entity) {
        Direction gd = GravityHelper.getGravityDirection(entity);
        if (gd == Direction.DOWN) return entity.getEyeY();
        return entity.getEyePosition().y;
    }

    @Redirect(method = "tick()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D", ordinal = 0))
    private double gravity$tick_getX(LivingEntity entity) {
        Direction gd = GravityHelper.getGravityDirection(entity);
        if (gd == Direction.DOWN) return entity.getX();
        return entity.getEyePosition().x;
    }

    @Redirect(method = "tick()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D", ordinal = 0))
    private double gravity$tick_getZ(LivingEntity entity) {
        Direction gd = GravityHelper.getGravityDirection(entity);
        if (gd == Direction.DOWN) return entity.getZ();
        return entity.getEyePosition().z;
    }
}
