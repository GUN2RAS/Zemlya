package com.example.mixin.client;

import com.example.api.GravityChanger;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.example.util.RotationUtil;

@Mixin(AvatarRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @Redirect(method = "extractFlightData(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Avatar;getViewVector(F)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 gravity$updateGliding_getViewVector(Avatar player, float tickDelta) {
        Vec3 vec3d = player.getViewVector(tickDelta);
        GravityChanger changer = (GravityChanger) player;
        return RotationUtil.vecPlayerToWorld(vec3d, changer.getGravityDirection().getDirection());
    }
}
