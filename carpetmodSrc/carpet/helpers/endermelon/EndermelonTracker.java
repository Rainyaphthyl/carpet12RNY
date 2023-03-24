package carpet.helpers.endermelon;

import carpet.utils.Messenger;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Tracks ONE enderman in the endermelon farm
 */
public class EndermelonTracker {
    public static boolean enabled = false;
    public final MinecraftServer server;
    public final World world;
    public boolean running = false;
    public EntityEnderman mob;
    public double melonVolume = 0.0;
    /**
     * {@code N/48}
     */
    public DoubleArrayList melonDensityList = new DoubleArrayList();
    /**
     * {@code 1/20 or 1/57}
     */
    public DoubleArrayList blockTakingChanceList = new DoubleArrayList();

    @ParametersAreNonnullByDefault
    public EndermelonTracker(EntityEnderman mob) throws NullPointerException {
        this.mob = mob;
        server = mob.getServer();
        world = mob.getEntityWorld();
    }

    /**
     * {@link net.minecraft.entity.monster.EntityEnderman}
     */
    public void checkSurroundingMelons(boolean verbose) {
        Vec3d boxCornerMin = new Vec3d(mob.posX - 2.0, mob.posY, mob.posZ - 2.0);
        Vec3d boxCornerMax = new Vec3d(boxCornerMin.x + 4.0, boxCornerMin.y + 3.0, boxCornerMin.z + 4.0);
        BlockPos gridMin = new BlockPos(boxCornerMin);
        BlockPos gridMax = new BlockPos(boxCornerMax);
        if (!world.isAreaLoaded(gridMin, gridMax, false)) {
            return;
        }
        melonVolume = 0.0;
        for (int y = gridMin.getY(); y <= gridMax.getY(); ++y) {
            Vec3d pointCenter = new Vec3d((float) MathHelper.floor(mob.posX) + 0.5F, (float) y + 0.5F, (float) MathHelper.floor(mob.posZ) + 0.5F);
            for (int z = gridMin.getZ(); z <= gridMax.getZ(); ++z) {
                for (int x = gridMin.getX(); x <= gridMax.getX(); ++x) {
                    BlockPos gridAim = new BlockPos(x, y, z);
                    IBlockState iblockstate = world.getBlockState(gridAim, "carpet12RNY endermelon tracker");
                    if (iblockstate.getBlock() == Blocks.MELON_BLOCK) {
                        Vec3d pointAim = new Vec3d((float) x + 0.5F, (float) y + 0.5F, (float) z + 0.5F);
                        RayTraceResult raytraceresult = world.rayTraceBlocks(pointCenter, pointAim, false, true, false);
                        if (raytraceresult != null && raytraceresult.getBlockPos().equals(gridAim)) {
                            // calculates the effective volume
                            double x_min = MathHelper.clamp(gridAim.getX(), boxCornerMin.x, boxCornerMax.x);
                            double x_max = MathHelper.clamp(gridAim.getX() + 1.0, boxCornerMin.x, boxCornerMax.x);
                            double z_min = MathHelper.clamp(gridAim.getZ(), boxCornerMin.z, boxCornerMax.z);
                            double z_max = MathHelper.clamp(gridAim.getZ() + 1.0, boxCornerMin.z, boxCornerMax.z);
                            double y_min = MathHelper.clamp(gridAim.getY(), boxCornerMin.y, boxCornerMax.y);
                            double y_max = MathHelper.clamp(gridAim.getY() + 1.0, boxCornerMin.y, boxCornerMax.y);
                            double unitVolume = (x_max - x_min) * (z_max - z_min) * (y_max - y_min);
                            melonVolume += unitVolume;
                            if (verbose) {
                                Messenger.print_server_message(server, String.format("Effective Melon at %s", gridAim));
                                Messenger.print_server_message(server, String.format("Volume += %f", unitVolume));
                            }
                        } else if (verbose) {
                            Messenger.print_server_message(server, String.format("BLOCKED Melon at %s", gridAim));
                        }
                    }
                }
            }
        }
    }

    public void reportInstantSurroundings(boolean verbose) {
        checkSurroundingMelons(verbose);
        Messenger.print_server_message(server, String.format("Enderman Age: %d gt", mob.ticksExisted));
        Messenger.print_server_message(server, String.format("Position: (%f, %f, %f)", mob.posX, mob.posY, mob.posZ));
        Messenger.print_server_message(server, String.format("Melon Density: %f / 48.0 = %f%%", melonVolume, melonVolume * (100.0 / 48.0)));
        if (verbose) {
            Messenger.print_server_message(server, String.format("also expressed as %f / 32.0\nor %f / 64.0", melonVolume * 2.0 / 3.0, melonVolume * 4.0 / 3.0));
        }
    }

    public void startTrackingOnce(boolean verbose) {
        if (!running) {
            melonDensityList.clear();
            blockTakingChanceList.clear();
            running = true;
        }
    }

    public void update() {

    }

}
