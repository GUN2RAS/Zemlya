package com.example.mixin.mob;

import com.example.util.GravityHelper;
import net.minecraft.world.entity.ai.goal.BegGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BegGoal.class)
public abstract class WolfBegGoalGravityMixin {

    @Redirect(method = "tick()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getEyeY()D", ordinal = 0))
    private double gravity$tick_getEyeY(Player player) {
        Direction gd = GravityHelper.getGravityDirection(player);
        if (gd == Direction.DOWN) return player.getEyeY();
        return player.getEyePosition().y;
    }

    @Redirect(method = "tick()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getX()D", ordinal = 0))
    private double gravity$tick_getX(Player player) {
        Direction gd = GravityHelper.getGravityDirection(player);
        if (gd == Direction.DOWN) return player.getX();
        return player.getEyePosition().x;
    }

    @Redirect(method = "tick()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getZ()D", ordinal = 0))
    private double gravity$tick_getZ(Player player) {
        Direction gd = GravityHelper.getGravityDirection(player);
        if (gd == Direction.DOWN) return player.getZ();
        return player.getEyePosition().z;
    }
}
