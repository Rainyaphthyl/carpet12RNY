package carpet.utils.portalcalculator;

import carpet.utils.Messenger;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
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
public class PortalSilentSearcher implements Runnable
{
    private static final double INTER_DIM_RATE = 8.0;
    private static final double BORDER_WIDTH = 16.0;
    private static final int BORDER_POS = 29999872;
    /**
     * Stores the portal positions, keeping the order of vanilla portal search.
     * <p>
     * Ranges from [~-256, 0, ~-256] to [~+256, 255, ~+256],<br>
     * OR from [~-128, 0, ~-128] to [~+128, 255, ~+128],<br>
     * "~" ranges with pattern size, so the map size may be larger than 513.
     * <p>
     * order: x+, z+, y-
     */
    private final PortalMegaCache portalBlockCache = new PortalMegaCache();
    /**
     * Stores only the "valid" "lowest" portal block in portal patterns
     */
    private final PortalMegaCache portalBottomCache = new PortalMegaCache();
    private final Long2ObjectMap<Chunk> chunkCache = new Long2ObjectOpenHashMap<>(289);
    private final MinecraftServer server;
    private final Vec3d posTarget;
    private final EnumTargetDirection direction;
    private final EnumTargetArea area;
    private final DimensionType dimension;
    private WorldServer world = null;
    private BlockPos posCenter = null;
    private PortalPattern patternCenter = null;
    private PortalPattern patternResult = null;
    private boolean initialized = false;
    private boolean successful = false;
    private double distSqCache = 0.0;

    public PortalSilentSearcher(MinecraftServer server, Vec3d posTarget, DimensionType dimension, EnumTargetDirection direction, EnumTargetArea area)
    {
        this.server = server;
        this.posTarget = posTarget;
        this.direction = direction;
        this.area = area;
        this.dimension = dimension;
    }

    public boolean isSuccessful()
    {
        return successful;
    }

    /**
     * {@link net.minecraft.block.BlockPortal#createPatternHelper}
     */
    @Nullable
    private PortalPattern getParentPattern(BlockPos blockPos)
    {
        IBlockState blockState = getBlockStateSilent(blockPos);
        if (blockState == null)
        {
            return null;
        }
        MutablePortalPattern pattern = new MutablePortalPattern(this, blockPos, EnumFacing.Axis.X);
        if (!pattern.isValid())
        {
            pattern = new MutablePortalPattern(this, blockPos, EnumFacing.Axis.Z);
        }
        return pattern.isValid() ? pattern.toImmutable() : new PortalPattern(blockPos, blockPos);
    }

    @Nullable
    public IBlockState getBlockStateSilent(BlockPos pos)
    {
        try
        {
            return getBlockStateSilent(pos.getX(), pos.getY(), pos.getZ());
        }
        catch (NullPointerException e)
        {
            return null;
        }
    }

    @Nonnull
    public IBlockState getBlockStateSilent(int x, int y, int z) throws NullPointerException
    {
        if (y < 0 || y >= 256)
        {
            return Objects.requireNonNull(Blocks.AIR).getDefaultState();
        }
        else
        {
            Chunk chunk = getChunkSilent(x >> 4, z >> 4);
            return Objects.requireNonNull(chunk).getBlockState(x, y, z);
        }
    }

    @Nullable
    private Chunk getChunkSilent(int x, int z)
    {
        long index = ChunkPos.asLong(x, z);
        if (chunkCache.containsKey(index))
        {
            return chunkCache.get(index);
        }
        Chunk chunk = null;
        ChunkProviderServer provider = world.getChunkProvider();
        if (provider.chunkExists(x, z))
        {
            chunk = provider.loadedChunks.get(ChunkPos.asLong(x, z));
        }
        else if (provider.chunkLoader.isChunkGeneratedAt(x, z))
        {
            try
            {
                chunk = provider.chunkLoader.loadChunk_silent(world, x, z);
            }
            catch (IOException ignored)
            {
            }
        }
        if (chunk != null)
        {
            chunkCache.put(index, chunk);
        }
        return chunk;
    }

    @Nonnull
    private BlockPos clampTeleportDestination(@Nonnull Vec3d posTeleported) throws NullPointerException
    {
        return clampTeleportDestination(posTeleported.x, posTeleported.y, posTeleported.z);
    }

