package carpet.utils.perimeter;

import carpet.CarpetServer;
import carpet.utils.LRUCache;
import carpet.utils.SilentChunkReader;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.*;
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
    public SpawningCalculator createInstance(WorldServer world, Vec3d posPeriCenter) {
        try {
            return new SpawningCalculator(world, posPeriCenter, null, null);
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Nullable
    @ParametersAreNonnullByDefault
    public SpawningCalculator createInstance(WorldServer world, BlockPos posTarget) {
        try {
            return new SpawningCalculator(world, null, posTarget, posTarget);
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Nullable
    @ParametersAreNonnullByDefault
    public SpawningCalculator createInstance(WorldServer world, BlockPos posCorner1, BlockPos posCorner2) {
        try {
            return new SpawningCalculator(world, null, posCorner1, posCorner2);
        } catch (NullPointerException e) {
            return null;
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
                    if (checkSpawningChance(posIter, null)) {
                        possibleTargetSet.add(posIter.toImmutable());
                    }
                }
            }
        }
        targetCheckFinished = true;
    }

    @ParametersAreNonnullByDefault
    public boolean checkSpawningChance(BlockPos posTarget, @Nullable Class<? extends EntityLiving> mobClass, CheckStage... steps) {
        if (steps.length == 0) {
            steps = CheckStage.values();
        }
        for (CheckStage step : steps) {
            if (!checkSpawningChance(posTarget, mobClass, step)) {
                return false;
            }
        }
        return true;
    }

    @ParametersAreNonnullByDefault
    public boolean checkSpawningChance(BlockPos posTarget, @Nullable Class<? extends EntityLiving> mobClass, CheckStage step) {
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
        Double distSq = distSqCache.get(posTarget);
        if (distSq == null) {
            int posY = posTarget.getY();
            float mobX = (float) posTarget.getX() + 0.5F;
            float mobZ = (float) posTarget.getZ() + 0.5F;
            distSq = posPeriCenter.squareDistanceTo(mobX, posY, mobZ);
            distSqCache.put(posTarget.toImmutable(), distSq);
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
     * @param mobId       {@code m}
     * @param posCurr     {@code \vec{r}}
     * @param roundsLeft  {@code w}
     * @param roundsTotal {@code c}
     * @return {@code s_m(r, w, c)}
     */
    public double getSpawningStepRate(int mobId, @Nonnull BlockPos posCurr, int roundsLeft, int roundsTotal) {
        PredictorKey key = new PredictorKey(mobId, posCurr, roundsLeft, roundsTotal);
        Double value;
        synchronized (spawningRateCache) {
            value = spawningRateCache.get(key);
        }
        if (value != null) {
            return value;
        }
        double rateCurr = 0.0;
        if (roundsLeft >= 0 && roundsLeft <= roundsTotal - 2) {
            // 0 <= w <= c-2
        } else if (roundsLeft == roundsTotal - 1) {
            // w == c-1
        } else if (roundsLeft == roundsTotal) {
            // w == c
        } else {
            return Double.NaN;
        }
        value = rateCurr;
        synchronized (spawningRateCache) {
            spawningRateCache.put(key, value);
        }
        return rateCurr;
    }

    public double getChancePosInChunk(@Nonnull BlockPos posTarget) {
        int height = access.getSpawningColumnHeight(posTarget);
        return 1.0 / (height * SpawnChecker.SECTION_UNIT * SpawnChecker.SECTION_UNIT);
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
}
