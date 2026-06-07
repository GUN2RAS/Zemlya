package com.example.mixin.client;

import com.example.api.GravityChanger;
import com.example.access.EntityRenderStateExtension;
import com.example.util.GravityHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V", at = @At("TAIL"))
    private void gravity$updateRenderState(Entity entity, EntityRenderState state, float tickProgress,
            CallbackInfo ci) {
        if (state instanceof EntityRenderStateExtension ext) {
            GravityChanger changer = (GravityChanger) entity;
            Direction gravityDirection = changer.getGravityDirection().getDirection();
            ext.gravity_setGravityDirection(gravityDirection);
            ext.gravity_setPrevGravityDirection(changer.getPrevGravityDirection().getDirection());
            ext.gravity_setGravityTransitionProgress(changer.getGravityTransitionProgress(tickProgress));
            ext.gravity_setIsPlayer(entity instanceof net.minecraft.world.entity.player.Player);
            ext.gravity_setYRot(entity.getYRot());

            if (com.example.api.GravityAnomalyZone.isActive()) {
                Entity rootVehicle = entity.getRootVehicle();
                double interpY = net.minecraft.util.Mth.lerp(tickProgress, rootVehicle.yo, rootVehicle.getY());
                float anomaly = com.example.api.GravityAnomalyZone.computeStrength(
                        interpY, rootVehicle.getX(), rootVehicle.getZ(), changer.getGravityDirection());
                ext.gravity_setAnomalyStrength(anomaly);
            } else {
                ext.gravity_setAnomalyStrength(0.0f);
            }
        }
    }
}
