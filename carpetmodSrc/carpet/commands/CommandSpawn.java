package carpet.commands;

import carpet.CarpetSettings;
import carpet.helpers.HopperCounter;
import carpet.helpers.TickSpeed;
import carpet.utils.Messenger;
import carpet.utils.SpawnReporter;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandSpawn extends CommandCarpetBase {
    private static final String MAIN_USAGE = "/spawn (list | entities | rates | mobcaps | tracking | test | mocking) <option>...";
    private static final String USAGE_LIST = "/spawn list <X> <Y> <Z>";
    private static final String USAGE_ENTITIES = "/spawn entities [passive | hostile | ambient | water]";
    private static final String USAGE_MOBCAPS = "/spawn mobcaps [(set <num>) | nether | overworld | end]]";
    private static final String USAGE_TRACKING = "/spawn tracking [stop | hostile | passive | water | ambient]" +
            "\n| /spawn tracking [re]start [<X1> <Y1> <Z1> <X2> <Y2> <Z2>]";
    private static final String USAGE_TEST = "/spawn test [<ticks> [<counter>]]";
    private static final String USAGE_MOCKING = "/spawn mocking (true|false)";
    private static final String USAGE_PREDICT = "/spawn predict (perimeter | block) <X> <Y> <Z> [<dimension>]" +
            "\n| /spawn predict range <X1> <Y1> <Z1> <X2> <Y2> <Z2> [<dimension>]";
    private static final String[] ALL_USAGES = new String[]{
            USAGE_LIST, USAGE_ENTITIES, USAGE_MOBCAPS, USAGE_TRACKING, USAGE_TEST, USAGE_MOCKING, USAGE_PREDICT
    };
    private static final String DETAILED_USAGE;

    static {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ALL_USAGES.length; ++i) {
            if (i > 0) {
                builder.append("\n| ");
            }
            builder.append(ALL_USAGES[i]);
        }
        DETAILED_USAGE = builder.toString();
    }

    /**
     * Gets the name of the command
     */
    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public String getUsage(ICommandSender sender) {
        return MAIN_USAGE;
    }

    @Override
    @Nonnull
    public String getName() {
        return "spawn";
    }

    /**
     * Callback for when the command is executed
     */
    @Override
    @ParametersAreNonnullByDefault
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!command_enabled("commandSpawn", sender)) return;
        if (args.length == 0) {
            throw new WrongUsageException(MAIN_USAGE);
        }
        World world = sender.getEntityWorld();
        if ("list".equalsIgnoreCase(args[0])) {
            BlockPos blockpos = parseBlockPos(sender, args, 1, false);
            if (!world.isBlockLoaded(blockpos)) {
                throw new CommandException("commands.setblock.outOfWorld");
            } else {
                msgFormatted(sender, SpawnReporter.report(blockpos, world));
                return;
            }
        } else if ("tracking".equalsIgnoreCase(args[0])) {
            if (args.length == 1) {
                msgFormatted(sender, SpawnReporter.tracking_report(world));
                return;
            } else if ("start".equalsIgnoreCase(args[1])) {
                if (SpawnReporter.track_spawns == 0L) {
                    trackStart(server, sender, args);
                    notifyCommandListener(sender, this, "Spawning tracking started.");
                } else {
                    notifyCommandListener(sender, this, "You are already tracking spawning.");
                }
            } else if ("restart".equalsIgnoreCase(args[1])) {
                if (SpawnReporter.track_spawns == 0L) {
                    trackStart(server, sender, args);
                    notifyCommandListener(sender, this, "Spawning tracking started.");
                } else {
                    trackStop(world, sender);
                    trackStart(server, sender, args);
                    notifyCommandListener(sender, this, "Spawning tracking stopped and restarted.");
                }
            } else if ("stop".equalsIgnoreCase(args[1])) {
                trackStop(world, sender);
                notifyCommandListener(sender, this, "Spawning tracking stopped.");
            } else {
                msgFormatted(sender, SpawnReporter.recent_spawns(world, args[1]));
            }
            return;
        } else if ("test".equalsIgnoreCase(args[0])) {
            String counter = null;
            long warp = 72000;
            if (args.length >= 2) {
                warp = parseInt(args[1], 20, 720000);
                if (args.length >= 3) {
                    counter = args[2];
                }
            }
            //stop tracking
            SpawnReporter.reset_spawn_stats(false);
            //start tracking
            SpawnReporter.track_spawns = (long) server.getTickCounter();
            //counter reset
            if (counter == null) {
                HopperCounter.resetAll(true);
            } else {
                HopperCounter hopperCounter = HopperCounter.getCounter(counter);
                if (hopperCounter != null) hopperCounter.reset(true);
            }
            // tick warp 0
            TickSpeed.tickrate_advance(null, 0, null, null);
            // tick warp given player
            EntityPlayer player = null;
            if (sender instanceof EntityPlayer) {
                player = (EntityPlayer) sender;
            }
            TickSpeed.tickrate_advance(player, warp, null, sender);
            notifyCommandListener(sender, this, String.format("Started spawn test for %d ticks", warp));
            return;
        } else if ("mocking".equalsIgnoreCase(args[0])) {
            boolean domock = parseBoolean(args[1]);
            if (domock) {
                SpawnReporter.initialize_mocking();
                notifyCommandListener(sender, this, "Mock spawns started, Spawn statistics reset");
            } else {
                SpawnReporter.stop_mocking();
                notifyCommandListener(sender, this, "Normal mob spawning, Spawn statistics reset");
            }
            return;
        } else if ("rates".equalsIgnoreCase(args[0])) {
            if (args.length >= 2 && "reset".equalsIgnoreCase(args[1])) {
                SpawnReporter.spawn_tries.replaceAll((s, v) -> 1);
            } else if (args.length >= 3) {
                String str = args[1];
                String code = SpawnReporter.get_creature_code_from_string(str);
                int num = parseInt(args[2], 0, 1000);
                SpawnReporter.spawn_tries.put(code, num);
            }
            if (sender instanceof EntityPlayerMP) {
                msgFormatted(sender, SpawnReporter.print_general_mobcaps(world));
            }
            return;
        } else if ("mobcaps".equalsIgnoreCase(args[0])) {
            if (args.length == 1) {
                msgFormatted(sender, SpawnReporter.print_general_mobcaps(world));
                return;
            }
            switch (args[1]) {
                case "set":
                    if (args.length > 2) {
                        int desired_mobcap = parseInt(args[2], 0);
                        double desired_ratio = (double) desired_mobcap / EnumCreatureType.MONSTER.getMaxNumberOfCreature();
                        SpawnReporter.mobcap_exponent = 4.0 * Math.log(desired_ratio) / Math.log(2.0);
                        notifyCommandListener(sender, this, String.format("Mobcaps for hostile mobs changed to %d, other groups will follow", desired_mobcap));
                        return;
                    }
                    msgFormatted(sender, SpawnReporter.print_general_mobcaps(world));
                    return;
                case "overworld":
                    msgFormatted(sender, SpawnReporter.printMobcapsForDimension(world, 0, "overworld"));
                    return;
                case "nether":
                    msgFormatted(sender, SpawnReporter.printMobcapsForDimension(world, -1, "nether"));
                    return;
                case "end":
                    msgFormatted(sender, SpawnReporter.printMobcapsForDimension(world, 1, "the end"));
                    return;
            }
            throw new WrongUsageException(USAGE_MOBCAPS);
        } else if ("entities".equalsIgnoreCase(args[0])) {
            if (args.length == 1) {
                msgFormatted(sender, SpawnReporter.print_general_mobcaps(world));
            } else {
                msgFormatted(sender, SpawnReporter.printEntitiesByType(args[1], world));
            }
            return;
        } else if ("predict".equalsIgnoreCase(args[0])) {
            Messenger.print_server_message(server, Messenger.c("r command ", "rbi /spawn predict", "r  is not implemented"));
            return;
        }
        throw new WrongUsageException(DETAILED_USAGE);
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
        if (!CarpetSettings.commandSpawn) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "list", "mocking", "tracking", "mobcaps", "rates", "entities", "test", "predict");
        }
        if ("list".equalsIgnoreCase(args[0]) && args.length <= 4) {
            return getTabCompletionCoordinate(args, 1, pos);
        }
        if (args.length == 2) {
            if ("tracking".equalsIgnoreCase(args[0])) {
                return getListOfStringsMatchingLastWord(args, "start", "stop", "restart", "hostile", "passive", "ambient", "water");
            }
            if ("mocking".equalsIgnoreCase(args[0])) {
                return getListOfStringsMatchingLastWord(args, "true", "false");
            }
            if ("entities".equalsIgnoreCase(args[0])) {
                return getListOfStringsMatchingLastWord(args, "hostile", "passive", "ambient", "water");
            }
            if ("rates".equalsIgnoreCase(args[0])) {
                return getListOfStringsMatchingLastWord(args, "reset", "hostile", "passive", "ambient", "water");
            }
            if ("mobcaps".equalsIgnoreCase(args[0])) {
                return getListOfStringsMatchingLastWord(args, "set", "nether", "overworld", "end");
            }
            if ("test".equalsIgnoreCase(args[0])) {
                return getListOfStringsMatchingLastWord(args, "24000", "72000");
            }
            if ("predict".equalsIgnoreCase(args[0])) {
                return getListOfStringsMatchingLastWord(args, "perimeter", "block", "range");
            }
        }
        if ("test".equalsIgnoreCase(args[0]) && (args.length == 3)) {
            List<String> lst = new ArrayList<>();
            for (EnumDyeColor clr : EnumDyeColor.values()) {
                lst.add(clr.toString());
            }
            String[] stockArr = new String[lst.size()];
            stockArr = lst.toArray(stockArr);
            return getListOfStringsMatchingLastWord(args, stockArr);
        }
        if ("mobcaps".equalsIgnoreCase(args[0]) && "set".equalsIgnoreCase(args[1]) && (args.length == 3)) {
            return getListOfStringsMatchingLastWord(args, "70");
        }
        if ("tracking".equalsIgnoreCase(args[0]) && ("start".equalsIgnoreCase(args[1]) || "restart".equalsIgnoreCase(args[1]))) {
            switch (args.length) {
                case 3:
                case 4:
                case 5:
                    return getTabCompletionCoordinate(args, 2, pos);
                case 6:
                case 7:
                case 8:
                    return getTabCompletionCoordinate(args, 5, pos);
            }
        }
        if ("predict".equalsIgnoreCase(args[0])) {
            boolean suggestDim = false;
            if ("perimeter".equalsIgnoreCase(args[1])) {
                if (args.length >= 3 && args.length <= 5) {
                    return getTabCompletionCoordinateExact(sender, args, 2, pos, 4);
                } else if (args.length == 6) {
                    suggestDim = true;
                }
            } else if ("block".equalsIgnoreCase(args[1])) {
                if (args.length >= 3 && args.length <= 5) {
                    return getTabCompletionMobPlace(sender, args, 2, pos);
                } else if (args.length == 6) {
                    suggestDim = true;
                }
            } else if ("range".equalsIgnoreCase(args[1])) {
                if (args.length >= 3 && args.length <= 5) {
                    return getTabCompletionMobPlace(sender, args, 2, pos);
                } else if (args.length >= 6 && args.length <= 8) {
                    return getTabCompletionMobPlace(sender, args, 5, pos);
                } else if (args.length == 9) {
                    suggestDim = true;
                }
            }
            if (suggestDim) {
                List<String> list = getListOfStringsMatchingLastWord(args, "nether", "overworld", "end");
                list.add("~");
                return list;
            }
        }
        return Collections.emptyList();
    }

    @ParametersAreNonnullByDefault
    private void trackStart(MinecraftServer server, ICommandSender sender, String[] args) throws NumberInvalidException, WrongUsageException {
        BlockPos lsl = null;
        BlockPos usl = null;
        if (args.length == 8) {
            BlockPos a = parseBlockPos(sender, args, 2, false);
            BlockPos b = parseBlockPos(sender, args, 5, false);
            lsl = new BlockPos(
                    Math.min(a.getX(), b.getX()),
                    Math.min(a.getY(), b.getY()),
                    Math.min(a.getZ(), b.getZ()));
            usl = new BlockPos(
                    Math.max(a.getX(), b.getX()),
                    Math.max(a.getY(), b.getY()),
                    Math.max(a.getZ(), b.getZ()));
        } else if (args.length != 2) {
            throw new WrongUsageException(USAGE_TRACKING);
        }
        SpawnReporter.reset_spawn_stats(false);
        SpawnReporter.track_spawns = (long) server.getTickCounter();
        SpawnReporter.lower_spawning_limit = lsl;
        SpawnReporter.upper_spawning_limit = usl;
    }

    private void trackStop(World world, ICommandSender sender) {
        msgFormatted(sender, SpawnReporter.tracking_report(world));
        SpawnReporter.reset_spawn_stats(false);
        SpawnReporter.track_spawns = 0L;
        SpawnReporter.lower_spawning_limit = null;
        SpawnReporter.upper_spawning_limit = null;
    }
}
