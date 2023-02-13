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
        return "Usage: \"/endermelon <instant|once>\"";
    }

    @Override
    @ParametersAreNonnullByDefault
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!command_enabled("commandEndermelon", sender)) {
            return;
        }
        WorldServer world = (WorldServer) sender.getEntityWorld();
        if (args.length == 1) {
            if ("instant".equalsIgnoreCase(args[0])) {
                world.endermelonMonitor.captureInstant();
                return;
            } else if ("once".equalsIgnoreCase(args[0])) {
                world.endermelonMonitor.captureOneMob();
                return;
            }
        }
        throw new CommandException(getUsage(sender));
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
        return Collections.emptyList();
    }
}
