package carpet.logging.logHelpers;

import carpet.logging.Logger;
import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;

import java.util.ArrayList;
import java.util.List;

public class PathReporter {
    public static final String[] LOGGER_OPTIONS = null;
    public static final String DEFAULT_OPTION = null;
    public static final String NAME = "pathFinding";
    private static Logger instance = null;

    public static Logger get_instance() {
        if (instance == null) {
            instance = LoggerRegistry.getLogger(NAME);
        }
        return instance;
    }

    public static void report(Path path) {
        get_instance().log((option, player) -> {
            List<ITextComponent> list = new ArrayList<>();
            if (path == null) {
                list.add(Messenger.s(null, "No Valid Paths!"));
            } else {
                for (int i = 0, length = path.getCurrentPathLength(); i < length; ++i) {
                    PathPoint point = path.getPathPointFromIndex(i);
                    list.add(Messenger.s(null, String.format("Path %d : [%d, %d, %d] / %d", i, point.x, point.y, point.z, length)));
                }
            }
            return list.toArray(new ITextComponent[0]);
        });
    }

    public static void drawParticleLine(EntityPlayerMP player, Vec3d src, Vec3d dst, float ratio, boolean successful) {
    }
}
