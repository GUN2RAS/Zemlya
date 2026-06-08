package com.example.item;

import com.example.api.Gravity;
import com.example.api.GravityChanger;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;

public class GravityWandItem extends Item {
    public GravityWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
        if (!user.level().isClientSide()) {
            if (entity instanceof GravityChanger changer) {
                // Если гравитация уже UP, возвращаем DOWN. Иначе ставим UP.
                if (changer.getGravityDirection() == Gravity.UP) {
                    changer.setGravity(Gravity.DOWN);
                } else {
                    changer.setGravity(Gravity.UP);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
}