package com.example.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class ModEntities {

        public static final EntityType<RiftEntity> RIFT = Registry.register(
                        BuiltInRegistries.ENTITY_TYPE,
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("nostalgia", "rift"),
                        EntityType.Builder.of(RiftEntity::new, MobCategory.MISC)
                                        .sized(40.0f, 40.0f)
                                        .build(ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("nostalgia", "rift"))));

    public static final EntityType<RiftEntity> RIFT_ENTITY = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            net.minecraft.resources.Identifier.fromNamespaceAndPath("nostalgia", "rift_entity"),
            EntityType.Builder.of(RiftEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .build(ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("nostalgia", "rift_entity")))
    );


        public static final EntityType<RiftLightningEntity> RIFT_LIGHTNING = Registry.register(
                        BuiltInRegistries.ENTITY_TYPE,
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("nostalgia", "rift_lightning"),
                        EntityType.Builder.of(RiftLightningEntity::new, MobCategory.MISC)
                                        .sized(0.0f, 0.0f)
                                        .build(ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("nostalgia", "rift_lightning"))));

        public static void register() {

        }
}

