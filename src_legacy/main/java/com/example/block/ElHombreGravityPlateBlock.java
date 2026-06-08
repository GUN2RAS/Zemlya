package com.example.block;

import com.example.api.Gravity;
import com.example.api.GravityChanger;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

public class ElHombreGravityPlateBlock extends Block {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    protected static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);

    public ElHombreGravityPlateBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(ACTIVE, false));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        if (entity instanceof Player player) {
            if (entity instanceof GravityChanger changer && changer.getGravityDirection() != Gravity.UP) {
                changer.setGravity(Gravity.UP);
            }
        }
        if (!world.isClientSide() && !state.getValue(ACTIVE)) {
            world.setBlock(pos, state.setValue(ACTIVE, true), 3);
            world.scheduleTick(pos, this, 30);
        }
        super.entityInside(state, world, pos, entity, null, false);
    }

    protected void tick(BlockState state, ServerLevel world, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (state.getValue(ACTIVE)) {
            world.setBlock(pos, state.setValue(ACTIVE, false), 3);
        }
    }
}
