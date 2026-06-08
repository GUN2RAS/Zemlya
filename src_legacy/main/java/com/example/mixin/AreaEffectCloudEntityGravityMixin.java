package com.example.mixin;

import com.example.util.GravityHelper;
import com.example.util.RotationUtil;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(AreaEffectCloud.class)
public abstract class AreaEffectCloudEntityGravityMixin extends Entity {

    public AreaEffectCloudEntityGravityMixin(EntityType<?> type, Level world) { super(type, world); }

    @Shadow public abstract boolean isWaiting();
    @Shadow public abstract float getRadius();

    @ModifyArgs(
        method = "clientTick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addAlwaysVisibleParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V")
    )
    private void gravity$tick_particle(Args args) {
        boolean bl = this.isWaiting();
        float f = this.getRadius();
        float g = bl ? 0.2F : f;

        float h = this.random.nextFloat() * 6.2831855F;
        float k = Mth.sqrt(this.random.nextFloat()) * g;

        double d = this.getX();
        double e = this.getY();
        double l = this.getZ();
        Vec3 modify = RotationUtil.vecWorldToPlayer(d, e, l, GravityHelper.getGravityDirection(this));
        d = modify.x + (double) (Mth.cos(h) * k);
        e = modify.y;
        l = modify.z + (double) (Mth.sin(h) * k);
        modify = RotationUtil.vecPlayerToWorld(d, e, l, GravityHelper.getGravityDirection(this));

        args.set(1, modify.x);
        args.set(2, modify.y);
        args.set(3, modify.z);
    }
}