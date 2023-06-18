package carpet.utils;

import carpet.utils.perimeter.SpawnChecker;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.storage.WorldInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * carpet12RNY feature
 * <p>
 * Only reads the chunks which have been generated.
 * <p>
 * Loads the chunks without side effects on entities or other data.
 */
public class SilentChunkReader implements IBlockAccess {
    // TODO: 2023/5/12,0012 Use Task ChunkReader instead of Permanent ChunkReader
    private final Long2ObjectMap<Chunk> chunkCache;
    private final WorldServer world;
    private BlockPos cachedWorldSpawn = null;

    {
        chunkCache = new Long2ObjectOpenHashMap<>();
        chunkCache.defaultReturnValue(null);
    }

    public SilentChunkReader(WorldServer world) {
        this.world = world;
    }

    @SuppressWarnings("unused")
    public static long chunkAsLong(@Nonnull ChunkPos chunkPos) {
        return ChunkPos.asLong(chunkPos.x, chunkPos.z);
    }

    /**
     * Only uses the X and Z, as if using {@link ChunkPos#asLong}
     */
    public static long blockHorizonLong(@Nonnull BlockPos blockPos) {
        return ChunkPos.asLong(blockPos.getX(), blockPos.getZ());
    }

    @SuppressWarnings("unused")
    @Nonnull
    public static ChunkPos chunkFromLong(long index) {
        int x = (int) (index & 0xFFFFFFFFL);
        int z = (int) ((index >>> 32) & 0xFFFFFFFFL);
        return new ChunkPos(x, z);
    }

    @Nonnull
    public IBlockState getBlockState(int x, int y, int z) {
        try {
            if (y < 0 || y >= 256) {
                return Objects.requireNonNull(Blocks.AIR).getDefaultState();
            } else {
                Chunk chunk = getChunk(x >> 4, z >> 4);
                return Objects.requireNonNull(chunk).getBlockState(x, y, z);
            }
        } catch (NullPointerException ignored) {
        }
        return BlockNull.STATE;
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

    @Nullable
    public Chunk getChunk(@Nonnull BlockPos blockPos) {
        return getChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4);
    }

