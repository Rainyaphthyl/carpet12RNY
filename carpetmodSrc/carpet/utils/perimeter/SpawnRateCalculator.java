package carpet.utils.perimeter;

import carpet.utils.SilentChunkReader;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

public class SpawnRateCalculator implements Runnable {
    private final WorldServer world;
    private final SilentChunkReader access;
    /**
     * Overrides the {@code posCorner*} fields if {@code posPeriCenter} is nonnull.
     */
    private final Vec3d posPeriCenter;
    private final BlockPos.MutableBlockPos posCornerMin = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos posCornerMax = new BlockPos.MutableBlockPos();
    private final Long2ObjectMap<BlockPos> possibleTargetSet = new Long2ObjectOpenHashMap<>();
    private boolean initialized = false;
    private boolean running = false;
    private boolean finished = false;

    private SpawnRateCalculator(@Nonnull WorldServer world, Vec3d posPeriCenter, BlockPos posCorner1, BlockPos posCorner2) throws NullPointerException {
        this.world = Objects.requireNonNull(world);
        access = Objects.requireNonNull(world.silentChunkReader);
        this.posPeriCenter = posPeriCenter;
        if (posPeriCenter == null) {
            posCornerMin.setPos(Objects.requireNonNull(posCorner1));
            posCornerMax.setPos(Objects.requireNonNull(posCorner2));
        }
    }

    @Nullable
    @ParametersAreNonnullByDefault
    public SpawnRateCalculator createInstance(WorldServer world, Vec3d posPeriCenter) {
        try {
            return new SpawnRateCalculator(world, posPeriCenter, null, null);
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Nullable
    @ParametersAreNonnullByDefault
    public SpawnRateCalculator createInstance(WorldServer world, BlockPos posTarget) {
        try {
            return new SpawnRateCalculator(world, null, posTarget, posTarget);
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Nullable
    @ParametersAreNonnullByDefault
    public SpawnRateCalculator createInstance(WorldServer world, BlockPos posCorner1, BlockPos posCorner2) {
        try {
            return new SpawnRateCalculator(world, null, posCorner1, posCorner2);
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Override
    public void run() {
        initialize();
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
}
