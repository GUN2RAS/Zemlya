package com.example.mixin;

import com.example.entity.ModEntities;
import com.example.entity.RiftEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragon.class)
public abstract class EnderDragonEntityMixin {

    @Shadow
    public int dragonDeathTime;

    @Inject(method = "tickDeath", at = @At("RETURN"))
    private void spawnRiftOnDeath(CallbackInfo ci) {
        if (this.dragonDeathTime == 200) {
            EnderDragon dragon = (EnderDragon) (Object) this;
            if (dragon.level() instanceof ServerLevel serverWorld) {
                RiftEntity rift = new RiftEntity(ModEntities.RIFT, serverWorld);
                rift.setPos(0, 220, 0);
                serverWorld.addFreshEntity(rift);
            }
        }
    }
}