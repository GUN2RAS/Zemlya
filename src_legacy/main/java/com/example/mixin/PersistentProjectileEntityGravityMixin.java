package com.example.mixin;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(AbstractArrow.class)
public abstract class PersistentProjectileEntityGravityMixin extends Entity {
    protected PersistentProjectileEntityGravityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @ModifyVariable(
        method = "tick()V",
        at = @At(value = "STORE"),
        ordinal = 0
    )
    public Vec3 gravity$tick(Vec3 modify) {
        Direction gravityDirection = GravityHelper.getGravityDirection(this);
        if (gravityDirection == Direction.DOWN) return modify;

        modify = new Vec3(modify.x, modify.y + ((net.minecraft.world.entity.Entity)(Object)this).getGravity(), modify.z);
        modify = RotationUtil.vecWorldToPlayer(modify, gravityDirection);
        modify = new Vec3(modify.x, modify.y - ((net.minecraft.world.entity.Entity)(Object)this).getGravity(), modify.z);
        modify = RotationUtil.vecPlayerToWorld(modify, gravityDirection);
        return modify;
    }

    @ModifyArgs(
        method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/arrow/AbstractArrow;<init>(Lnet/minecraft/world/entity/EntityType;DDDLnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V"
        )
    )
    private static void gravity$init_pos(
        Args args, EntityType<?> type,
        LivingEntity owner, Level world, ItemStack itemStack, @Nullable ItemStack shotFrom
    ) {
        Direction gravityDirection = GravityHelper.getGravityDirection(owner);
        if (gravityDirection == Direction.DOWN) return;

        Vec3 pos = owner.getEyePosition().subtract(RotationUtil.vecPlayerToWorld(0.0D, 0.1F, 0.0D, gravityDirection));
        args.set(1, pos.x);
        args.set(2, pos.y);
        args.set(3, pos.z);
    }
}
