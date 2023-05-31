package carpet.utils;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import java.util.*;

public class PerimeterResult {
    private final Object2IntMap<EnumCreatureType> generalCounts = new Object2IntOpenHashMap<>();
    private final Object2IntMap<Class<? extends EntityLiving>> specificCounts = new Object2IntOpenHashMap<>();
    private final Map<Class<? extends EntityLiving>, Set<BlockPos>> spotSamples = new HashMap<>();

    private PerimeterResult() {
    }

    @Nonnull
    public static PerimeterResult getEmptyResult() {
        PerimeterResult result = new PerimeterResult();
        for (EnumCreatureType creatureType : EnumCreatureType.values()) {
            result.generalCounts.put(creatureType, 0);
        }
        return result;
    }

    @Nonnull
    public static PerimeterResult getEmptyResult(Collection<Class<? extends EntityLiving>> entityTypes) {
        PerimeterResult result = getEmptyResult();
        if (entityTypes != null) {
            for (Class<? extends EntityLiving> entityType : entityTypes) {
                result.specificCounts.put(entityType, 0);
                result.spotSamples.put(entityType, new LinkedHashSet<>());
            }
        }
        return result;
    }
}
