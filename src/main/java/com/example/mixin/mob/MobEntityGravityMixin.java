package com.example.mixin.mob;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Mob.class)
public abstract class MobEntityGravityMixin {

    @Inject(method = "lookAt(Lnet/minecraft/world/entity/Entity;FF)V", at = @At("HEAD"), cancellable = true)
    private void gravity$lookAtEntity(Entity targetEntity, float maxYawChange, float maxPitchChange,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        Vec3 targetAngles = com.example.api.physics.GravityNavigation.transformLookAngles((Mob) (Object) this, targetEntity.position(),
                targetEntity instanceof LivingEntity l ? l.getEyeY() : (targetEntity.getBoundingBox().minY + targetEntity.getBoundingBox().maxY) / 2.0);

        if (targetAngles == null) return;

        float targetYaw = (float) targetAngles.x;
        float targetPitch = (float) targetAngles.y;

        ((Mob) (Object) this)
                .setXRot(gravity$changeAngle(((Mob) (Object) this).getXRot(), targetPitch, maxPitchChange));
        ((Mob) (Object) this)
                .setYRot(gravity$changeAngle(((Mob) (Object) this).getYRot(), targetYaw, maxYawChange));

        ci.cancel();
    }

    private float gravity$changeAngle(float from, float to, float max) {
        float f = net.minecraft.util.Mth.wrapDegrees(to - from);
        if (f > max) {
            f = max;
        }
        if (f < -max) {
            f = -max;
        }
        return from + f;
    }
}
