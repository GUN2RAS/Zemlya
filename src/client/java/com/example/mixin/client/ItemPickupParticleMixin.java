package com.example.mixin.client;

import net.minecraft.client.particle.ItemPickupParticle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemPickupParticle.class)
public abstract class ItemPickupParticleMixin {

    @Shadow
    @Final
    private Entity target;
    @Shadow
    protected double targetX;
    @Shadow
    protected double targetY;
    @Shadow
    protected double targetZ;

    @Inject(method = "updatePosition()V", at = @At("HEAD"), cancellable = true)
    private void gravity$updateTargetPos(CallbackInfo ci) {
        Vec3 entityPos = this.target.position();
        Vec3 eyePos = this.target.getEyePosition();
        Vec3 mid = eyePos.add(entityPos).scale(0.5);

        this.targetX = mid.x;
        this.targetY = mid.y;
        this.targetZ = mid.z;
        ci.cancel();
    }
}
