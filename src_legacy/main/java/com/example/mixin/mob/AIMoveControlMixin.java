package com.example.mixin.mob;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MoveControl.class)
public abstract class AIMoveControlMixin {

    @Shadow
    @Final
    protected Mob mob;
    @Shadow
    protected double wantedX;
    @Shadow
    protected double wantedY;
    @Shadow
    protected double wantedZ;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;atan2(DD)D"))
    private double gravity$redirectAtan2yaw(double dz, double dx) {
        Direction gravityDirection = GravityHelper.getGravityDirection(this.mob);
        if (gravityDirection == Direction.DOWN) {
            return Mth.atan2(dz, dx);
        }

        Vec3 diff = new Vec3(this.wantedX - this.mob.getX(), this.wantedY - this.mob.getY(),
                this.wantedZ - this.mob.getZ());
        Vec3 localDiff = RotationUtil.vecWorldToPlayer(diff, gravityDirection);
        return Mth.atan2(localDiff.z, localDiff.x);
    }

    @Inject(method = "isWalkable", at = @At("HEAD"), cancellable = true)
    private void gravity$isWalkable(float x, float z,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (GravityHelper.getGravityDirection(this.mob) != Direction.DOWN) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void gravity$tickTail(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        Direction gravityDirection = GravityHelper.getGravityDirection(this.mob);
        if (gravityDirection == Direction.DOWN) return;

        // Hack for inverted mobs: since pathfinding is a straight line, they don't know when to jump.
        // If they are trying to move but hit a wall, force them to jump.
        if (this.mob.horizontalCollision && this.mob.onGround()) {
            this.mob.getJumpControl().jump();
        }
    }
}
