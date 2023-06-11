package carpet.utils.perimeter;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PerimeterResult {
    private final Object2IntMap<EnumCreatureType> creatureCounts = new Object2IntOpenHashMap<>();
    /**
     * Spots are counted with long value: the higher bits for the spots outside 128m, the lower bits for the inner spots
     */
    private final Object2LongMap<EntityLiving.SpawnPlacementType> placementCounts = new Object2LongOpenHashMap<>();
    private final Object2IntMap<Class<? extends EntityLiving>> specificCounts = new Object2IntOpenHashMap<>();
    private final Map<Class<? extends EntityLiving>, Set<BlockPos>> spotSampleSets = new HashMap<>();

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

    public void addGeneralSpot(EntityLiving.SpawnPlacementType placementType, BlockPos posTarget) {

    }

    public boolean containsPos(Class<? extends EntityLiving> entityType, BlockPos pos) {
        if (pos == null) {
            return false;
        }
        boolean answer = false;
        if (entityType != null) {
            Set<BlockPos> samples = spotSampleSets.get(entityType);
            if (samples != null && samples.contains(pos)) {
                answer = true;
            }
        } else {
            for (Set<BlockPos> samples : spotSampleSets.values()) {
                if (samples != null && samples.contains(pos)) {
                    answer = true;
                    break;
                }
            }
        }
        return answer;
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
         * {@code r < 24}, Not Spawning (including dist to the world's spawning point)
         */
        CLOSE;

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
