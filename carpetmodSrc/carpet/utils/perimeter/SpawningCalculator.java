package carpet.utils.perimeter;

import carpet.utils.LRUCache;
import carpet.utils.SilentChunkReader;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

public class SpawningCalculator {
    public static final int MIN_GROUP_NUM = 1;
    public static final int MAX_GROUP_NUM = 4;
    private final WorldServer world;
    private final SilentChunkReader access;
    private final SpawnChecker checker;
    private final Vec3d posPeriCenter;
    private final BlockPos.MutableBlockPos posCornerMin = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos posCornerMax = new BlockPos.MutableBlockPos();
    private final Long2ObjectMap<BlockPos> possibleTargetSet = new Long2ObjectOpenHashMap<>();
    private final LRUCache<BlockPos, Double> distSqCache = new LRUCache<>(64);
    private boolean initialized = false;
    private boolean running = false;
    private boolean finished = false;

    private SpawningCalculator(@Nonnull WorldServer world, Vec3d posPeriCenter, BlockPos posCorner1, BlockPos posCorner2) throws NullPointerException {
        this.world = Objects.requireNonNull(world);
        access = Objects.requireNonNull(world.silentChunkReader);
        checker = new SpawnChecker(world, posPeriCenter);
        this.posPeriCenter = posPeriCenter;
        if (posPeriCenter == null) {
            posCornerMin.setPos(Objects.requireNonNull(posCorner1));
            posCornerMax.setPos(Objects.requireNonNull(posCorner2));
        } else if (posCorner1 != null && posCorner2 != null) {
            posCornerMin.setPos(posCorner1);
            posCornerMax.setPos(posCorner2);
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

    private synchronized void initialize() {
        if (!initialized) {
            if (posPeriCenter == null) {
                sortCorners();
            }
            initialized = true;
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
            posCornerMin.setPos(x1, y1, z1);
            posCornerMax.setPos(x2, y2, z2);
        }
    }

    @ParametersAreNonnullByDefault
    public boolean checkSpawningChance(BlockPos posTarget, Class<? extends EntityLiving> mobClass, CheckStage... steps) {
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
    public boolean checkSpawningChance(BlockPos posTarget, Class<? extends EntityLiving> mobClass, CheckStage step) {
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

    public double getTargetSpawningRate(@Nonnull BlockPos posTarget, Class<? extends EntityLiving> mobClass) {
        return 0.0;
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
