package com.example.mixin.client;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
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

    @WrapOperation(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"))
    private void gravity$update_setPos(
            Camera camera, double x, double y, double z,
            Operation<Void> original,
            float partialTicks) {

        this.currentTickDelta = partialTicks;

        Direction gravityDirection = GravityHelper.getGravityDirection(this.entity);
        if (gravityDirection == Direction.DOWN) {
            original.call(camera, x, y, z);
            return;
        }

        // Recalculate position with gravity-aware eye offset
        double entityX = Mth.lerp((double) partialTicks, this.entity.xo, this.entity.getX());
        double entityY = Mth.lerp((double) partialTicks, this.entity.yo, this.entity.getY());
        double entityZ = Mth.lerp((double) partialTicks, this.entity.zo, this.entity.getZ());

        double currentEyeHeight = Mth.lerp(partialTicks, this.eyeHeightOld, this.eyeHeight);

        // Transform the eye offset (0, eyeHeight, 0) from player-local to world space
        Vec3 eyeOffset = RotationUtil.vecPlayerToWorld(0.0, currentEyeHeight, 0.0, gravityDirection);

        original.call(
                camera,
                entityX + eyeOffset.x,
                entityY + eyeOffset.y,
                entityZ + eyeOffset.z);
    }

    @Inject(method = "setRotation", at = @At(value = "INVOKE", target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;", shift = At.Shift.AFTER))
    private void gravity$setRotation(float yaw, float pitch, CallbackInfo ci) {
        if (com.example.util.VivecraftHelper.isVRRunning()) {
            return;
        }
        if (this.entity != null) {
            GravityChanger changer = (GravityChanger) this.entity;
            Direction gravityDirection = changer.getGravityDirection().getDirection();
            Direction prevGravityDirection = changer.getPrevGravityDirection().getDirection();

            if (gravityDirection == Direction.DOWN && prevGravityDirection == Direction.DOWN)
                return;

            // Use the interpolated quaternion for smooth camera rotation
            float progress = changer.getGravityTransitionProgress(this.currentTickDelta);
            Quaternionf gravityQuat = new Quaternionf(RotationUtil
                    .getInterpolatedCameraRotationQuaternion(gravityDirection, prevGravityDirection, progress));

            gravityQuat.mul(this.rotation);
            this.rotation.set(gravityQuat.x(), gravityQuat.y(), gravityQuat.z(), gravityQuat.w());
        }
    }
}