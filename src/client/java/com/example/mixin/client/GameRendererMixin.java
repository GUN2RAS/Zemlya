package com.example.mixin.client;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @org.spongepowered.asm.mixin.injection.Inject(method = "getFov", at = @org.spongepowered.asm.mixin.injection.At("RETURN"), cancellable = true)
    private void gravity$getFov(net.minecraft.client.Camera camera, float tickDelta, boolean changingFov, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Double> cir) {
        if (net.minecraft.client.Minecraft.getInstance().player instanceof com.example.api.GravityChanger changer) {
            float progress = changer.getGravityTransitionProgress(tickDelta);
            if (progress > 0.0f && progress < 1.0f) {
                double fovBump = (1.0 - Math.pow(2.0 * progress - 1.0, 2)) * 25.0;
                cir.setReturnValue(cir.getReturnValue() + fovBump);
            }
        }
    }
}
