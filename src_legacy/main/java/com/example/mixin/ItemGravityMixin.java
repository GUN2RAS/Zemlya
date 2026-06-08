package com.example.mixin;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Item.class, priority = 1001)
public class ItemGravityMixin {

    @WrapOperation(method = "getPlayerPOVHitResult",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getYRot()F", ordinal = 0))
    private static float gravity$raycast_getYRot(Player player, Operation<Float> original) {
        Direction gd = GravityHelper.getGravityDirection(player);
        if (gd == Direction.DOWN) return original.call(player);
        return RotationUtil.rotPlayerToWorld(original.call(player), player.getXRot(), gd).x;
    }

    @WrapOperation(method = "getPlayerPOVHitResult",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getXRot()F", ordinal = 0))
    private static float gravity$raycast_getXRot(Player player, Operation<Float> original) {
        Direction gd = GravityHelper.getGravityDirection(player);
        if (gd == Direction.DOWN) return original.call(player);
        return RotationUtil.rotPlayerToWorld(player.getYRot(), original.call(player), gd).y;
    }
}