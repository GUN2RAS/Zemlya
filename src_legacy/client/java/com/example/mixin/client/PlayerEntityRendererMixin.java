package com.example.mixin.client;

import com.example.access.EntityRenderStateExtension;
import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.Avatar;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fixes gravity-aware rendering for player entities in 26.1.1 pipeline.
 */
@Mixin(AvatarRenderer.class)
public abstract class PlayerEntityRendererMixin {

    // Fix getViewVector in extractFlightData to return player-local rotation
    @Redirect(method = "extractFlightData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Avatar;getViewVector(F)Lnet/minecraft/world/phys/Vec3;"))
    private <T extends Avatar> Vec3 gravity$extractFlightData_getViewVector(T player,
            float tickProgress) {
        Vec3 rotationVec = player.getViewVector(tickProgress);
        Direction gravityDirection = GravityHelper.getGravityDirection(player);
        if (gravityDirection == Direction.DOWN) {
            return rotationVec;
        }
        return RotationUtil.vecWorldToPlayer(rotationVec, gravityDirection);
    }
}
