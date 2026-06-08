package com.example.mixin;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Fix ThrowableItemProjectile constructor to use gravity-aware eye position.
 * In 1.21.11, ThrowableItemProjectile(EntityType, LivingEntity, Level, ItemStack) calls
 * this(type, owner.getX(), owner.getEyeY() - 0.1, owner.getZ(), world, stack)
 * We redirect the position to be gravity-aware.
 */
@Mixin(ThrowableItemProjectile.class)
public abstract class ThrownItemEntityGravityMixin extends Entity {
    protected ThrownItemEntityGravityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @ModifyArgs(
        method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/throwableitemprojectile/ThrowableItemProjectile;<init>(Lnet/minecraft/world/entity/EntityType;DDDLnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)V",
            ordinal = 0
        )
    )
    private static void gravity$init_pos(Args args, EntityType<?> type, LivingEntity owner, Level world, net.minecraft.world.item.ItemStack stack) {
        Direction gravityDirection = GravityHelper.getGravityDirection(owner);
        if (gravityDirection == Direction.DOWN) return;

        Vec3 pos = owner.getEyePosition().subtract(RotationUtil.vecPlayerToWorld(0.0D, 0.1D, 0.0D, gravityDirection));
        args.set(1, pos.x);
        args.set(2, pos.y);
        args.set(3, pos.z);
    }
}