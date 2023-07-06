package carpet.commands;

import carpet.CarpetSettings;
import carpet.logging.LogHandler;
import carpet.logging.Logger;
import carpet.logging.LoggerOptions;
import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

public class CommandLog extends CommandCarpetBase {

    private final String USAGE = "/log (interactive menu) \nOR /log <logName> [?option] [player] [handler ...] \nOR /log <logName> clear [player] \nOR /log defaults (interactive menu) \nOR /log setDefault <logName> [?option] [handler ...] \nOR /log removeDefault <logName> \nOR /log copy <another_player>";

    public static String get_name_with_option(String logName, String option) {
        if (option != null) {
            return logName + " (" + option + ')';
        } else {
            return logName;
        }
    }

    @Override
    @Nonnull
    public String getName() {
        return "log";
    }

    @Override
    @Nonnull
    public String getUsage(ICommandSender sender) {
        return USAGE;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!command_enabled("commandLog", sender)) return;
        EntityPlayer player = null;
        if (sender instanceof EntityPlayer) {
            player = (EntityPlayer) sender;
        }

        if (args.length == 0) {
            displayPlayerLoggerMenu(player);
            return;
        }

        // toggle to default
        if ("reset".equalsIgnoreCase(args[0])) {
            if (args.length > 1) {
                player = server.getPlayerList().getPlayerByUsername(args[1]);
            }
            if (player == null) {
                throw new WrongUsageException("No player specified");
            }
            LoggerRegistry.resetSubscriptions(server, player.getName());
            notifyCommandListener(sender, this, "Unsubscribed from all logs and restored default subscriptions");
            return;
        }

        if ("copy".equalsIgnoreCase(args[0])) {
            if (player == null) {
                throw new WrongUsageException("Command \"/log copy\" must be executed by players");
            }
            String srcName = null;
            if (args.length > 1) {
                srcName = args[1];
            }
            if (srcName == null) {
                throw new WrongUsageException("No src player specified");
            }
            String dstName = player.getName();
            if (srcName.equalsIgnoreCase(dstName)) {
                Messenger.m(sender, "gi You should not copy from yourself!");
            } else {
                Map<String, LoggerOptions> currMap = LoggerRegistry.hasSubscriptions(dstName) ? LoggerRegistry.getPlayerSubscriptions(dstName) : Collections.emptyMap();
                Map<String, LoggerOptions> logMap = LoggerRegistry.hasSubscriptions(srcName) ? LoggerRegistry.getPlayerSubscriptions(srcName) : Collections.emptyMap();
                int copies = 0;
                for (LoggerOptions options : logMap.values()) {
                    String logger = options.logger;
                    String option = options.option;
                    LoggerOptions currOpt = currMap.get(logger);
                    if (!Objects.equals(currOpt, options)) {
                        LogHandler handler = null;
                        try {
                            handler = LogHandler.createHandler(options.handlerName, options.extraArgs);
                        } catch (NullPointerException ignored) {
                        }
                        LoggerRegistry.switchPlayerSubscription(server, dstName, options.logger, option, handler);
                        Messenger.m(player, "gi Subscribed to " + get_name_with_option(logger, option) + '.');
                        ++copies;
                    }
                }
                if (copies > 0) {
                    Messenger.m(player, "gi Copied " + copies + " loggers from " + srcName);
                } else {
                    Messenger.m(player, "gi You had no loggers to sync from " + srcName);
                }
            }
            return;
        }

        if ("defaults".equalsIgnoreCase(args[0])) {
            displayDefaultLoggerMenu(player);
            return;
        }

        if ("setDefault".equalsIgnoreCase(args[0])) {
            if (args.length >= 2) {
                Logger logger = LoggerRegistry.getLogger(args[1]);
                if (logger != null) {
                    String option = logger.getDefault();
                    if (args.length >= 3) {
                        option = logger.getAcceptedOption(args[2]);
                    }
                    LogHandler handler = null;
                    if (args.length >= 4) {
                        handler = LogHandler.createHandler(args[3], ArrayUtils.subarray(args, 4, args.length));
                        if (handler == null) {
                            throw new CommandException("Invalid handler");
                        }
                    }
                    LoggerRegistry.setDefault(server, args[1], option, handler);
                    Messenger.m(player, "gi Added " + logger.getNameWithOption(option) + " to default subscriptions.");
                    return;
                } else {
                    throw new WrongUsageException("No logger named " + args[1] + ".");
                }
            } else {
                throw new WrongUsageException("No logger specified.");
            }
        }

