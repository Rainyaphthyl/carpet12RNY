package carpet.commands;

import carpet.CarpetSettings;
import carpet.utils.Messenger;
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
import java.util.Collections;
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
                if ("overworld".equalsIgnoreCase(args[5])) {
                    dimension = DimensionType.OVERWORLD;
                } else if ("nether".equalsIgnoreCase(args[5])) {
                    dimension = DimensionType.NETHER;
                } else {
                    dimension = DimensionType.getById(parseInt(args[5], -1, 0));
                }
            } else {
                dimension = sender.getEntityWorld().provider.getDimensionType();
            }
        }
        if (direction != null && area != null) {
            try {
                PortalSilentSearcher searcher = new PortalSilentSearcher(server, posTarget, dimension, direction, area);
                Messenger.print_server_message(server, "Hello!");
            } catch (NullPointerException e) {
            }
        } else {
            throw new WrongUsageException(USAGE);
        }
    }

    @Nonnull
    @Override
    @ParametersAreNonnullByDefault
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (!CarpetSettings.commandPortal) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "from", "to");
        } else if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, "point", "range");
        } else if (args.length > 2 && args.length <= 5) {
            return getTabCompletionCoordinate(args, 2, targetPos);
        } else if (args.length == 6) {
            DimensionType dimension = sender.getEntityWorld().provider.getDimensionType();
            String[] possibleList = new String[2];
            String nether = "nether", overworld = "overworld";
            if (dimension == DimensionType.NETHER) {
                possibleList[0] = nether;
                possibleList[1] = overworld;
            } else {
                possibleList[0] = overworld;
                possibleList[1] = nether;
            }
            return getListOfStringsMatchingLastWord(args, possibleList);
        }
        return Collections.emptyList();
    }

}
