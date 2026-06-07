package com.example.util;

import com.example.api.Gravity;
import com.example.api.GravityChanger;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class GravityHelper {
    private GravityHelper() {}

    public static Direction getGravityDirection(Entity entity) {
        if (entity instanceof GravityChanger gc) {
            return gc.getGravityDirection().getDirection();
        }
        return Direction.DOWN;
    }

    public static Gravity getGravity(Entity entity) {
        if (entity instanceof GravityChanger gc) {
            return gc.getGravityDirection();
        }
        return Gravity.DOWN;
    }

    public static boolean isDown(Entity entity) {
        return getGravityDirection(entity) == Direction.DOWN;
    }

    public static Vec3 getBottomCenter(AABB box, Direction gravityDirection) {
        if (gravityDirection == Direction.DOWN) {
            return box.getBottomCenter();
        }
        AABB playerBox = RotationUtil.boxWorldToPlayer(box, gravityDirection);
        Vec3 playerBottomCenter = new Vec3(
            (playerBox.minX + playerBox.maxX) / 2.0,
            playerBox.minY,
            (playerBox.minZ + playerBox.maxZ) / 2.0
        );
        return RotationUtil.vecPlayerToWorld(playerBottomCenter, gravityDirection);
    }

    public static net.minecraft.world.phys.AABB buildGravityAwareBox(net.minecraft.world.phys.AABB originalBox, net.minecraft.world.phys.Vec3 pos, net.minecraft.world.entity.EntityDimensions dimensions, Direction gravityDirection) {
        if (gravityDirection == Direction.DOWN) return originalBox;
        
        float w = dimensions.width() / 2.0F;
        float h = dimensions.height();
        
        switch (gravityDirection) {
            case UP: return new net.minecraft.world.phys.AABB(pos.x - w, pos.y - h, pos.z - w, pos.x + w, pos.y, pos.z + w);
            case NORTH: return new net.minecraft.world.phys.AABB(pos.x - w, pos.y - w, pos.z, pos.x + w, pos.y + w, pos.z + h);
            case SOUTH: return new net.minecraft.world.phys.AABB(pos.x - w, pos.y - w, pos.z - h, pos.x + w, pos.y + w, pos.z);
            case WEST: return new net.minecraft.world.phys.AABB(pos.x, pos.y - w, pos.z - w, pos.x + h, pos.y + w, pos.z + w);
            case EAST: return new net.minecraft.world.phys.AABB(pos.x - h, pos.y - w, pos.z - w, pos.x, pos.y + w, pos.z + w);
            default: return originalBox;
        }
    }
}
