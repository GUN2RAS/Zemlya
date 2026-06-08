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
        Direction gd = GravityHelper.getGravityDirection(target);
        if (gd == Direction.DOWN) return target.getX();
        return target.position().add(RotationUtil.vecPlayerToWorld(0.0D, target.getBbHeight() * 0.3333333333333333D, 0.0D, gd)).x;
    }

    @Redirect(method = "performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getY(D)D", ordinal = 0))
    private double gravity$shootAt_getBodyY(LivingEntity target, double heightScale) {
        Direction gd = GravityHelper.getGravityDirection(target);
        if (gd == Direction.DOWN) return target.getY(heightScale);
        return target.position().add(RotationUtil.vecPlayerToWorld(0.0D, target.getBbHeight() * 0.3333333333333333D, 0.0D, gd)).y;
    }

    @Redirect(method = "performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D", ordinal = 0))
    private double gravity$shootAt_getZ(LivingEntity target) {
        Direction gd = GravityHelper.getGravityDirection(target);
        if (gd == Direction.DOWN) return target.getZ();
        return target.position().add(RotationUtil.vecPlayerToWorld(0.0D, target.getBbHeight() * 0.3333333333333333D, 0.0D, gd)).z;
    }

    @Redirect(method = "performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
        at = @At(value = "INVOKE", target = "Ljava/lang/Math;sqrt(D)D"))
    private double gravity$shootAt_sqrt(double value, LivingEntity target, float pullProgress) {
        Direction gd = GravityHelper.getGravityDirection(target);
        if (gd == Direction.DOWN) return Math.sqrt(value);
        return Math.sqrt(Math.sqrt(value));
    }
}
