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

    // tryAttack no longer calls getYaw() in 1.21.11 — knockback is handled by
    // LivingEntity.knockbackTarget() which is covered in LivingEntityGravityMixin

    @Inject(method = "lookAt(Lnet/minecraft/world/entity/Entity;FF)V", at = @At("HEAD"), cancellable = true)
    private void gravity$lookAtEntity(Entity targetEntity, float maxYawChange, float maxPitchChange,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        Direction gravityDirection = GravityHelper.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN) {
            return;
        }

        double d = targetEntity.getX() - ((Mob) (Object) this).getX();
        double e = targetEntity.getZ() - ((Mob) (Object) this).getZ();
        double f;
        if (targetEntity instanceof LivingEntity livingEntity) {
            f = livingEntity.getEyeY() - ((Mob) (Object) this).getEyeY();
        } else {
            f = (targetEntity.getBoundingBox().minY + targetEntity.getBoundingBox().maxY) / 2.0
                    - ((Mob) (Object) this).getEyeY();
        }

        Vec3 localDiff = RotationUtil.vecWorldToPlayer(new Vec3(d, f, e), gravityDirection);

        double g = Math.sqrt(localDiff.x * localDiff.x + localDiff.z * localDiff.z);
        float targetYaw = (float) (net.minecraft.util.Mth.atan2(localDiff.z, localDiff.x)
                * 57.2957763671875) - 90.0F;
        float targetPitch = (float) (-(net.minecraft.util.Mth.atan2(localDiff.y, g) * 57.2957763671875));

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
