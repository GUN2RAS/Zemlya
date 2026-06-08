package com.example.mixin;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ThrowableProjectile.class)
public abstract class ThrownEntityGravityMixin extends Entity {
    public ThrownEntityGravityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @ModifyVariable(
        method = "tick()V",
        at = @At(value = "STORE"),
        ordinal = 0
    )
    public Vec3 gravity$tick(Vec3 modify) {
        Direction gravityDirection = GravityHelper.getGravityDirection((ThrowableProjectile) (Object) this);
        if (gravityDirection == Direction.DOWN) return modify;

        modify = new Vec3(modify.x, modify.y + ((net.minecraft.world.entity.Entity)(Object)this).getGravity(), modify.z);
        modify = RotationUtil.vecWorldToPlayer(modify, gravityDirection);
        modify = new Vec3(modify.x, modify.y - ((net.minecraft.world.entity.Entity)(Object)this).getGravity(), modify.z);
        modify = RotationUtil.vecPlayerToWorld(modify, gravityDirection);
        return modify;
    }
}
