package carpet.utils.portalcalculator;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.util.Objects;

/**
 * Runs portal searching and matching without making chunks loaded.
 */
public class PortalSilentSearcher implements Runnable {
    /**
     * Stores the portal positions, keeping the order of vanilla portal search.
     * <p>
     * Ranges from [~-256, 0, ~-256] to [~+256, 255, ~+256],<br>
     * OR from [~-128, 0, ~-128] to [~+128, 255, ~+128],<br>
     * "~" ranges with pattern size, so the map size may be larger than 513.
     */
    private final ObjectSet<BlockPos> portalImageSource = null;
    private final WorldServer world;
    private final Vec3d posTarget;
    private final EnumTargetDirection direction;
    private final EnumTargetArea area;
    private final DimensionType dimension;
    private BlockPos posCenter = null;
    private PortalPattern patternCenter = null;

    @ParametersAreNonnullByDefault
    public PortalSilentSearcher(WorldServer worldIn, Vec3d posTarget, DimensionType dimension, EnumTargetDirection direction, EnumTargetArea area) {
        this.world = worldIn;
        this.posTarget = posTarget;
        this.direction = direction;
        this.area = area;
        this.dimension = dimension;
    }

    @Nullable
    private PortalPattern getParentPattern(BlockPos blockPos) {
        IBlockState blockState = getBlockStateSilent(blockPos);
        if (blockState == null) {
            return null;
        }
        return new PortalPattern(0, 0, 0, 0, 0, 0);
    }

    @Nullable
    private IBlockState getBlockStateSilent(BlockPos pos) {
        try {
            return getBlockStateSilent(pos.getX(), pos.getY(), pos.getZ());
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Nonnull
    private IBlockState getBlockStateSilent(int x, int y, int z) throws NullPointerException {
        if (y < 0 || y >= 256) {
            return Objects.requireNonNull(Blocks.AIR).getDefaultState();
        } else {
            Chunk chunk = getChunkSilent(x >> 4, z >> 4);
            return Objects.requireNonNull(chunk).getBlockState(x, y, z);
        }
    }

    @Nullable
    private Chunk getChunkSilent(int x, int z) {
        Chunk chunk = null;
        ChunkProviderServer provider = world.getChunkProvider();
        if (provider.chunkExists(x, z)) {
            chunk = provider.loadedChunks.get(ChunkPos.asLong(x, z));
        } else if (provider.chunkLoader.isChunkGeneratedAt(x, z)) {
            try {
                chunk = provider.chunkLoader.loadChunk(world, x, z, true);
            } catch (IOException ignored) {
            }
        }
        return chunk;
    }

    @Nonnull
    private BlockPos getMatchingBlockPos(@Nonnull Vec3d posEntity, boolean intoNether) {
        return new BlockPos(posEntity);
    }

    @Override
    public void run() {
        posCenter = getMatchingBlockPos(posTarget,
                (dimension == DimensionType.NETHER && direction == EnumTargetDirection.TO)
                        || (dimension == DimensionType.OVERWORLD && direction == EnumTargetDirection.FROM));
        if (area == EnumTargetArea.RANGE) {
            patternCenter = getParentPattern(posCenter);
        } else {
            patternCenter = new PortalPattern(posCenter, posCenter);
        }
    }
}
