package com.example.mixin;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PistonMovingBlockEntity.class)
public abstract class PistonBlockEntityGravityMixin {

    @Redirect(
        method = "moveEntityByPiston(Lnet/minecraft/core/Direction;Lnet/minecraft/world/entity/Entity;DLnet/minecraft/core/Direction;)V",
        at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/world/phys/Vec3;", ordinal = 0)
    )
    private static Vec3 gravity$moveEntity(double x, double y, double z, Direction direction, Entity entity, double d, Direction direction2) {
        Direction gd = GravityHelper.getGravityDirection(entity);
        if (gd == Direction.DOWN) return new Vec3(x, y, z);
        return RotationUtil.vecWorldToPlayer(x, y, z, gd);
    }
}