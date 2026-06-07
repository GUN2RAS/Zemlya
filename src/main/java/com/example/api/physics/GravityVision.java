package com.example.api.physics;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class GravityVision {
    private GravityVision() {}

    public static BlockPos calculateEyeBlockPos(Entity entity, double x, double y, double z) {
        Direction gravityDirection = GravityHelper.getGravityDirection(entity);
        if (gravityDirection == Direction.DOWN) {
            return BlockPos.containing(x, y, z);
        }
        return BlockPos.containing(entity.getEyePosition());
    }

    public static Vec3 transformParticleSpawn_Add(Entity entity, Vec3 originalVec, double x, double y, double z, Operation<Vec3> original) {
        Direction gravityDirection = GravityHelper.getGravityDirection(entity);
        if (gravityDirection == Direction.DOWN) {
            return original.call(originalVec, x, y, z);
        }
        Vec3 rotated = RotationUtil.vecPlayerToWorld(originalVec, gravityDirection);
        return original.call(entity.getEyePosition(), rotated.x, rotated.y, rotated.z);
    }
    
    public static Vec3 transformParticleSpawn_RotateY(Entity entity, Vec3 vec3d) {
        Direction gravityDirection = GravityHelper.getGravityDirection(entity);
        if (gravityDirection == Direction.DOWN) {
            return vec3d;
        }
        return RotationUtil.vecPlayerToWorld(vec3d, gravityDirection);
    }
    
    public static double getTargetEyeX(Entity entity) {
        Direction gd = GravityHelper.getGravityDirection(entity);
        return gd == Direction.DOWN ? entity.getX() : entity.getEyePosition().x;
    }

    public static double getTargetEyeY(Entity entity) {
        Direction gd = GravityHelper.getGravityDirection(entity);
        return gd == Direction.DOWN ? entity.getEyeY() : entity.getEyePosition().y;
    }

    public static double getTargetEyeZ(Entity entity) {
        Direction gd = GravityHelper.getGravityDirection(entity);
        return gd == Direction.DOWN ? entity.getZ() : entity.getEyePosition().z;
    }
}
