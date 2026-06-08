package com.example.mixin.client;

import com.example.util.RotationUtil;
import com.example.access.EntityRenderStateExtension;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;

/**
 * Applies gravity rotation to entity rendering targeting the new decoupled rendering pipeline.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderManagerMixin {

    @Inject(method = "submit", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$SubmitNodeCollector;Lnet/minecraft/client/render/state/CameraRenderState;)V"))
    private <S extends EntityRenderState> void gravity$submit_beforeSubmit(
            S renderState, CameraRenderState camera, double x, double y, double z,
            PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
        if (renderState instanceof EntityRenderStateExtension ext) {
            Direction gravityDirection = ext.gravity_getGravityDirection();
            Direction prevGravityDirection = ext.gravity_getPrevGravityDirection();
            float progress = ext.gravity_getGravityTransitionProgress();

            if (gravityDirection != Direction.DOWN || prevGravityDirection != Direction.DOWN) {
                poseStack.mulPose(RotationUtil.getInterpolatedCameraRotationQuaternion(gravityDirection,
                        prevGravityDirection, progress));
            }
        }
    }
}
