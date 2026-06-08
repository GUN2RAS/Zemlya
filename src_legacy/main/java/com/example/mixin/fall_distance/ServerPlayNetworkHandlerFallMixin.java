package com.example.mixin.fall_distance;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPlayNetworkHandlerFallMixin {

    @Shadow
    public ServerPlayer player;

    @Shadow
    private double firstGoodX;

    @Shadow
    private double firstGoodY;

    @Shadow
    private double firstGoodZ;

    @ModifyVariable(
        method = "handleMovePlayer",
        at = @At(value = "STORE"),
        ordinal = 1
    )
    private boolean gravity$modifyFallFlag(boolean originalFlag, net.minecraft.network.protocol.game.ServerboundMovePlayerPacket packet) {
        Direction gravity = GravityHelper.getGravityDirection(player);

        double dx = packet.getX(player.getX()) - firstGoodX;
        double dy = packet.getY(player.getY()) - firstGoodY;
        double dz = packet.getZ(player.getZ()) - firstGoodZ;

        Vec3 localVec = RotationUtil.vecWorldToPlayer(dx, dy, dz, gravity);
        return localVec.y > 0.0;
    }
}
