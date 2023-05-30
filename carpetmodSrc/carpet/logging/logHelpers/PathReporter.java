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
import java.util.Objects;
import java.util.Random;

public class PathReporter {
    public static final String[] LOGGER_OPTIONS = new String[]{"visual", "chat", "all"};
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
                    drawParticleLine((EntityPlayerMP) player, entity.getPositionVector(), target, successful);
                    drawParticlePath((EntityPlayerMP) player, entity.getPositionVector(), path);
                }
            }
            return list.toArray(new ITextComponent[0]);
        });
    }

    private static void drawParticlePath(EntityPlayerMP player, Vec3d entityPos, Path path) {
        if (path == null || player == null) {
            return;
        }
        PathPoint point = path.getFinalPathPoint();
        PathPoint curr = path.isFinished() ? point : path.getPathPointFromIndex(path.getCurrentPathIndex());
        while (point != null) {
            point = drawParticleSegment(player, entityPos, point, curr);
        }
    }

    private static PathPoint drawParticleSegment(EntityPlayerMP player, Vec3d entityPos, PathPoint dst, PathPoint curr) {
        if (dst == null) {
            return null;
        }
        PathPoint src = dst.previous;
        if (src != null && Objects.equals(curr, dst) && entityPos != null) {
            src = null;
        }
        Vec3d increment;
        double distance;
        double x, y, z;
        if (src == null) {
            if (entityPos == null) {
                return null;
            }
            double dstX = dst.x + 0.5;
            double dstZ = dst.z + 0.5;
            increment = new Vec3d(dstX - entityPos.x, dst.y - entityPos.y, dstZ - entityPos.z);
            distance = entityPos.distanceTo(new Vec3d(dstX, dst.y, dstZ));
            x = entityPos.x;
            y = entityPos.y;
            z = entityPos.z;
        } else {
            increment = new Vec3d(dst.x - src.x, dst.y - src.y, dst.z - src.z);
            distance = dst.distanceTo(src);
            x = src.x + 0.5;
            y = src.y;
            z = src.z + 0.5;
        }
        increment = increment.normalize();
        Random random = new Random();
        EnumParticleTypes particleLine = EnumParticleTypes.REDSTONE;
        double speed = increment.y == 0.0 ? 0.0 : 1.0;
        for (double progress = 0.0, delta; progress <= distance; progress += delta) {
            delta = 0.25 * random.nextDouble();
            ((WorldServer) player.world).spawnParticle(player, particleLine, true, x, y, z,
                    1, 0.0, 0.0, 0.0, speed);
            x += delta * increment.x;
            y += delta * increment.y;
            z += delta * increment.z;
        }
        return src;
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
        EnumParticleTypes particleLine = EnumParticleTypes.END_ROD;
        double speed = 0.0;
        double interval = successful ? 0.5 : 1.0;
        Vec3d increment = dst.subtract(src).normalize();
        Random random = new Random();
        double distance = dst.distanceTo(src);
        double x = dst.x;
        double y = dst.y;
        double z = dst.z;
        for (double progress = 0.0, delta; progress <= distance; progress += delta) {
            delta = interval * random.nextDouble();
            ((WorldServer) player.world).spawnParticle(player, particleLine, true, x, y, z,
                    1, 0.0, 0.0, 0.0, speed);
            x -= delta * increment.x;
            y -= delta * increment.y;
            z -= delta * increment.z;
        }
        if (successful) {
            ((WorldServer) player.world).spawnParticle(player, EnumParticleTypes.DRAGON_BREATH,
                    true, dst.x, dst.y, dst.z,
                    2, 0.0, 0.5, 0.0, 0.0);
        }
    }
}
