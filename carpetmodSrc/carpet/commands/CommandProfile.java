package carpet.commands;

import carpet.CarpetSettings;
import carpet.utils.CarpetProfiler;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;

public class CommandProfile extends CommandCarpetBase {
    @Override
    @Nonnull
    public String getName() {
        return "profile";
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public String getUsage(ICommandSender sender) {
        return "/profile (entities | health) <ticks>";
    }

    @Override
    @ParametersAreNonnullByDefault
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!command_enabled("commandProfile", sender)) {
            return;
        }
        if (args.length > 0) {
            if ("health".equalsIgnoreCase(args[0])) {
                int step = 100;
                if (args.length > 1) {
                    step = parseInt(args[1], 20, 72000);
                }
                CarpetProfiler.prepare_tick_report(step);
                return;
            } else if ("entities".equalsIgnoreCase(args[0])) {
                int step = 100;
                if (args.length > 1) {
                    step = parseInt(args[1], 20, 72000);
                }
                CarpetProfiler.prepare_entity_report(step);
                return;
            }
        } else {
            CarpetProfiler.prepare_tick_report(100);
            return;
        }
        throw new WrongUsageException(getUsage(sender));
    }

    @Nonnull
    @ParametersAreNonnullByDefault
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
        if (!CarpetSettings.commandProfile) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "entities", "health");
        } else if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, "20", "100", "900", "72000");
        }
        return Collections.emptyList();
    }
}
