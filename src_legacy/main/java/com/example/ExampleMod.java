package com.example;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import com.example.item.GravityWandItem;


import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.LoggerFactory;

public class ExampleMod implements ModInitializer {
	public static final String MOD_ID = "modid";

	public static final ResourceKey<Item> WAND_KEY = ResourceKey.create(Registries.ITEM,
			net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "gravity_wand"));
	public static final Item GRAVITY_WAND = new GravityWandItem(new Item.Properties().setId(WAND_KEY));

	public static final ResourceKey<Item> CORE_KEY = ResourceKey.create(Registries.ITEM,
			net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "singularity_core"));
	public static final Item SINGULARITY_CORE = new com.example.item.SingularityCoreItem(new Item.Properties().setId(CORE_KEY));

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public void onInitialize() {
		LOGGER.info("Hello Fabric world! Adding Gravity Wand...");

		Registry.register(BuiltInRegistries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "gravity_wand"), GRAVITY_WAND);
		Registry.register(BuiltInRegistries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "singularity_core"), SINGULARITY_CORE);

		net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT
				.register(com.example.command.GravityCommand::register);
				

		
		com.example.entity.ModEntities.register();
	}

}
