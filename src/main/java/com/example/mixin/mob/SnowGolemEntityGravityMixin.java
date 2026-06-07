package com.example.mixin.mob;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SnowGolem.class)
public abstract class SnowGolemEntityGravityMixin {

    @Redirect(method = "performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D", ordinal = 0))
    private double gravity$shootAt_getX(LivingEntity target) {
        return com.example.api.physics.GravityCombat.calculateProjectileTargetX(target, target.getEyeHeight() - 1.100000023841858D);
    }

    @Redirect(method = "performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getEyeY()D", ordinal = 0))
    private double gravity$shootAt_getEyeY(LivingEntity target) {
        return com.example.api.physics.GravityCombat.calculateProjectileTargetY(target, target.getEyeHeight() - 1.100000023841858D, target.getEyeY()) + (GravityHelper.getGravityDirection(target) != Direction.DOWN ? 1.100000023841858D : 0);
    }

    @Redirect(method = "performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D", ordinal = 0))
    private double gravity$shootAt_getZ(LivingEntity target) {
        return com.example.api.physics.GravityCombat.calculateProjectileTargetZ(target, target.getEyeHeight() - 1.100000023841858D);
    }

}
