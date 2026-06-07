package com.example.mixin.client;

import com.example.util.GravityHelper;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.BiomeAmbientSoundsHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BiomeAmbientSoundsHandler.class)
public abstract class BiomeEffectSoundPlayerMixin {

    @Shadow @Final private LocalPlayer player;
}
