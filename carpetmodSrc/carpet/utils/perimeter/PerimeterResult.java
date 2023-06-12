package carpet.utils.perimeter;

import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2IntAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import java.util.*;

public class PerimeterResult {
    private final Object2IntMap<EnumCreatureType> creatureCounts = new Object2IntOpenHashMap<>();
    private final Map<EntityLiving.SpawnPlacementType, Object2IntSortedMap<EnumDistLevel>> placementCounts = new HashMap<>();
    private final Map<Class<? extends EntityLiving>, Object2IntSortedMap<EnumDistLevel>> specificCounts = new HashMap<>();
    private final Map<Class<? extends EntityLiving>, Map<EnumDistLevel, LongSet>> spotSampleSets = new HashMap<>();

    private PerimeterResult() {
    }

    @Nonnull
    public static PerimeterResult createEmptyResult() {
        PerimeterResult result = new PerimeterResult();
        for (EnumCreatureType creatureType : EnumCreatureType.values()) {
            result.creatureCounts.put(creatureType, 0);
        }
        return result;
    }

    @Nonnull
    public static Object2IntSortedMap<EnumDistLevel> createNewLevelMap() {
        Object2IntSortedMap<EnumDistLevel> tempMap = new Object2IntAVLTreeMap<>();
        tempMap.defaultReturnValue(0);
        return tempMap;
    }

    @Nonnull
    public LongSet getSampleSet(Class<? extends EntityLiving> entityType, EnumDistLevel distLevel) {
        Map<EnumDistLevel, LongSet> levelMap = spotSampleSets.computeIfAbsent(entityType, et -> new EnumMap<>(EnumDistLevel.class));
        return levelMap.computeIfAbsent(distLevel, dl -> new LongAVLTreeSet());
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
            LongSet sampleSet = getSampleSet(entityType, distLevel);
            if (!sampleSet.contains(index)) {
                sampleSet.add(index);
                adding = true;
            }
        }
        if (adding) {
            Object2IntSortedMap<EnumDistLevel> levelMap = specificCounts.computeIfAbsent(entityType, et -> createNewLevelMap());
            int count = levelMap.getInt(distLevel);
            levelMap.put(distLevel, count + 1);
        }
    }

    public boolean containsPos(Class<? extends EntityLiving> entityType, BlockPos pos) {
        if (pos == null) {
            return false;
        }
        long index = pos.toLong();
        List<Map<EnumDistLevel, LongSet>> maps = new ArrayList<>();
        if (entityType != null) {
            maps.add(spotSampleSets.get(entityType));
        } else {
            maps.addAll(spotSampleSets.values());
        }
        for (Map<EnumDistLevel, LongSet> levelMap : maps) {
            if (levelMap != null) {
                for (LongSet samples : levelMap.values()) {
                    if (samples != null && samples.contains(index)) {
                        return true;
                    }
                }
            }
        }
        return false;
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
