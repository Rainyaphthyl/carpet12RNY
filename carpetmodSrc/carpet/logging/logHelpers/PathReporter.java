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

public class PathReporter {
    public static final String[] LOGGER_OPTIONS = new String[]{"chat", "visual", "all"};
    public static final String DEFAULT_OPTION = LOGGER_OPTIONS[0];
    public static final String NAME = "pathFinding";
    private static Logger instance = null;
    //private static IParticleData lvl1;
    //private static IParticleData lvl2;
    //private static IParticleData lvl3;
    //
    //static {
    //    failedPath = parseParticle("angry_villager");
    //    successfulPath = parseParticle("happy_villager");
    //    lvl1 = parseParticle("dust 1 1 0 1");
    //    lvl2 = parseParticle("dust 1 0.5 0 1");
    //    lvl3 = parseParticle("dust 1 0 0 1");
    //}

    public static Logger get_instance() {
        if (instance == null) {
            instance = LoggerRegistry.getLogger(NAME);
        }
        return instance;
    }

    public static void report(Entity entity, Vec3d target, float milliseconds, Path path) {
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
                    drawParticleLine((EntityPlayerMP) player, entity.getPositionVector(), target, 50, path != null);
                }
            }
            return list.toArray(new ITextComponent[0]);
        });
    }

    public static void drawParticleLine(EntityPlayerMP player, Vec3d src, Vec3d dst, float ratio, boolean successful) {
        if (player == null) {
            return;
        }
        EnumParticleTypes accent = successful ? EnumParticleTypes.VILLAGER_HAPPY : EnumParticleTypes.VILLAGER_ANGRY;
        ((WorldServer) player.world).spawnParticle(player, accent, true, src.x, src.y, src.z,
                5, 0.5, 0.5, 0.5, 0.0);
        //IParticleData accent = successful ? successfulPath : failedPath;
        //IParticleData color = (ratio < 2)? lvl1 : ((ratio < 4)?lvl2:lvl3);
        //
        //((WorldServer)player.world).spawnParticle(
        //        player,
        //        accent,
        //        true,
        //        from.x, from.y, from.z, 5,
        //        0.5, 0.5, 0.5, 0.0);
        //
        //double lineLengthSq = from.squareDistanceTo(to);
        //if (lineLengthSq == 0) return;
        //
        //Vec3d incvec = to.subtract(from).normalize();//    multiply(50/sqrt(lineLengthSq));
        //int pcount = 0;
        //for (Vec3d delta = new Vec3d(0.0,0.0,0.0);
        //     delta.lengthSquared()<lineLengthSq;
        //     delta = delta.add(incvec.scale(player.world.rand.nextFloat())))
        //{
        //    ((WorldServer)player.world).spawnParticle(
        //            player,
        //            color,
        //            true,
        //            delta.x+from.x, delta.y+from.y, delta.z+from.z, 1,
        //            0.0, 0.0, 0.0, 0.0);
        //}
    }
}
