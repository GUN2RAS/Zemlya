package com.example.api.physics;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.phys.Vec3;

public final class GravityNavigation {
    private GravityNavigation() {}

    public static double transformMoveAngles(Mob mob, double wantedX, double wantedY, double wantedZ, double dz, double dx) {
        Direction gravityDirection = GravityHelper.getGravityDirection(mob);
        if (gravityDirection == Direction.DOWN) {
            return Mth.atan2(dz, dx);
        }

        Vec3 diff = new Vec3(wantedX - mob.getX(), wantedY - mob.getY(), wantedZ - mob.getZ());
        Vec3 localDiff = RotationUtil.vecWorldToPlayer(diff, gravityDirection);
        return Mth.atan2(localDiff.z, localDiff.x);
    }


    public static Vec3 transformLookAngles(Mob mob, Vec3 targetPos, double targetEyeY) {
        Direction gravityDirection = GravityHelper.getGravityDirection(mob);
        if (gravityDirection == Direction.DOWN) {
            return null;
        }

        double d = targetPos.x - mob.getX();
        double e = targetPos.z - mob.getZ();
        double f = targetEyeY - mob.getEyeY();

        Vec3 localDiff = RotationUtil.vecWorldToPlayer(new Vec3(d, f, e), gravityDirection);

        double g = Math.sqrt(localDiff.x * localDiff.x + localDiff.z * localDiff.z);
        float targetYaw = (float) (Mth.atan2(localDiff.z, localDiff.x) * 57.2957763671875) - 90.0F;
        float targetPitch = (float) (-(Mth.atan2(localDiff.y, g) * 57.2957763671875));

        return new Vec3(targetYaw, targetPitch, 0);
    }

    public static Node calculatePathNode(Mob mob, double targetX, double targetY, double targetZ) {
        Direction gravityDirection = GravityHelper.getGravityDirection(mob);
        if (gravityDirection == Direction.DOWN) {
            return null;
        }

        return new Node(Mth.floor(targetX), Mth.floor(targetY), Mth.floor(targetZ));
    }
}
