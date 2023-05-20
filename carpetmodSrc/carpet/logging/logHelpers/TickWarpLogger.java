package carpet.logging.logHelpers;

import carpet.helpers.TickSpeed;
import carpet.logging.Logger;
import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import com.google.common.collect.Lists;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Ported from <a href="https://github.com/TISUnion/TISCarpet113">TISCarpet113</a>
 */
public class TickWarpLogger {
    public static final String[] LOGGER_OPTIONS = new String[]{"bar", "value"};
    public static final String DEFAULT_OPTION = LOGGER_OPTIONS[0];
    public static final String NAME = "tickWarp";
    private static Logger instance = null;

    public static Logger get_instance() {
        if (instance == null) {
            instance = LoggerRegistry.getLogger(NAME);
        }
        return instance;
    }

    public static void update_log_HUD() {
        if (TickSpeed.time_bias > 0) {
            get_instance().log((option, player) -> {
                long totalTicks = TickSpeed.time_warp_scheduled_ticks;
                long doneTicks = totalTicks - TickSpeed.time_bias;
                double progressRate = (double) doneTicks / Math.max(totalTicks, 1);
                List<Object> components = new ArrayList<>();
                components.add("g Warp ");
                switch (option) {
                    case "bar":
                        components.add(get_progress_bar(progressRate));
                        break;
                    case "value":
                        components.add(get_duration_ratio(doneTicks, totalTicks));
                        break;
                    default:
                        return new ITextComponent[0];
                }
                components.add("w  ");
                components.add(get_progress_percentage(progressRate));
                return new ITextComponent[]{Messenger.c(components.toArray())};
            });
        }
    }

    @Nonnull
    private static ITextComponent get_progress_percentage(double progressRate) {
        return Messenger.c(String.format("g %.1f%%", progressRate * 100));
    }

    @Nonnull
    private static ITextComponent get_progress_bar(double progressRate) {
        List<Object> list = Lists.newArrayList();
        list.add("g [");
        for (int i = 1; i <= 10; i++) {
            list.add(progressRate >= i / 10.0D ? "g #" : "f -");
        }
        list.add("g ]");
        return Messenger.c(list.toArray(new Object[0]));
    }

    @Nonnull
    private static ITextComponent get_duration_ratio(long doneTicks, long totalTicks) {
        return Messenger.c(String.format("g %d", doneTicks), "f /", String.format("g %d", totalTicks));
    }

}
