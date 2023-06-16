package carpet.utils.perimeter;

import carpet.utils.Messenger;
import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;
import java.util.*;

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
        if (levelMap != null) {
            if (distLevels == null) {
                distLevels = EnumDistLevel.values();
            }
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

    public int getSpotCountForDist(Class<? extends EntityLiving> entityType, EnumDistLevel... distLevels) {
        Object2IntSortedMap<EnumDistLevel> levelMap = specificCounts.get(entityType);
        return countLevelMap(levelMap, distLevels);
    }

    public int getSpotCountForDist(EntityLiving.SpawnPlacementType placementType, EnumDistLevel... distLevels) {
        Object2IntSortedMap<EnumDistLevel> levelMap = placementCounts.get(placementType);
        return countLevelMap(levelMap, distLevels);
    }

    public ITextComponent[] createBannedSampleReports(Class<? extends EntityLiving> entityType, int maxSampleNum) {
        List<ITextComponent> components = new ArrayList<>(Math.max(maxSampleNum, 0));
        Map<EnumDistLevel, Long2ObjectMap<BlockPos>> levelMap = spotSampleSets.get(entityType);
        if (levelMap != null) {
            ObjectSet<BlockPos> totalSampleSet = new ObjectOpenHashSet<>();
            Long2ObjectMap<BlockPos> sampleSet = levelMap.get(EnumDistLevel.BANNED);
            if (sampleSet != null) {
                totalSampleSet.addAll(sampleSet.values());
            }
            ObjectIterator<BlockPos> iterator = totalSampleSet.iterator();
            for (int i = 0; i < maxSampleNum && iterator.hasNext(); ++i) {
                BlockPos sample = iterator.next();
                if (sample != null) {
                    components.add(Messenger.c("w   ",
                            Messenger.tpa("m", sample.getX(), sample.getY(), sample.getZ())));
                }
            }
            if (iterator.hasNext()) {
                components.add(Messenger.c("m   ..."));
            }
        }
        return components.toArray(new ITextComponent[0]);
    }

    public ITextComponent[] createSpawningSampleReports(Class<? extends EntityLiving> entityType, int maxSampleNum) {
        List<ITextComponent> components = new ArrayList<>(Math.max(maxSampleNum, 0));
        Map<EnumDistLevel, Long2ObjectMap<BlockPos>> levelMap = spotSampleSets.get(entityType);
        if (levelMap != null) {
            Set<EnumDistLevel> distRange = new TreeSet<>();
            distRange.add(EnumDistLevel.NORMAL);
            distRange.add(EnumDistLevel.NEARBY);
            String color, note;
            if (PerimeterCalculator.canImmediatelyDespawn(entityType)) {
                color = "e";
                note = "within r=128 sphere";
            } else {
                distRange.add(EnumDistLevel.DISTANT);
                color = "q";
                note = "persistent creatures";
            }
            ObjectSet<BlockPos> totalSampleSet = new ObjectOpenHashSet<>();
            for (EnumDistLevel distLevel : distRange) {
                Long2ObjectMap<BlockPos> sampleSet = levelMap.get(distLevel);
                if (sampleSet != null) {
                    totalSampleSet.addAll(sampleSet.values());
                }
            }
            ObjectIterator<BlockPos> iterator = totalSampleSet.iterator();
            if (iterator.hasNext()) {
                components.add(Messenger.c("w Sample of effective locations (" + note + "):"));
            }
            for (int i = 0; i < maxSampleNum && iterator.hasNext(); ++i) {
                BlockPos sample = iterator.next();
                if (sample != null) {
                    components.add(Messenger.c("w   ",
                            Messenger.tpa(color, sample.getX(), sample.getY(), sample.getZ())));
                }
            }
            if (iterator.hasNext()) {
                components.add(Messenger.c(color + "   ..."));
            }
        }
        return components.toArray(new ITextComponent[0]);
    }

    public ITextComponent createStandardReport(Class<? extends EntityLiving> entityType) {
        int[] counts = new int[5];
        counts[0] = getSpotCountForDist(entityType, EnumDistLevel.NEARBY, EnumDistLevel.NORMAL);
        counts[1] = getSpotCountForDist(entityType, EnumDistLevel.DISTANT);
        counts[2] = counts[0] + counts[1];
        counts[3] = getSpotCountForDist(entityType, EnumDistLevel.CLOSE);
        counts[4] = getSpotCountForDist(entityType, EnumDistLevel.BANNED);
        String name = EntityList.getTranslationName(EntityList.getKey(entityType));
        String[] colors = new String[5];
        colors[0] = counts[0] > 0 ? "l " : "e ";
        colors[1] = counts[1] > 0 ? "y " : "d ";
        colors[2] = counts[2] > 0 ? "c " : "q ";
        colors[3] = counts[3] > 0 ? "r " : "n ";
        colors[4] = counts[4] > 0 ? "m " : "p ";
        return Messenger.c("w   " + name + ": ",
                colors[0] + " " + counts[0], "^e 24 <= dist <= 128",
                "g  + ", colors[1] + counts[1], "^d dist > 128",
                "g  = ", colors[2] + counts[2], "^q dist >= 24",
                "g  ; ", colors[3] + counts[3], "^n dist < 24",
                "g  ; ", colors[4] + counts[4], "^p banned by world spawn point");
    }

    public ITextComponent createStandardReport(EntityLiving.SpawnPlacementType placementType) {
        int[] counts = new int[5];
        counts[0] = getSpotCountForDist(placementType, EnumDistLevel.NEARBY, EnumDistLevel.NORMAL);
        counts[1] = getSpotCountForDist(placementType, EnumDistLevel.DISTANT);
        counts[2] = counts[0] + counts[1];
        counts[3] = getSpotCountForDist(placementType, EnumDistLevel.CLOSE);
        counts[4] = getSpotCountForDist(placementType, EnumDistLevel.BANNED);
        String name;
        switch (placementType) {
            case IN_WATER:
                name = "in-liquid";
                break;
            case ON_GROUND:
                name = "on-ground";
                break;
            case IN_AIR:
                name = "in-air";
                break;
            default:
                name = "unknown";
        }
        String[] colors = new String[5];
        colors[0] = counts[0] > 0 ? "l " : "e ";
        colors[1] = counts[1] > 0 ? "y " : "d ";
        colors[2] = counts[2] > 0 ? "c " : "q ";
        colors[3] = counts[3] > 0 ? "r " : "n ";
        colors[4] = counts[4] > 0 ? "m " : "p ";
        return Messenger.c("w   potential " + name + ": ",
                colors[0] + " " + counts[0], "^e 24 <= dist <= 128",
                "g  + ", colors[1] + counts[1], "^d dist > 128",
                "g  = ", colors[2] + counts[2], "^q dist >= 24",
                "g  ; ", colors[3] + counts[3], "^n dist < 24",
                "g  ; ", colors[4] + counts[4], "^p banned by world spawn point");
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
         * {@code r < 24} (to the World Spawn Point), Not Spawning
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
