package carpet.utils.perimeter;

import carpet.CarpetServer;
import carpet.utils.LRUCache;
import carpet.utils.SilentChunkReader;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.*;
import net.minecraft.util.HttpUtil;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.border.WorldBorder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

public class SpawningCalculator {
    public static final int MIN_GROUP_NUM = 1;
    public static final int MAX_GROUP_NUM = 4;
    private final WorldServer world;
    private final SilentChunkReader access;
    private final SpawnChecker checker;
    private final Vec3d posPeriCenter;
    private final boolean fullArea;
    private final BlockPos.MutableBlockPos posCornerMin = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos posCornerMax = new BlockPos.MutableBlockPos();
    private final Set<BlockPos> possibleTargetSet = new HashSet<>();
    private final Map<BlockPos, Double> distSqCache = new LRUCache<>(64);
    private final Map<PredictorKey, Double> spawningRateCache = new HashMap<>();
    private final Map<Integer, Map<BlockPos, Double>> biomeChanceCache = new HashMap<>();
    private boolean targetCheckFinished = false;
    private boolean rangeCheckFinished = false;
    private int minChunkX = 0;
    private int maxChunkX = 0;
    private int minChunkZ = 0;
    private int maxChunkZ = 0;

    private SpawningCalculator(@Nonnull WorldServer world, Vec3d posPeriCenter, BlockPos posCorner1, BlockPos posCorner2) throws NullPointerException {
        this.world = Objects.requireNonNull(world);
        access = Objects.requireNonNull(world.silentChunkReader);
        checker = new SpawnChecker(world, posPeriCenter);
        fullArea = posCorner1 == null || posCorner2 == null;
        if (fullArea) {
            this.posPeriCenter = Objects.requireNonNull(posPeriCenter);
        } else {
            this.posPeriCenter = posPeriCenter;
            posCornerMin.setPos(Objects.requireNonNull(posCorner1));
            posCornerMax.setPos(Objects.requireNonNull(posCorner2));
            sortCorners();
        }
    }