        if ("removeDefault".equalsIgnoreCase(args[0])) {
            if (args.length > 1) {
                Logger logger = LoggerRegistry.getLogger(args[1]);
                if (logger != null) {
                    LoggerRegistry.removeDefault(server, args[1]);
                    Messenger.m(player, "gi Removed " + logger.getLogName() + " from default subscriptions.");
                    return;
                } else {
                    throw new WrongUsageException("No logger named " + args[1] + ".");
                }
            } else {
                throw new WrongUsageException("No logger specified.");
            }
        }

        Logger logger = LoggerRegistry.getLogger(args[0]);
        if (logger != null) {
            String option = null;
            if (args.length >= 2) {
                option = logger.getAcceptedOption(args[1]);
            }
            if (args.length >= 3) {
                player = server.getPlayerList().getPlayerByUsername(args[2]);
            }
            if (player == null) {
                throw new WrongUsageException("No player specified");
            }
            LogHandler handler = null;
            if (args.length >= 4) {
                handler = LogHandler.createHandler(args[3], ArrayUtils.subarray(args, 4, args.length));
                if (handler == null) {
                    throw new CommandException("Invalid handler");
                }
            }
            boolean subscribed;
            if (args.length >= 2 && "clear".equalsIgnoreCase(args[1])) {
                LoggerRegistry.unsubscribePlayer(server, player.getName(), logger.getLogName());
                subscribed = false;
            } else if (option == null) {
                subscribed = LoggerRegistry.togglePlayerSubscription(server, player.getName(), logger.getLogName(), handler);
            } else {
                subscribed = LoggerRegistry.switchPlayerSubscription(server, player.getName(), logger.getLogName(), option, handler);
            }
            if (subscribed) {
                Messenger.m(player, "gi Subscribed to " + logger.getNameWithOption(option) + '.');
            } else {
                Messenger.m(player, "gi Unsubscribed from " + logger.getLogName() + ".");
            }
        } else {
            throw new WrongUsageException("No logger named " + args[0] + ".");
        }
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (!CarpetSettings.commandLog) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            Set<String> options = new HashSet<>(Arrays.asList(LoggerRegistry.getLoggerNames(classType())));
            options.add("clear");
            options.add("defaults");
            options.add("setDefault");
            options.add("removeDefault");
            options.add("copy");
            return getListOfStringsMatchingLastWord(args, options);
        } else if (args.length == 2) {
            if ("clear".equalsIgnoreCase(args[0])) {
                List<String> players = Arrays.asList(server.getOnlinePlayerNames());
                return getListOfStringsMatchingLastWord(args, players.toArray(new String[0]));
            }

            if ("copy".equalsIgnoreCase(args[0])) {
                PlayerList serverPlayerList = server.getPlayerList();
                Set<String> playerNames = new HashSet<>();
                playerNames.addAll(Arrays.asList(serverPlayerList.getOnlinePlayerNames()));
                playerNames.addAll(Arrays.asList(serverPlayerList.getWhitelistedPlayerNames()));
                playerNames.addAll(Arrays.asList(serverPlayerList.getOppedPlayerNames()));
                return getListOfStringsMatchingLastWord(args, playerNames.toArray(new String[0]));
            }

            if ("setDefault".equalsIgnoreCase(args[0]) || "removeDefault".equalsIgnoreCase(args[0])) {
                Set<String> options = new HashSet<>(Arrays.asList(LoggerRegistry.getLoggerNames(classType())));
                return getListOfStringsMatchingLastWord(args, options);
            }

            Logger logger = LoggerRegistry.getLogger(args[0]);
            if (logger != null) {
                String[] opts = logger.getOptions();
                List<String> options = new ArrayList<>();
                options.add("clear");
                if (opts != null)
                    options.addAll(Arrays.asList(opts));
                else
                    options.add("on");
                return getListOfStringsMatchingLastWord(args, options.toArray(new String[0]));
            }
        } else if (args.length == 3) {
            if ("setDefault".equalsIgnoreCase(args[0])) {
                Logger logger = LoggerRegistry.getLogger(args[1]);
                if (logger != null) {
                    String[] opts = logger.getOptions();
                    List<String> options = new ArrayList<>();
                    if (opts != null)
                        options.addAll(Arrays.asList(opts));

                    return getListOfStringsMatchingLastWord(args, options.toArray(new String[0]));
                }
            }

            List<String> players = Arrays.asList(server.getOnlinePlayerNames());
            return getListOfStringsMatchingLastWord(args, players.toArray(new String[0]));
        } else if (args.length == 4) {
            return getListOfStringsMatchingLastWord(args, LogHandler.getHandlerNames());
        }

        return Collections.emptyList();
    }

    private int classType() {
        if (this instanceof CommandDebuglogger) return 2;
        if (this instanceof CommandSubscribe) return 3;
        return 1;
    }

    public void displayPlayerLoggerMenu(EntityPlayer player) {
        if (player == null) {
            return;
        }
        Map<String, LoggerOptions> subs = LoggerRegistry.getPlayerSubscriptions(player.getName());
        if (subs == null) {
            subs = new HashMap<>();
        }
        List<String> all_logs = Arrays.asList(LoggerRegistry.getLoggerNames(classType()));
        Collections.sort(all_logs);
        Messenger.m(player, "w _____________________");
        Messenger.m(player, "w Available logging options:");
        for (String lname : all_logs) {
            List<Object> comp = new ArrayList<>();
            String color = subs.containsKey(lname) ? "w" : "g";
            comp.add("w  - " + lname + ": ");
            Logger logger = LoggerRegistry.getLogger(lname);
            String[] options = logger.getOptions();
            if (options == null) {
                if (subs.containsKey(lname)) {
                    comp.add("l Subscribed ");
                } else {
                    comp.add(color + " [Subscribe] ");
                    comp.add("^w toggle subscription to " + lname);
                    comp.add("!/logMenu " + lname);
                }
            } else {
                for (String option : logger.getOptions()) {
                    if (subs.containsKey(lname) && option.equalsIgnoreCase(subs.get(lname).option)) {
                        comp.add("l [" + option + "] ");
                    } else {
                        comp.add(color + " [" + option + "] ");
                        comp.add("^w toggle subscription to " + lname + " " + option);
                        comp.add("!/logMenu " + lname + " " + option);
                    }

                }
            }
            if (subs.containsKey(lname)) {
                comp.add("nb [X]");
                comp.add("^w Click to toggle subscription");
                comp.add("!/logMenu " + lname);
            }
            Messenger.m(player, comp.toArray(new Object[0]));
        }
    }

    public void displayDefaultLoggerMenu(EntityPlayer player) {
        if (player == null) {
            return;
        }
        Map<String, LoggerOptions> subs = LoggerRegistry.getDefaultSubscriptions();
        List<String> all_logs = Arrays.asList(LoggerRegistry.getLoggerNames(classType()));
        Collections.sort(all_logs);
        Messenger.m(player, "w _____________________");
        Messenger.m(player, "w Available logging options:");
        for (String lname : all_logs) {
            List<Object> comp = new ArrayList<>();
            String color = subs.containsKey(lname) ? "w" : "g";
            comp.add("w  - " + lname + ": ");
            Logger logger = LoggerRegistry.getLogger(lname);
            String[] options = logger.getOptions();
            if (options == null) {
                if (subs.containsKey(lname)) {
                    comp.add("l Subscribed ");
                } else {
                    comp.add(color + " [Subscribe] ");
                    comp.add("^w set default subscription to " + lname);
                    comp.add("!/logMenu setDefault " + lname);
                }
            } else {
                for (String option : logger.getOptions()) {
                    if (subs.containsKey(lname) && option.equalsIgnoreCase(subs.get(lname).option)) {
                        comp.add("l [" + option + "] ");
                    } else {
                        comp.add(color + " [" + option + "] ");
                        comp.add("^w set default subscription to " + lname + " " + option);
                        comp.add("!/logMenu setDefault " + lname + " " + option);
                    }

                }
            }
            if (subs.containsKey(lname)) {
                comp.add("nb [X]");
                comp.add("^w Click to remove default subscription");
                comp.add("!/logMenu removeDefault " + lname);
            }
            Messenger.m(player, comp.toArray(new Object[0]));
        }
    }
}
