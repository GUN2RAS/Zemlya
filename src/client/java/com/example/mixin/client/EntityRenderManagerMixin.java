package com.example.mixin.client;

import com.example.util.RotationUtil;
import com.example.access.EntityRenderStateExtension;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderManagerMixin {

    @Inject(method = "submit", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"))
    private <S extends EntityRenderState> void gravity$submit_beforeRender(
            S renderState, CameraRenderState cameraRenderState, double worldX, double worldY, double worldZ,
            PoseStack matrices, SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
        
        if (renderState instanceof EntityRenderStateExtension ext) {
            Direction gravityDirection = ext.gravity_getGravityDirection();
            Direction prevGravityDirection = ext.gravity_getPrevGravityDirection();
            float progress = ext.gravity_getGravityTransitionProgress();
            float anomaly = ext.gravity_getAnomalyStrength();

            if (anomaly > 0.0f && gravityDirection == Direction.DOWN && prevGravityDirection == Direction.DOWN) {
                org.joml.Quaternionf upRot = RotationUtil.getWorldRotationQuaternion(Direction.UP);
                org.joml.Quaternionf rot = new org.joml.Quaternionf().slerp(upRot, anomaly);
                matrices.mulPose(rot);
            } else if (anomaly > 0.0f && gravityDirection == Direction.UP && prevGravityDirection == Direction.UP) {
                org.joml.Quaternionf upRot = RotationUtil.getWorldRotationQuaternion(Direction.UP);
                org.joml.Quaternionf rot = new org.joml.Quaternionf(upRot).slerp(new org.joml.Quaternionf(), anomaly);
                matrices.mulPose(rot);
            } else if (gravityDirection != null && prevGravityDirection != null && (gravityDirection != Direction.DOWN || prevGravityDirection != Direction.DOWN)) {
                org.joml.Quaternionf rot = null;
                
                if (ext.gravity_isPlayer() && gravityDirection.getOpposite() == prevGravityDirection) {
                    float yaw = ext.gravity_getYRot() * ((float)Math.PI / 180F);
                    float axisX = net.minecraft.util.Mth.cos(yaw);
                    float axisZ = net.minecraft.util.Mth.sin(yaw);
                    
                    float angle = progress * (float)Math.PI;
                    org.joml.Quaternionf flipQuat = new org.joml.Quaternionf().fromAxisAngleRad(axisX, 0.0f, axisZ, angle);
                    
                    org.joml.Quaternionf base = RotationUtil.getWorldRotationQuaternion(prevGravityDirection);
                    rot = new org.joml.Quaternionf(base).mul(flipQuat).conjugate();
                } else {
                    rot = RotationUtil.getInterpolatedCameraRotationQuaternion(gravityDirection, prevGravityDirection, progress);
                }
                
                matrices.mulPose(rot);
            }
        }
    }
}
