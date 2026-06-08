package com.example.mixin;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerGravityMixin {

    @Shadow
    public ServerPlayer player;

    @Redirect(
        method = "handleMovePlayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"
        )
    )
    private void gravity$onPlayerMove(ServerPlayer instance, MoverType type, Vec3 vec3d) {
        Direction gravityDirection = GravityHelper.getGravityDirection(this.player);
        if (gravityDirection == Direction.DOWN) {
            instance.move(type, vec3d);
            return;
        }
        instance.move(type, RotationUtil.vecWorldToPlayer(vec3d, gravityDirection));
    }

    @Redirect(
        method = "handleMoveVehicle",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"
        )
    )
    private void gravity$onVehicleMove(Entity instance, MoverType type, Vec3 vec3d) {
        Direction gravityDirection = GravityHelper.getGravityDirection(this.player);
        if (gravityDirection == Direction.DOWN) {
            instance.move(type, vec3d);
            return;
        }
        instance.move(type, RotationUtil.vecWorldToPlayer(vec3d, gravityDirection));
    }

    @Redirect(
        method = "noBlocksAround",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/phys/AABB;expandTowards(DDD)Lnet/minecraft/world/phys/AABB;"
        )
    )
    private AABB gravity$isEntityOnAir(AABB instance, double x, double y, double z) {
        Direction gravityDirection = GravityHelper.getGravityDirection(this.player);
        if (gravityDirection != Direction.DOWN) {
            Vec3 argVec = new Vec3(x, y, z);
            argVec = RotationUtil.vecWorldToPlayer(argVec, gravityDirection);
            return instance.expandTowards(argVec.x, argVec.y, argVec.z);
        }
        return instance.expandTowards(x, y, z);
    }
}
