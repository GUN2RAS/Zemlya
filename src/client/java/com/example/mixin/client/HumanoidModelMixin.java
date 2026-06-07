package com.example.mixin.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.example.access.EntityRenderStateExtension;
import net.minecraft.util.Mth;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void gravity$setupAnim(HumanoidRenderState state, CallbackInfo ci) {
        if (state instanceof EntityRenderStateExtension ext) {
            float progress = ext.gravity_getGravityTransitionProgress();
            if (progress > 0.0F && progress < 1.0F && ext.gravity_isPlayer()) {
                HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
                
                float tuckAmount = 1.0F - (2.0F * progress - 1.0F) * (2.0F * progress - 1.0F);
                
                model.rightArm.xRot = Mth.lerp(tuckAmount, model.rightArm.xRot, -1.5F);
                model.leftArm.xRot = Mth.lerp(tuckAmount, model.leftArm.xRot, -1.5F);
                model.rightLeg.xRot = Mth.lerp(tuckAmount, model.rightLeg.xRot, -1.5F);
                model.leftLeg.xRot = Mth.lerp(tuckAmount, model.leftLeg.xRot, -1.5F);
                model.rightLeg.y = Mth.lerp(tuckAmount, model.rightLeg.y, 8.0F);
                model.leftLeg.y = Mth.lerp(tuckAmount, model.leftLeg.y, 8.0F);
            }
        }
    }
}
