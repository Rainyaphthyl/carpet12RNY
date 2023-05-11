package carpet.utils;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
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
public class SilentChunkReader implements IBlockAccess {
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

    @Nonnull
    public IBlockState getBlockState(int x, int y, int z) throws NullPointerException {
        if (y < 0 || y >= 256) {
            return Objects.requireNonNull(Blocks.AIR).getDefaultState();
        } else {
            Chunk chunk = getChunk(x >> 4, z >> 4);
            return Objects.requireNonNull(chunk).getBlockState(x, y, z);
        }
    }

    public boolean isChunkValid(BlockPos blockPos, boolean allowRemote) {
        return blockPos != null && isChunkValid(blockPos.getX() >> 4, blockPos.getZ() >> 4, allowRemote);
    }

    @SuppressWarnings("unused")
    public boolean isChunkValid(ChunkPos chunkPos, boolean allowRemote) {
        return chunkPos != null && isChunkValid(chunkPos.x, chunkPos.z, allowRemote);
    }

    public boolean isChunkValid(int x, int z, boolean allowRemote) {
        ChunkProviderServer provider = world.getChunkProvider();
        if (provider.chunkExists(x, z)) {
            return true;
        } else if (allowRemote) {
            return chunkCache.containsKey(ChunkPos.asLong(x, z)) || provider.chunkLoader.isChunkGeneratedAt(x, z);
        } else {
            return false;
        }
    }

    @Nullable
    private Chunk getChunk(int x, int z) {
        long index = ChunkPos.asLong(x, z);
        ChunkProviderServer provider = world.getChunkProvider();
        Chunk chunk = provider.loadedChunks.get(index);
        if (chunk == null) {
            chunk = chunkCache.get(index);
            if (chunk == null && provider.chunkLoader.isChunkGeneratedAt(x, z)) {
                try {
                    chunk = provider.chunkLoader.loadChunk_silent(world, x, z);
                } catch (IOException ignored) {
                }
                if (chunk != null) {
                    chunkCache.put(index, chunk);
                }
            }
        } else if (chunkCache.containsKey(index)) {
            chunkCache.remove(index);
        }
        return chunk;
    }

    private Chunk getChunk(@Nonnull BlockPos pos) {
        return getChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public int getLightFor(EnumSkyBlock lightType, @Nonnull BlockPos pos) {
        Chunk chunk = getChunk(pos);
        if (chunk == null) {
            return lightType.defaultLightValue;
        }
        return chunk.getLightFor(lightType, pos);
    }

    public int getLight(@Nonnull BlockPos pos) {
        if (pos.getY() < 0) {
            return 0;
        } else {
            if (pos.getY() >= 256) {
                pos = new BlockPos(pos.getX(), 255, pos.getZ());
            }
            Chunk chunk = getChunk(pos);
            if (chunk == null) {
                return 0;
            }
            return chunk.getLightSubtracted(pos, 0);
        }
    }

    public float getLightBrightness(BlockPos pos) {
        return world.provider.getLightBrightnessTable()[getLightFromNeighbors(pos, true)];
    }

    public int getLightFromNeighbors(@Nonnull BlockPos pos, boolean checkNeighbors) {
        if (pos.getX() >= -30000000 && pos.getZ() >= -30000000 && pos.getX() < 30000000 && pos.getZ() < 30000000) {
            IBlockState state = getBlockState(pos);
            if (checkNeighbors && state.useNeighborBrightness()) {
                int lightUp = getLightFromNeighbors(pos.up(), false);
                int lightEast = getLightFromNeighbors(pos.east(), false);
                int lightWest = getLightFromNeighbors(pos.west(), false);
                int lightSouth = getLightFromNeighbors(pos.south(), false);
                int lightNorth = getLightFromNeighbors(pos.north(), false);
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
                return 0;
            } else {
                if (pos.getY() >= 256) {
                    pos = new BlockPos(pos.getX(), 255, pos.getZ());
                }
                Chunk chunk = getChunk(pos);
                if (chunk == null) {
                    return 15;
                }
                return chunk.getLightSubtracted(pos, world.getSkylightSubtracted());
            }
        } else {
            return 15;
        }
    }

    @Override
    @Nullable
    public TileEntity getTileEntity(BlockPos pos) {
        return null;
    }

    @Override
    @Nonnull
    public IBlockState getBlockState(@Nonnull BlockPos pos) {
        IBlockState state = BlockNull.STATE;
        try {
            state = getBlockState(pos.getX(), pos.getY(), pos.getZ());
        } catch (NullPointerException ignored) {
        }
        return state;
    }

    /**
     * Checks to see if an air block exists at the provided location. Note that this only checks to see if the blocks
     * material is set to air, meaning it is possible for non-vanilla blocks to still pass this check.
     */
    @Override
    public boolean isAirBlock(@Nonnull BlockPos pos) {
        return getBlockState(pos).getMaterial() == Material.AIR;
    }

    @Override
    @ParametersAreNonnullByDefault
    public int getStrongPower(BlockPos pos, EnumFacing direction) {
        return getBlockState(pos).getStrongPower(this, pos, direction);
    }

    /**
     * Returns the single highest strong power out of all directions using getStrongPower(BlockPos, EnumFacing)
     */
    public int getStrongPower(@Nonnull BlockPos pos) {
        int power = 0;
        power = Math.max(power, getStrongPower(pos.down(), EnumFacing.DOWN));
        if (power < 15) {
            power = Math.max(power, getStrongPower(pos.up(), EnumFacing.UP));
            if (power < 15) {
                power = Math.max(power, getStrongPower(pos.north(), EnumFacing.NORTH));
                if (power < 15) {
                    power = Math.max(power, getStrongPower(pos.south(), EnumFacing.SOUTH));
                    if (power < 15) {
                        power = Math.max(power, getStrongPower(pos.west(), EnumFacing.WEST));
                        if (power < 15) {
                            power = Math.max(power, getStrongPower(pos.east(), EnumFacing.EAST));
                        }
                    }
                }
            }
        }
        return power;
    }

    public int getRedstonePowerFromNeighbors(BlockPos pos) {
        int origin = 0;
        for (EnumFacing enumfacing : EnumFacing.values()) {
            int neighbor = getRedstonePower(pos.offset(enumfacing), enumfacing);
            if (neighbor >= 15) {
                return 15;
            }
            if (neighbor > origin) {
                origin = neighbor;
            }
        }
        return origin;
    }

    public int getRedstonePower(BlockPos pos, EnumFacing facing) {
        IBlockState state = getBlockState(pos);
        return state.isNormalCube() ? getStrongPower(pos) : state.getWeakPower(this, pos, facing);
    }
}