package carpet.logging.logHelpers;

import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldServer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * {@code /log rngManip}
 */
public class RNGMonitor {
    public static int rngTrackingRange = 20;
    private final WorldServer worldServerIn;
    private final MinecraftServer minecraftServer;
    private final DimensionType dimension;
    private final HashMap<RNGAppType, Long> currRandSeeds;
    private final HashMap<RNGAppType, LinkedList<Integer>> updateTickLists;

    public RNGMonitor(WorldServer worldServerIn, MinecraftServer minecraftServer) {
        this.worldServerIn = worldServerIn;
        this.minecraftServer = minecraftServer;
        if (worldServerIn != null && minecraftServer != null && !worldServerIn.isRemote) {
            this.dimension = worldServerIn.provider.getDimensionType();
            this.currRandSeeds = new HashMap<>();
            this.updateTickLists = new HashMap<>();
            for (RNGAppType rngAppType : RNGAppType.values()) {
                this.currRandSeeds.putIfAbsent(rngAppType, 0L);
                this.updateTickLists.putIfAbsent(rngAppType, new LinkedList<>());
            }
        } else {
            this.dimension = null;
            this.currRandSeeds = null;
            this.updateTickLists = null;
        }
    }

    public boolean isValid() {
        return dimension == DimensionType.OVERWORLD;
    }

    public void tryUpdateSeeds(RNGAppType rngAppType) {
        if (this.isValid() && rngAppType != null) {
            LinkedList<Integer> updateTicks = updateTickLists.get(rngAppType);
            int currTick = minecraftServer.getTickCounter();
            long currSeed = worldServerIn.getRandSeed();
            // count only once per game tick
            boolean tickFresh = updateTicks.isEmpty() || currTick > updateTicks.getLast();
            if (tickFresh && currSeed != currRandSeeds.get(rngAppType)) {
                currRandSeeds.replace(rngAppType, currSeed);
                updateTicks.addLast(currTick);
            }
            // forget the ticks out of date
            while (tickFresh && !updateTicks.isEmpty()) {
                if (currTick - updateTicks.getFirst() >= rngTrackingRange) {
                    updateTicks.removeFirst();
                } else {
                    tickFresh = false;
                }
            }
        }
    }

    public void updateLogHUD() {
        if (this.isValid()) {
            try {
                LoggerRegistry.getLogger("rngManip").log(playerOption -> {
                    RNGAppType rngAppType = RNGAppType.valueOf(playerOption);
                    ITextComponent[] components = new ITextComponent[2];
                    double faultRate = updateTickLists.get(rngAppType).size() * (100.0 / rngTrackingRange);
                    String rateColor = Messenger.heatmap_color(faultRate + 75.0, 150.0);
                    String typeColor = Messenger.rng_app_type_color(rngAppType);
                    components[0] = Messenger.m(null, String.format("%s RNG %s: %d", typeColor, rngAppType, currRandSeeds.get(rngAppType)));
                    components[1] = Messenger.m(null, "g RNG fault rate: ", String.format("%s %.4f%%", rateColor, faultRate));
                    return components;
                });
            } catch (IllegalArgumentException | NullPointerException | NoSuchElementException ignored) {
            }
        }
    }

    public enum RNGAppType {
        raw, fortune, mobSpawning
    }
}
