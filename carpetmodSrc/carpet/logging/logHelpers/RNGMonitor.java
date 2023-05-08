package carpet.logging.logHelpers;

import carpet.CarpetSettings;
import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldServer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * {@code /log rngManip}
 */
public class RNGMonitor {
    public final static int RNG_SAMPLE_RANGE = 100;
    public final static double FREQUENCY_THRESHOLD = 0.75;
    private final WorldServer worldServerIn;
    private final MinecraftServer minecraftServer;
    private final DimensionType dimension;

    private final HashMap<RNGAppType, RNGSeedBundle> randSeedBundles;

    public RNGMonitor(WorldServer worldServerIn, MinecraftServer minecraftServer) {
        this.worldServerIn = worldServerIn;
        this.minecraftServer = minecraftServer;
        if (worldServerIn != null && minecraftServer != null && !worldServerIn.isRemote) {
            this.dimension = worldServerIn.provider.getDimensionType();
            this.randSeedBundles = new HashMap<>();
            for (RNGAppType rngAppType : RNGAppType.values()) {
                this.randSeedBundles.putIfAbsent(rngAppType, new RNGSeedBundle());
            }
        } else {
            this.dimension = null;
            this.randSeedBundles = null;
        }
    }

    public static int getRngTrackingRange() {
        return CarpetSettings.rngTrackingRange == 0 ? CarpetSettings.HUDUpdateInterval : CarpetSettings.rngTrackingRange;
    }

    public boolean isValid() {
        return dimension == DimensionType.OVERWORLD;
    }

    public void registerRandSeed(RNGAppType rngAppType) {
        if (rngAppType == RNGAppType.raw || rngAppType == null) {
            return;
        }
        RNGSeedBundle seedBundle = randSeedBundles.get(rngAppType);
        if (!seedBundle.registerFlags) {
            seedBundle.registerFlags = true;
            Messenger.print_server_message(minecraftServer, String.format("Started registering RNG seed for %s", rngAppType));
        }
    }

    public void clearSeedRegister(RNGAppType rngAppType) {
        if (rngAppType == RNGAppType.raw || rngAppType == null) {
            return;
        }
        RNGSeedBundle seedBundle = randSeedBundles.get(rngAppType);
        long oldSeed = seedBundle.storedRandSeed;
        boolean oldFlag = seedBundle.registerFlags;
        seedBundle.registerFlags = false;
        seedBundle.storedRandSeed = 0;
        if (oldSeed != 0) {
            Messenger.print_server_message(minecraftServer, String.format("Cleared RNG seed monitor for %s on %d", rngAppType, oldSeed));
        } else if (oldFlag) {
            Messenger.print_server_message(minecraftServer, String.format("Stopped registering RNG seed monitor for %s", rngAppType));
        }
    }

    public void tryUpdateSeeds(RNGAppType rngAppType) {
        if (this.isValid() && rngAppType != null) {
            RNGSeedBundle seedBundle = randSeedBundles.get(rngAppType);
            long currSeed = worldServerIn.getRandSeed();
            if (rngAppType == RNGAppType.raw) {
                seedBundle.currentRandSeed = currSeed;
                return;
            }
            LinkedList<Integer> errorTicks = seedBundle.abnormalTicks;
            int currTick = minecraftServer.getTickCounter();
            // count only once per game tick
            boolean tickFresh = errorTicks.isEmpty() || currTick > errorTicks.getLast();
            if (!tickFresh) {
                return;
            }
            if (seedBundle.storedRandSeed == 0 && currSeed != seedBundle.currentRandSeed
                    || seedBundle.storedRandSeed != 0 && currSeed != seedBundle.storedRandSeed) {
                errorTicks.addLast(currTick);
            }
            seedBundle.currentRandSeed = currSeed;
            // forget the ticks out of date
            while (tickFresh && !errorTicks.isEmpty()) {
                if (currTick - errorTicks.getFirst() >= getRngTrackingRange()) {
                    errorTicks.removeFirst();
                } else {
                    tickFresh = false;
                }
            }
            if (seedBundle.registerFlags) {
                seedBundle.trackedRandSeeds.addLast(currSeed);
                // count the most frequent seed
                if (seedBundle.trackedRandSeeds.size() >= RNG_SAMPLE_RANGE) {
                    boolean successful = seedBundle.countFrequentSeed();
                    seedBundle.registerFlags = false;
                    seedBundle.trackedRandSeeds.clear();
                    if (successful) {
                        Messenger.print_server_message(minecraftServer, String.format("Registered RNG seed for %s: %d", rngAppType, seedBundle.storedRandSeed));
                    } else {
                        Messenger.print_server_message(minecraftServer, String.format("Failed to register RNG seed for %s", rngAppType));
                    }
                }
            }
        }
    }

