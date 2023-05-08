package carpet.utils;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Objects;

/**
 * carpet12RNY feature
 * <p>
 * Only reads the chunks which have been generated.
 * <p>
 * Loads the chunks without side effects on entities or other data.
 */
public class SilentChunkReader {
    private final Long2ObjectMap<Chunk> chunkCache = new Long2ObjectOpenHashMap<>();
    private final WorldServer world;

    public SilentChunkReader(WorldServer world) {
        this.world = world;
    }

    public IBlockState getBlockState(BlockPos pos) {
        try {
            return getBlockState(pos.getX(), pos.getY(), pos.getZ());
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Nonnull
    public IBlockState getBlockState(int x, int y, int z) throws NullPointerException {
        if (y < 0 || y >= 256) {
            return Objects.requireNonNull(Blocks.AIR).getDefaultState();
        } else {
            Chunk chunk = getChunk(x >> 4, z >> 4);
            return Objects.requireNonNull(chunk).getBlockState(x, y, z);
        }
    }

    @Nullable
    private Chunk getChunk(int x, int z) {
        long index = ChunkPos.asLong(x, z);
        if (chunkCache.containsKey(index)) {
            return chunkCache.get(index);
        }
        Chunk chunk = null;
        ChunkProviderServer provider = world.getChunkProvider();
        if (provider.chunkExists(x, z)) {
            chunk = provider.loadedChunks.get(ChunkPos.asLong(x, z));
        } else if (provider.chunkLoader.isChunkGeneratedAt(x, z)) {
            try {
                chunk = provider.chunkLoader.loadChunk_silent(world, x, z);
            } catch (IOException ignored) {
            }
        }
        if (chunk != null) {
            chunkCache.put(index, chunk);
        }
        return chunk;
    }

    public void forgetChunk(int x, int z) {
        long index = ChunkPos.asLong(x, z);
        if (chunkCache.containsKey(index)) {
            chunkCache.remove(index);
        }
    }
}
