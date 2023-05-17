package carpet.logging.logHelpers;

import carpet.logging.Logger;
import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PathReporter {
    public static final String[] LOGGER_OPTIONS = new String[]{"chat", "visual", "all"};
    public static final String DEFAULT_OPTION = LOGGER_OPTIONS[0];
    public static final String NAME = "pathFinding";
    private static Logger instance = null;

    public static Logger get_instance() {
        if (instance == null) {
            instance = LoggerRegistry.getLogger(NAME);
        }
        return instance;
    }

    public static void report(Entity entity, Vec3d target, Path path) {
        get_instance().log((option, player) -> {
            boolean visual = false, chat = false;
            switch (option) {
                case "chat":
                    chat = true;
                    break;
                case "visual":
                    visual = true;
                    break;
                case "all":
                    chat = true;
                    visual = true;
                    break;
            }
            List<ITextComponent> list = new ArrayList<>();
            if (chat) {
                if (path == null) {
                    list.add(Messenger.s(null, "No Valid Paths!"));
                } else {
                    for (int i = 0, length = path.getCurrentPathLength(); i < length; ++i) {
                        PathPoint point = path.getPathPointFromIndex(i);
                        list.add(Messenger.s(null, String.format("Path %d : [%d, %d, %d] / %d", i, point.x, point.y, point.z, length)));
                    }
                }
            }
            if (visual) {
                if (player instanceof EntityPlayerMP) {
                    drawParticleLine((EntityPlayerMP) player, entity.getPositionVector(), target, path != null);
                }
            }
            return list.toArray(new ITextComponent[0]);
        });
    }

    private static void drawParticleLine(EntityPlayerMP player, Vec3d src, Vec3d dst, boolean successful) {
        if (player == null) {
            return;
        }
        if (successful) {
            ((WorldServer) player.world).spawnParticle(player, EnumParticleTypes.VILLAGER_HAPPY,
                    true, src.x, src.y, src.z,
                    5, 0.0, 0.5, 0.0, 0.0);
        }
        EnumParticleTypes particleLine = EnumParticleTypes.REDSTONE;
        double intensity = successful ? 0.5 : 2.0;
        Vec3d increment = dst.subtract(src).normalize();
        Random random = new Random();
        double distance = dst.distanceTo(src);
        double x = dst.x;
        double y = dst.y;
        double z = dst.z;
        for (double progress = 0.0, delta; progress <= distance; progress += delta) {
            delta = intensity * random.nextDouble();
            x -= delta * increment.x;
            y -= delta * increment.y;
            z -= delta * increment.z;
            ((WorldServer) player.world).spawnParticle(player, particleLine, true, x, y, z,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
        if (successful) {
            ((WorldServer) player.world).spawnParticle(player, EnumParticleTypes.DRAGON_BREATH,
                    true, dst.x, dst.y, dst.z,
                    2, 0.0, 0.5, 0.0, 0.0);
        }
    }
}
