package com.example.mixin.mob;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(LookControl.class)
public abstract class AILookControlMixin {
    @Shadow
    @Final
    protected Mob mob;
    @Shadow
    protected double wantedX;
    @Shadow
    protected double wantedY;
    @Shadow
    protected double wantedZ;

    @Inject(method = "getYRotD", at = @At("HEAD"), cancellable = true)
    private void gravity$getYRotD(CallbackInfoReturnable<Optional<Float>> cir) {
        Vec3 angles = com.example.api.physics.GravityNavigation.transformLookAngles(this.mob, new Vec3(this.wantedX, 0, this.wantedZ), this.wantedY);
        if (angles != null) {
            Direction gravityDirection = GravityHelper.getGravityDirection(this.mob);
            Vec3 diff = new Vec3(this.wantedX - this.mob.getX(), this.wantedY - this.mob.getEyeY(), this.wantedZ - this.mob.getZ());
            Vec3 localDiff = RotationUtil.vecWorldToPlayer(diff, gravityDirection);
            
            if (Math.abs(localDiff.z) <= 1.0E-5 && Math.abs(localDiff.x) <= 1.0E-5) {
                cir.setReturnValue(Optional.empty());
            } else {
                cir.setReturnValue(Optional.of((float) angles.x));
            }
        }
    }

    @Inject(method = "getXRotD", at = @At("HEAD"), cancellable = true)
    private void gravity$getXRotD(CallbackInfoReturnable<Optional<Float>> cir) {
        Vec3 angles = com.example.api.physics.GravityNavigation.transformLookAngles(this.mob, new Vec3(this.wantedX, 0, this.wantedZ), this.wantedY);
        if (angles != null) {
            Direction gravityDirection = GravityHelper.getGravityDirection(this.mob);
            Vec3 diff = new Vec3(this.wantedX - this.mob.getX(), this.wantedY - this.mob.getEyeY(), this.wantedZ - this.mob.getZ());
            Vec3 localDiff = RotationUtil.vecWorldToPlayer(diff, gravityDirection);
            double horizontalDist = Math.sqrt(localDiff.x * localDiff.x + localDiff.z * localDiff.z);

            if (Math.abs(localDiff.y) <= 1.0E-5 && horizontalDist <= 1.0E-5) {
                cir.setReturnValue(Optional.empty());
            } else {
                cir.setReturnValue(Optional.of((float) angles.y));
            }
        }
    }
}
