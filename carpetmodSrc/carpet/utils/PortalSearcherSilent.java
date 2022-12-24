package carpet.utils;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Objects;

/**
 * run portal searching and matching without making chunks loaded
 */
public class PortalSearcherSilent {
    private final WorldServer worldIn;

    public PortalSearcherSilent(WorldServer worldIn) {
        this.worldIn = worldIn;
    }

    public PortalRange getParentFrame(BlockPos blockPos) {
        IBlockState blockState = getBlockStateSilent(blockPos);
        if (blockState == null) {
            return null;
        }
        Teleporter teleporter = new Teleporter(worldIn);
        teleporter.placeInExistingPortal(new EntityXPOrb(worldIn), 1.0f);
        return new PortalRange(0, 0, 0, 0, 0, 0);
    }

    @Nullable
    private IBlockState getBlockStateSilent(BlockPos pos) {
        try {
            return getBlockStateSilent(pos.getX(), pos.getY(), pos.getZ());
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Nonnull
    private IBlockState getBlockStateSilent(int x, int y, int z) throws NullPointerException {
        if (y < 0 || y >= 256) {
            return Objects.requireNonNull(Blocks.AIR).getDefaultState();
        } else {
            Chunk chunk = getChunkSilent(x >> 4, z >> 4);
            return Objects.requireNonNull(chunk).getBlockState(x, y, z);
        }
    }

    @Nullable
    private Chunk getChunkSilent(int x, int z) {
        Chunk chunk = null;
        ChunkProviderServer provider = worldIn.getChunkProvider();
        long index = ChunkPos.asLong(x, z);
        if (provider.loadedChunks.containsKey(index)) {
            chunk = provider.loadedChunks.get(index);
        } else if (provider.chunkLoader.isChunkGeneratedAt(x, z)) {
            try {
                chunk = provider.chunkLoader.loadChunk(worldIn, x, z, true);
            } catch (IOException ignored) {
            }
        }
        return chunk;
    }
}
