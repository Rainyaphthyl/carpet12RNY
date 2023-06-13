package carpet.utils.perimeter;

import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class PerimeterResult {
    private final Map<EntityLiving.SpawnPlacementType, Object2IntSortedMap<EnumDistLevel>> placementCounts = new HashMap<>();
    private final Map<Class<? extends EntityLiving>, Object2IntSortedMap<EnumDistLevel>> specificCounts = new HashMap<>();
    private final Map<Class<? extends EntityLiving>, Map<EnumDistLevel, Long2ObjectMap<BlockPos>>> spotSampleSets = new HashMap<>();

    private PerimeterResult() {
    }

    @Nonnull
    public static PerimeterResult createEmptyResult() {
        return new PerimeterResult();
    }

    @Nonnull
    private static Object2IntSortedMap<EnumDistLevel> createNewLevelMap() {
        Object2IntSortedMap<EnumDistLevel> tempMap = new Object2IntAVLTreeMap<>();
        tempMap.defaultReturnValue(0);
        return tempMap;
    }

    private static int countLevelMap(Object2IntMap<EnumDistLevel> levelMap, EnumDistLevel... distLevels) {
        int count = 0;
        if (distLevels == null) {
            distLevels = EnumDistLevel.values();
        }
        if (levelMap != null) {
            for (EnumDistLevel distLevel : distLevels) {
                if (levelMap.containsKey(distLevel)) {
                    count += levelMap.getInt(distLevel);
                }
            }
        }
        return count;
    }

    @Nonnull
    private Long2ObjectMap<BlockPos> getSampleSet(Class<? extends EntityLiving> entityType, EnumDistLevel distLevel) {
        Map<EnumDistLevel, Long2ObjectMap<BlockPos>> levelMap = spotSampleSets.computeIfAbsent(entityType, et -> new EnumMap<>(EnumDistLevel.class));
        return levelMap.computeIfAbsent(distLevel, dl -> new Long2ObjectAVLTreeMap<>());
    }

    public void addGeneralSpot(EntityLiving.SpawnPlacementType placementType, EnumDistLevel distLevel) {
        Object2IntSortedMap<EnumDistLevel> levelMap = placementCounts.computeIfAbsent(placementType, pt -> createNewLevelMap());
        int count = levelMap.getInt(distLevel);
        levelMap.put(distLevel, count + 1);
    }

    public void addSpecificSpot(Class<? extends EntityLiving> entityType, EnumDistLevel distLevel, BlockPos pos) {
        boolean adding = false;
        if (pos == null) {
            adding = true;
        } else {
            long index = pos.toLong();
            Long2ObjectMap<BlockPos> sampleSet = getSampleSet(entityType, distLevel);
            if (!sampleSet.containsKey(index)) {
                sampleSet.put(index, pos.toImmutable());
                adding = true;
            }
        }
        if (adding) {
            Object2IntSortedMap<EnumDistLevel> levelMap = specificCounts.computeIfAbsent(entityType, et -> createNewLevelMap());
            int count = levelMap.getInt(distLevel);
            levelMap.put(distLevel, count + 1);
        }
    }

    public int getSpecificCount(Class<? extends EntityLiving> entityType, EnumDistLevel... distLevels) {
        Object2IntSortedMap<EnumDistLevel> levelMap = specificCounts.get(entityType);
        return countLevelMap(levelMap, distLevels);
    }

    public int getPlacementCount(EntityLiving.SpawnPlacementType placementType, EnumDistLevel... distLevels) {
        Object2IntSortedMap<EnumDistLevel> levelMap = placementCounts.get(placementType);
        return countLevelMap(levelMap, distLevels);
    }

    public enum EnumDistLevel {
        /**
         * {@code r > 128}, Immediately Despawning
         */
        DISTANT,
        /**
         * {@code 32 < r <= 128}, Randomly Despawning
         */
        NORMAL,
        /**
         * {@code 24 <= r <= 32}, Not Despawning
         */
        NEARBY,
        /**
         * {@code r < 24} (to the player), Not Spawning
         */
        CLOSE,
        /**
         * {@code r < 24} (to the World Spawn), Not Spawning
         */
        BANNED;

        public static EnumDistLevel getLevelOfDistSq(double distSq) {
            if (distSq > 1024.0) {
                if (distSq > 16384.0) {
                    return DISTANT;
                } else {
                    return NORMAL;
                }
            } else {
                if (distSq >= 576.0) {
                    return NEARBY;
                } else {
                    return CLOSE;
                }
            }
        }
    }
}
