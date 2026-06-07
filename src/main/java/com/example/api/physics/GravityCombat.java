package com.example.api.physics;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class GravityCombat {
    private GravityCombat() {}

    public static double transformDamageSourceX(Entity target, Vec3 damageSourcePosition) {
        Direction gravityDirection = GravityHelper.getGravityDirection(target);
        if (gravityDirection == Direction.DOWN) {
            return damageSourcePosition.x;
        }
        return RotationUtil.vecWorldToPlayer(damageSourcePosition, gravityDirection).x;
    }

    public static double transformDamageSourceZ(Entity target, Vec3 damageSourcePosition) {
        Direction gravityDirection = GravityHelper.getGravityDirection(target);
        if (gravityDirection == Direction.DOWN) {
            return damageSourcePosition.z;
        }
        return RotationUtil.vecWorldToPlayer(damageSourcePosition, gravityDirection).z;
    }

    public static double transformTargetX(LivingEntity target) {
        Direction gravityDirection = GravityHelper.getGravityDirection(target);
        if (gravityDirection == Direction.DOWN) {
            return target.getX();
        }
        return RotationUtil.vecWorldToPlayer(target.position(), gravityDirection).x;
    }

    public static double transformTargetZ(LivingEntity target) {
        Direction gravityDirection = GravityHelper.getGravityDirection(target);
        if (gravityDirection == Direction.DOWN) {
            return target.getZ();
        }
        return RotationUtil.vecWorldToPlayer(target.position(), gravityDirection).z;
    }
    
    public static Vec3 transformKnockbackDir(Vec3 worldDir, Direction gravityDirection) {
        if (gravityDirection == Direction.DOWN) {
            return worldDir;
        }
        return RotationUtil.vecWorldToPlayer(worldDir, gravityDirection);
    }

    public static float transformAttackerYaw(Entity attacker, float originalYaw, float attackerPitch) {
        Direction gravityDirection = GravityHelper.getGravityDirection(attacker);
        if (gravityDirection == Direction.DOWN) {
            return originalYaw;
        }
        return RotationUtil.rotPlayerToWorld(originalYaw, attackerPitch, gravityDirection).x;
    }

    public static double calculateProjectileTargetX(LivingEntity target) {
        return calculateProjectileTargetX(target, target.getBbHeight() * 0.3333333333333333D);
    }

    public static double calculateProjectileTargetY(LivingEntity target, double heightScale) {
        return calculateProjectileTargetY(target, target.getBbHeight() * 0.3333333333333333D, target.getY(heightScale));
    }

    public static double calculateProjectileTargetZ(LivingEntity target) {
        return calculateProjectileTargetZ(target, target.getBbHeight() * 0.3333333333333333D);
    }

    public static double calculateProjectileTargetX(LivingEntity target, double yOffset) {
        Direction gd = GravityHelper.getGravityDirection(target);
        if (gd == Direction.DOWN) return target.getX();
        return target.position().add(RotationUtil.vecPlayerToWorld(0.0D, yOffset, 0.0D, gd)).x;
    }

    public static double calculateProjectileTargetY(LivingEntity target, double yOffset, double vanillaY) {
        Direction gd = GravityHelper.getGravityDirection(target);
        if (gd == Direction.DOWN) return vanillaY;
        return target.position().add(RotationUtil.vecPlayerToWorld(0.0D, yOffset, 0.0D, gd)).y;
    }

    public static double calculateProjectileTargetZ(LivingEntity target, double yOffset) {
        Direction gd = GravityHelper.getGravityDirection(target);
        if (gd == Direction.DOWN) return target.getZ();
        return target.position().add(RotationUtil.vecPlayerToWorld(0.0D, yOffset, 0.0D, gd)).z;
    }
}
