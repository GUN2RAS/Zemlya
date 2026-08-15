package com.example.mixin.client;

import com.example.util.CameraShakeTracker;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraShakeMixin {

    @Shadow
    private float xRot;
    @Shadow
    private float yRot;

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "update", at = @At("RETURN"))
    private void applyShake(net.minecraft.client.DeltaTracker deltaTracker, CallbackInfo ci) {
        if (CameraShakeTracker.shakeIntensity > 0.0f) {
            float delta = deltaTracker.getGameTimeDeltaTicks();
            CameraShakeTracker.shakeTime += delta;

            float duration = 12.0f;
            float progress = Math.min(1.0f, CameraShakeTracker.shakeTime / duration);

            float envelope = (float) Math.sin(progress * Math.PI) * (1.0f - progress * 0.5f);
            float strengthFactor = Math.min(CameraShakeTracker.maxIntensity, 15.0f) / 15.0f;

            float pitchOffset = envelope * 3.5f * strengthFactor;
            float subtleRoll = (float) Math.sin(progress * Math.PI * 2.0) * 0.5f * strengthFactor;

            this.setRotation(this.yRot + subtleRoll, this.xRot + pitchOffset);

            CameraShakeTracker.shakeIntensity -= 1.5f * delta;
            if (CameraShakeTracker.shakeIntensity <= 0.0f || progress >= 1.0f) {
                CameraShakeTracker.shakeIntensity = 0.0f;
                CameraShakeTracker.maxIntensity = 1.0f;
                CameraShakeTracker.shakeTime = 0.0f;
            }
        }
    }
}
