package carpet.helpers.endermelon;

import carpet.utils.Messenger;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

public class EndermelonMonitor {
    public WorldServer world;
    public int queueSizeForOnce = 0;
    public boolean enabled = false;

    public EndermelonMonitor(WorldServer world) {
        this.world = world;
    }

    public static void report_surrounding_melons(EntityEnderman mob) {
        if (mob == null) {
            return;
        }
        MinecraftServer server = mob.getServer();
        Messenger.print_server_message(server, Messenger.s(null, "c Hello, Endermelon!"));
        Messenger.print_server_message(server, Messenger.s(null, String.format("w Coordinate: %f, %f, %f", mob.posX, mob.posY, mob.posZ)));
        Messenger.print_server_message(server, Messenger.s(null, String.format("c Age: %d gt", mob.ticksExisted)));
    }

    public void captureOneMob() {
        enabled = true;
        ++queueSizeForOnce;
    }

    public void captureInstant() {
        EntityEnderman aimMob = null;
        {
            Entity tempMob;
            int size = world.loadedEntityList.size();
            for (int i = 0; aimMob == null && i < size; ++i) {
                tempMob = world.loadedEntityList.get(i);
                if (tempMob instanceof EntityEnderman) {
                    aimMob = (EntityEnderman) tempMob;
                }
            }
        }
        if (aimMob != null) {
            report_surrounding_melons(aimMob);
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
