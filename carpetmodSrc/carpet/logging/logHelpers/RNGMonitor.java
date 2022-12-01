package carpet.logging.logHelpers;

import carpet.CarpetSettings;
import carpet.carpetclient.CarpetClientChunkLogger;
import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;

import java.util.HashMap;

/**
 * {@code /log rngManip}
 */
public class RNGMonitor {
    public final World worldIn;
    public final DimensionType dimension;
    private final HashMap<RNGAppType, Long> allRandSeeds;

    public RNGMonitor(World worldIn) {
        this.worldIn = worldIn;
        if (worldIn != null && !worldIn.isRemote) {
            this.dimension = worldIn.provider.getDimensionType();
            this.allRandSeeds = new HashMap<>();
            for (RNGAppType rngAppType : RNGAppType.values()) {
                allRandSeeds.putIfAbsent(rngAppType, 0L);
            }
        } else {
            this.dimension = null;
            this.allRandSeeds = null;
        }
    }

    public boolean isValid() {
        return LoggerRegistry.__rngManip && dimension == DimensionType.OVERWORLD;
    }

    public void tryUpdateSeeds(RNGAppType rngAppType) {
        if (rngAppType == null || !this.isValid()) {
            return;
        }
        Long currSeed = worldIn.getRandSeed();
        if (!currSeed.equals(allRandSeeds.get(rngAppType))) {
            allRandSeeds.replace(rngAppType, currSeed);
            LoggerRegistry.getLogger("rngManip").log(playerOption -> {
                if (rngAppType.name().equals(playerOption)) {
                    if (CarpetSettings.chunkDebugTool && "raw".equals(playerOption)) {
                        return new ITextComponent[]{Messenger.s(null, String.format("RNG on %s: %d", CarpetClientChunkLogger.reason, currSeed))};
                    } else {
                        return new ITextComponent[]{Messenger.s(null, String.format("RNG %s: %d", rngAppType.name(), currSeed))};
                    }
                } else {
                    return null;
                }
            });
        }
    }

    public enum RNGAppType {
        raw, fortune, mobSpawning
    }
}
