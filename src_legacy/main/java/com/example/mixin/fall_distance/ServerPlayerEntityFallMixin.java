package com.example.mixin.fall_distance;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * In 1.21.11, handleFall(xDiff, yDiff, zDiff, onGround) is on Entity.
 * It passes yDiff directly to fall() as heightDifference.
 * For non-DOWN gravity, the real fall distance is along the gravity axis,
 * not the world Y axis. We capture all 3 diffs at HEAD and then modify
 * the yDifference parameter before it reaches fall().
 */
@Mixin(value = Entity.class, priority = 1100)
public abstract class ServerPlayerEntityFallMixin {

    @Unique
    private double gravity$localYDiff;

    @Inject(method = "doCheckFallDamage", at = @At("HEAD"))
    private void gravity$handleFall_capture(double xDifference, double yDifference, double zDifference,
            boolean onGround, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        Direction gravity = GravityHelper.getGravityDirection(self);
        if (gravity == Direction.DOWN) {
            gravity$localYDiff = yDifference;
        } else {
            Vec3 localVec = RotationUtil.vecWorldToPlayer(xDifference, yDifference, zDifference, gravity);
            gravity$localYDiff = localVec.y;
        }
    }

    @ModifyVariable(method = "doCheckFallDamage", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private double gravity$handleFall_modifyY(double yDifference) {
        return gravity$localYDiff;
    }
}
