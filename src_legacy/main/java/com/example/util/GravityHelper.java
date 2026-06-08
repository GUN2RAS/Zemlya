package com.example.util;

import com.example.api.Gravity;
import com.example.api.GravityChanger;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.Direction;

/**
 * Helper to get gravity direction from any entity, returning Direction
 * for compatibility with RotationUtil methods.
 */
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
}
