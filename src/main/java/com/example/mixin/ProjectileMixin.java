package com.example.mixin;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Projectile.class)
public abstract class ProjectileMixin {

    @ModifyArgs(method = "shootFromRotation(Lnet/minecraft/world/entity/Entity;FFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;shoot(DDDFF)V"))
    private void gravity$shootFromRotation(Args args, Entity shooter, float pitch, float yaw, float roll, float velocity, float inaccuracy) {
        Direction gravityDirection = GravityHelper.getGravityDirection(shooter);
        if (gravityDirection == Direction.DOWN) return;

        double dx = args.get(0);
        double dy = args.get(1);
        double dz = args.get(2);

        net.minecraft.world.phys.Vec3 worldVec = RotationUtil.vecPlayerToWorld(dx, dy, dz, gravityDirection);

        args.set(0, worldVec.x);
        args.set(1, worldVec.y);
        args.set(2, worldVec.z);
    }
}
