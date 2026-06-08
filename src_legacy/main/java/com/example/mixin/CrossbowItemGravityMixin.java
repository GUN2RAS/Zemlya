package com.example.mixin;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CrossbowItem.class)
public abstract class CrossbowItemGravityMixin {

    @Shadow
    private static Vector3f getProjectileShotVector(LivingEntity shooter, Vec3 direction, float yaw) {
        throw new AssertionError();
    }

    @Redirect(method = "createProjectile",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D", ordinal = 0))
    private double gravity$createArrow_getX(LivingEntity entity) {
        Direction gd = GravityHelper.getGravityDirection(entity);
        if (gd == Direction.DOWN) return entity.getX();
        return entity.getEyePosition().subtract(RotationUtil.vecPlayerToWorld(0.0D, 0.15F, 0.0D, gd)).x;
    }

    @Redirect(method = "createProjectile",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getEyeY()D", ordinal = 0))
    private double gravity$createArrow_getEyeY(LivingEntity entity) {
        Direction gd = GravityHelper.getGravityDirection(entity);
        if (gd == Direction.DOWN) return entity.getEyeY();
        return entity.getEyePosition().subtract(RotationUtil.vecPlayerToWorld(0.0D, 0.15F, 0.0D, gd)).y + 0.15F;
    }

    @Redirect(method = "createProjectile",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D", ordinal = 0))
    private double gravity$createArrow_getZ(LivingEntity entity) {
        Direction gd = GravityHelper.getGravityDirection(entity);
        if (gd == Direction.DOWN) return entity.getZ();
        return entity.getEyePosition().subtract(RotationUtil.vecPlayerToWorld(0.0D, 0.15F, 0.0D, gd)).z;
    }

    @Redirect(method = "shootProjectile",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CrossbowItem;getProjectileShotVector(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/Vec3;F)Lorg/joml/Vector3f;"))
    private Vector3f gravity$shoot_calcVelocity(LivingEntity shooter, Vec3 direction, float yaw,
                                                 @Local Projectile projectile, @Local(ordinal = 1) LivingEntity target) {
        Direction gd = GravityHelper.getGravityDirection(target);
        if (gd == Direction.DOWN) return getProjectileShotVector(shooter, direction, yaw);

        Vec3 targetPos = target.position().add(RotationUtil.vecPlayerToWorld(0.0D, target.getBbHeight() * 0.3333333333333333D, 0.0D, gd));
        double d = targetPos.x - shooter.getX();
        double e = targetPos.z - shooter.getZ();
        double f = Math.sqrt(d * d + e * e);
        if (gd != Direction.UP) f = Math.sqrt(f);
        double g = targetPos.y - projectile.getY() + f * 0.2F;
        return getProjectileShotVector(shooter, new Vec3(d, g, e), yaw);
    }
}
