package com.example.mixin.client;

import com.example.access.EntityRenderStateExtension;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenRendererMixin {

    /**
     * Resets the gravity rendering state for the entity being drawn in the
     * inventory screen.
     * This prevents the player model from rendering upside down or sideways when
     * affected by gravity.
     */
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private static void gravity$inventoryScreenDrawEntity(LivingEntity entity,
            CallbackInfoReturnable<EntityRenderState> cir) {
        EntityRenderState state = cir.getReturnValue();
        if (state instanceof EntityRenderStateExtension ext) {
            // Force the render state to use normal gravity for the GUI
            ext.gravity_setGravityDirection(Direction.DOWN);
            ext.gravity_setPrevGravityDirection(Direction.DOWN);
        }
    }
}