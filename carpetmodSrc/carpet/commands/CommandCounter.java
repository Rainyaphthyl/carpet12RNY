package carpet.commands;

import carpet.CarpetSettings;
import carpet.helpers.HopperCounter;
import carpet.helpers.ItemWithMeta;
import carpet.utils.Messenger;
import carpet.utils.counter.ItemUnit;
import carpet.utils.counter.TimeUnit;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CommandCounter extends CommandCarpetBase
{
    @Nullable
    @ParametersAreNonnullByDefault
    public static ItemWithMeta parseItemWithMeta(ICommandSender sender, String[] args, int ordinal) throws NumberInvalidException
    {
        if (ordinal >= args.length)
        {
            return null;
        }
        Item item = getItemByText(sender, args[ordinal]);
        ++ordinal;
        int metadata = ordinal < args.length ? parseInt(args[ordinal]) : 0;
        return new ItemWithMeta(item, metadata);
    }

    /**
     * Gets the name of the command
     */
    @Nonnull
    @ParametersAreNonnullByDefault
    public String getUsage(ICommandSender sender)
    {
        return "Usage: /counter [<color>|cactus|all] (reset|stop|realtime|raw)" +
                "\nOR /counter [<color>|cactus|all] unit (amount|time) <unit>";
    }

    @Nonnull
    public String getName()
    {
        return "counter";
    }

    /**
     * Callback for when the command is executed
     */
    @ParametersAreNonnullByDefault
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (CarpetSettings.hopperCounters == CarpetSettings.HopperCounters.off && !CarpetSettings.cactusCounter)
        {
            msg(sender, Messenger.m(null, "Need cactusCounter or hopperCounters to be enabled to use this command."));
            return;
        }
        if (args.length == 0)
        {
            Messenger.send(sender, HopperCounter.formatAll(false, true));
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT))
        {
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
                Messenger.send(sender, HopperCounter.formatAllDistribution(parseItemWithMeta(sender, args, 1)));
                return;
            case "unit":
                if (args.length >= 3)
                {
                    HopperCounter.setAllUnits(args[1], args[2]);
                    notifyCommandListener(sender, this, "Unit display of all counters is reset.");
                    return;
                }
                break;
        }
        HopperCounter counter = HopperCounter.getCounter(args[0]);
        if (counter == null) throw new WrongUsageException("Invalid color");
        if (args.length == 1)
        {
            Messenger.send(sender, counter.format(false, false, true));
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT))
        {
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
                Messenger.send(sender, counter.formatDistribution(parseItemWithMeta(sender, args, 2)));
                return;
            case "unit":
                if (args.length >= 4)
                {
                    counter.setUnits(args[2], args[3]);
                    notifyCommandListener(sender, this, "Unit display of " + args[0] + " counter is reset.");
                    return;
                }
                break;
        }
        throw new WrongUsageException(getUsage(sender));

    }

    @Nonnull
    @ParametersAreNonnullByDefault
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos)
    {
        if (CarpetSettings.hopperCounters == CarpetSettings.HopperCounters.off && !CarpetSettings.cactusCounter)
        {
            msg(sender, Messenger.m(null, "Need cactusCounter or hopperCounters to be enabled to use this command."));
            return Collections.emptyList();
        }
        if (args.length == 1)
        {
            List<String> lst = new ArrayList<>();
            lst.add("reset");
            for (EnumDyeColor clr : EnumDyeColor.values())
            {
                lst.add(clr.name().toLowerCase(Locale.ROOT));
            }
            lst.add("cactus");
            lst.add("all");
            lst.add("realtime");
            lst.add("stop");
            lst.add("raw");
            lst.add("distribution");
            lst.add("unit");
            String[] stockArr = new String[lst.size()];
            stockArr = lst.toArray(stockArr);
            return getListOfStringsMatchingLastWord(args, stockArr);
        }
        if (args.length >= 2)
        {
            int prev = args.length - 2;
            if ("distribution".equalsIgnoreCase(args[prev]))
            {
                return getListOfStringsMatchingLastWord(args, Item.REGISTRY.getKeys());
            }
            if ("unit".equalsIgnoreCase(args[prev]))
            {
                return getListOfStringsMatchingLastWord(args, "amount", "time");
            }
            if (args.length == 2)
            {
                return getListOfStringsMatchingLastWord(args, "reset", "realtime", "stop", "raw", "distribution", "unit");
            }
        }
        if (args.length >= 3)
        {
            int prev = args.length - 2;
            List<String> list = new ArrayList<>();
            switch (args[prev].toLowerCase(Locale.ROOT))
            {
                case "amount":
                    for (ItemUnit unit : ItemUnit.values())
                    {
                        list.add(unit.name());
                    }
                    break;
                case "time":
                    for (TimeUnit unit : TimeUnit.values())
                    {
                        list.add(unit.name());
                    }
                    break;
            }
            return getListOfStringsMatchingLastWord(args, list);
        }
        return Collections.emptyList();
    }
}
