package com.example.util;

import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public abstract class RotationUtil {
    private static final Direction[][] DIR_WORLD_TO_PLAYER = new Direction[6][];

    static {
        for (Direction gravityDirection : Direction.values()) {
            DIR_WORLD_TO_PLAYER[gravityDirection.ordinal()] = new Direction[6];
            for (Direction direction : Direction.values()) {
                Vec3 directionVector = direction.getUnitVec3();
                directionVector = RotationUtil.vecWorldToPlayer(directionVector, gravityDirection);
                DIR_WORLD_TO_PLAYER[gravityDirection.ordinal()][direction.ordinal()] = Direction
                        .getNearest((int)directionVector.x, (int)directionVector.y, (int)directionVector.z, null);
            }
        }
    }

    public static Direction dirWorldToPlayer(Direction direction, Direction gravityDirection) {
        return DIR_WORLD_TO_PLAYER[gravityDirection.ordinal()][direction.ordinal()];
    }

    private static final Direction[][] DIR_PLAYER_TO_WORLD = new Direction[6][];

    static {
        for (Direction gravityDirection : Direction.values()) {
            DIR_PLAYER_TO_WORLD[gravityDirection.ordinal()] = new Direction[6];
            for (Direction direction : Direction.values()) {
                Vec3 directionVector = direction.getUnitVec3();
                directionVector = RotationUtil.vecPlayerToWorld(directionVector, gravityDirection);
                DIR_PLAYER_TO_WORLD[gravityDirection.ordinal()][direction.ordinal()] = Direction
                        .getNearest((int)directionVector.x, (int)directionVector.y, (int)directionVector.z, null);
            }
        }
    }

    public static Direction dirPlayerToWorld(Direction direction, Direction gravityDirection) {
        return DIR_PLAYER_TO_WORLD[gravityDirection.ordinal()][direction.ordinal()];
    }

    public static Vec3 vecWorldToPlayer(double x, double y, double z, Direction gravityDirection) {
        return switch (gravityDirection) {
            case DOWN -> new Vec3(x, y, z);
            case UP -> new Vec3(-x, -y, z);
            case NORTH -> new Vec3(x, z, -y);
            case SOUTH -> new Vec3(-x, -z, -y);
            case WEST -> new Vec3(-z, x, -y);
            case EAST -> new Vec3(z, -x, -y);
        };
    }

    public static Vec3 vecWorldToPlayer(Vec3 vec3d, Direction gravityDirection) {
        return vecWorldToPlayer(vec3d.x, vec3d.y, vec3d.z, gravityDirection);
    }

    public static Vec3 vecPlayerToWorld(double x, double y, double z, Direction gravityDirection) {
        return switch (gravityDirection) {
            case DOWN -> new Vec3(x, y, z);
            case UP -> new Vec3(-x, -y, z);
            case NORTH -> new Vec3(x, -z, y);
            case SOUTH -> new Vec3(-x, -z, -y);
            case WEST -> new Vec3(y, -z, -x);
            case EAST -> new Vec3(-y, -z, x);
        };
    }

    public static Vec3 vecPlayerToWorld(Vec3 vec3d, Direction gravityDirection) {
        return vecPlayerToWorld(vec3d.x, vec3d.y, vec3d.z, gravityDirection);
    }

    public static Vector3f vecWorldToPlayer(float x, float y, float z, Direction gravityDirection) {
        return switch (gravityDirection) {
            case DOWN -> new Vector3f(x, y, z);
            case UP -> new Vector3f(-x, -y, z);
            case NORTH -> new Vector3f(x, z, -y);
            case SOUTH -> new Vector3f(-x, -z, -y);
            case WEST -> new Vector3f(-z, x, -y);
            case EAST -> new Vector3f(z, -x, -y);
        };
    }

    public static Vector3f vecWorldToPlayer(Vector3f vector3F, Direction gravityDirection) {
        return vecWorldToPlayer(vector3F.x(), vector3F.y(), vector3F.z(), gravityDirection);
    }

    public static Vector3f vecPlayerToWorld(float x, float y, float z, Direction gravityDirection) {
        return switch (gravityDirection) {
            case DOWN -> new Vector3f(x, y, z);
            case UP -> new Vector3f(-x, -y, z);
            case NORTH -> new Vector3f(x, -z, y);
            case SOUTH -> new Vector3f(-x, -z, -y);
            case WEST -> new Vector3f(y, -z, -x);
            case EAST -> new Vector3f(-y, -z, x);
        };
    }

    public static Vector3f vecPlayerToWorld(Vector3f vector3F, Direction gravityDirection) {
        return vecPlayerToWorld(vector3F.x(), vector3F.y(), vector3F.z(), gravityDirection);
    }

    public static Vec3 maskWorldToPlayer(double x, double y, double z, Direction gravityDirection) {
        return switch (gravityDirection) {
            case DOWN, UP -> new Vec3(x, y, z);
            case NORTH, SOUTH -> new Vec3(x, z, y);
            case WEST, EAST -> new Vec3(z, x, y);
        };
    }

    public static Vec3 maskWorldToPlayer(Vec3 vec3d, Direction gravityDirection) {
        return maskWorldToPlayer(vec3d.x, vec3d.y, vec3d.z, gravityDirection);
    }

    public static Vec3 maskPlayerToWorld(double x, double y, double z, Direction gravityDirection) {
        return switch (gravityDirection) {
            case DOWN, UP -> new Vec3(x, y, z);
            case NORTH, SOUTH -> new Vec3(x, z, y);
            case WEST, EAST -> new Vec3(y, z, x);
        };
    }

    public static Vec3 maskPlayerToWorld(Vec3 vec3d, Direction gravityDirection) {
        return maskPlayerToWorld(vec3d.x, vec3d.y, vec3d.z, gravityDirection);
    }

    public static AABB boxWorldToPlayer(AABB box, Direction gravityDirection) {
        return new AABB(
                RotationUtil.vecWorldToPlayer(box.minX, box.minY, box.minZ, gravityDirection),
                RotationUtil.vecWorldToPlayer(box.maxX, box.maxY, box.maxZ, gravityDirection));
    }

    public static AABB boxPlayerToWorld(AABB box, Direction gravityDirection) {
        return new AABB(
                RotationUtil.vecPlayerToWorld(box.minX, box.minY, box.minZ, gravityDirection),
                RotationUtil.vecPlayerToWorld(box.maxX, box.maxY, box.maxZ, gravityDirection));
    }

    public static Quaternionf getWorldRotationQuaternion(Direction gravityDirection) {
        return switch (gravityDirection) {
            case DOWN -> new Quaternionf();
            case UP -> new Quaternionf().rotateZ((float) Math.toRadians(-180));
            case NORTH -> new Quaternionf().rotateX((float) Math.toRadians(-90));
            case SOUTH -> new Quaternionf().rotateX((float) Math.toRadians(-90)).rotateY((float) Math.toRadians(-180));
            case WEST -> new Quaternionf().rotateX((float) Math.toRadians(-90)).rotateY((float) Math.toRadians(-90));
            case EAST -> new Quaternionf().rotateX((float) Math.toRadians(-90)).rotateY((float) Math.toRadians(90));
        };
    }

    public static Quaternionf getInterpolatedWorldRotationQuaternion(Direction currentGravity, Direction prevGravity,
            float progress) {
        if (progress >= 1.0f || currentGravity == prevGravity) {
            return getWorldRotationQuaternion(currentGravity);
        }

        Quaternionf currentQuat = getWorldRotationQuaternion(currentGravity);
        Quaternionf prevQuat = getWorldRotationQuaternion(prevGravity);

        // slerp (spherical linear interpolation) is built into JOML Quaternionf
        return prevQuat.slerp(currentQuat, progress);
    }

    public static Quaternionf getCameraRotationQuaternion(Direction gravityDirection) {
        return getWorldRotationQuaternion(gravityDirection).conjugate(new Quaternionf());
    }

    public static Quaternionf getInterpolatedCameraRotationQuaternion(Direction currentGravity, Direction prevGravity,
            float progress) {
        return getInterpolatedWorldRotationQuaternion(currentGravity, prevGravity, progress)
                .conjugate(new Quaternionf());
    }

    public static net.minecraft.world.phys.Vec2 rotWorldToPlayer(float yaw, float pitch, Direction gravityDirection) {
        Vec3 vec = com.example.util.RotationUtil.getRotationVector(yaw, pitch);
        vec = com.example.util.RotationUtil.vecWorldToPlayer(vec, gravityDirection);
        return com.example.util.RotationUtil.getRotationFromVector(vec);
    }

    public static net.minecraft.world.phys.Vec2 rotPlayerToWorld(float yaw, float pitch, Direction gravityDirection) {
        Vec3 vec = com.example.util.RotationUtil.getRotationVector(yaw, pitch);
        vec = com.example.util.RotationUtil.vecPlayerToWorld(vec, gravityDirection);
        return com.example.util.RotationUtil.getRotationFromVector(vec);
    }

    private static Vec3 getRotationVector(float yaw, float pitch) {
        float f = pitch * ((float) Math.PI / 180);
        float g = -yaw * ((float) Math.PI / 180);
        float h = Mth.cos(g);
        float i = Mth.sin(g);
        float j = Mth.cos(f);
        float k = Mth.sin(f);
        return new Vec3(i * j, -k, h * j);
    }

    private static net.minecraft.world.phys.Vec2 getRotationFromVector(Vec3 vec) {
        double d = vec.x;
        double e = vec.y;
        double f = vec.z;
        double g = Math.sqrt(d * d + f * f);
        float pitch = (float) (Mth.wrapDegrees(Math.atan2(e, g) * (double) (180F / (float) Math.PI)));
        float yaw = (float) (Mth.wrapDegrees(Math.atan2(f, d) * (double) (180F / (float) Math.PI)) - 90.0F);
        return new net.minecraft.world.phys.Vec2(yaw, -pitch);
    }
}
