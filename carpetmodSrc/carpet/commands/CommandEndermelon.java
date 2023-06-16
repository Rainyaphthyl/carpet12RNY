package carpet.commands;

import carpet.CarpetSettings;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;

public class CommandEndermelon extends CommandCarpetBase {

    @Override
    @Nonnull
    public String getName() {
        return "endermelon";
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public String getUsage(ICommandSender sender) {
        return "/endermelon (instant|once) [verbose|brief]";
    }

    @Override
    @ParametersAreNonnullByDefault
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!command_enabled("commandEndermelon", sender)) {
            return;
        }
        WorldServer world = (WorldServer) sender.getEntityWorld();
        if (args.length >= 1) {
            boolean verbose = false;
            if (args.length >= 2) {
                if ("verbose".equalsIgnoreCase(args[1])) {
                    verbose = true;
                }
            }
            if ("instant".equalsIgnoreCase(args[0])) {
                world.endermelonMonitor.captureInstant(verbose);
            } else if ("once".equalsIgnoreCase(args[0])) {
                world.endermelonMonitor.captureOneMob(verbose);
            } else {
                throw new CommandException(getUsage(sender));
            }
        } else {
            throw new CommandException(getUsage(sender));
        }
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (!CarpetSettings.commandEndermelon) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "instant", "once");
        }
        if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, "verbose", "brief");
        }
        return Collections.emptyList();
    }
}
