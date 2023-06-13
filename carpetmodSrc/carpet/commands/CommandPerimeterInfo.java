package carpet.commands;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;

public class CommandPerimeterInfo extends CommandCarpetBase {
    private static final String USAGE = "/perimeterinfo <x> <y> <z> [<target_entity>]" +
            "\n | perimeterinfo <player> [<target_entity>]";

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

    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 4) {
            return getListOfStringsMatchingLastWord(args, EntityList.getEntityNameList());
        } else {
            // TODO: 2023/6/14,0014 Parse | Complete "double or int" coordinates and player names
            return args.length > 0 && args.length <= 3 ? getTabCompletionCoordinate(args, 0, targetPos) : Collections.emptyList();
        }
    }
}
