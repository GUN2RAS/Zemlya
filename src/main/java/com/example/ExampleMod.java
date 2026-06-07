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
	public static final String MOD_ID = "zemlya";

	public static final ResourceKey<Item> WAND_KEY = ResourceKey.create(net.minecraft.core.registries.Registries.ITEM,
			net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "gravity_wand"));
	public static final Item GRAVITY_WAND = new GravityWandItem(new Item.Properties().setId(WAND_KEY));

	public static final ResourceKey<Item> CORE_KEY = ResourceKey.create(net.minecraft.core.registries.Registries.ITEM,
			net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "singularity_core"));
	public static final Item SINGULARITY_CORE = new com.example.item.SingularityCoreItem(new Item.Properties().setId(CORE_KEY));

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final ResourceKey<net.minecraft.world.item.CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "items"));
	public static final net.minecraft.world.item.CreativeModeTab ZEMLYA_TAB = net.minecraft.world.item.CreativeModeTab.builder(net.minecraft.world.item.CreativeModeTab.Row.TOP, 7)
			.title(net.minecraft.network.chat.Component.literal("Zemlya"))
			.icon(() -> new net.minecraft.world.item.ItemStack(GRAVITY_WAND))
			.displayItems((params, output) -> {
				output.accept(GRAVITY_WAND);
				output.accept(SINGULARITY_CORE);
			}).build();

	public void onInitialize() {

		Registry.register(BuiltInRegistries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "gravity_wand"), GRAVITY_WAND);
		Registry.register(BuiltInRegistries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "singularity_core"), SINGULARITY_CORE);
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, ZEMLYA_TAB);

		net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT
				.register(com.example.command.GravityCommand::register);
				

		
		com.example.entity.ModEntities.register();
	}

}
