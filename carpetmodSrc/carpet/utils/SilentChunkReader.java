package carpet.utils;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
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
    // TODO: 2023/5/11,0011 implements IBlockAccess
    private final Long2ObjectMap<Chunk> chunkCache;
    private final WorldServer world;

    {
        chunkCache = new Long2ObjectOpenHashMap<>();
        chunkCache.defaultReturnValue(null);
    }

    public SilentChunkReader(WorldServer world) {
        this.world = world;
    }

    public IBlockState getBlockState(BlockPos pos, boolean allowRemote) {
        try {
            return getBlockState(pos.getX(), pos.getY(), pos.getZ(), allowRemote);
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Nonnull
    public IBlockState getBlockState(int x, int y, int z, boolean allowRemote) throws NullPointerException {
        if (y < 0 || y >= 256) {
            return Objects.requireNonNull(Blocks.AIR).getDefaultState();
        } else {
            Chunk chunk = getChunk(x >> 4, z >> 4, allowRemote);
            return Objects.requireNonNull(chunk).getBlockState(x, y, z);
        }
    }

    public boolean isBlockInValidChunk(BlockPos pos, boolean allowRemote) {
        return pos != null && isChunkValid(pos.getX() >> 4, pos.getZ() >> 4, allowRemote);
    }

    public boolean isChunkValid(int x, int z, boolean allowRemote) {
        ChunkProviderServer provider = world.getChunkProvider();
        return provider.chunkExists(x, z) || (allowRemote && provider.chunkLoader.isChunkGeneratedAt(x, z));
    }

    @Nullable
    private Chunk getChunk(int x, int z, boolean allowRemote) {
        long index = ChunkPos.asLong(x, z);
        Chunk chunk = null;
        ChunkProviderServer provider = world.getChunkProvider();
        if (provider.chunkExists(x, z)) {
            chunk = provider.loadedChunks.get(index);
            if (chunkCache.containsKey(index)) {
                chunkCache.remove(index);
            }
        } else if (allowRemote && provider.chunkLoader.isChunkGeneratedAt(x, z)) {
            chunk = chunkCache.get(index);
            if (chunk == null) {
                try {
                    chunk = provider.chunkLoader.loadChunk_silent(world, x, z);
                } catch (IOException ignored) {
                }
                if (chunk != null) {
                    chunkCache.put(index, chunk);
                }
            }
        }
        return chunk;
    }

    @ParametersAreNonnullByDefault
    private Chunk getChunk(BlockPos pos, boolean allowRemote) {
        return getChunk(pos.getX() >> 4, pos.getZ() >> 4, allowRemote);
    }

    @ParametersAreNonnullByDefault
    public int getLightFor(EnumSkyBlock lightType, BlockPos pos, boolean allowRemote) {
        Chunk chunk = getChunk(pos.getX() >> 4, pos.getZ() >> 4, allowRemote);
        if (chunk == null) {
            return lightType.defaultLightValue;
        }
        return chunk.getLightFor(lightType, pos);
    }

    @ParametersAreNonnullByDefault
    public int getLight(BlockPos pos, boolean allowRemote) {
        if (pos.getY() < 0) {
            return 0;
        } else {
            if (pos.getY() >= 256) {
                pos = new BlockPos(pos.getX(), 255, pos.getZ());
            }
            Chunk chunk = getChunk(pos.getX() >> 4, pos.getZ() >> 4, allowRemote);
            if (chunk == null) {
                return 0;
            }
            return chunk.getLightSubtracted(pos, 0);
        }
    }

    public float getLightBrightness(BlockPos pos, boolean allowRemote) {
        return world.provider.getLightBrightnessTable()[getLightFromNeighbors(pos, true, allowRemote)];
    }

    @ParametersAreNonnullByDefault
    public int getLightFromNeighbors(BlockPos pos, boolean checkNeighbors, boolean allowRemote) {
        final int UNLOADED = 15;
        final int INVALID = 0;
        if (pos.getX() >= -30000000 && pos.getZ() >= -30000000 && pos.getX() < 30000000 && pos.getZ() < 30000000) {
            IBlockState state = getBlockState(pos, allowRemote);
            if (state == null) {
                return UNLOADED;
            }
            if (checkNeighbors && state.useNeighborBrightness()) {
                int lightUp = getLightFromNeighbors(pos.up(), false, allowRemote);
                int lightEast = getLightFromNeighbors(pos.east(), false, allowRemote);
                int lightWest = getLightFromNeighbors(pos.west(), false, allowRemote);
                int lightSouth = getLightFromNeighbors(pos.south(), false, allowRemote);
                int lightNorth = getLightFromNeighbors(pos.north(), false, allowRemote);
                if (lightEast > lightUp) {
                    lightUp = lightEast;
                }
                if (lightWest > lightUp) {
                    lightUp = lightWest;
                }
                if (lightSouth > lightUp) {
                    lightUp = lightSouth;
                }
                if (lightNorth > lightUp) {
                    lightUp = lightNorth;
                }
                return lightUp;
            } else if (pos.getY() < 0) {
                return INVALID;
            } else {
                if (pos.getY() >= 256) {
                    pos = new BlockPos(pos.getX(), 255, pos.getZ());
                }
                Chunk chunk = getChunk(pos, allowRemote);
                if (chunk == null) {
                    return UNLOADED;
                }
                return chunk.getLightSubtracted(pos, world.getSkylightSubtracted());
            }
        } else {
            return UNLOADED;
        }
    }

    public int getStrongPower(BlockPos pos, EnumFacing direction, boolean allowRemote) throws NullPointerException {
        // Block Access might be null
        IBlockState blockState = getBlockState(pos, allowRemote);
        IBlockAccess blockAccess = isBlockInValidChunk(pos, allowRemote) ? world : null;
        return blockState.getStrongPower(blockAccess, pos, direction);
    }

    public int getStrongPower(@Nonnull BlockPos pos, boolean allowRemote) {
        int currMax = 0;
        currMax = Math.max(currMax, getStrongPower(pos.down(), EnumFacing.DOWN, allowRemote));
        if (currMax < 15) {
            currMax = Math.max(currMax, getStrongPower(pos.up(), EnumFacing.UP, allowRemote));
            if (currMax < 15) {
                currMax = Math.max(currMax, getStrongPower(pos.north(), EnumFacing.NORTH, allowRemote));
                if (currMax < 15) {
                    currMax = Math.max(currMax, getStrongPower(pos.south(), EnumFacing.SOUTH, allowRemote));
                    if (currMax < 15) {
                        currMax = Math.max(currMax, getStrongPower(pos.west(), EnumFacing.WEST, allowRemote));
                        if (currMax < 15) {
                            currMax = Math.max(currMax, getStrongPower(pos.east(), EnumFacing.EAST, allowRemote));
                        }
                    }
                }
            }
        }
        return currMax;
    }

}
