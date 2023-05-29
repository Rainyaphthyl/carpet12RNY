package carpet.commands;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

public class CommandLogMenu extends CommandLog {
    @Override
    @Nonnull
    public String getName() {
        return "logMenu";
    }

    @Override
    @Nonnull
    public String getUsage(ICommandSender sender) {
        return "/logMenu (interactive menu) OR /logMenu <logName> [?option] [player] [handler ...] OR /logMenu <logName> clear [player] OR /logMenu defaults (interactive menu) OR /logMenu setDefault <logName> [?option] [handler ...] OR /logMenu removeDefault <logName>";
    }

    @Override
    @ParametersAreNonnullByDefault
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        super.execute(server, sender, args);
        boolean showingDefault = args.length > 0 &&
                ("setDefault".equalsIgnoreCase(args[0]) || "removeDefault".equalsIgnoreCase(args[0]));
        EntityPlayer player = null;
        if (sender instanceof EntityPlayer) {
            player = (EntityPlayer) sender;
        }
        if (showingDefault) {
            displayDefaultLoggerMenu(player);
        } else {
            displayPlayerLoggerMenu(player);
        }
    }
}
