package com.example.mixin.client;

import com.example.api.GravityChanger;
import com.example.access.EntityRenderStateExtension;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects into EntityRenderer.extractRenderState to store gravity direction in
 * the render state.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void gravity$updateRenderState(Entity entity, EntityRenderState state, float tickProgress,
            CallbackInfo ci) {
        if (state instanceof EntityRenderStateExtension ext) {
            GravityChanger changer = (GravityChanger) entity;
            Direction gravityDirection = changer.getGravityDirection().getDirection();
            ext.gravity_setGravityDirection(gravityDirection);
            ext.gravity_setPrevGravityDirection(changer.getPrevGravityDirection().getDirection());
            ext.gravity_setGravityTransitionProgress(changer.getGravityTransitionProgress(tickProgress));
        }
    }
}