    @Nullable
    @ParametersAreNonnullByDefault
    public static SpawningCalculator createInstance(WorldServer world, Vec3d posPeriCenter) {
        try {
            return new SpawningCalculator(world, posPeriCenter, null, null);
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Nullable
    @ParametersAreNonnullByDefault
    public static SpawningCalculator createInstance(WorldServer world, BlockPos posTarget) {
        try {
            return new SpawningCalculator(world, null, posTarget, posTarget);
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Nullable
    @ParametersAreNonnullByDefault
    public static SpawningCalculator createInstance(WorldServer world, BlockPos posCorner1, BlockPos posCorner2) {
        try {
            return new SpawningCalculator(world, null, posCorner1, posCorner2);
        } catch (NullPointerException e) {
            return null;
        }
    }

    @ParametersAreNonnullByDefault
    public static void asyncExecute(ICommandSender sender, ICommand command, World world, EnumMode mode, Object... options) throws CommandException {
        if (world instanceof WorldServer) {
            CommandBase.notifyCommandListener(sender, command, "Calculating rates of mob spawning ...");
            HttpUtil.DOWNLOADER_EXECUTOR.submit(() -> {
                try {
                    SpawningCalculator calculator = null;
                    switch (mode) {
                        case BLOCK:
                            calculator = createInstance((WorldServer) world, (BlockPos) options[0]);
                            break;
                        case RANGE:
                            calculator = createInstance((WorldServer) world, (BlockPos) options[0], (BlockPos) options[1]);
                            break;
                        case PERIMETER:
                            calculator = createInstance((WorldServer) world, (Vec3d) options[0]);
                    }
                    Objects.requireNonNull(calculator);
                    CommandBase.notifyCommandListener(sender, command, "Finished mob-spawn-rate calculation");
                } catch (Throwable e) {
                    CommandBase.notifyCommandListener(sender, command, "Failed to calculate mob-spawn-rate");
                    e.printStackTrace();
                }
            });
        } else {
            throw new CommandException("commands.compare.outOfWorld");
        }
    }

    /**
     * Set {@code posCornerMin} and {@code posCornerMax} to the correct order
     */
    private void sortCorners() {
        boolean changed = false;
        int x1 = posCornerMin.getX();
        int x2 = posCornerMax.getX();
        if (x1 > x2) {
            changed = true;
            int temp = x2;
            x2 = x1;
            x1 = temp;
        }
        int y1 = posCornerMin.getY();
        int y2 = posCornerMax.getY();
        if (y1 > y2) {
            changed = true;
            int temp = y2;
            y2 = y1;
            y1 = temp;
        }
        int z1 = posCornerMin.getZ();
        int z2 = posCornerMax.getZ();
        if (z1 > z2) {
            changed = true;
            int temp = z2;
            z2 = z1;
            z1 = temp;
        }
        if (changed) {
            posCornerMin.setPos(x1, Math.max(y1, 0), z1);
            posCornerMax.setPos(x2, y2, z2);
        }
    }

    public boolean isEligibleChunk(int chunkX, int chunkZ) {
        initSetTargetRange();
        return chunkX >= minChunkX && chunkX <= maxChunkX && chunkZ >= minChunkZ && chunkZ <= maxChunkZ;
    }

    public synchronized void initSetTargetRange() {
        if (rangeCheckFinished) {
            return;
        }
        if (posPeriCenter != null) {
            int chunkX = MathHelper.floor(posPeriCenter.x / 16.0);
            int chunkZ = MathHelper.floor(posPeriCenter.z / 16.0);
            // checking player chunk map
            int radius = Math.min(CarpetServer.minecraft_server.getPlayerList().getViewDistance(), 7);
            WorldBorder worldBorder = world.getWorldBorder();
            int borderCenterX = MathHelper.floor(worldBorder.getCenterX() / 16.0);
            int borderCenterZ = MathHelper.floor(worldBorder.getCenterZ() / 16.0);
            minChunkX = chunkX + radius;
            maxChunkX = chunkX - radius;
            for (int cx = -radius; cx <= radius; ++cx) {
                ChunkPos chunkPos = new ChunkPos(chunkX + cx, borderCenterZ);
                if (worldBorder.contains(chunkPos)) {
                    if (minChunkX < chunkPos.x) {
                        minChunkX = chunkPos.x;
                    }
                    if (maxChunkX > chunkPos.x) {
                        maxChunkX = chunkPos.x;
                    }
                }
            }
            minChunkZ = chunkZ + radius;
            maxChunkZ = chunkZ - radius;
            for (int cz = -radius; cz <= radius; ++cz) {
                ChunkPos chunkPos = new ChunkPos(borderCenterX, chunkZ + cz);
                if (worldBorder.contains(chunkPos)) {
                    if (minChunkZ < chunkPos.z) {
                        minChunkZ = chunkPos.z;
                    }
                    if (maxChunkZ > chunkPos.z) {
                        maxChunkZ = chunkPos.z;
                    }
                }
            }
            if (fullArea) {
                int expand = 20;
                int worldLimit = worldBorder.getSize();
                int yMin = 0;
                int yMax = world.getHeight() - 1;
                int xMin = MathHelper.clamp(((chunkX - radius) << 4) - expand, -worldLimit, worldLimit);
                int xMax = MathHelper.clamp(((chunkX + radius) << 4) + 15 + expand, -worldLimit, worldLimit);
                int zMin = MathHelper.clamp(((chunkZ - radius) << 4) - expand, -worldLimit, worldLimit);
                int zMax = MathHelper.clamp(((chunkZ + radius) << 4) + 15 + expand, -worldLimit, worldLimit);
                posCornerMin.setPos(xMin, yMin, zMin);
                posCornerMax.setPos(xMax, yMax, zMax);
            }
        }
        rangeCheckFinished = true;
    }

    public synchronized void initCheckPossibleTargets() {
        if (targetCheckFinished) {
            return;
        }
        initSetTargetRange();
        BlockPos.MutableBlockPos posIter = new BlockPos.MutableBlockPos();
        for (int x = posCornerMin.getX(); x <= posCornerMax.getX(); ++x) {
            for (int z = posCornerMin.getZ(); z <= posCornerMax.getZ(); ++z) {
                posIter.setPos(x, -1, z);
                int height = Math.min(access.getSpawningColumnHeight(posIter), posCornerMax.getY() + 1);
                for (int y = posCornerMin.getY(); y < height; ++y) {
                    posIter.setY(y);
                    if (checkSpawningPossibility(posIter, null)) {
                        possibleTargetSet.add(posIter.toImmutable());
                    }
                }
            }
        }
        targetCheckFinished = true;
    }

    @ParametersAreNonnullByDefault
    public boolean checkSpawningPossibility(BlockPos posTarget, @Nullable Class<? extends EntityLiving> mobClass, CheckStage... steps) {
        if (steps.length == 0) {
            steps = CheckStage.values();
        }
        for (CheckStage step : steps) {
            if (!checkSpawningPossibility(posTarget, mobClass, step)) {
                return false;
            }
        }
        return true;
    }

    @ParametersAreNonnullByDefault
    public boolean checkSpawningPossibility(BlockPos posTarget, @Nullable Class<? extends EntityLiving> mobClass, CheckStage step) {
        switch (step) {
            case PROTECTION:
                return isMobNotProtected(posTarget);
            case BIOME:
                return isMobInCorrectBiome(posTarget, mobClass);
            case PLACEMENT:
                return isMobPlaceable(posTarget, mobClass);
            case POSITION:
                return isMobPositionCorrect(posTarget, mobClass);
            case COLLISION:
                return isMobNotColliding(posTarget, mobClass);
            case DESPAWNING:
                return isMobNotDespawning(posTarget, mobClass);
            default:
                return false;
        }
    }

    public boolean isMobNotProtected(@Nonnull BlockPos posTarget) {
        int posY = posTarget.getY();
        float mobX = (float) posTarget.getX() + 0.5F;
        float mobZ = (float) posTarget.getZ() + 0.5F;
        BlockPos posWorldSpawn = access.getSpawnPoint();
        if (posWorldSpawn.distanceSq(mobX, posY, mobZ) >= 576.0) {
            if (posPeriCenter == null) {
                return true;
            } else {
                return getDistSqToPlayer(posTarget) >= 576.0;
            }
        } else {
            return false;
        }
    }

    public double getDistSqToPlayer(@Nonnull BlockPos posTarget) {
        posTarget = posTarget.toImmutable();
        Double distSq = distSqCache.get(posTarget);
        if (distSq == null) {
            int posY = posTarget.getY();
            float mobX = (float) posTarget.getX() + 0.5F;
            float mobZ = (float) posTarget.getZ() + 0.5F;
            distSq = posPeriCenter.squareDistanceTo(mobX, posY, mobZ);
            distSqCache.put(posTarget, distSq);
        }
        return distSq;
    }

    public boolean isMobNotDespawning(@Nonnull BlockPos posTarget, Class<? extends EntityLiving> mobClass) {
        if (posPeriCenter != null && SpawnChecker.canImmediatelyDespawn(mobClass)) {
            return getDistSqToPlayer(posTarget) <= 16384.0;
        } else {
            return true;
        }
    }

    public boolean isMobPlaceable(@Nonnull BlockPos posTarget, Class<? extends EntityLiving> mobClass) {
        if (mobClass == null) {
            for (EntityLiving.SpawnPlacementType placementType : EntityLiving.SpawnPlacementType.values()) {
                if (access.isCreaturePlaceable(posTarget, placementType)) {
                    return true;
                }
            }
            return false;
        } else {
            return access.isCreaturePlaceable(posTarget, EntitySpawnPlacementRegistry.getPlacementForEntity(mobClass));
        }
    }

    public boolean isMobInCorrectBiome(@Nonnull BlockPos posTarget, Class<? extends EntityLiving> mobClass) {
        if (mobClass == null) {
            Biome biome = access.getBiome(posTarget);
            if (biome == null) {
                return false;
            }
            for (EnumCreatureType creatureType : EnumCreatureType.values()) {
                if (!biome.getSpawnableList(creatureType).isEmpty()) {
                    return true;
                }
            }
        } else {
            EnumCreatureType creatureType = SpawnChecker.checkCreatureType(mobClass);
            for (Biome.SpawnListEntry entryOption : access.getPossibleCreatures(creatureType, posTarget)) {
                if (Objects.equals(entryOption.entityClass, mobClass)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isMobPositionCorrect(@Nonnull BlockPos posTarget, Class<? extends EntityLiving> mobClass) {
        if (mobClass == null) {
            return true;
        } else {
            return checker.isPositionAllowing(mobClass, posTarget);
        }
    }

    public boolean isMobNotColliding(@Nonnull BlockPos posTarget, Class<? extends EntityLiving> mobClass) {
        if (mobClass == null) {
            AxisAlignedBB minBox = SpawnChecker.getMinimumBoundingBox(posTarget);
            return checker.isNotColliding(minBox);
        } else {
            return checker.isNotColliding(mobClass, posTarget);
        }
    }

    public double getSpawningRate(@Nonnull BlockPos posTarget, Class<? extends EntityLiving> mobClass) {
        double rateTotal = 0.0;
        int mobId = EntityList.REGISTRY.getIDForObject(mobClass);
        for (int c = MIN_GROUP_NUM; c <= MAX_GROUP_NUM; ++c) {
            rateTotal += getSpawningStepRate(mobId, posTarget, 0, c);
        }
        return rateTotal;
    }

    /**
     * @param mobId     {@code m}
     * @param posCurr   {@code \vec{r}}
     * @param rounds    {@code w}
     * @param groupSize {@code c}
     * @return {@code s_m(r, w, c)}
     */
    public double getSpawningStepRate(int mobId, @Nonnull BlockPos posCurr, int rounds, int groupSize) {
        PredictorKey key = new PredictorKey(mobId, posCurr, rounds, groupSize);
        Double value;
        value = spawningRateCache.get(key);
        if (value != null) {
            return value;
        }
        double rateCurr;
        if (rounds == groupSize) {
            rateCurr = getChancePosInChunk(posCurr) * getChanceGroupSize(groupSize);
        } else if (rounds == groupSize - 1) {
            rateCurr = 0.0;
            for (int dx = -5; dx <= 5; ++dx) {
                for (int dz = -5; dz <= 5; ++dz) {
                    double biomeChance = getChanceMobAtBiome(mobId, posCurr);
                    double unit = getSpawningStepRate(mobId, posCurr.add(dx, 0, dz), groupSize, groupSize);
                    rateCurr += biomeChance * unit * getChancePackDeviation(-dx, -dz);
                }
            }
        } else if (rounds >= 0 && rounds <= groupSize - 2) {
            int roundsPrev = rounds + 1;
            rateCurr = 0.0;
            for (int dx = -5; dx <= 5; ++dx) {
                for (int dz = -5; dz <= 5; ++dz) {
                    double unit = getSpawningStepRate(mobId, posCurr.add(dx, 0, dz), roundsPrev, groupSize);
                    rateCurr += unit * getChancePackDeviation(-dx, -dz);
                }
            }
        } else {
            return Double.NaN;
        }
        value = rateCurr;
        spawningRateCache.put(key, value);
        return rateCurr;
    }

    public double getChancePosInChunk(@Nonnull BlockPos posTarget) {
        return getChancePosInChunk(posTarget.getX(), posTarget.getY(), posTarget.getZ());
    }

    public double getChancePosInChunk(int blockX, int blockY, int blockZ) {
        IBlockState stateTarget = access.getBlockState(blockX, blockY, blockZ);
        if (stateTarget.isNormalCube()) {
            return 0.0;
        }
        int height = access.getSpawningColumnHeight(blockX, blockZ);
        if (blockY >= 0 && blockY < height) {
            return 1.0 / (SpawnChecker.SECTION_UNIT * SpawnChecker.SECTION_UNIT * height);
        } else {
            return 0.0;
        }
    }

    public double getChanceGroupSize(int groupSize) {
        if (groupSize >= MIN_GROUP_NUM && groupSize <= MAX_GROUP_NUM) {
            return 3.0 / (MAX_GROUP_NUM - MIN_GROUP_NUM + 1);
        } else {
            return 0.0;
        }
    }

    public double getChancePackDeviation(int dx, int dz) {
        int adx = Math.abs(dx);
        int adz = Math.abs(dz);
        if (adx > 5 || adz > 5) {
            return 0.0;
        }
        int weightX = 6 - adx;
        int weightZ = 6 - adz;
        return (weightX * weightZ) / 1296.0;
    }

    /**
     * Checks biomes and structures
     */
    public double getChanceMobAtBiome(int mobId, @Nonnull BlockPos posTarget) {
        Map<BlockPos, Double> blockMap = biomeChanceCache.computeIfAbsent(mobId, lambdaMobId -> new HashMap<>());
        posTarget = posTarget.toImmutable();
        Double value = blockMap.get(posTarget);
        if (value != null) {
            return value;
        }
        Class<? extends Entity> mobClass = EntityList.REGISTRY.getObjectById(mobId);
        EnumCreatureType creatureType = SpawnChecker.checkCreatureType(mobClass);
        List<Biome.SpawnListEntry> spawnList = access.getPossibleCreatures(creatureType, posTarget);
        int range = 0;
        int interest = 0;
        for (Biome.SpawnListEntry entry : spawnList) {
            int weight = entry.getWeight();
            int otherId = EntityList.REGISTRY.getIDForObject(entry.entityClass);
            if (mobId == otherId) {
                interest += weight;
            }
            range += weight;
        }
        double chance = (double) interest / range;
        value = chance;
        blockMap.put(posTarget, value);
        return chance;
    }

    public enum CheckStage {
        /**
         * Distance < 24
         */
        PROTECTION,
        BIOME,
        PLACEMENT,
        POSITION,
        COLLISION,
        /**
         * Distance > 128
         */
        DESPAWNING
    }

    public enum EnumMode {
        BLOCK,
        RANGE,
        PERIMETER
    }
}
