package com.example.mixin.mob;

import com.example.util.GravityHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnderMan.class)
public abstract class EndermanEntityGravityMixin {

    @Redirect(method = "teleportTowards(Lnet/minecraft/world/entity/Entity;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getEyeY()D", ordinal = 0))
    private double gravity$teleportTo_getEyeY(Entity entity) {
        Direction gd = GravityHelper.getGravityDirection(entity);
        if (gd == Direction.DOWN) return entity.getEyeY();
        return entity.getEyePosition().y;
    }

    @Redirect(method = "teleportTowards(Lnet/minecraft/world/entity/Entity;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getX()D", ordinal = 0))
    private double gravity$teleportTo_getX(Entity entity) {
        Direction gd = GravityHelper.getGravityDirection(entity);
        if (gd == Direction.DOWN) return entity.getX();
        return entity.getEyePosition().x;
    }

    @Redirect(method = "teleportTowards(Lnet/minecraft/world/entity/Entity;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getZ()D", ordinal = 0))
    private double gravity$teleportTo_getZ(Entity entity) {
        Direction gd = GravityHelper.getGravityDirection(entity);
        if (gd == Direction.DOWN) return entity.getZ();
        return entity.getEyePosition().z;
    }
}
