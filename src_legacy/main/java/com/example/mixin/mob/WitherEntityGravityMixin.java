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
        Direction gd = GravityHelper.getGravityDirection(target);
        if (gd == Direction.DOWN) return target.getX();
        return target.position().add(RotationUtil.vecPlayerToWorld(0.0D, target.getEyeHeight() * 0.5D, 0.0D, gd)).x;
    }

    @Redirect(method = "performRangedAttack(ILnet/minecraft/world/entity/LivingEntity;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getY()D", ordinal = 0))
    private double gravity$shootSkull_getY(LivingEntity target) {
        Direction gd = GravityHelper.getGravityDirection(target);
        if (gd == Direction.DOWN) return target.getY();
        return target.position().add(RotationUtil.vecPlayerToWorld(0.0D, target.getEyeHeight() * 0.5D, 0.0D, gd)).y - target.getEyeHeight() * 0.5D;
    }

    @Redirect(method = "performRangedAttack(ILnet/minecraft/world/entity/LivingEntity;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D", ordinal = 0))
    private double gravity$shootSkull_getZ(LivingEntity target) {
        Direction gd = GravityHelper.getGravityDirection(target);
        if (gd == Direction.DOWN) return target.getZ();
        return target.position().add(RotationUtil.vecPlayerToWorld(0.0D, target.getEyeHeight() * 0.5D, 0.0D, gd)).z;
    }

    @Redirect(method = "aiStep()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getEyeY()D", ordinal = 0))
    private double gravity$tickMovement_getEyeY(Entity entity) {
        Direction gd = GravityHelper.getGravityDirection(entity);
        if (gd == Direction.DOWN) return entity.getEyeY();
        return entity.getEyePosition().y;
    }

    @Redirect(method = "aiStep()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getX()D", ordinal = 0))
    private double gravity$tickMovement_getX(Entity entity) {
        Direction gd = GravityHelper.getGravityDirection(entity);
        if (gd == Direction.DOWN) return entity.getX();
        return entity.getEyePosition().x;
    }

    @Redirect(method = "aiStep()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getZ()D", ordinal = 0))
    private double gravity$tickMovement_getZ(Entity entity) {
        Direction gd = GravityHelper.getGravityDirection(entity);
        if (gd == Direction.DOWN) return entity.getZ();
        return entity.getEyePosition().z;
    }
}
