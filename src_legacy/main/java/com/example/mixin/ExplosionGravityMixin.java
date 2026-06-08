package com.example.mixin;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ServerExplosion.class, priority = 1001)
public abstract class ExplosionGravityMixin {

    @WrapOperation(method = "hurtEntities", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(Lnet/minecraft/world/phys/Vec3;)V", remap = false, ordinal = 0))
    private void gravity$damageEntities_addVelocity(Entity entity, Vec3 vec3d, Operation<Void> original) {
        Direction gravityDirection = GravityHelper.getGravityDirection(entity);
        if (gravityDirection == Direction.DOWN) {
            original.call(entity, vec3d);
            return;
        }
        original.call(entity, RotationUtil.vecWorldToPlayer(vec3d, gravityDirection));
    }
}