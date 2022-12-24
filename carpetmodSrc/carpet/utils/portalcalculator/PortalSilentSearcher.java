package carpet.utils.portalcalculator;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Objects;

/**
 * Runs portal searching and matching without making chunks loaded.
 */
public class PortalSilentSearcher implements Runnable {
    private static final double INTER_DIM_RATE = 8.0;
    private static final double BORDER_WIDTH = 16.0;
    private static final int BORDER_POS = 29999872;
    /**
     * Stores the portal positions, keeping the order of vanilla portal search.
     * <p>
     * Ranges from [~-256, 0, ~-256] to [~+256, 255, ~+256],<br>
     * OR from [~-128, 0, ~-128] to [~+128, 255, ~+128],<br>
     * "~" ranges with pattern size, so the map size may be larger than 513.
     */
    private final ObjectSet<BlockPos> portalImageSource = null;
    private final MinecraftServer server;
    private final Vec3d posTarget;
    private final EnumTargetDirection direction;
    private final EnumTargetArea area;
    private final DimensionType dimension;
    private WorldServer world = null;
    private BlockPos posCenter = null;
    private PortalPattern patternCenter = null;
    private boolean initialized = false;
    private boolean successful = false;

    public PortalSilentSearcher(MinecraftServer server, Vec3d posTarget, DimensionType dimension, EnumTargetDirection direction, EnumTargetArea area) {
        this.server = server;
        this.posTarget = posTarget;
        this.direction = direction;
        this.area = area;
        this.dimension = dimension;
    }

    public boolean isSuccessful() {
        return successful;
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
    private BlockPos clampTeleportDestination(@Nonnull Vec3d posTeleported) throws NullPointerException {
        return clampTeleportDestination(posTeleported.x, posTeleported.y, posTeleported.z);
    }

    @Nonnull
    private BlockPos clampTeleportDestination(double x, double y, double z) throws NullPointerException {
        // copied from vanilla codes
        WorldBorder borderObj = Objects.requireNonNull(world).getWorldBorder();
        x = MathHelper.clamp(x, borderObj.minX() + BORDER_WIDTH, borderObj.maxX() - BORDER_WIDTH);
        z = MathHelper.clamp(z, borderObj.minZ() + BORDER_WIDTH, borderObj.maxZ() - BORDER_WIDTH);
        x = MathHelper.clamp((int) x, -BORDER_POS, BORDER_POS);
        z = MathHelper.clamp((int) z, -BORDER_POS, BORDER_POS);
        return new BlockPos(x, y, z);
    }

    private void initFields() throws NullPointerException {
        double x = posTarget.x;
        double y = posTarget.y;
        double z = posTarget.z;
        switch (direction) {
            case FROM:
                switch (dimension) {
                    case NETHER:
                        x *= INTER_DIM_RATE;
                        z *= INTER_DIM_RATE;
                        world = server.getWorld(DimensionType.OVERWORLD.getId());
                        break;
                    case OVERWORLD:
                        x /= INTER_DIM_RATE;
                        z /= INTER_DIM_RATE;
                        world = server.getWorld(DimensionType.NETHER.getId());
                        break;
                }
                break;
            case TO:
                world = server.getWorld(dimension.getId());
                break;
        }
        posCenter = clampTeleportDestination(x, y, z);
        initialized = true;
    }

    @Override
    public void run() {
        try {
            successful = false;
            initFields();
            if (area == EnumTargetArea.RANGE) {
                patternCenter = getParentPattern(posCenter);
            } else {
                patternCenter = new PortalPattern(posCenter, posCenter);
            }
            // TODO: 2022/12/25,0025 portal calculator to be continued...
        } catch (Throwable ignored) {
        }
    }
}
