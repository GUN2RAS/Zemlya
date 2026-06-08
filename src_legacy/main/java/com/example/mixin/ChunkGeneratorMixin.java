package com.example.mixin;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorMixin {

    private static final int INVERT_BASE_Y = 300;

    @Inject(method = "applyBiomeDecoration", at = @At("RETURN"))
    private void cloneEndIslandUpsideDown(net.minecraft.world.level.WorldGenLevel world, net.minecraft.world.level.chunk.ChunkAccess chunk, net.minecraft.world.level.StructureManager structureManager,
            CallbackInfo ci) {
        // Only run for the End dimension
        if (world.getLevel().dimension() != Level.END) {
            return;
        }

        int chunkX = chunk.getPos().x();
        int chunkZ = chunk.getPos().z();

        // Roughly limit to the main End Island area (radius ~ 480 blocks = 30 chunks)
        if (Math.abs(chunkX) > 30 || Math.abs(chunkZ) > 30) {
            return;
        }

        int startX = chunkX * 16;
        int startZ = chunkZ * 16;

        BlockPos.MutableBlockPos mutableReadPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos mutableWritePos = new BlockPos.MutableBlockPos();

        // Iterate through the chunk column block by block up to Y=100 (which covers the
        // whole island)
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 100; y++) {
                    mutableReadPos.set(startX + x, y, startZ + z);

                    // Inside generateFeatures, use the chunk directly or the structure world access
                    BlockState state = chunk.getBlockState(mutableReadPos);

                    if (!state.isAir() && state.is(Blocks.END_STONE)) {
                        // Invert Y mapping: Y=0 becomes Y=300, Y=50 becomes Y=250
                        int newY = INVERT_BASE_Y - y;
                        mutableWritePos.set(startX + x, newY, startZ + z);

                        // Set the block safely in the generation context (18 = no neighbor updates to
                        // avoid cascades)
                        chunk.setBlockState(mutableWritePos, state, 18);
                    }
                }
            }
        }
    }
}
