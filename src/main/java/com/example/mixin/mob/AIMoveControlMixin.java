package com.example.mixin.mob;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
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
        return com.example.api.physics.GravityNavigation.transformMoveAngles(this.mob, this.wantedX, this.wantedY, this.wantedZ, dz, dx);
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

        if (this.mob.horizontalCollision && this.mob.onGround()) {
            this.mob.getJumpControl().jump();
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    private VoxelShape gravity$redirectCollisionShape(BlockState state, BlockGetter level, BlockPos pos) {
        Direction gd = GravityHelper.getGravityDirection(this.mob);
        if (gd != Direction.DOWN) {
            return Shapes.empty();
        }
        return state.getCollisionShape(level, pos);
    }
}
