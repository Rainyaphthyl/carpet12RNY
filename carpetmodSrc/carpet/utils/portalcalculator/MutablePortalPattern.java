package carpet.utils.portalcalculator;

import carpet.utils.SilentChunkReader;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A PortalPattern expanding from a given BlockPos
 */
public class MutablePortalPattern {
    private final SilentChunkReader reader;
    private final BlockPos posInitial;
    private final EnumFacing.Axis axis;
    private final EnumFacing rightDir;
    private final EnumFacing leftDir;
    private BlockPos bottomLeft = null;
    private BlockPos topRight = null;
    private int height = 0;
    private int width = 0;

    @ParametersAreNonnullByDefault
    public MutablePortalPattern(SilentChunkReader reader, BlockPos posInitial, EnumFacing.Axis axis, boolean expansive) {
        this.reader = reader;
        this.posInitial = posInitial;
        this.axis = axis;
        if (axis == EnumFacing.Axis.X) {
            leftDir = EnumFacing.EAST;
            rightDir = EnumFacing.WEST;
        } else {
            leftDir = EnumFacing.NORTH;
            rightDir = EnumFacing.SOUTH;
        }
        if (expansive) {
            expand();
        }
    }

    @ParametersAreNonnullByDefault
    public MutablePortalPattern(SilentChunkReader reader, BlockPos posInitial, EnumFacing.Axis axis) {
        this(reader, posInitial, axis, true);
    }

    public static boolean isEmptyBlock(IBlockState blockState) {
        if (blockState == null) {
            return false;
        }
        Material material = blockState.getMaterial();
        Block blockIn = blockState.getBlock();
        return material == Material.AIR || blockIn == Blocks.FIRE || blockIn == Blocks.PORTAL;
    }

    public PortalPattern toImmutable() {
        if (bottomLeft != null && topRight != null) {
            return new PortalPattern(bottomLeft, topRight);
        } else {
            return new PortalPattern(posInitial, posInitial);
        }
    }

    public EnumFacing.Axis getAxis() {
        return axis;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public boolean isEmptyBlockAt(int x, int y, int z) {
        try {
            IBlockState blockState = reader.getBlockState(x, y, z);
            return isEmptyBlock(blockState);
        } catch (NullPointerException e) {
            return false;
        }
    }

    public Block getBlockAt(int x, int y, int z) {
        try {
            IBlockState blockState = reader.getBlockState(x, y, z);
            return blockState.getBlock();
        } catch (NullPointerException e) {
            return null;
        }
    }

    public boolean isValid() {
        return bottomLeft != null && width >= 2 && width <= 21 && height >= 3 && height <= 21;
    }

    /**
     * {@link net.minecraft.block.BlockPortal.Size}
     */
    public void expand() {
        BlockPos.PooledMutableBlockPos posSlide = BlockPos.PooledMutableBlockPos.retain();
        posSlide.setPos(posInitial);
        int yActualBottom = posInitial.getY();
        while (yActualBottom > posInitial.getY() - 21 && yActualBottom > 0
                && isEmptyBlockAt(posInitial.getX(), yActualBottom - 1, posInitial.getZ())) {
            --yActualBottom;
        }
        posSlide.setY(yActualBottom);
        int distance = getDistanceUntilEdge(posSlide, leftDir) - 1;
        if (distance >= 0) {
            bottomLeft = posSlide.offset(leftDir, distance);
            width = getDistanceUntilEdge(bottomLeft, rightDir);

            if (width < 2 || width > 21) {
                bottomLeft = null;
                width = 0;
            }
        }
        if (bottomLeft != null) {
            calculatePortalHeight();
        }
        if (bottomLeft != null) {
            topRight = bottomLeft.offset(rightDir, width - 1).up(height - 1);
        }
    }

    @ParametersAreNonnullByDefault
    private int getDistanceUntilEdge(BlockPos posOrigin, EnumFacing facing) {
        int dx = 0, dz = 0;
        switch (facing) {
            case EAST:
                dx = 1;
                break;
            case WEST:
                dx = -1;
                break;
            case SOUTH:
                dz = 1;
                break;
            case NORTH:
                dz = -1;
                break;
            default:
                return 0;
        }
        int x = posOrigin.getX(), z = posOrigin.getZ();
        int yCurr = posOrigin.getY();
        int yFloor = yCurr - 1;
        int distance = 0;
        while (distance < 22) {
            if (!isEmptyBlockAt(x, yCurr, z) || getBlockAt(x, yFloor, z) != Blocks.OBSIDIAN) {
                break;
            }
            x += dx;
            z += dz;
            ++distance;
        }
        x = posOrigin.getX() + dx * distance;
        z = posOrigin.getZ() + dz * distance;
        Block blockWall = getBlockAt(x, yCurr, z);
        return blockWall == Blocks.OBSIDIAN ? distance : 0;
    }

    private void calculatePortalHeight() {
        int dx = 0, dz = 0;
        switch (rightDir) {
            case EAST:
                dx = 1;
                break;
            case WEST:
                dx = -1;
                break;
            case SOUTH:
                dz = 1;
                break;
            case NORTH:
                dz = -1;
                break;
            default:
                return;
        }
        int x, z;
        int y = bottomLeft.getY();
        labelInit:
        for (height = 0; height < 21; ++height) {
            x = bottomLeft.getX();
            z = bottomLeft.getZ();
            for (int i = 0; i < width; ++i) {
                if (!isEmptyBlockAt(x, y, z)) {
                    break labelInit;
                }
                Block block;
                // Ignoring ++portalBlockCount
                if (i == 0) {
                    block = getBlockAt(x - dx, y, z - dz);
                    if (block != Blocks.OBSIDIAN) {
                        break labelInit;
                    }
                } else if (i == width - 1) {
                    block = getBlockAt(x + dx, y, z + dz);
                    if (block != Blocks.OBSIDIAN) {
                        break labelInit;
                    }
                }
                x += dx;
                z += dz;
            }
            ++y;
        }
        x = bottomLeft.getX();
        z = bottomLeft.getZ();
        y = bottomLeft.getY() + height;
        for (int i = 0; i < width; ++i) {
            if (getBlockAt(x, y, z) != Blocks.OBSIDIAN) {
                height = 0;
                break;
            }
            x += dx;
            z += dz;
        }
        if (height > 21 || height < 3) {
            bottomLeft = null;
            width = 0;
            height = 0;
        }
    }

}
