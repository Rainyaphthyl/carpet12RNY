package carpet.commands;

import carpet.CarpetSettings;
import carpet.helpers.HopperCounter;
import carpet.helpers.ItemWithMeta;
import carpet.utils.Messenger;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CommandCounter extends CommandCarpetBase {
    /**
     * Gets the name of the command
     */
    @Nonnull
    @ParametersAreNonnullByDefault
    public String getUsage(ICommandSender sender) {
        return "Usage: /counter [<color>|cactus|all] (reset|stop|realtime|raw)";
    }

    @Nonnull
    public String getName() {
        return "counter";
    }

    /**
     * Callback for when the command is executed
     */
    @ParametersAreNonnullByDefault
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (CarpetSettings.hopperCounters == CarpetSettings.HopperCounters.off && !CarpetSettings.cactusCounter) {
            msg(sender, Messenger.m(null, "Need cactusCounter or hopperCounters to be enabled to use this command."));
            return;
        }
        if (args.length == 0) {
            Messenger.send(sender, HopperCounter.formatAll(false, true));
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "raw":
                Messenger.send(sender, HopperCounter.formatAll(false, false));
                return;
            case "realtime":
                Messenger.send(sender, HopperCounter.formatAll(true, false));
                return;
            case "reset":
                HopperCounter.resetAll(true);
                notifyCommandListener(sender, this, "All counters restarted instantly.");
                return;
            case "stop":
                HopperCounter.resetAll(false);
                notifyCommandListener(sender, this,
                        "All counters have stopped and will restart on triggered.");
                return;
            case "distribution":
                Messenger.print_server_message(server,
                        "Command \"counter [<color>] distribution\" is a work in progress.");
                return;
        }
        HopperCounter counter = HopperCounter.getCounter(args[0]);
        if (counter == null) throw new WrongUsageException("Invalid color");
        if (args.length == 1) {
            Messenger.send(sender, counter.format(false, false, true));
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "raw":
                Messenger.send(sender, counter.format(false, false, false));
                return;
            case "realtime":
                Messenger.send(sender, counter.format(true, false, false));
                return;
            case "reset":
                counter.reset(true);
                notifyCommandListener(sender, this, String.format("%s counter restarted instantly.", args[0]));
                return;
            case "stop":
                counter.reset(false);
                notifyCommandListener(sender, this, String.format(
                        "%s counter has stopped and will restart on triggered.", args[0]));
                return;
            case "distribution":
                // TODO: 2023/4/19,0019 Remove the hard coded debugger!
                Messenger.send(sender, counter.formatDistribution(new ItemWithMeta(Items.GUNPOWDER, 0)));
                Messenger.print_server_message(server,
                        "Command \"counter [<color>] distribution\" is a work in progress.");
                return;
        }
        throw new WrongUsageException(getUsage(sender));

    }

    @Nonnull
    @ParametersAreNonnullByDefault
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
        if (CarpetSettings.hopperCounters == CarpetSettings.HopperCounters.off && !CarpetSettings.cactusCounter) {
            msg(sender, Messenger.m(null, "Need cactusCounter or hopperCounters to be enabled to use this command."));
            return Collections.emptyList();
        }
        if (args.length == 1) {
            List<String> lst = new ArrayList<>();
            lst.add("reset");
            for (EnumDyeColor clr : EnumDyeColor.values()) {
                lst.add(clr.name().toLowerCase(Locale.ROOT));
            }
            lst.add("cactus");
            lst.add("all");
            lst.add("realtime");
            lst.add("stop");
            lst.add("raw");
            lst.add("distribution");
            String[] stockArr = new String[lst.size()];
            stockArr = lst.toArray(stockArr);
            return getListOfStringsMatchingLastWord(args, stockArr);
        }
        if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, "reset", "realtime", "stop", "raw", "distribution");
        }
        return Collections.emptyList();
    }
}
