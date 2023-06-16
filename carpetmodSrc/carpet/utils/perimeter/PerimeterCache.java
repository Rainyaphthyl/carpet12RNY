package carpet.utils.perimeter;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.util.math.ChunkPos;

public class PerimeterCache {
    /**
     * Random order for optimization
     */
    public final Object2IntMap<ChunkPos> eligibleChunkHeightMap = new Object2IntOpenHashMap<>();
}
