package com.example.mixin.mob;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Witch.class)
public abstract class WitchEntityGravityMixin {

    @ModifyVariable(method = "performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
        at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/entity/LivingEntity;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;", ordinal = 0),
        ordinal = 0)
    private Vec3 gravity$shootAt_velocity(Vec3 value, LivingEntity target, float pullProgress) {
        Direction gd = GravityHelper.getGravityDirection(target);
        if (gd == Direction.DOWN) return value;
        return RotationUtil.vecPlayerToWorld(value, gd);
    }

    @Redirect(method = "performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D", ordinal = 0))
    private double gravity$shootAt_getX(LivingEntity target) {
        Direction gd = GravityHelper.getGravityDirection(target);
        if (gd == Direction.DOWN) return target.getX();
        return target.position().add(RotationUtil.vecPlayerToWorld(0.0D, target.getEyeHeight() - 1.100000023841858D, 0.0D, gd)).x;
    }

    @Redirect(method = "performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getEyeY()D", ordinal = 0))
    private double gravity$shootAt_getEyeY(LivingEntity target) {
        Direction gd = GravityHelper.getGravityDirection(target);
        if (gd == Direction.DOWN) return target.getEyeY();
        return target.position().add(RotationUtil.vecPlayerToWorld(0.0D, target.getEyeHeight() - 1.100000023841858D, 0.0D, gd)).y + 1.100000023841858D;
    }

    @Redirect(method = "performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D", ordinal = 0))
    private double gravity$shootAt_getZ(LivingEntity target) {
        Direction gd = GravityHelper.getGravityDirection(target);
        if (gd == Direction.DOWN) return target.getZ();
        return target.position().add(RotationUtil.vecPlayerToWorld(0.0D, target.getEyeHeight() - 1.100000023841858D, 0.0D, gd)).z;
    }

    @Redirect(method = "performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
        at = @At(value = "INVOKE", target = "Ljava/lang/Math;sqrt(D)D"))
    private double gravity$shootAt_sqrt(double value, LivingEntity target, float pullProgress) {
        Direction gd = GravityHelper.getGravityDirection(target);
        if (gd == Direction.DOWN) return Math.sqrt(value);
        return Math.sqrt(Math.sqrt(value));
    }
}
