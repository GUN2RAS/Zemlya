package com.example.mixin.mob;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractSkeleton.class)
public abstract class AbstractSkeletonEntityGravityMixin {

    @Redirect(method = "performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D", ordinal = 0))
    private double gravity$shootAt_getX(LivingEntity target) {
        return com.example.api.physics.GravityCombat.calculateProjectileTargetX(target);
    }

    @Redirect(method = "performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getY(D)D", ordinal = 0))
    private double gravity$shootAt_getBodyY(LivingEntity target, double heightScale) {
        return com.example.api.physics.GravityCombat.calculateProjectileTargetY(target, heightScale);
    }

    @Redirect(method = "performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D", ordinal = 0))
    private double gravity$shootAt_getZ(LivingEntity target) {
        return com.example.api.physics.GravityCombat.calculateProjectileTargetZ(target);
    }

}
