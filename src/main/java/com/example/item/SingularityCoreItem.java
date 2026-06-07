package com.example.item;

import com.example.api.Gravity;
import com.example.api.GravityChanger;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand; 
import net.minecraft.world.InteractionResult;

import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class SingularityCoreItem extends Item {
    public SingularityCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        
        if (!world.isClientSide()) {
            if (user.isCrouching()) {
                if (user instanceof GravityChanger changer) {
                    changer.setGravity(changer.getGravityDirection() == Gravity.UP ? Gravity.DOWN : Gravity.UP);
                }
            } else {
                double maxDistance = 30.0;
                Vec3 startPos = user.getEyePosition();
                Vec3 lookVec = user.getViewVector(1.0f);
                Vec3 endPos = startPos.add(lookVec.scale(maxDistance));
                
                AABB box = user.getBoundingBox().expandTowards(lookVec.scale(maxDistance)).inflate(1.0, 1.0, 1.0);
                
                Entity hitEntity = null;
                double closestDist = maxDistance;
                
                for (Entity entity : world.getEntities(user, box, e -> e instanceof LivingEntity && !e.isSpectator())) {
                    AABB entityBox = entity.getBoundingBox().inflate(0.3f);
                    Optional<Vec3> hitPos = entityBox.clip(startPos, endPos);
                    if (hitPos.isPresent()) {
                        double dist = startPos.distanceTo(hitPos.get());
                        if (dist < closestDist) {
                            closestDist = dist;
                            hitEntity = entity;
                        }
                    }
                }
                
                if (hitEntity != null && hitEntity instanceof GravityChanger changer) {
                    changer.setGravity(Gravity.UP);

                }
                
                if (world instanceof ServerLevel serverWorld) {
                    serverWorld.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, 
                            startPos.x, startPos.y, startPos.z, 20, lookVec.x, lookVec.y, lookVec.z, 0.5);
                }
            }
        }
        
        return InteractionResult.SUCCESS;
    }
}
