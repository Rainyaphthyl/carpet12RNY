package carpet.commands;

import carpet.utils.PortalRange;
import carpet.utils.PortalSearcherSilent;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class CommandPortal extends CommandCarpetBase {
    private static final String USAGE = "Usage: /portal <check|search> <src|dst> <x> <y> <z>";

    @Override
    public String getName() {
        return "portal";
    }

    @Override
    @ParametersAreNonnullByDefault
    public String getUsage(ICommandSender sender) {
        return USAGE;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!command_enabled("commandPortal", sender) || !checkPermission(server, sender)) {
            return;
        }
        World world = sender.getEntityWorld();
        if (!(world instanceof WorldServer)) {
            return;
        }
        EnumFunction function = null;
        EnumDirection direction = null;
        int dimensionID = sender.getEntityWorld().provider.getDimensionType().getId();
        DimensionType dimensionType = DimensionType.getById(dimensionID);
        Vec3d posAim = null;
        if (args.length >= 5) {
            if ("check".equalsIgnoreCase(args[0])) {
                function = EnumFunction.CHECK;
            } else if ("search".equalsIgnoreCase(args[0])) {
                function = EnumFunction.SEARCH;
            }
            if ("src".equalsIgnoreCase(args[1])) {
                direction = EnumDirection.SOURCE;
            } else if ("dst".equalsIgnoreCase(args[1])) {
                direction = EnumDirection.DESTINATION;
            }
            Vec3d posBase = sender.getPositionVector();
            double posAimX = parseDouble(posBase.x, args[2], true);
            double posAimY = parseDouble(posBase.y, args[3], false);
            double posAimZ = parseDouble(posBase.z, args[4], true);
            posAim = new Vec3d(posAimX, posAimY, posAimZ);
        }
        if (function != null && direction != null) {
            PortalSearcherSilent searcher = new PortalSearcherSilent((WorldServer) world);
            PortalRange range = searcher.getParentFrame(new BlockPos(posAim.x, posAim.y, posAim.z));
        } else {
            throw new WrongUsageException(USAGE);
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        return super.getTabCompletions(server, sender, args, targetPos);
    }

    private enum EnumFunction {
        CHECK, SEARCH
    }

    private enum EnumDirection {
        SOURCE, DESTINATION
    }
}
