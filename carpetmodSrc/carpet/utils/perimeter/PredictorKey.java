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
    public final int roundsLeft;
    public final int roundsTotal;

    private PredictorKey(int mobId, int blockX, int blockY, int blockZ, int roundsLeft, int roundsTotal) {
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.mobId = roundsLeft == roundsTotal ? -1 : mobId;
        this.roundsLeft = roundsLeft;
        this.roundsTotal = roundsTotal;
    }

    public PredictorKey(Class<? extends EntityLiving> mobClass, @Nonnull BlockPos blockPos, int roundsLeft, int roundsTotal) {
        this(blockPos.getZ(), EntityList.REGISTRY.getIDForObject(mobClass), blockPos.getX(), blockPos.getY(), roundsLeft, roundsTotal);
    }

    public PredictorKey(int mobId, @Nonnull BlockPos blockPos, int roundsLeft, int roundsTotal) {
        this(blockPos.getZ(), mobId, blockPos.getX(), blockPos.getY(), roundsLeft, roundsTotal);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof PredictorKey) {
            PredictorKey that = (PredictorKey) o;
            return mobId == that.mobId && roundsLeft == that.roundsLeft && roundsTotal == that.roundsTotal
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
        hash = 31 * hash + roundsLeft;
        hash = 31 * hash + roundsTotal;
        return hash;
    }

    @Override
    public int compareTo(@Nonnull PredictorKey o) {
        if (this == o) {
            return 0;
        } else if (mobId != o.mobId) {
            return mobId < o.mobId ? -1 : 1;
        } else if (roundsTotal != o.roundsTotal) {
            return roundsTotal < o.roundsTotal ? -1 : 1;
        } else if (roundsLeft != o.roundsLeft) {
            return roundsLeft < o.roundsLeft ? -1 : 1;
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