    @Nonnull
    private BlockPos clampTeleportDestination(double x, double y, double z) throws NullPointerException
    {
        // copied from vanilla codes
        WorldBorder borderObj = Objects.requireNonNull(world).getWorldBorder();
        x = MathHelper.clamp(x, borderObj.minX() + BORDER_WIDTH, borderObj.maxX() - BORDER_WIDTH);
        z = MathHelper.clamp(z, borderObj.minZ() + BORDER_WIDTH, borderObj.maxZ() - BORDER_WIDTH);
        x = MathHelper.clamp((int) x, -BORDER_POS, BORDER_POS);
        z = MathHelper.clamp((int) z, -BORDER_POS, BORDER_POS);
        return new BlockPos(x, y, z);
    }

    private void initFields() throws NullPointerException
    {
        double x = posTarget.x;
        double y = posTarget.y;
        double z = posTarget.z;
        switch (direction)
        {
            case FROM:
                switch (dimension)
                {
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

    /**
     * Simulates vanilla searching
     */
    @Nonnull
    private BlockPos findPointDestination(BlockPos posOrigin)
    {
        final int actualLimit = world.getActualHeight() - 1;
        BlockPos.PooledMutableBlockPos posResult = BlockPos.PooledMutableBlockPos.retain();
        BlockPos.PooledMutableBlockPos posPortal = BlockPos.PooledMutableBlockPos.retain();
        double distSqMin = -1.0;
        for (int bx = -128; bx <= 128; ++bx)
        {
            int xDetect = posOrigin.getX() + bx;
            for (int bz = -128; bz <= 128; ++bz)
            {
                int zDetect = posOrigin.getZ() + bz;
                posPortal.setPos(xDetect, 0, zDetect);
                for (int yDetect = actualLimit; yDetect >= 0; --yDetect)
                {
                    IBlockState stateToDetect = getBlockStateSilent(xDetect, yDetect, zDetect);
                    if (stateToDetect.getBlock() == Blocks.PORTAL)
                    {
                        Messenger.print_server_message(server, String.format("Detected PORTAL block at %s", new BlockPos(xDetect, yDetect, zDetect)));
                        // find the lowest portal block in current portal pattern to detect
                        int yBottom = yDetect - 1;
                        while (getBlockStateSilent(xDetect, yBottom, zDetect).getBlock() == Blocks.PORTAL)
                        {
                            Messenger.print_server_message(server, String.format("Detected PORTAL block at %s", new BlockPos(xDetect, yBottom, zDetect)));
                            --yBottom;
                        }
                        yDetect = yBottom + 1;
                        posPortal.setY(yBottom + 1);
                        double distSqTemp = posPortal.distanceSq(posOrigin);
                        Messenger.print_server_message(server, String.format("portal at %s, dist = %.1f", posPortal, Math.sqrt(distSqTemp)));
                        if (distSqMin < 0.0 || distSqTemp < distSqMin)
                        {
                            Messenger.print_server_message(server, String.format("closer portal! %.1f -> %.1f", Math.sqrt(distSqMin), Math.sqrt(distSqTemp)));
                            distSqMin = distSqTemp;
                            posResult.setPos(posPortal);
                        }
                    }
                }
            }
        }
        distSqCache = distSqMin;
        return posResult.toImmutable();
    }

    @Override
    public void run()
    {
        try
        {
            successful = false;
            initFields();
            if (area == EnumTargetArea.RANGE)
            {
                patternCenter = getParentPattern(posCenter);
            }
            else
            {
                patternCenter = new PortalPattern(posCenter, posCenter);
            }
            if (direction == EnumTargetDirection.FROM)
            {
                if (area == EnumTargetArea.POINT)
                {
                    BlockPos posDest = findPointDestination(posCenter);
                    PortalPattern patternDest = getParentPattern(posDest);
                    Messenger.print_server_message(server, String.format("Destination Block: %s, dist: %.1f", posDest, Math.sqrt(distSqCache)));
                    Messenger.print_server_message(server, String.format("Destination Frame: %s", patternDest));
                }
            }
            successful = true;
        }
        catch (Exception e)
        {
            Messenger.print_server_message(server, "Exceptions in \"/portal\"");
            e.printStackTrace();
        }
    }
}
