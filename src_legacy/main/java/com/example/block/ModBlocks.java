package com.example.block;

import com.example.ExampleMod;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

import net.minecraft.world.level.block.SoundType;

public class ModBlocks {

    public static final net.minecraft.resources.Identifier GRAVITY_PLATE_ID = net.minecraft.resources.Identifier.fromNamespaceAndPath(ExampleMod.MOD_ID, "elhombre_gravity_plate");
    public static final ResourceKey<Block> GRAVITY_PLATE_BLOCK_KEY = ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, GRAVITY_PLATE_ID);
    public static final ResourceKey<Item> GRAVITY_PLATE_ITEM_KEY = ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, GRAVITY_PLATE_ID);

    public static final Block ELHOMBRE_GRAVITY_PLATE = registerBlock(GRAVITY_PLATE_ID,
            new ElHombreGravityPlateBlock(BlockBehaviour.Properties.of()
                    .setId(GRAVITY_PLATE_BLOCK_KEY)
                    .sound(SoundType.METAL)
                    .strength(2.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()),
            GRAVITY_PLATE_BLOCK_KEY, GRAVITY_PLATE_ITEM_KEY);

    private static Block registerBlock(net.minecraft.resources.Identifier id, Block block, ResourceKey<Block> blockKey,
            ResourceKey<Item> itemKey) {
        Block registeredBlock = Registry.register(BuiltInRegistries.BLOCK, id, block);
        Item registeredItem = Registry.register(BuiltInRegistries.ITEM, id,
                new BlockItem(registeredBlock, new Item.Properties().setId(itemKey)));

        return registeredBlock;
    }

    public static void registerModBlocks() {
        ExampleMod.LOGGER.info("Registering Mod Blocks for " + ExampleMod.MOD_ID);
    }
}

