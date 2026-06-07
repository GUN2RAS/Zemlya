package com.example.mixin.client;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayer {

    public ClientPlayerEntityMixin(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }



    private boolean gravity$wouldCollideAt_impl(BlockPos pos, Direction gravityDirection) {
        AABB playerBox = this.getBoundingBox();
        Vec3 playerMask = RotationUtil.maskPlayerToWorld(0.0, 1.0, 0.0, gravityDirection);
        AABB posBox = new AABB(pos);
        Vec3 posMask = RotationUtil.maskPlayerToWorld(1.0, 0.0, 1.0, gravityDirection);

        AABB checkBox = new AABB(
                playerMask.multiply(playerBox.minX, playerBox.minY, playerBox.minZ)
                        .add(posMask.multiply(posBox.minX, posBox.minY, posBox.minZ)),
                playerMask.multiply(playerBox.maxX, playerBox.maxY, playerBox.maxZ)
                        .add(posMask.multiply(posBox.maxX, posBox.maxY, posBox.maxZ)))
                .deflate(1.0E-7);

        return !this.level().noCollision(this, checkBox);
    }

    @Inject(method = "suffocatesAt(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
    private void gravity$wouldCollideAt(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Direction gravityDirection = GravityHelper.getGravityDirection(this);
        if (gravityDirection == Direction.DOWN)
            return;

        cir.setReturnValue(gravity$wouldCollideAt_impl(pos, gravityDirection));
    }

    @Inject(method = "moveTowardsClosestSpace(DD)V", at = @At("HEAD"), cancellable = true)
    private void gravity$pushOutOfBlocks(double x, double z, CallbackInfo ci) {
        Direction gravityDirection = GravityHelper.getGravityDirection(this);
        if (gravityDirection == Direction.DOWN)
            return;

        ci.cancel();

        Vec3 pos = RotationUtil.vecPlayerToWorld(x - this.getX(), 0.0, z - this.getZ(), gravityDirection)
                .add(this.position());
        BlockPos blockPos = BlockPos.containing(pos);
        if (gravity$wouldCollideAt_impl(blockPos, gravityDirection)) {
            double dx = pos.x - (double) blockPos.getX();
            double dy = pos.y - (double) blockPos.getY();
            double dz = pos.z - (double) blockPos.getZ();
            Direction direction = null;
            double minDistToEdge = Double.MAX_VALUE;

            Direction[] directions = new Direction[] { Direction.WEST, Direction.EAST, Direction.NORTH,
                    Direction.SOUTH };
            for (Direction playerDirection : directions) {
                Direction worldDirection = RotationUtil.dirPlayerToWorld(playerDirection, gravityDirection);

                double g = worldDirection.getAxis().choose(dx, dy, dz);
                double distToEdge = worldDirection.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0 - g : g;
                if (distToEdge < minDistToEdge
                        && !gravity$wouldCollideAt_impl(blockPos.relative(worldDirection), gravityDirection)) {
                    minDistToEdge = distToEdge;
                    direction = playerDirection;
                }
            }

            if (direction != null) {
                Direction worldDirection = RotationUtil.dirPlayerToWorld(direction, gravityDirection);
                Vec3 velocity = this.getDeltaMovement();
                double vx = velocity.x;
                double vy = velocity.y;
                double vz = velocity.z;

                if (worldDirection.getAxis() == Direction.Axis.X) {
                    vx = 0.1 * (double) worldDirection.getStepX();
                } else if (worldDirection.getAxis() == Direction.Axis.Y) {
                    vy = 0.1 * (double) worldDirection.getStepY();
                } else if (worldDirection.getAxis() == Direction.Axis.Z) {
                    vz = 0.1 * (double) worldDirection.getStepZ();
                }

                this.setDeltaMovement(new net.minecraft.world.phys.Vec3(vx, vy, vz));
            }
        }
    }
}
