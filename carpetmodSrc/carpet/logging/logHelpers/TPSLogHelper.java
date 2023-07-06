package carpet.logging.logHelpers;

import carpet.CarpetSettings;
import carpet.helpers.TickSpeed;
import carpet.logging.Logger;
import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;
import java.util.Locale;

public class TPSLogHelper {
    public static final String[] LOGGER_OPTIONS = new String[]{"average", "sample", "peak"};
    public static final String DEFAULT_OPTION = LOGGER_OPTIONS[0];
    public static final String NAME = "tps";
    private static Logger instance = null;

    public static Logger get_instance() {
        if (instance == null) {
            instance = LoggerRegistry.getLogger(NAME);
        }
        return instance;
    }

    public static void update_log_HUD(MinecraftServer server) {
        if (server == null) {
            return;
        }
        double[] pair = new double[2];
        get_instance().log(option -> {
            ITextComponent[] message = new ITextComponent[1];
            double msptWarping = Double.NaN;
            double msptActual = Double.NaN;
            boolean warping = TickSpeed.time_bias > 0;
            long[] sampleTimesArray = server.tickTimeArray;
            switch (option) {
                case "sample":
                    sampleTimesArray = get_partial_stats(server);
                case "average":
                    msptWarping = MathHelper.average(sampleTimesArray) * 1.0E-6D;
                    if (warping) {
                        msptActual = msptWarping;
                    } else {
                        long stdInterval = TickSpeed.mspt * 1_000_000L;
                        msptActual = get_average_truncated(sampleTimesArray, stdInterval, Long.MAX_VALUE) * 1.0E-6D;
                    }
                    break;
                case "peak":
                    sampleTimesArray = get_partial_stats(server);
                    msptWarping = get_max(sampleTimesArray) * 1.0E-6D;
                    if (warping) {
                        msptActual = msptWarping;
                    } else {
                        long stdInterval = TickSpeed.mspt * 1_000_000L;
                        msptActual = get_max_truncated(sampleTimesArray, stdInterval, Long.MAX_VALUE) * 1.0E-6D;
                    }
                    break;
            }
            double tpsActual = 1000.0 / msptActual;
            String colorMspt = Messenger.heatmap_color(msptWarping, TickSpeed.mspt);
            String colorTps = (warping || msptActual <= TickSpeed.mspt || msptWarping > TickSpeed.mspt)
                    ? colorMspt : Messenger.heatmap_color(msptActual, TickSpeed.mspt);
            message[0] = Messenger.m(null,
                    "g TPS: ", String.format(Locale.US, "%s %.1f", colorTps, tpsActual),
                    "g  MSPT: ", String.format(Locale.US, "%s %.1f", colorMspt, msptWarping));
            pair[0] = msptWarping;
            pair[1] = tpsActual;
            return message;
        }, "MSPT", pair[0], "TPS", pair[1]);
    }

    public static double get_average_truncated(long[] values, long floor, long ceil) {
        if (values == null || floor > ceil) {
            return Double.NaN;
        }
        long sum = 0L;
        int lowers = 0;
        int uppers = 0;
        for (long value : values) {
            if (value < floor) {
                value = floor;
                ++lowers;
            } else if (value > ceil) {
                value = ceil;
                ++uppers;
            }
            sum += value;
        }
        if (lowers == values.length) {
            return floor;
        } else if (uppers == values.length) {
            return ceil;
        }
        return (double) sum / values.length;
    }

    public static long get_max_truncated(long[] values, long floor, long ceil) {
        if (floor > ceil) {
            return ceil;
        } else if (values == null) {
            return floor;
        }
        long maxVal = floor;
        for (long value : values) {
            if (value >= ceil) {
                return ceil;
            } else if (value > maxVal) {
                maxVal = value;
            }
        }
        return maxVal;
    }

    public static long get_max(long[] values) {
        if (values == null) {
            return Long.MIN_VALUE;
        }
        long maxVal = Long.MIN_VALUE;
        for (long value : values) {
            if (value > maxVal) {
                maxVal = value;
            }
        }
        return maxVal;
    }

    public static long[] get_partial_stats(@Nonnull MinecraftServer server) {
        int length = Math.max(1, CarpetSettings.HUDUpdateInterval);
        if (length >= server.tickTimeArray.length) {
            return server.tickTimeArray;
        }
        long[] sampleTimesArray = new long[length];
        for (int i = 0, t = server.getTickCounter(); i < length; ++i) {
            sampleTimesArray[i] = server.tickTimeArray[--t % server.tickTimeArray.length];
        }
        return sampleTimesArray;
    }
}
