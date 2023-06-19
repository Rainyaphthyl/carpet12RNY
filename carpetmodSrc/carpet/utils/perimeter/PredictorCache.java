package carpet.utils.perimeter;

import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;

public class PredictorCache extends Object2DoubleOpenHashMap<PredictorKey> {
    public double put(Class<? extends EntityLiving> mobClass, @Nonnull BlockPos blockPos, int roundsLeft, int roundsTotal, double v) {
        PredictorKey k = new PredictorKey(mobClass, blockPos, roundsLeft, roundsTotal);
        return put(k, v);
    }

    public double addTo(Class<? extends EntityLiving> mobClass, @Nonnull BlockPos blockPos, int roundsLeft, int roundsTotal, double inc) {
        PredictorKey k = new PredictorKey(mobClass, blockPos, roundsLeft, roundsTotal);
        return addTo(k, inc);
    }

    public double removeDouble(Class<? extends EntityLiving> mobClass, @Nonnull BlockPos blockPos, int roundsLeft, int roundsTotal) {
        PredictorKey k = new PredictorKey(mobClass, blockPos, roundsLeft, roundsTotal);
        return removeDouble(k);
    }

    public double getDouble(Class<? extends EntityLiving> mobClass, @Nonnull BlockPos blockPos, int roundsLeft, int roundsTotal) {
        PredictorKey k = new PredictorKey(mobClass, blockPos, roundsLeft, roundsTotal);
        return getDouble(k);
    }

    public boolean containsKey(Class<? extends EntityLiving> mobClass, @Nonnull BlockPos blockPos, int roundsLeft, int roundsTotal) {
        PredictorKey k = new PredictorKey(mobClass, blockPos, roundsLeft, roundsTotal);
        return containsKey(k);
    }
}
