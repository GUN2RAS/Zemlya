package com.example.mixin.client;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.example.api.GravityChanger;
import net.minecraft.client.DeltaTracker;

@Mixin(value = Camera.class, priority = 1001)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    private Entity entity;

    @Shadow
    @Final
    private Quaternionf rotation;

    @Shadow
    private float eyeHeightOld;

    @Shadow
    private float eyeHeight;

    @Unique
    private float currentTickDelta = 1.0f;

    @Unique
    private long lastLogTime = 0;

    @Inject(method = "update(Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"))
    private void gravity$captureTickDelta(DeltaTracker deltaTracker, CallbackInfo ci) {
        this.currentTickDelta = deltaTracker.getGameTimeDeltaPartialTick(true);
    }

    @WrapOperation(method = "alignWithEntity(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"))
    private void gravity$alignWithEntity_setPosition(
            Camera camera, double x, double y, double z,
            Operation<Void> original,
            float partialTicks) {

        Direction gravityDirection = GravityHelper.getGravityDirection(this.entity);
        if (gravityDirection == Direction.DOWN) {
            original.call(camera, x, y, z);
            return;
        }

        double entityX = Mth.lerp((double) partialTicks, this.entity.xo, this.entity.getX());
        double entityY = Mth.lerp((double) partialTicks, this.entity.yo, this.entity.getY());
        double entityZ = Mth.lerp((double) partialTicks, this.entity.zo, this.entity.getZ());

        double currentCameraY = Mth.lerp(partialTicks, this.eyeHeightOld, this.eyeHeight);

        Vec3 eyeOffset = RotationUtil.vecPlayerToWorld(0.0, currentCameraY, 0.0, gravityDirection);

        original.call(
                camera,
                entityX + eyeOffset.x,
                entityY + eyeOffset.y,
                entityZ + eyeOffset.z);
    }

    @Inject(method = "setRotation(FF)V", at = @At(value = "INVOKE", target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;", shift = At.Shift.AFTER))
    private void gravity$setRotation(float yaw, float pitch, CallbackInfo ci) {
        if (this.entity != null) {
            GravityChanger changer = (GravityChanger) this.entity;
            Direction gravityDirection = changer.getGravityDirection().getDirection();
            Direction prevGravityDirection = changer.getPrevGravityDirection().getDirection();
            float anomaly = 0.0f;
            if (com.example.api.GravityAnomalyZone.isActive() && changer.getGravityAnomalyStrength() > 0.0f) {
                Entity rootVehicle = this.entity.getRootVehicle();
                double interpY = net.minecraft.util.Mth.lerp((double) this.currentTickDelta, rootVehicle.yo, rootVehicle.getY());
                anomaly = com.example.api.GravityAnomalyZone.computeStrength(
                        interpY, rootVehicle.getX(), rootVehicle.getZ(), changer.getGravityDirection());
            }

            if (anomaly > 0.0f && gravityDirection == Direction.DOWN && prevGravityDirection == Direction.DOWN) {
                Quaternionf upRot = com.example.api.Gravity.UP.getRotation();
                Quaternionf partial = new Quaternionf().slerp(upRot, anomaly);
                partial.mul(this.rotation);
                this.rotation.set(partial.x(), partial.y(), partial.z(), partial.w());
                return;
            }

            if (anomaly > 0.0f && gravityDirection == Direction.UP && prevGravityDirection == Direction.UP) {
                Quaternionf upRot = com.example.api.Gravity.UP.getRotation();
                Quaternionf partial = new Quaternionf(upRot).slerp(new Quaternionf(), anomaly);
                partial.mul(this.rotation);
                this.rotation.set(partial.x(), partial.y(), partial.z(), partial.w());
                return;
            }

            if (gravityDirection == Direction.DOWN && prevGravityDirection == Direction.DOWN)
                return;

            float progress = changer.getGravityTransitionProgress(this.currentTickDelta);
            Quaternionf gravityQuat = new Quaternionf(RotationUtil
                    .getInterpolatedCameraRotationQuaternion(gravityDirection, prevGravityDirection, progress));

            gravityQuat.mul(this.rotation);
            this.rotation.set(gravityQuat.x(), gravityQuat.y(), gravityQuat.z(), gravityQuat.w());
        }
    }
}
