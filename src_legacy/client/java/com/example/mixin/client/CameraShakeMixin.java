package com.example.mixin.client;

import com.example.client.CameraShakeTracker;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
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
        if (CameraShakeTracker.isShaking) {
            float shakeX = (float) ((Math.random() - 0.5) * 1.5);
            float shakeY = (float) ((Math.random() - 0.5) * 1.5);

            this.setRotation(this.yRot + shakeX, this.xRot + shakeY);
        }
    }
}