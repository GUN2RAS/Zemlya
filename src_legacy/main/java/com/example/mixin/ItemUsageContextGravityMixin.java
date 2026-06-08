package com.example.mixin;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(UseOnContext.class)
public abstract class ItemUsageContextGravityMixin {

    @WrapOperation(method = "getRotation",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getYRot()F", ordinal = 0))
    private float gravity$getPlayerYaw(Player entity, Operation<Float> original) {
        Direction gd = GravityHelper.getGravityDirection(entity);
        if (gd == Direction.DOWN) return original.call(entity);
        return RotationUtil.rotPlayerToWorld(original.call(entity), entity.getXRot(), gd).x;
    }
}