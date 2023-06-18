package carpet.utils.perimeter;

import carpet.CarpetServer;
import carpet.utils.Messenger;
import carpet.utils.SilentChunkReader;
import carpet.utils.perimeter.PerimeterResult.EnumDistLevel;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLiving.SpawnPlacementType;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.HttpUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.border.WorldBorder;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * {@link net.minecraft.world.WorldEntitySpawner#findChunksForSpawning}
 */
public class PerimeterCalculator {
    public static final int SECTION_UNIT = 16;
    private static final int SAMPLE_REPORT_NUM = 10;
    private static final int yMin = 0;
    private final Class<? extends EntityLiving> entityType;
    private final WorldServer worldServer;
    private final Vec3d center;
    private final SpawnChecker checker;
    private int yMax = SECTION_UNIT - 1;
    private Long2BooleanMap biomeAllowanceCache = null;
    private Int2ObjectSortedMap<Long2ObjectMap<EnumDistLevel>> distanceCacheLayered = null;
    private EnumCreatureType creatureType = null;
    private SpawnPlacementType placementType = null;
    private SilentChunkReader access = null;
    private PerimeterResult resultCached = null;
    private BlockPos worldSpawnPoint = null;
    private boolean specific = false;
    private int xMin = 0;
    private int xMax = 0;
    private int zMin = 0;
    private int zMax = 0;
    private boolean initialized = false;

    private PerimeterCalculator(WorldServer worldServer, Vec3d center, Class<? extends EntityLiving> entityType) {
        this.worldServer = worldServer;
        this.center = center;
        this.entityType = entityType;
        checker = new SpawnChecker(worldServer, center);
    }

    public static void asyncSearch(ICommandSender sender, ICommand command, World world, Vec3d center, Class<? extends EntityLiving> entityType) throws CommandException {
        if (world instanceof WorldServer) {
            CommandBase.notifyCommandListener(sender, command, "Start checking perimeter ...");
            HttpUtil.DOWNLOADER_EXECUTOR.submit(() -> {
                try {
                    PerimeterCalculator calculator = new PerimeterCalculator((WorldServer) world, center, entityType);
                    PerimeterResult result = calculator.countSpots();
                    CommandBase.notifyCommandListener(sender, command, "Finish checking perimeter info");
                    calculator.printResult(sender, result);
                } catch (Exception e) {
                    CommandBase.notifyCommandListener(sender, command, "Failed to check perimeter");
                    e.printStackTrace();
                }
            });
        } else {
            throw new CommandException("commands.compare.outOfWorld");
        }
    }

    /**
     * Some non-final fields should not be null
     */
    private synchronized void initialize() {
        if (!initialized) {
            access = worldServer.silentChunkReader;
            if (entityType != null) {
                specific = true;
                creatureType = SpawnChecker.checkCreatureType(entityType);
                placementType = EntitySpawnPlacementRegistry.getPlacementForEntity(entityType);
            } else {
                specific = false;
            }
            initSpawningRange();
            distanceCacheLayered = new Int2ObjectAVLTreeMap<>();
            biomeAllowanceCache = new Long2BooleanOpenHashMap();
            initialized = true;
        }
    }

    /**
     * {@link net.minecraft.world.WorldEntitySpawner#findChunksForSpawning}
     */
    public PerimeterResult countSpots() {
        // Only count the possible spots. Do NOT calculate the rates.
        // The spawning rates can be 0 while the spot is counted as spawn-able.
        initialize();
        PerimeterResult result = PerimeterResult.createEmptyResult();
        BlockPos.MutableBlockPos posTarget = new BlockPos.MutableBlockPos();
        for (int y = yMin; y <= yMax; ++y) {
            for (int x = xMin; x <= xMax; ++x) {
                for (int z = zMin; z <= zMax; ++z) {
                    posTarget.setPos(x, y, z);
                    IBlockState stateTarget = access.getBlockState(posTarget);
                    IBlockState stateDown = access.getBlockState(x, y - 1, z);
                    IBlockState stateUp = access.getBlockState(x, y + 1, z);
                    // check placement in liquid
                    boolean flagLiquid = stateTarget.getMaterial() == Material.WATER
                            && stateDown.getMaterial() == Material.WATER && !stateUp.isNormalCube();
                    boolean flagGround;
                    if (!stateDown.isTopSolid()) {
                        flagGround = false;
                    } else {
                        Block blockDown = stateDown.getBlock();
                        boolean flag = blockDown != Blocks.BEDROCK && blockDown != Blocks.BARRIER;
                        flagGround = flag && WorldEntitySpawner.isValidEmptySpawnBlock(stateTarget)
                                && WorldEntitySpawner.isValidEmptySpawnBlock(stateUp);
                    }
                    if (flagLiquid) {
                        result.addGeneralSpot(SpawnPlacementType.IN_WATER, getDistLevelOf(posTarget));
                    }
                    if (flagGround) {
                        result.addGeneralSpot(SpawnPlacementType.ON_GROUND, getDistLevelOf(posTarget));
                    }
                    if (specific && isBiomeAllowing(posTarget)) {
                        boolean placeable = false;
                        switch (placementType) {
                            case ON_GROUND:
                                placeable = flagGround;
                                break;
                            case IN_WATER:
                                placeable = flagLiquid;
                                break;
                        }
                        if (placeable && checker.isPositionAllowing(entityType, posTarget) && checker.isNotColliding(entityType, posTarget)) {
                            result.addSpecificSpot(entityType, getDistLevelOf(posTarget), posTarget);
                        }
                    }
                }
            }
            distanceCacheLayered.remove(y);
        }
        resultCached = result;
        return result;
    }

    public void printResult(ICommandSender sender, PerimeterResult result) {
        if (result == null) {
            synchronized (this) {
                if (resultCached != null) {
                    result = resultCached;
                } else {
                    return;
                }
            }
        }
        Messenger.m(sender, "g format: <normal> + <despawning> = <spawning> ; <banned>...");
        Messenger.m(sender, "w Spawning spaces around ", Messenger.tpa("y", center.x, center.y, center.z, 2));
        Messenger.m(sender, result.createStandardReport(SpawnPlacementType.IN_WATER));
        Messenger.m(sender, result.createStandardReport(SpawnPlacementType.ON_GROUND));
        if (specific) {
            Messenger.m(sender, result.createStandardReport(entityType));
            ITextComponent[] bannedSampleList = result.createBannedSampleReports(entityType, SAMPLE_REPORT_NUM);
            if (bannedSampleList.length > 0) {
                if (worldSpawnPoint == null) {
                    worldSpawnPoint = access.getSpawnPoint();
                }
                Messenger.m(sender, "w Current ", "w WSP", "^g World Spawn Point", "w  in use: ",
                        Messenger.tpa("m", (double) worldSpawnPoint.getX(), worldSpawnPoint.getY(), worldSpawnPoint.getZ()));
                Messenger.m(sender, "w Forbidden locations around the World Spawn Point:");
                for (ITextComponent sample : bannedSampleList) {
                    Messenger.m(sender, sample);
                }
            }
            ITextComponent[] normalSampleList = result.createSpawningSampleReports(entityType, SAMPLE_REPORT_NUM);
            for (ITextComponent sample : normalSampleList) {
                Messenger.m(sender, sample);
            }
        }
        Messenger.m(sender, "g ------------------------");
    }

    private void initSpawningRange() {
        int chunkX = MathHelper.floor(center.x / 16.0);
        int chunkZ = MathHelper.floor(center.z / 16.0);
        // checking player chunk map
        int radius = Math.min(CarpetServer.minecraft_server.getPlayerList().getViewDistance(), 7);
        for (int cx = -radius; cx <= radius; ++cx) {
            for (int cz = -radius; cz <= radius; ++cz) {
                ChunkPos chunkPos = new ChunkPos(chunkX + cx, chunkZ + cz);
                int height = access.getSpawningHeight(chunkPos);
                if (height > yMax) {
                    yMax = height;
                }
            }
        }
        int expand = 20;
        WorldBorder worldBorder = worldServer.getWorldBorder();
        int worldLimit = worldBorder.getSize();
        xMin = MathHelper.clamp(((chunkX - radius) << 4) - expand, -worldLimit, worldLimit);
        xMax = MathHelper.clamp(((chunkX + radius) << 4) + 15 + expand, -worldLimit, worldLimit);
        zMin = MathHelper.clamp(((chunkZ - radius) << 4) - expand, -worldLimit, worldLimit);
        zMax = MathHelper.clamp(((chunkZ + radius) << 4) + 15 + expand, -worldLimit, worldLimit);
    }

    /**
     * The 24-meter check for the world spawn point and the closest player
     */
    private EnumDistLevel getDistLevelOf(@Nonnull BlockPos posTarget) {
        int posY = posTarget.getY();
        Long2ObjectMap<EnumDistLevel> cacheLayer;
        if (distanceCacheLayered.containsKey(posY)) {
            cacheLayer = distanceCacheLayered.get(posY);
        } else {
            cacheLayer = new Long2ObjectOpenHashMap<>();
            cacheLayer.defaultReturnValue(null);
            distanceCacheLayered.put(posY, cacheLayer);
        }
        long index = SilentChunkReader.blockHorizonLong(posTarget);
        if (cacheLayer.containsKey(index)) {
            return cacheLayer.get(index);
        }
        float mobX = (float) posTarget.getX() + 0.5F;
        float mobZ = (float) posTarget.getZ() + 0.5F;
        EnumDistLevel level;
        if (worldSpawnPoint == null) {
            worldSpawnPoint = access.getSpawnPoint();
        }
        if (worldSpawnPoint.distanceSq(mobX, posY, mobZ) >= 576.0) {
            double distSq = center.squareDistanceTo(mobX, posY, mobZ);
            level = EnumDistLevel.getLevelOfDistSq(distSq);
        } else {
            level = EnumDistLevel.BANNED;
        }
        cacheLayer.put(index, level);
        return level;
    }

    private boolean isBiomeAllowing(@Nonnull BlockPos posTarget) {
        long index = SilentChunkReader.blockHorizonLong(posTarget);
        if (biomeAllowanceCache.containsKey(index)) {
            return biomeAllowanceCache.get(index);
        }
        boolean allowing = false;
        for (Biome.SpawnListEntry entryOption : access.getPossibleCreatures(creatureType, posTarget)) {
            if (Objects.equals(entryOption.entityClass, entityType)) {
                allowing = true;
                break;
            }
        }
        biomeAllowanceCache.put(index, allowing);
        return allowing;
    }
}