    public void updateLogHUD() {
        if (this.isValid()) {
            try {
                LoggerRegistry.getLogger("rngManip").log(playerOption -> {
                    RNGAppType rngAppType = RNGAppType.valueOf(playerOption);
                    RNGSeedBundle seedBundle = randSeedBundles.get(rngAppType);
                    long currSeed = seedBundle.currentRandSeed;
                    if (rngAppType == RNGAppType.raw) {
                        return new ITextComponent[]{Messenger.m(null, String.format("g RNG raw: %d", currSeed))};
                    }
                    ITextComponent[] components = new ITextComponent[2];
                    double faultRate = seedBundle.abnormalTicks.size() * (100.0 / getRngTrackingRange());
                    String rateColor = Messenger.heatmap_color(faultRate + 75.0, 150.0);
                    if (seedBundle.storedRandSeed == 0) {
                        String color = seedBundle.registerFlags ? "d" : rateColor;
                        components[0] = Messenger.m(null, String.format("%s RNG %s: %d", color, rngAppType, currSeed));
                        components[1] = Messenger.m(null, String.format("%s RNG instability: ", color), String.format("%s %.4f%%", color, faultRate));
                    } else {
                        String typeColor = Messenger.rng_app_type_color(rngAppType);
                        components[0] = Messenger.m(null, String.format("%s RNG %s: %d", typeColor, rngAppType, currSeed));
                        components[1] = Messenger.m(null, String.format("%s RNG fault rate: ", "g"), String.format("%s %.4f%%", rateColor, faultRate));
                    }
                    return components;
                });
            } catch (IllegalArgumentException | NullPointerException | NoSuchElementException ignored) {
            }
        }
    }

    public enum RNGAppType {
        raw, fortune, mobSpawn, ironFarm,
        /**
         * Light Update Suppression
         */
        lightCheck, chunkTick, farmer
    }

    private static class RNGSeedBundle {
        public final LinkedList<Long> trackedRandSeeds;
        public final LinkedList<Integer> abnormalTicks;
        public long currentRandSeed;
        /**
         * registered RNG seed
         */
        public long storedRandSeed;
        /**
         * true if the RNG seed is being registered
         */
        public boolean registerFlags;

        public RNGSeedBundle() {
            trackedRandSeeds = new LinkedList<>();
            abnormalTicks = new LinkedList<>();
            currentRandSeed = 0;
            storedRandSeed = 0;
            registerFlags = false;
        }

        public boolean countFrequentSeed() {
            HashMap<Long, Integer> seedFrequencyMap = new HashMap<>();
            int maxCount = 0;
            long majorSeed = 0;
            int capacity = trackedRandSeeds.size();
            for (Iterator<Long> iterator = trackedRandSeeds.iterator(); iterator.hasNext(); iterator.remove()) {
                long seed = iterator.next();
                Integer count = seedFrequencyMap.get(seed);
                if (count == null) {
                    count = 1;
                } else {
                    ++count;
                }
                seedFrequencyMap.put(seed, count);
                if (count > maxCount) {
                    maxCount = count;
                    if (majorSeed != seed) {
                        majorSeed = seed;
                    }
                }
            }
            if (maxCount >= FREQUENCY_THRESHOLD * capacity) {
                storedRandSeed = majorSeed;
                return true;
            }
            return false;
        }
    }
}
