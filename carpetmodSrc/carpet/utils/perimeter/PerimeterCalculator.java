package carpet.utils.perimeter;

import carpet.CarpetServer;
import carpet.utils.Messenger;
import carpet.utils.SilentChunkReader;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.util.HttpUtil;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * {@link net.minecraft.world.WorldEntitySpawner#findChunksForSpawning}
 */
public class PerimeterCalculator implements Runnable {
    private static final int SECTION_UNIT = 16;
    private static final int yMin = 0;
    private static final int yMax = 255;
    private final Class<? extends EntityLiving> entityType;
    private final WorldServer worldServer;
    private final Vec3d center;
    private Long2IntMap eligibleChunkHeightMap = null;
    private AxisAlignedBB alignedBB = null;
    private Long2BooleanMap biomeAllowanceCache = null;
    private Long2ObjectMap<PerimeterResult.EnumDistLevel> spawnAllowanceCache = null;
    private EnumCreatureType creatureType = null;
    private EntityLiving.SpawnPlacementType placementType = null;
    private SilentChunkReader reader = null;
    private PerimeterResult result = null;
    private BlockPos worldSpawnPoint = null;
    private int xMin = 0;
    private int xMax = 0;
    private int zMin = 0;
    private int zMax = 0;

    private PerimeterCalculator(WorldServer worldServer, Vec3d center, Class<? extends EntityLiving> entityType) {
        this.worldServer = worldServer;
        this.center = center;
        this.entityType = entityType;
    }

    public static void asyncSearch(World world, Vec3d center, Class<? extends EntityLiving> entityType) {
        if (world instanceof WorldServer) {
            Messenger.print_server_message(world.getMinecraftServer(), "Start checking perimeter ...");
            PerimeterCalculator calculator = new PerimeterCalculator((WorldServer) world, center, entityType);
            HttpUtil.DOWNLOADER_EXECUTOR.submit(calculator);
        }
    }

    public static EnumCreatureType checkCreatureType(Class<? extends EntityLiving> entityType) {
        if (entityType == null) {
            return null;
        }
        EnumCreatureType creatureType = EnumCreatureType.MONSTER;
        if (EntityAnimal.class.isAssignableFrom(entityType)) {
            creatureType = EnumCreatureType.CREATURE;
        } else if (EntityWaterMob.class.isAssignableFrom(entityType)) {
            creatureType = EnumCreatureType.WATER_CREATURE;
        } else if (EntityAmbientCreature.class.isAssignableFrom(entityType)) {
            creatureType = EnumCreatureType.AMBIENT;
        }
        return creatureType;
    }

    private void initSpawningRange() {
        int chunkX = MathHelper.floor(center.x / 16.0);
        int chunkZ = MathHelper.floor(center.z / 16.0);
        // checking player chunk map
        int radius = Math.min(CarpetServer.minecraft_server.getPlayerList().getViewDistance(), 7);
        int expand = 20;
        WorldBorder worldBorder = worldServer.getWorldBorder();
        int worldLimit = worldBorder.getSize();
        xMin = MathHelper.clamp(((chunkX - radius) << 4) - expand, -worldLimit, worldLimit);
        xMax = MathHelper.clamp(((chunkX + radius) >> 4) + 15 + expand, -worldLimit, worldLimit);
        zMin = MathHelper.clamp(((chunkZ - radius) << 4) - expand, -worldLimit, worldLimit);
        zMax = MathHelper.clamp(((chunkZ + radius) >> 4) + 15 + expand, -worldLimit, worldLimit);
    }

    /**
     * Select a random block position in the chunk
     */
    private void forEachChunkPosition(@Nonnull ChunkPos chunkPos, Consumer<BlockPos> consumer) {
        final int originX = chunkPos.x * 16;
        final int originZ = chunkPos.z * 16;
        Chunk chunk = reader.getChunk(chunkPos);
        int height = MathHelper.roundUp(chunk.getHeight(new BlockPos(originX + 8, 0, originZ + 8)) + 1, 16);
        if (height <= 0) {
            height = chunk.getTopFilledSegment() + 15;
        }
        BlockPos.MutableBlockPos posIter = new BlockPos.MutableBlockPos();
        for (int y = 0; y < height; ++y) {
            for (int dx = 0; dx < 16; ++dx) {
                final int x = originX + dx;
                for (int dz = 0; dz < 16; ++dz) {
                    final int z = originZ + dz;
                    posIter.setPos(x, y, z);
                    consumer.accept(posIter);
                }
            }
        }
    }

    public int checkChunkHeight(@Nonnull ChunkPos chunkPos) {
        final int originX = chunkPos.x * 16;
        final int originZ = chunkPos.z * 16;
        Chunk chunk = reader.getChunk(chunkPos);
        int height = MathHelper.roundUp(chunk.getHeight(new BlockPos(originX + 8, 0, originZ + 8)) + 1, SECTION_UNIT);
        if (height <= 0) {
            height = chunk.getTopFilledSegment() + (SECTION_UNIT - 1);
        }
        return height;
    }

    /**
     * Wandering from [-5, 0, -5] to [+5, 0, +5] for one round
     */
    private void forEachWanderingSpawn(@Nonnull BlockPos posBegin, int rounds, Consumer<BlockPos> consumer) {
        if (rounds <= 0) {
            return;
        }
        BlockPos.MutableBlockPos posTarget = new BlockPos.MutableBlockPos(posBegin);
        int originX = posBegin.getX();
        int originY = posBegin.getY();
        int originZ = posBegin.getZ();
        // positive & negative
        for (int dxp = 0; dxp < 6; ++dxp) {
            for (int dxn = 0; dxn < 6; ++dxn) {
                // dyp or dzp is always 0, ignored
                for (int dzp = 0; dzp < 6; ++dzp) {
                    for (int dzn = 0; dzn < 6; ++dzn) {
                        posTarget.setPos(originX + dxp - dxn, originY, originZ + dzp - dzn);
                        consumer.accept(posTarget);
                    }
                }
            }
        }
        if (rounds > 1) {
            --rounds;
            for (int dxp = 0; dxp < 6; ++dxp) {
                for (int dxn = 0; dxn < 6; ++dxn) {
                    // dyp or dzp is always 0, ignored
                    for (int dzp = 0; dzp < 6; ++dzp) {
                        for (int dzn = 0; dzn < 6; ++dzn) {
                            posTarget.setPos(originX + dxp - dxn, originY, originZ + dzp - dzn);
                            forEachWanderingSpawn(posBegin, rounds, consumer);
                        }
                    }
                }
            }
        }
    }

    /**
     * The 24-meter check for the world spawn point and the closest player
     */
    private boolean isSpawnAllowed(double mobX, double mobY, double mobZ) {
        boolean valid = center.squareDistanceTo(mobX, mobY, mobZ) >= 576.0;
        if (valid) {
            if (worldSpawnPoint == null) {
                worldSpawnPoint = reader.getSpawnPoint();
            }
            valid = worldSpawnPoint.distanceSq(mobX, mobY, mobZ) >= 576.0;
        }
        return valid;
    }

    /**
     * The 24-meter check for the world spawn point and the closest player
     */
    private PerimeterResult.EnumDistLevel getDistLevelTo(@Nonnull BlockPos posTarget) {
        long index = posTarget.toLong();
        if (spawnAllowanceCache.containsKey(index)) {
            return spawnAllowanceCache.get(index);
        }
        float mobX = (float) posTarget.getX() + 0.5F;
        float mobZ = (float) posTarget.getZ() + 0.5F;
        int mobY = posTarget.getY();
        PerimeterResult.EnumDistLevel level;
        if (worldSpawnPoint == null) {
            worldSpawnPoint = reader.getSpawnPoint();
        }
        if (worldSpawnPoint.distanceSq(mobX, mobY, mobZ) >= 576.0) {
            double distSq = center.squareDistanceTo(mobX, mobY, mobZ);
            level = PerimeterResult.EnumDistLevel.getLevelOfDistSq(distSq);
        } else {
            level = PerimeterResult.EnumDistLevel.CLOSE;
        }
        spawnAllowanceCache.put(index, level);
        return level;
    }

    private boolean isSpawnBiomeAllowing(@Nonnull BlockPos posTarget) {
        long index = SilentChunkReader.blockHorizonLong(posTarget);
        if (biomeAllowanceCache.containsKey(index)) {
            return biomeAllowanceCache.get(index);
        }
        boolean allowing = false;
        for (Biome.SpawnListEntry entryOption : reader.getPossibleCreatures(creatureType, posTarget)) {
            if (Objects.equals(entryOption.entityClass, entityType)) {
                allowing = true;
                break;
            }
        }
        biomeAllowanceCache.put(index, allowing);
        return allowing;
    }

    /**
     * Some non-final fields should not be null
     */
    private void initialize() {
        reader = worldServer.silentChunkReader;
        result = PerimeterResult.createEmptyResult();
        creatureType = checkCreatureType(entityType);
        placementType = EntitySpawnPlacementRegistry.getPlacementForEntity(entityType);
        initSpawningRange();
        spawnAllowanceCache = new Long2ObjectOpenHashMap<>();
        biomeAllowanceCache = new Long2BooleanOpenHashMap();
    }

    private void countSpots() {
        // TODO: 2023/6/1,0001 Calculate the spawning rate / probability when going through the random choices
        // TODO: 2023/6/12,0012 Only count the possible spots. Do NOT calculate the rates.
        //  The spawning rates can be 0 while the spot is counted as spawn-able.
        BlockPos.MutableBlockPos posTarget = new BlockPos.MutableBlockPos();
        for (int y = yMin; y <= yMax; ++y) {
            for (int x = xMin; x <= xMax; ++x) {
                for (int z = zMin; z <= zMax; ++z) {
                    posTarget.setPos(x, y, z);
                    if (isSpawnBiomeAllowing(posTarget)) {
                        PerimeterResult.EnumDistLevel distLevel = getDistLevelTo(posTarget);
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            initialize();
            countSpots();
            // print result
            Messenger.print_server_message(CarpetServer.minecraft_server, "Perimeter Info:");
        } catch (Exception e) {
            // failed
            Messenger.print_server_message(CarpetServer.minecraft_server, "Failed to check perimeter");
            e.printStackTrace();
        }
    }

}
