package com.example.mixin.mob;

import com.example.util.GravityHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LookAtPlayerGoal.class)
public abstract class LookAtEntityGoalGravityMixin {

    @Redirect(method = "tick()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getEyeY()D", ordinal = 0))
    private double gravity$tick_getEyeY(Entity entity) {
        return com.example.api.physics.GravityVision.getTargetEyeY(entity);
    }

    @Redirect(method = "tick()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getX()D", ordinal = 0))
    private double gravity$tick_getX(Entity entity) {
        return com.example.api.physics.GravityVision.getTargetEyeX(entity);
    }

    @Redirect(method = "tick()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getZ()D", ordinal = 0))
    private double gravity$tick_getZ(Entity entity) {
        return com.example.api.physics.GravityVision.getTargetEyeZ(entity);
    }
}
