package carpet.utils.portalcalculator;

import net.minecraft.util.math.BlockPos;

import javax.annotation.ParametersAreNonnullByDefault;

public class PortalPattern {
    public final int xMin;
    public final int yMin;
    public final int zMin;
    public final int xMax;
    public final int yMax;
    public final int zMax;

    public PortalPattern(int x1, int y1, int z1, int x2, int y2, int z2) {
        if (x1 < x2) {
            xMin = x1;
            xMax = x2;
        } else {
            xMin = x2;
            xMax = x1;
        }
        if (y1 < y2) {
            yMin = y1;
            yMax = y2;
        } else {
            yMin = y2;
            yMax = y1;
        }
        if (z1 < z2) {
            zMin = z1;
            zMax = z2;
        } else {
            zMin = z2;
            zMax = z1;
        }
    }

    @ParametersAreNonnullByDefault
    public PortalPattern(BlockPos corner1, BlockPos corner2) {
        this(corner1.getX(), corner1.getY(), corner1.getZ(), corner2.getX(), corner1.getY(), corner2.getZ());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof PortalPattern) {
            return xMin == ((PortalPattern) obj).xMin && xMax == ((PortalPattern) obj).xMax
                    && yMin == ((PortalPattern) obj).yMin && yMax == ((PortalPattern) obj).yMax
                    && zMin == ((PortalPattern) obj).zMin && zMax == ((PortalPattern) obj).zMax;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return String.format("PortalRange[(%d,%d,%d)->(%d,%d,%d)]", xMin, yMin, zMin, xMax, yMax, zMax);
    }
}
