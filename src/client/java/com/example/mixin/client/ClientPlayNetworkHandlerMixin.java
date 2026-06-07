package com.example.mixin.client;

import com.example.util.GravityHelper;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ClientPacketListener.class, priority = 1001)
public abstract class ClientPlayNetworkHandlerMixin {

    @Redirect(method = "handleGameEvent(Lnet/minecraft/network/protocol/game/ClientboundGameEventPacket;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getEyeY()D", ordinal = 0))
    private double gravity$onGameStateChange_getEyeY(LocalPlayer playerEntity) {
        Direction gravityDirection = GravityHelper.getGravityDirection(playerEntity);
        if (gravityDirection == Direction.DOWN)
            return playerEntity.getEyeY();
        return playerEntity.getEyePosition().y;
    }

    @Redirect(method = "handleGameEvent(Lnet/minecraft/network/protocol/game/ClientboundGameEventPacket;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getX()D", ordinal = 0))
    private double gravity$onGameStateChange_getX(LocalPlayer playerEntity) {
        Direction gravityDirection = GravityHelper.getGravityDirection(playerEntity);
        if (gravityDirection == Direction.DOWN)
            return playerEntity.getX();
        return playerEntity.getEyePosition().x;
    }

    @Redirect(method = "handleGameEvent(Lnet/minecraft/network/protocol/game/ClientboundGameEventPacket;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getZ()D", ordinal = 0))
    private double gravity$onGameStateChange_getZ(LocalPlayer playerEntity) {
        Direction gravityDirection = GravityHelper.getGravityDirection(playerEntity);
        if (gravityDirection == Direction.DOWN)
            return playerEntity.getZ();
        return playerEntity.getEyePosition().z;
    }
}
