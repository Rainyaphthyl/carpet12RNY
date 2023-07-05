package carpet.logging.logHelpers;

import carpet.helpers.TickSpeed;
import carpet.logging.Logger;
import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;

import java.util.Locale;

public class TPSLogHelper {
    public static final String[] LOGGER_OPTIONS = new String[]{"sample", "peak", "average"};
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
            switch (option) {
                case "average":
                case "sample":
                    msptWarping = MathHelper.average(server.tickTimeArray) * 1.0E-6D;
                    long minInterval = (TickSpeed.time_bias > 0 ? 0L : TickSpeed.mspt) * 1_000_000L;
                    msptActual = get_limited_average(server.tickTimeArray, minInterval, Long.MAX_VALUE) * 1.0E-6D;
                    break;
                case "peak":
            }
            double tpsActual = 1000.0 / msptActual;
            String colorMspt = Messenger.heatmap_color(msptWarping, TickSpeed.mspt);
            String colorTps = Messenger.heatmap_color(msptActual, TickSpeed.mspt);
            message[0] = Messenger.m(null,
                    "g TPS: ", String.format(Locale.US, "%s %.1f", colorTps, tpsActual),
                    "g  MSPT: ", String.format(Locale.US, "%s %.1f", colorMspt, msptWarping));
            pair[0] = msptWarping;
            pair[1] = tpsActual;
            return message;
        }, "MSPT", pair[0], "TPS", pair[1]);
    }

    public static double get_limited_average(long[] values, long floor, long ceil) {
        if (values == null) {
            return Double.NaN;
        }
        long sum = 0L;
        for (long elem : values) {
            long value = Math.min(Math.max(elem, floor), ceil);
            sum += value;
        }
        return (double) sum / (double) values.length;
    }
}
