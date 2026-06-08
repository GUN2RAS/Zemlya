package com.example.mixin.client;

import net.minecraft.client.resources.sounds.AmbientSoundHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Fixes gravity-aware eye position in ambient sound calculations.
 */
@Mixin(AmbientSoundHandler.class)
public abstract class BiomeEffectSoundPlayerMixin {

    @Shadow @Final private LocalPlayer player;

    // TODO: Re-add gravity-aware mood sound position.
}