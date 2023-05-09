package carpet.logging.logHelpers;

import carpet.logging.Logger;
import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.EnumSkyBlock;

public class LightCheckReporter {
    private static Logger instance = null;

    public static Logger get_instance() {
        if (instance == null) {
            instance = LoggerRegistry.getLogger("lightCheck");
        }
        return instance;
    }

    public static void report_light_update(EnumSkyBlock lightType, int oldValue, int newValue, BlockPos pos, EntityPlayer player, int playerIndex, int playerListSize) {
        get_instance().log(option -> {
            ITextComponent message = Messenger.c(String.format("w %s light %d->%d at [%d, %d, %d] by %s (%d/%d)",
                    lightType.name(), oldValue, newValue, pos.getX(), pos.getY(), pos.getZ(),
                    player.getName(), playerIndex, playerListSize));
            return new ITextComponent[]{message};
        });
    }
}
