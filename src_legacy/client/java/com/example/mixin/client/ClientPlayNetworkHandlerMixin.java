package com.example.mixin.client;

import com.example.util.GravityHelper;

import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * method_11085 = onGameStateChange(GameStateChangeS2CPacket) in
 * ClientPlayNetworkHandler (1.21.11 intermediary)
 * method_23320 = getEyeY() in Player (class_1657)
 * method_23317 = getX() in Player
 * method_23321 = getZ() in Player
 */
@Mixin(value = net.minecraft.client.multiplayer.ClientPacketListener.class, priority = 1001)
public abstract class ClientPlayNetworkHandlerMixin {

    @Redirect(method = "handleGameEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getEyeY()D", ordinal = 0))
    private double gravity$onGameStateChange_getEyeY(Player playerEntity) {
        Direction gravityDirection = GravityHelper.getGravityDirection(playerEntity);
        if (gravityDirection == Direction.DOWN)
            return playerEntity.getEyeY();
        return playerEntity.getEyePosition().y;
    }

    @Redirect(method = "handleGameEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getX()D", ordinal = 0))
    private double gravity$onGameStateChange_getX(Player playerEntity) {
        Direction gravityDirection = GravityHelper.getGravityDirection(playerEntity);
        if (gravityDirection == Direction.DOWN)
            return playerEntity.getX();
        return playerEntity.getEyePosition().x;
    }

    @Redirect(method = "handleGameEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getZ()D", ordinal = 0))
    private double gravity$onGameStateChange_getZ(Player playerEntity) {
        Direction gravityDirection = GravityHelper.getGravityDirection(playerEntity);
        if (gravityDirection == Direction.DOWN)
            return playerEntity.getZ();
        return playerEntity.getEyePosition().z;
    }
}