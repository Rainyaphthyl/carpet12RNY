package carpet.utils.perimeter;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;

public class PredictorKey implements Comparable<PredictorKey> {
    public final int blockY;
    public final int blockX;
    public final int blockZ;
    /**
     * {@link EntityList#init()}
     */
    public final int mobId;
    public final int rounds;
    public final int groupSize;

    private PredictorKey(int mobId, int blockX, int blockY, int blockZ, int rounds, int groupSize) {
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        if (rounds == groupSize) {
            this.mobId = -1;
        } else {
            this.mobId = mobId;
        }
        this.rounds = rounds;
        this.groupSize = groupSize;
    }

    public PredictorKey(Class<? extends EntityLiving> mobClass, @Nonnull BlockPos blockPos, int rounds, int groupSize) {
        this(blockPos.getZ(), EntityList.REGISTRY.getIDForObject(mobClass), blockPos.getX(), blockPos.getY(), rounds, groupSize);
    }

    public PredictorKey(int mobId, @Nonnull BlockPos blockPos, int rounds, int groupSize) {
        this(blockPos.getZ(), mobId, blockPos.getX(), blockPos.getY(), rounds, groupSize);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof PredictorKey) {
            PredictorKey that = (PredictorKey) o;
            return mobId == that.mobId && rounds == that.rounds && groupSize == that.groupSize
                    && blockX == that.blockX && blockY == that.blockY && blockZ == that.blockZ;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = mobId;
        hash = 31 * hash + blockX;
        hash = 31 * hash + blockY;
        hash = 31 * hash + blockZ;
        hash = 31 * hash + rounds;
        hash = 31 * hash + groupSize;
        return hash;
    }

    @Override
    public int compareTo(@Nonnull PredictorKey o) {
        if (this == o) {
            return 0;
        } else if (mobId != o.mobId) {
            return mobId < o.mobId ? -1 : 1;
        } else if (groupSize != o.groupSize) {
            return groupSize < o.groupSize ? -1 : 1;
        } else if (rounds != o.rounds) {
            return rounds < o.rounds ? -1 : 1;
        } else if (blockY != o.blockY) {
            return blockY < o.blockY ? -1 : 1;
        } else if (blockX != o.blockX) {
            return blockX < o.blockX ? -1 : 1;
        } else if (blockZ != o.blockZ) {
            return blockZ < o.blockZ ? -1 : 1;
        } else {
            return 0;
        }
    }
}