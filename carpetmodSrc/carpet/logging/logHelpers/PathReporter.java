package carpet.logging.logHelpers;

import carpet.logging.Logger;
import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.pathfinding.Path;
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
            boolean successful = path != null;
            if (chat) {
                String style = successful ? "w" : "r";
                String nameStyle = entity.hasCustomName() ? "c" : style;
                Vec3d src = entity.getPositionVector();
                list.add(Messenger.m(null,
                        nameStyle + ' ' + entity.getName(), style + "  : ",
                        String.format("%s [%.1f, %.1f, %.1f]", style, src.x, src.y, src.z),
                        String.format("^g " + src.x + ", " + src.y + ", " + src.z),
                        String.format("/tp " + src.x + ' ' + src.y + ' ' + src.z),
                        style + "  -> ",
                        String.format("%s [%.1f, %.1f, %.1f]", style, target.x, target.y, target.z),
                        String.format("^g " + target.x + ", " + target.y + ", " + target.z),
                        String.format("/tp " + target.x + ' ' + target.y + ' ' + target.z),
                        style + ' ' + (successful ? "" : " F")
                ));
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
        double speed = successful ? 1.0 : 0.0;
        double interval = successful ? 0.5 : 1.0;
        Vec3d increment = dst.subtract(src).normalize();
        Random random = new Random();
        double distance = dst.distanceTo(src);
        double x = dst.x;
        double y = dst.y;
        double z = dst.z;
        for (double progress = 0.0, delta; progress <= distance; progress += delta) {
            delta = interval * random.nextDouble();
            x -= delta * increment.x;
            y -= delta * increment.y;
            z -= delta * increment.z;
            ((WorldServer) player.world).spawnParticle(player, particleLine, true, x, y, z,
                    1, 0.0, 0.0, 0.0, speed);
        }
        if (successful) {
            ((WorldServer) player.world).spawnParticle(player, EnumParticleTypes.DRAGON_BREATH,
                    true, dst.x, dst.y, dst.z,
                    2, 0.0, 0.5, 0.0, 0.0);
        }
    }
}
