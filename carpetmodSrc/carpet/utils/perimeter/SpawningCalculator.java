package carpet.utils.perimeter;

import carpet.CarpetServer;
import carpet.utils.LRUCache;
import carpet.utils.Messenger;
import carpet.utils.SilentChunkReader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.*;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.*;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.HttpUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.*;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;

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
    private final Map<BlockPos, Double> distSqCache = new LRUCache<>(1024);
    private final Map<PredictorKey, Double> spawningRateCache = new LRUCache<>(32768);
    private final Map<Integer, Map<BlockPos, Double>> biomeChanceCache = new HashMap<>();
    private boolean targetCheckFinished = false;
    private boolean rangeCheckFinished = false;
    private int minChunkX = 0;
    private int maxChunkX = 0;
    private int minChunkZ = 0;
    private int maxChunkZ = 0;
    private long recursionCount = 0;
    private long cachingCount = 0;

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
                    long timeStart = System.currentTimeMillis();
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
                    if (mode == EnumMode.BLOCK) {
                        calculator.recursionCount = 0;
                        calculator.cachingCount = 0;
                        Map<Class<? extends EntityLiving>, Double> resultMap = calculator.getCumulativeResult((BlockPos) options[0], null);
                        long timeFinish = System.currentTimeMillis();
                        long duration = timeFinish - timeStart;
                        calculator.spawningRateCache.forEach((k, v) -> System.out.println(k + " -> " + v));
                        for (Map.Entry<Class<? extends EntityLiving>, Double> entry : resultMap.entrySet()) {
                            Class<? extends EntityLiving> mobClass = entry.getKey();
                            Double value = entry.getValue();
                            ResourceLocation resource = EntityList.REGISTRY.getNameForObject(mobClass);
                            String name = resource == null ? mobClass.getSimpleName() : resource.getPath();
                            Double period = value == null ? null : (1.0 / value);
                            Messenger.m(sender, "w " + name, "g : ",
                                    "c " + String.format("%.2f", period), "^g " + period, "w  rounds",
                                    "g , or ", "w " + value);
                        }
                        Messenger.m(sender, "gi Duration = " + duration + " ms");
                        Messenger.m(sender, "gi Recursion count = " + calculator.recursionCount);
                        Messenger.m(sender, "gi Cached result count = " + calculator.cachingCount);
                        Messenger.m(sender, "gi Cache size = " + calculator.spawningRateCache.size());
                    }
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
     * Events with probability exactly equaling to 1.0 or 0.0
     *
     * @param value The boolean value
     * @return {@code 1.0} for {@code true}; {@code 0.0} for {@code false}
     */
    public static double booleanToChance(boolean value) {
        return value ? 1.0 : 0.0;
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
                    if (isSpawningPossible(posIter, null)) {
                        possibleTargetSet.add(posIter.toImmutable());
                    }
                }
            }
        }
        targetCheckFinished = true;
    }

    @ParametersAreNonnullByDefault
    public boolean isSpawningPossible(BlockPos posTarget, @Nullable Class<? extends EntityLiving> mobClass, CheckStage... steps) {
        if (steps.length == 0) {
            steps = CheckStage.values();
            if (targetCheckFinished && !possibleTargetSet.contains(posTarget.toImmutable())) {
                return false;
            }
        }
        for (CheckStage step : steps) {
            if (!isSpawningPossible(posTarget, mobClass, step)) {
                return false;
            }
        }
        return true;
    }

    @ParametersAreNonnullByDefault
    public boolean isSpawningPossible(BlockPos posTarget, @Nullable Class<? extends EntityLiving> mobClass, CheckStage step) {
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
            return checker.isNotColliding(posTarget, minBox);
        } else {
            return checker.isNotColliding(mobClass, posTarget);
        }
    }

    /**
     * For all mobs
     */
    @ParametersAreNonnullByDefault
    public Map<Class<? extends EntityLiving>, Double> getCumulativeResult(
            BlockPos posTarget, @Nullable Map<Class<? extends EntityLiving>, Double> resultMap) {
        Set<Class<? extends EntityLiving>> mobClassSet = new HashSet<>();
        for (EnumCreatureType creatureType : EnumCreatureType.values()) {
            List<Biome.SpawnListEntry> spawnList = access.getPossibleCreatures(creatureType, posTarget);
            for (Biome.SpawnListEntry entry : spawnList) {
                mobClassSet.add(entry.entityClass);
            }
        }
        if (resultMap == null) {
            resultMap = new HashMap<>();
        }
        for (Class<? extends EntityLiving> mobClass : mobClassSet) {
            double rate = getSpawningRate(posTarget, mobClass);
            Double oldValue = resultMap.get(mobClass);
            if (oldValue != null && !oldValue.equals(0.0)) {
                rate += oldValue;
            }
            if (rate == 0.0) {
                resultMap.remove(mobClass);
            } else {
                resultMap.put(mobClass, rate);
            }
        }
        return resultMap;
    }

    @ParametersAreNonnullByDefault
    public double getSpawningRate(BlockPos posTarget, Class<? extends EntityLiving> mobClass) {
        double rate = 0.0;
        if (isSpawningPossible(posTarget, mobClass)) {
            double factor = getChanceValidPosition(mobClass, posTarget) * getChanceNotColliding(mobClass, posTarget);
            if (factor != 0.0) {
                int mobId = EntityList.REGISTRY.getIDForObject(mobClass);
                for (int c = MIN_GROUP_NUM; c <= MAX_GROUP_NUM; ++c) {
                    // TODO: 2023/6/22,0022 Add special check for random failure, e.g. slimes
                    //  EntityLiving#getCanSpawnHere() and isNotColliding()
                    rate += getSpawningStepRate(mobId, posTarget, 0, c);
                }
                rate *= factor;
            }
        }
        return rate;
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
            ++cachingCount;
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
            rateCurr = Double.NaN;
        }
        ++recursionCount;
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
        Map<BlockPos, Double> blockMap = biomeChanceCache.computeIfAbsent(mobId, lambdaMobId -> new LRUCache<>(1024));
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

    /**
     * {@link EntityLiving#getCanSpawnHere()}
     */
    @ParametersAreNonnullByDefault
    public double getChanceValidPosition(Class<? extends EntityLiving> mobClass, BlockPos posTarget) {
        if (!checker.isPositionAllowing(mobClass, posTarget)) {
            return 0.0;
        }
        int blockX = posTarget.getX();
        int blockZ = posTarget.getZ();
        float mobX = (float) blockX + 0.5F;
        float mobZ = (float) blockZ + 0.5F;
        int mobY = posTarget.getY();
        IBlockState stateDown = access.getBlockState(blockX, mobY - 1, blockZ);
        Block blockDown = stateDown.getBlock();
        if (EntityBat.class.isAssignableFrom(mobClass)) {
            if (mobY >= world.getSeaLevel()) {
                return 0.0;
            } else {
                int lightLevel = access.getLightFromNeighbors(posTarget, true);
                int limit = 4;
                double chance = (double) Math.max(0, limit - lightLevel) / limit;
                // TODO: 2023/6/22,0022 Add Halloween check
                return chance * getChanceValidPosition(EntityLiving.class, posTarget);
            }
        } else if (EntityCreature.class.isAssignableFrom(mobClass)) {
            if (EntityAnimal.class.isAssignableFrom(mobClass)) {
                if (EntityOcelot.class.isAssignableFrom(mobClass)) {
                    return 2.0 / 3.0;
                } else if (EntityParrot.class.isAssignableFrom(mobClass)) {
                    return booleanToChance(blockDown instanceof BlockLeaves || blockDown == Blocks.GRASS
                            || blockDown instanceof BlockLog || blockDown == Blocks.AIR && access.getLight(posTarget) > 8)
                            * getChanceValidPosition(EntityAnimal.class, posTarget);
                } else {
                    return booleanToChance(blockDown == SpawnChecker.getSpawnableBlock(mobClass)
                            && access.getLight(posTarget) > 8) * getChanceValidPosition(EntityCreature.class, posTarget);
                }
            } else if (EntityMob.class.isAssignableFrom(mobClass)) {
                if (EntityEndermite.class.isAssignableFrom(mobClass)) {
                    return booleanToChance(posPeriCenter == null || posPeriCenter.squareDistanceTo(mobX, mobY, mobZ) >= 25.0)
                            * getChanceValidPosition(EntityMob.class, posTarget);
                } else if (EntityGuardian.class.isAssignableFrom(mobClass)) {
                    return booleanToChance(access.canBlockSeeSky(posTarget))
                            * getChanceValidPosition(EntityMob.class, posTarget) / 20.0;
                } else if (EntitySilverfish.class.isAssignableFrom(mobClass)) {
                    return booleanToChance(posPeriCenter == null || posPeriCenter.squareDistanceTo(mobX, mobY, mobZ) >= 25.0)
                            * getChanceValidPosition(EntityMob.class, posTarget);
                } else if (EntityHusk.class.isAssignableFrom(mobClass)) {
                    return booleanToChance(access.canSeeSky(posTarget))
                            * getChanceValidPosition(EntityMob.class, posTarget);
                } else if (EntityPigZombie.class.isAssignableFrom(mobClass)) {
                    return 1.0;
                } else if (EntityStray.class.isAssignableFrom(mobClass)) {
                    return booleanToChance(access.canSeeSky(posTarget))
                            * getChanceValidPosition(EntityMob.class, posTarget);
                } else {
                    return getChanceValidLight(mobClass, posTarget) * getChanceValidPosition(EntityCreature.class, posTarget);
                }
            } else {
                return booleanToChance(checker.getBlockPathWeight(mobClass, posTarget) >= 0.0F)
                        * getChanceValidPosition(EntityLiving.class, posTarget);
            }
        } else if (EntityGhast.class.isAssignableFrom(mobClass)) {
            return getChanceValidPosition(EntityLiving.class, posTarget) / 20.0;
        } else if (EntitySlime.class.isAssignableFrom(mobClass)) {
            if (EntityMagmaCube.class.isAssignableFrom(mobClass)) {
                return 1.0;
            } else {
                Chunk chunk = access.getChunk(posTarget);
                Biome biome = access.getBiome(posTarget);
                double chance;
                if (biome == Biomes.SWAMPLAND && mobY > 50 && mobY < 70) {
                    chance = 0.5;
                    float moonPhase = world.getCurrentMoonPhaseFactor();
                    chance *= moonPhase;
                    int lightLevel = access.getLightFromNeighbors(posTarget, true);
                    chance *= Math.max(0, 8 - lightLevel) / 8.0;
                    chance *= getChanceValidPosition(EntityLiving.class, posTarget);
                } else if (chunk != null && chunk.getRandomWithSeed(987234911L).nextInt(10) == 0 && mobY < 40) {
                    chance = getChanceValidPosition(EntityLiving.class, posTarget) / 10.0;
                } else {
                    return 0.0;
                }
                if (world.getWorldInfo().getTerrainType() == WorldType.FLAT) {
                    return chance / 4.0;
                } else {
                    return chance;
                }
            }
        } else if (EntityWaterMob.class.isAssignableFrom(mobClass)) {
            if (EntitySquid.class.isAssignableFrom(mobClass)) {
                return booleanToChance(mobY > 45 && mobY < world.getSeaLevel())
                        * getChanceValidPosition(EntityWaterMob.class, posTarget);
            } else {
                return 1.0;
            }
        } else {
            return booleanToChance(blockDown != Blocks.MAGMA || SpawnChecker.isImmuneToFire(mobClass));
        }
    }

    @ParametersAreNonnullByDefault
    public double getChanceValidLight(Class<? extends EntityLiving> mobClass, BlockPos pos) {
        if (!EntityMob.class.isAssignableFrom(mobClass)) {
            return 0.0;
        }
        if (EntityBlaze.class.isAssignableFrom(mobClass) || EntityEndermite.class.isAssignableFrom(mobClass) || EntityGuardian.class.isAssignableFrom(mobClass) || EntitySilverfish.class.isAssignableFrom(mobClass)) {
            return 1.0;
        }
        int skyLight = access.getLightFor(EnumSkyBlock.SKY, pos);
        double factor = (32 - skyLight) / 32.0;
        int lightLevel = access.getLightFromNeighbors(pos, true);
        double rate = Math.max(0, 8 - lightLevel) / 8.0;
        return factor * rate;
    }

    /**
     * Checking random sizes of slimes and magma cubes
     * <p>
     * {@link EntityLiving#isNotColliding()}
     */
    @ParametersAreNonnullByDefault
    public double getChanceNotColliding(Class<? extends EntityLiving> mobClass, BlockPos posTarget) {
        // TODO: 2023/6/22,0022 Check random sizes of slimes and magma cubes
        return booleanToChance(checker.isNotColliding(mobClass, posTarget));
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
