package com.example.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * DISABLED: Collision is now handled by SHA's EntityMoveMixin via pure-math Shapes.collide sweep.
 * This old mixin used Rapier KCC which created phantom walls (cobweb effect).
 */
@Mixin(Entity.class)
public abstract class EntityMoveMixin {
    // Intentionally empty — SHA handles sub-entity collision natively now.
}