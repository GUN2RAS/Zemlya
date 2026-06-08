package com.example.mixin;

import com.example.util.GravityHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PointedDripstoneBlock.class)
public abstract class PointedDripstoneBlockGravityMixin {

    @WrapOperation(
        method = "fallOn",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getValue(Lnet/minecraft/world/level/block/state/properties/Property;)Ljava/lang/Comparable;", ordinal = 0)
    )
    private Comparable<Direction> gravity$onLandedUpon(BlockState blockState, Property<Direction> property, Operation<Comparable<Direction>> original,
                                                       Level world, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        Direction gd = GravityHelper.getGravityDirection(entity);
        if (gd == Direction.DOWN) return original.call(blockState, property);
        return original.call(blockState, property) == gd.getOpposite() ? Direction.UP : Direction.DOWN;
    }
}