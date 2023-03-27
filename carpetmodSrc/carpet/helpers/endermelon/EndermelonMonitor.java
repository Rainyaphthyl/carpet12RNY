package carpet.helpers.endermelon;

import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.world.WorldServer;

public class EndermelonMonitor {
    public WorldServer world;
    public int queueSizeForOnce = 0;
    public boolean enabled = false;

    public EndermelonMonitor(WorldServer world) {
        this.world = world;
    }

    public void captureOneMob(boolean verbose) {
        enabled = true;
        ++queueSizeForOnce;
    }

    public void captureInstant(boolean verbose) {
        EntityEnderman aimMob = null;
        {
            Entity tempMob;
            for (int i = world.loadedEntityList.size() - 1; aimMob == null && i >= 0; --i) {
                tempMob = world.loadedEntityList.get(i);
                if (tempMob instanceof EntityEnderman) {
                    aimMob = (EntityEnderman) tempMob;
                }
            }
        }
        if (aimMob != null) {
            EndermelonTracker tracker = new EndermelonTracker(aimMob);
            tracker.reportInstantSurroundings(verbose);
        }
    }

    public void update() {
        if (!enabled) {
            return;
        }
        if (queueSizeForOnce > 0) {

        }
    }
}
