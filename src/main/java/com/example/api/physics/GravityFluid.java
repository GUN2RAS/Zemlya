package com.example.api.physics;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class GravityFluid {
    private GravityFluid() {}


    public static boolean handleJumpOutOfFluid(LivingEntity entity, double oldWorldY) {
        Direction gravityDirection = GravityHelper.getGravityDirection(entity);
        if (gravityDirection == Direction.DOWN) {
            return false;
        }

        Vec3 movement = entity.getDeltaMovement();
        
        if (entity.horizontalCollision) {
            double actualWorldYTravel = entity.getY() - oldWorldY;
            Vec3 actualTravel = new Vec3(entity.getX() - entity.xOld, actualWorldYTravel, entity.getZ() - entity.zOld);
            Vec3 localTravel = RotationUtil.vecWorldToPlayer(actualTravel, gravityDirection);
            Vec3 localMovement = RotationUtil.vecWorldToPlayer(movement, gravityDirection);
            
            Vec3 localOffset = new Vec3(localMovement.x, localMovement.y + 0.6000000238418579 - localTravel.y, localMovement.z);
            Vec3 worldOffset = RotationUtil.vecPlayerToWorld(localOffset, gravityDirection);
            
            if (entity.isFree(worldOffset.x, worldOffset.y, worldOffset.z)) {
                Vec3 newLocal = new Vec3(localMovement.x, 0.30000001192092896, localMovement.z);
                entity.setDeltaMovement(RotationUtil.vecPlayerToWorld(newLocal, gravityDirection));
            }
        }
        return true;
    }


    public static Vec3 handleFluidFallingAdjustedMovement(LivingEntity entity, double baseGravity, boolean isFalling, Vec3 movement) {
        Direction gravityDirection = GravityHelper.getGravityDirection(entity);
        if (gravityDirection == Direction.DOWN) {
            return null;
        }
        
        if (baseGravity != 0.0 && !entity.isSprinting()) {
            Vec3 localMovement = RotationUtil.vecWorldToPlayer(movement, gravityDirection);
            
            double yd;
            if (isFalling && Math.abs(localMovement.y - 0.005) >= 0.003 && Math.abs(localMovement.y - baseGravity / 16.0) < 0.003) {
                yd = -0.003;
            } else {
                yd = localMovement.y - baseGravity / 16.0;
            }

            Vec3 adjustedLocal = new Vec3(localMovement.x, yd, localMovement.z);
            return RotationUtil.vecPlayerToWorld(adjustedLocal, gravityDirection);
        } else {
            return movement;
        }
    }
}
