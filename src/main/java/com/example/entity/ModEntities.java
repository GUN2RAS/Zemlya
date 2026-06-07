package com.example.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class ModEntities {


        public static final EntityType<GravityFieldEntity> GRAVITY_FIELD = Registry.register(
                        BuiltInRegistries.ENTITY_TYPE,
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("zemlya", "gravity_field"),
                        EntityType.Builder.of(GravityFieldEntity::new, MobCategory.MISC)
                                        .sized(5.0f, 5.0f)
                                        .build(ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("zemlya", "gravity_field"))));

        public static void register() {

        }
}

