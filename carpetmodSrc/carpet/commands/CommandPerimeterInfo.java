package carpet.commands;

import carpet.CarpetSettings;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.EntityList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandPerimeterInfo extends CommandCarpetBase {
    private static final String USAGE = "/perimeterinfo <x> <y> <z> [<target_entity>]";

    @Nonnull
    @ParametersAreNonnullByDefault
    public static List<String> getTabCompletionCoordinateExact(ICommandSender sender, String[] inputArgs, int index, @Nullable BlockPos target) {
        Vec3d posBaseE = sender.getPositionVector();
        BlockPos posBaseB = sender.getPosition();
        List<String> list = new ArrayList<>();
        int i = inputArgs.length - 1;
        double posCurrE;
        int posCurrB;
        int posTarget;
        switch (i - index) {
            case 0:
                posCurrE = posBaseE.x;
                posCurrB = posBaseB.getX();
                posTarget = posCurrB;
                if (target != null) {
                    posTarget = target.getX();
                }
                break;
            case 1:
                posCurrE = posBaseE.y;
                posCurrB = posBaseB.getY();
                posTarget = posCurrB;
                if (target != null) {
                    posTarget = target.getY();
                }
                break;
            case 2:
                posCurrE = posBaseE.z;
                posCurrB = posBaseB.getZ();
                posTarget = posCurrB;
                if (target != null) {
                    posTarget = target.getZ();
                }
                break;
            default:
                return list;
        }
        list.add("~");
        list.add(Double.toString(posCurrE));
        list.add(Integer.toString(posCurrB));
        if (posTarget != posCurrB) {
            list.add(Integer.toString(posTarget));
        }
        return list;
    }

    @Override
    @Nonnull
    public String getName() {
        return "perimeterinfo";
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public String getUsage(ICommandSender sender) {
        return USAGE;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!command_enabled("commandPerimeterInfo", sender)) {
            return;
        }
        if (args.length < 3) {
            throw new WrongUsageException(USAGE);
        } else {
            Vec3d posBase = sender.getPositionVector();
            double posX = parseDouble(posBase.x, args[0], true);
            double posY = parseDouble(posBase.y, args[1], false);
            double posZ = parseDouble(posBase.z, args[2], true);
        }
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (!CarpetSettings.commandPerimeterInfo) {
            return Collections.emptyList();
        } else if (args.length == 4) {
            return getListOfStringsMatchingLastWord(args, EntityList.getEntityNameList());
        } else {
            return args.length > 0 && args.length <= 3 ?
                    getTabCompletionCoordinateExact(sender, args, 0, null) : Collections.emptyList();
        }
    }
}
