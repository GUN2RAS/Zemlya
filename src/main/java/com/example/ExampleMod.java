package com.example;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.LoggerFactory;

public class ExampleMod implements ModInitializer {
	public static final String MOD_ID = "zemlya";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public void onInitialize() {
		net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT
				.register(com.example.command.GravityCommand::register);

		com.example.entity.ModEntities.register();
	}
}
