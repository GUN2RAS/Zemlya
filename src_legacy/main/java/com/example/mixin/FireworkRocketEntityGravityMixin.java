package com.example.mixin;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityGravityMixin extends Entity {

    @Shadow private @Nullable LivingEntity attachedToEntity;

    public FireworkRocketEntityGravityMixin(EntityType<?> type, Level world) { super(type, world); }

    @ModifyVariable(
        method = "tick()V",
        at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"),
        ordinal = 0
    )
    public Vec3 gravity$tick(Vec3 value) {
        if (attachedToEntity != null) {
            value = RotationUtil.vecWorldToPlayer(value, GravityHelper.getGravityDirection(attachedToEntity));
        }
        return value;
    }
}