    @Nullable
    public Chunk getChunk(@Nonnull ChunkPos chunkPos) {
        return getChunk(chunkPos.x, chunkPos.z);
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
    public TileEntity getTileEntity(@Nonnull BlockPos pos) {
        // get tile entities only from real chunks
        if (isChunkValid(pos, false)) {
            return world.getTileEntity(pos);
        } else {
            return null;
        }
    }

    @Override
    @Nonnull
    public IBlockState getBlockState(@Nonnull BlockPos pos) {
        return getBlockState(pos.getX(), pos.getY(), pos.getZ());
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

    public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos blockPos) {
        Biome biome = getBiome(blockPos);
        List<Biome.SpawnListEntry> entryList = biome == null ? Collections.emptyList() : biome.getSpawnableList(creatureType);
        // structure check for dimensions...
        IChunkGenerator generator = world.getChunkProvider().chunkGenerator;
        if (generator instanceof ChunkGeneratorOverworld) {
        }
        return entryList;
    }

    @Nullable
    public Biome getBiome(final BlockPos pos) {
        Chunk chunk = getChunk(pos);
        Biome biome = null;
        if (chunk != null) {
            int i = pos.getX() & 15;
            int j = pos.getZ() & 15;
            byte[] biomeArray = chunk.getBiomeArray();
            int k = biomeArray[j << 4 | i] & 255;
            biome = Biome.getBiome(k);
        }
        return biome;
    }

    /**
     * @return the spawn point in the world
     */
    public BlockPos getSpawnPoint() {
        if (cachedWorldSpawn == null) {
            WorldInfo worldInfo = world.getWorldInfo();
            BlockPos posWorldSpawn = new BlockPos(worldInfo.getSpawnX(), worldInfo.getSpawnY(), worldInfo.getSpawnZ());
            WorldBorder worldBorder = world.getWorldBorder();
            if (!worldBorder.contains(posWorldSpawn)) {
                int centerX = MathHelper.floor(worldBorder.getCenterX());
                int centerZ = MathHelper.floor(worldBorder.getCenterZ());
                posWorldSpawn = new BlockPos(centerX, getHeight(centerX, centerZ), centerZ);
            }
            cachedWorldSpawn = posWorldSpawn;
        }
        return cachedWorldSpawn;
    }

    /**
     * {@link net.minecraft.world.World#getHeight}
     *
     * @return from the height map, the height of the highest block at this x and z coordinate.
     */
    public int getHeight(int blockX, int blockZ) {
        int height;
        if (blockX >= -30000000 && blockZ >= -30000000 && blockX < 30000000 && blockZ < 30000000) {
            Chunk chunk = getChunk(blockX >> 4, blockZ >> 4);
            if (chunk == null) {
                height = 0;
            } else {
                height = chunk.getHeightValue(blockX & 15, blockZ & 15);
            }
        } else {
            height = world.getSeaLevel() + 1;
        }
        return height;
    }

    public boolean canBlockSeeSky(@Nonnull BlockPos pos) {
        int targetX = pos.getX();
        int targetZ = pos.getZ();
        int targetY = pos.getY();
        int seaLevel = world.getSeaLevel();
        if (targetY >= seaLevel) {
            return canSeeSky(pos);
        } else {
            BlockPos seaPos = new BlockPos(targetX, seaLevel, targetZ);
            if (!canSeeSky(seaPos)) {
                return false;
            } else {
                for (int y = seaLevel - 1; y > targetY; --y) {
                    IBlockState iblockstate = getBlockState(targetX, y, targetZ);
                    if (iblockstate.getLightOpacity() > 0 && !iblockstate.getMaterial().isLiquid()) {
                        return false;
                    }
                }
                return true;
            }
        }
    }

    public boolean canSeeSky(@Nonnull BlockPos pos) {
        Chunk chunk = getChunk(pos);
        return chunk != null && chunk.canSeeSky(pos);
    }

    /**
     * Checks if any of the blocks within the aabb are liquids.
     */
    public boolean containsAnyLiquid(@Nonnull AxisAlignedBB bb) {
        int minX = MathHelper.floor(bb.minX);
        int maxX = MathHelper.ceil(bb.maxX);
        int minY = MathHelper.floor(bb.minY);
        int maxY = MathHelper.ceil(bb.maxY);
        int minZ = MathHelper.floor(bb.minZ);
        int maxZ = MathHelper.ceil(bb.maxZ);
        for (int x = minX; x < maxX; ++x) {
            for (int y = minY; y < maxY; ++y) {
                for (int z = minZ; z < maxZ; ++z) {
                    IBlockState state = getBlockState(x, y, z);
                    if (state.getMaterial().isLiquid()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * @return {@code true} if the AABB list is not empty
     */
    public boolean optimizedGetCollisionBoxes(@Nonnull AxisAlignedBB aabb, boolean strictCheck, @Nullable List<AxisAlignedBB> outList) {
        final int startX = MathHelper.floor(aabb.minX) - 1;
        final int endX = MathHelper.ceil(aabb.maxX) + 1;
        final int startY = MathHelper.floor(aabb.minY) - 1;
        final int endY = MathHelper.ceil(aabb.maxY) + 1;
        final int startZ = MathHelper.floor(aabb.minZ) - 1;
        final int endZ = MathHelper.ceil(aabb.maxZ) + 1;
        BlockPos.PooledMutableBlockPos posMutable = BlockPos.PooledMutableBlockPos.retain();
        if (outList == null) {
            outList = new ArrayList<>();
        }
        try {
            final int chunkStartX = (startX >> 4);
            final int chunkStartZ = (startZ >> 4);
            final int chunkEndX = (endX >> 4);
            final int chunkEndZ = (endZ >> 4);
            final int yMin = Math.max(0, startY);
            for (int cx = chunkStartX; cx <= chunkEndX; cx++) {
                for (int cz = chunkStartZ; cz <= chunkEndZ; cz++) {
                    Chunk chunk = getChunk(cx, cz);
                    if (chunk != null) {
                        final int xMin = Math.max(cx << 4, startX);
                        final int zMin = Math.max(cz << 4, startZ);
                        final int xMax = Math.min((cx << 4) + 15, endX - 1);
                        final int zMax = Math.min((cz << 4) + 15, endZ - 1);
                        final int yMax = Math.min(chunk.getTopFilledSegment() + 15, endY - 1);
                        for (int x = xMin; x <= xMax; ++x) {
                            for (int z = zMin; z <= zMax; ++z) {
                                boolean xIsEdge = x == startX || x == endX - 1;
                                boolean zIsEdge = z == startZ || z == endZ - 1;
                                if (!xIsEdge || !zIsEdge) {
                                    for (int y = yMin; y <= yMax; ++y) {
                                        if (!xIsEdge && !zIsEdge || y != endY - 1) {
                                            if (strictCheck) {
                                                if (x < -30000000 || x >= 30000000 || z < -30000000 || z >= 30000000) {
                                                    return true;
                                                }
                                            }
                                            posMutable.setPos(x, y, z);
                                            IBlockState state = chunk.getBlockState(posMutable);
                                            state.addCollisionBoxToList_silent(this, posMutable.toImmutable(), aabb, outList, null, false);
                                            if (strictCheck && !outList.isEmpty()) {
                                                return true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            posMutable.release();
        }
        return !outList.isEmpty();
    }

    @ParametersAreNonnullByDefault
    public boolean isCreaturePlaceable(BlockPos posTarget, EntityLiving.SpawnPlacementType placementType) {
        IBlockState stateDown = getBlockState(posTarget.getX(), posTarget.getY() - 1, posTarget.getZ());
        IBlockState stateTarget = getBlockState(posTarget);
        IBlockState stateUp = getBlockState(posTarget.getX(), posTarget.getY() + 1, posTarget.getZ());
        return SpawnChecker.isEntityPlaceable(placementType, stateDown, stateTarget, stateUp);
    }
}
