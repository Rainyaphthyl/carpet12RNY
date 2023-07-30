package carpet.helpers;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.logging.LoggerRegistry;
import carpet.logging.logHelpers.RNGMonitor;
import carpet.logging.logHelpers.TickWarpLogger;
import carpet.pubsub.PubSubInfoProvider;
import carpet.utils.Messenger;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.MathHelper;
import redstone.multimeter.common.TickTask;
import redstone.multimeter.helper.WorldHelper;

import javax.annotation.Nonnull;
import java.util.Objects;

public class TickSpeed {
    public static final int PLAYER_GRACE = 2;
    public static float tickrate = 20.0f;
    private static final PubSubInfoProvider<Float> PUBSUB_TICKRATE = new PubSubInfoProvider<>(CarpetServer.PUBSUB, "carpet.tick.rate", 0, () -> tickrate);
    public static long mspt = 50L;
    public static long warp_temp_mspt = 1L;
    public static long time_bias = 0;
    public static long time_warp_start_time = 0;
    public static long time_warp_scheduled_ticks = 0;
    public static EntityPlayer time_advancerer = null;
    public static String tick_warp_callback = null;
    public static ICommandSender tick_warp_sender = null;
    public static int player_active_timeout = 0;
    public static boolean process_entities = true;
    public static boolean is_paused = false;
    public static boolean is_superHot = false;
    public static boolean gamePaused = false;

    static {
        new PubSubInfoProvider<>(CarpetServer.PUBSUB, "minecraft.performance.mspt", CarpetSettings.HUDUpdateInterval, TickSpeed::getMSPT);
        new PubSubInfoProvider<>(CarpetServer.PUBSUB, "minecraft.performance.tps", CarpetSettings.HUDUpdateInterval, TickSpeed::getTPS);
    }

    public static void reset_player_active_timeout() {
        if (player_active_timeout < PLAYER_GRACE) {
            player_active_timeout = PLAYER_GRACE;
        }
    }

    public static void add_ticks_to_run_in_pause(int ticks) {
        player_active_timeout = PLAYER_GRACE + ticks;
    }

    public static void tickrate(float rate) {
        tickrate = rate;
        mspt = (long) (1000.0 / tickrate);
        if (mspt <= 0) {
            mspt = 1L;
            tickrate = 1000.0f;
        }
        PUBSUB_TICKRATE.publish();
    }

    /**
     * @param advance use {@code 0} to interrupt tick warping; {@code -1} to query tick warping status
     */
    @Nonnull
    public static String tickrate_advance(EntityPlayer player, long advance, String callback, ICommandSender icommandsender) {
        if (0 == advance) {
            tick_warp_callback = null;
            tick_warp_sender = null;
            long doneTicks = finish_time_warp();
            return "Warp interrupted after " + doneTicks + " ticks";
        } else if (advance < 0) {
            TickWarpLogger.query_status(player);
            return "";
        }
        if (time_bias > 0) {
            Messenger.m(player, "g Check the status with command ", "gbi /tick warp status", "/tick warp status", "g !");
            if (time_advancerer == null) {
                return "The server is already advancing time at the moment. Try later or talk to the admins";
            } else {
                return time_advancerer.getName() + " is already advancing time at the moment. Try later or talk to them";
            }
        }
        time_advancerer = player;
        time_warp_start_time = System.nanoTime();
        time_warp_scheduled_ticks = advance;
        time_bias = advance;
        tick_warp_callback = callback;
        tick_warp_sender = icommandsender;
        return "Warp speed for " + advance + " ticks ....";
    }

    /**
     * @return completed ticks
     */
    public static long finish_time_warp() {

        long completed_ticks = time_warp_scheduled_ticks - time_bias;
        double milis_to_complete = System.nanoTime() - time_warp_start_time;
        if (milis_to_complete == 0.0) {
            milis_to_complete = 1.0;
        }
        milis_to_complete /= 1000000.0;
        int tps = (int) (1000.0D * completed_ticks / milis_to_complete);
        double mspt = milis_to_complete / completed_ticks;
        time_warp_scheduled_ticks = 0;
        time_warp_start_time = 0;
        if (tick_warp_callback != null) {
            ICommandManager icommandmanager = Objects.requireNonNull(tick_warp_sender.getServer()).getCommandManager();
            try {
                int j = icommandmanager.executeCommand(tick_warp_sender, tick_warp_callback);

                if (j < 1) {
                    if (time_advancerer != null) {
                        Messenger.m(time_advancerer, "r Command Callback failed: ", "rb /" + tick_warp_callback, "/" + tick_warp_callback);
                    }
                }
            } catch (Throwable var23) {
                if (time_advancerer != null) {
                    Messenger.m(time_advancerer, "r Command Callback failed - unknown error: ", "rb /" + tick_warp_callback, "/" + tick_warp_callback);
                }
            }
            tick_warp_callback = null;
            tick_warp_sender = null;
        }
        boolean broadcast = true;
        if (time_advancerer != null) {
            MinecraftServer server = time_advancerer.getServer();
            //noinspection ConstantValue
            if (server != null && server.getPlayerList().getPlayerByUUID(time_advancerer.getUniqueID()) != null) {
                broadcast = false;
                String text = String.format("... Time warp completed with %d tps, or %.2f mspt", tps, mspt);
                Messenger.s(time_advancerer, text, "wi");
                Messenger.s(server, text, "gi");
            }
            time_advancerer = null;
        }
        if (broadcast) {
            Messenger.print_server_message(CarpetServer.minecraft_server, String.format("... Time warp completed with %d tps, or %.2f mspt", tps, mspt));
        }
        time_bias = 0;
        return completed_ticks;
    }

    public static boolean continueWarp() {
        if (time_bias > 0) {
            if (time_bias == time_warp_scheduled_ticks) //first call after previous tick, adjust start time
            {
                time_warp_start_time = System.nanoTime();
            }
            time_bias -= 1;
            return true;
        } else {
            finish_time_warp();
            return false;
        }
    }

    public static void tick() {
        process_entities = true;
        if (player_active_timeout > 0) {
            player_active_timeout--;
        }
        if (is_paused) {
            if (player_active_timeout < PLAYER_GRACE) {
                process_entities = false;
            }
        } else if (is_superHot) {
            if (player_active_timeout <= 0) {
                process_entities = false;

            }
        }
    }

    /**
     * @param server the Minecraft Server instance
     * @return {@code true} if the game should tick normally, or {@code false} if the game is paused
     */
    public static boolean tickOrPause(MinecraftServer server) {
        boolean paused = gamePaused;
        if (paused) {
            server.profiler.startSection("jobs");
            WorldHelper.startTickTask(TickTask.PACKETS);
            LagSpikeHelper.processLagSpikes(null, LagSpikeHelper.TickPhase.PLAYER, LagSpikeHelper.PrePostSubPhase.PRE);
            if (LoggerRegistry.__rngManip) {
                server.worlds[0].rngMonitor.tryUpdateSeeds(RNGMonitor.RNGAppType.fortune);
            }
            server.runFutureTasks();
            LagSpikeHelper.processLagSpikes(null, LagSpikeHelper.TickPhase.PLAYER, LagSpikeHelper.PrePostSubPhase.POST);
            server.profiler.endSection();
            WorldHelper.endTickTask(); // RSMM
        }
        return !paused;
    }

    public static double getMSPT() {
        return MathHelper.average(CarpetServer.minecraft_server.tickTimeArray) * 1.0E-6D;
    }

    public static double getTPS() {
        return 1000.0D / Math.max((time_warp_start_time != 0) ? 0.0 : TickSpeed.mspt, getMSPT());
    }
}
