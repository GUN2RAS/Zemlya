package com.example.mixin.mob;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WitherBoss.class)
public abstract class WitherEntityGravityMixin {

    @Redirect(method = "performRangedAttack(ILnet/minecraft/world/entity/LivingEntity;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D", ordinal = 0))
    private double gravity$shootSkull_getX(LivingEntity target) {
        return com.example.api.physics.GravityCombat.calculateProjectileTargetX(target, target.getEyeHeight() * 0.5D);
    }

    @Redirect(method = "performRangedAttack(ILnet/minecraft/world/entity/LivingEntity;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getY()D", ordinal = 0))
    private double gravity$shootSkull_getY(LivingEntity target) {
        return com.example.api.physics.GravityCombat.calculateProjectileTargetY(target, target.getEyeHeight() * 0.5D, target.getY()) - (GravityHelper.getGravityDirection(target) != Direction.DOWN ? target.getEyeHeight() * 0.5D : 0);
    }

    @Redirect(method = "performRangedAttack(ILnet/minecraft/world/entity/LivingEntity;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D", ordinal = 0))
    private double gravity$shootSkull_getZ(LivingEntity target) {
        return com.example.api.physics.GravityCombat.calculateProjectileTargetZ(target, target.getEyeHeight() * 0.5D);
    }

    @Redirect(method = "aiStep()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getEyeY()D", ordinal = 0))
    private double gravity$tickMovement_getEyeY(Entity entity) {
        return com.example.api.physics.GravityVision.getTargetEyeY(entity);
    }

    @Redirect(method = "aiStep()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getX()D", ordinal = 0))
    private double gravity$tickMovement_getX(Entity entity) {
        return com.example.api.physics.GravityVision.getTargetEyeX(entity);
    }

    @Redirect(method = "aiStep()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getZ()D", ordinal = 0))
    private double gravity$tickMovement_getZ(Entity entity) {
        return com.example.api.physics.GravityVision.getTargetEyeZ(entity);
    }
}
