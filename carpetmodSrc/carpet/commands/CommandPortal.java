package carpet.commands;

import carpet.utils.portalcalculator.EnumTargetArea;
import carpet.utils.portalcalculator.EnumTargetDirection;
import carpet.utils.portalcalculator.PortalSilentSearcher;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class CommandPortal extends CommandCarpetBase {
    private static final String USAGE = "Usage: /portal <from|to> <point|range> <x> <y> <z> [dimension]";

    @Nonnull
    @Override
    public String getName() {
        return "portal";
    }

    @Nonnull
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
        EnumTargetDirection direction = null;
        EnumTargetArea area = null;
        DimensionType dimension = null;
        Vec3d posTarget = null;
        if (args.length > 4) {
            if ("from".equalsIgnoreCase(args[0])) {
                direction = EnumTargetDirection.FROM;
            } else if ("to".equalsIgnoreCase(args[0])) {
                direction = EnumTargetDirection.TO;
            }
            if ("point".equalsIgnoreCase(args[1])) {
                area = EnumTargetArea.POINT;
            } else if ("range".equalsIgnoreCase(args[1])) {
                area = EnumTargetArea.RANGE;
            }
            Vec3d posBase = sender.getPositionVector();
            double posAimX = parseDouble(posBase.x, args[2], true);
            double posAimY = parseDouble(posBase.y, args[3], false);
            double posAimZ = parseDouble(posBase.z, args[4], true);
            posTarget = new Vec3d(posAimX, posAimY, posAimZ);
            if (args.length > 5) {
                if (DimensionType.OVERWORLD.name().equalsIgnoreCase(args[4])) {
                    dimension = DimensionType.OVERWORLD;
                } else if (DimensionType.NETHER.name().equalsIgnoreCase(args[4]) || DimensionType.NETHER.getName().equalsIgnoreCase(args[4])) {
                    dimension = DimensionType.NETHER;
                } else {
                    dimension = DimensionType.getById(parseInt(args[4], -1, 0));
                }
            } else {
                dimension = sender.getEntityWorld().provider.getDimensionType();
            }
        }
        if (direction != null && area != null) {
            PortalSilentSearcher searcher = new PortalSilentSearcher((WorldServer) world, posTarget, dimension, direction, area);
            (new Thread(searcher)).start();
        } else {
            throw new WrongUsageException(USAGE);
        }
    }

    @Nonnull
    @Override
    @ParametersAreNonnullByDefault
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        return super.getTabCompletions(server, sender, args, targetPos);
    }

}
