package com.example.mixin.mob;

import com.example.util.GravityHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(PathNavigation.class)
public abstract class AIEntityNavigationMixin {

    @Shadow
    @Final
    protected Mob mob;

    @Inject(method = "createPath(Lnet/minecraft/world/entity/Entity;I)Lnet/minecraft/world/level/pathfinder/Path;", at = @At("HEAD"), cancellable = true)
    private void gravity$findPathToEntity(Entity target, int distance, CallbackInfoReturnable<Path> cir) {
        Node start = com.example.api.physics.GravityNavigation.calculatePathNode(this.mob, this.mob.getX(), this.mob.getY(), this.mob.getZ());
        if (start != null) {
            Node end = new Node(Mth.floor(target.getX()), Mth.floor(target.getY()), Mth.floor(target.getZ()));
            cir.setReturnValue(new Path(new ArrayList<>(List.of(start, end)), target.blockPosition(), true));
        }
    }

    @Inject(method = "createPath(Lnet/minecraft/core/BlockPos;I)Lnet/minecraft/world/level/pathfinder/Path;", at = @At("HEAD"), cancellable = true)
    private void gravity$findPathToBlock(BlockPos target, int distance, CallbackInfoReturnable<Path> cir) {
        Node start = com.example.api.physics.GravityNavigation.calculatePathNode(this.mob, this.mob.getX(), this.mob.getY(), this.mob.getZ());
        if (start != null) {
            Node end = new Node(target.getX(), target.getY(), target.getZ());
            cir.setReturnValue(new Path(new ArrayList<>(List.of(start, end)), target, true));
        }
    }
}
