package carpet.logging.logHelpers;

import carpet.logging.Logger;
import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.EnumSkyBlock;

import java.util.ArrayList;
import java.util.List;

public class LightCheckReporter {
    public static final String[] LOGGER_OPTIONS = new String[]{"raw", "relative", "verbose"};
    public static final String DEFAULT_OPTION = LOGGER_OPTIONS[0];
    private static Logger instance = null;

    public static Logger get_instance() {
        if (instance == null) {
            instance = LoggerRegistry.getLogger("lightCheck");
        }
        return instance;
    }

    public static void report_light_update(EnumSkyBlock lightType, int oldValue, int newValue, BlockPos pos, EntityPlayer player, int playerIndex, int playerListSize) {
        try {
            get_instance().log((option, loggingPlayer) -> {
                BlockPos source = player.getPosition();
                BlockPos diff = pos.subtract(source);
                List<Object> msgParts = new ArrayList<>();
                boolean warning = loggingPlayer.getUniqueID().equals(player.getUniqueID());
                String warnStyle = warning ? "y" : "w";
                String lightStyle;
                switch (lightType) {
                    case SKY:
                        lightStyle = "c";
                        break;
                    case BLOCK:
                        lightStyle = "l";
                        break;
                    default:
                        lightStyle = warnStyle;
                }
                boolean verbose = false;
                msgParts.add(String.format("%s %s light %d -> %d at ", lightStyle, lightType.name(), oldValue, newValue));
                switch (option) {
                    case "verbose":
                        verbose = true;
                    case "raw":
                        msgParts.add(String.format("%s [%d, %d, %d]", warnStyle, pos.getX(), pos.getY(), pos.getZ()));
                        msgParts.add(String.format("^g relative: [%+d, %+d, %+d]", diff.getX(), diff.getY(), diff.getZ()));
                        msgParts.add(String.format("/tp %d %d %d", pos.getX(), pos.getY(), pos.getZ()));
                        msgParts.add(String.format("%s  by ", warnStyle));
                        msgParts.add(String.format("%s %s", warnStyle, player.getName()));
                        msgParts.add(String.format("^g [%d, %d, %d]", source.getX(), source.getY(), source.getZ()));
                        break;
                    case "relative":
                        msgParts.add(String.format("%s [%+d, %+d, %+d]", warnStyle, diff.getX(), diff.getY(), diff.getZ()));
                        msgParts.add(String.format("^g position: [%d, %d, %d]", pos.getX(), pos.getY(), pos.getZ()));
                        msgParts.add(String.format("/tp %d %d %d", pos.getX(), pos.getY(), pos.getZ()));
                        msgParts.add(String.format("%s  from ", warnStyle));
                        msgParts.add(String.format("%s [%d, %d, %d]", warnStyle, source.getX(), source.getY(), source.getZ()));
                        msgParts.add(String.format("^g %s", player.getName()));
                        break;
                }
                msgParts.add(String.format("/tp %d %d %d", source.getX(), source.getY(), source.getZ()));
                msgParts.add("g  ");
                msgParts.add(String.format("%s (%d/%d)", warnStyle, playerIndex, playerListSize));
                msgParts.add(String.format("^g player %d is chosen from [0, %d] among all %d player(s)",
                        playerIndex, playerListSize - 1, playerListSize));
                if (verbose) {
                    ITextComponent[] components = new ITextComponent[2];
                    components[0] = Messenger.c(msgParts.toArray(new Object[0]));
                    components[1] = Messenger.c(
                            String.format("%s   - deviating [%+d, %+d, %+d] from [%d, %d, %d]",
                                    warning ? "d" : "g",
                                    diff.getX(), diff.getY(), diff.getZ(),
                                    source.getX(), source.getY(), source.getZ()));
                    return components;
                } else {
                    return new ITextComponent[]{Messenger.c(msgParts.toArray(new Object[0]))};
                }
            });
        } catch (Exception ignored) {
        }
    }
}
