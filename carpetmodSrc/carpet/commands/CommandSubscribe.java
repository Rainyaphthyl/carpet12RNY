package carpet.commands;

import net.minecraft.command.ICommandSender;

import javax.annotation.Nonnull;

public class CommandSubscribe extends CommandLog {

    private final String USAGE = "/subscribe <subscribeName> [?option]";

    @Override
    @Nonnull
    public String getName() {
        return "subscribe";
    }

    @Override
    @Nonnull
    public String getUsage(ICommandSender sender) {
        return USAGE;
    }
}
