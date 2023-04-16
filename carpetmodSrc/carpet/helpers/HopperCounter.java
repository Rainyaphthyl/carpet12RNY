package carpet.helpers;

import carpet.CarpetServer;
import carpet.pubsub.PubSubInfoProvider;
import carpet.utils.Messenger;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class HopperCounter {
    public static final HopperCounter CACTUS = new HopperCounter(EnumDyeColor.GREEN, "cactus");
    public static final HopperCounter ALL = new HopperCounter(EnumDyeColor.GRAY, "all");
    public static final Map<String, HopperCounter> COUNTERS;
    private static long currSyncTick = 0;

    static {
        COUNTERS = new HashMap<>();
        for (EnumDyeColor color : EnumDyeColor.values()) {
            COUNTERS.put(color.getName(), new HopperCounter(color, color.getName()));
        }
        COUNTERS.put("cactus", CACTUS);
        COUNTERS.put("all", ALL);
    }

    public final EnumDyeColor color;
    private final Object2LongMap<ItemWithMeta> linearPartials = new Object2LongLinkedOpenHashMap<>();
    private final Object2LongMap<ItemWithMeta> squaredPartials = new Object2LongLinkedOpenHashMap<>();
    private final Object2LongMap<ItemWithMeta> currentPartials = new Object2LongLinkedOpenHashMap<>();
    private final PubSubInfoProvider<Long> pubSubProvider;
    private final String name;
    private long startTick = -1;
    private long startMillis = 0;
    private long linearTotal = 0;
    /**
     * used for debug
     */
    private long actualTicks = 0;
    private long squaredTotal = 0;

    private HopperCounter(EnumDyeColor color, String name) {
        this.name = name;
        this.color = color;
        pubSubProvider = new PubSubInfoProvider<>(CarpetServer.PUBSUB, "carpet.counter." + name, 0, this::getTotalItems);
    }

    public static void resetAll(boolean instant) {
        for (HopperCounter counter : COUNTERS.values()) {
            counter.reset(instant);
        }
    }

    public static List<ITextComponent> formatAll(boolean realtime) {
        List<ITextComponent> text = new ArrayList<>();
        for (HopperCounter counter : COUNTERS.values()) {
            List<ITextComponent> temp = counter.format(realtime, false);
            if (temp.size() > 1) {
                text.addAll(temp);
            }
        }
        if (text.isEmpty()) {
            text.add(Messenger.s(null, "No items have been counted yet."));
        }
        return text;
    }

    @Nullable
    public static HopperCounter getCounter(String color) {
        try {
            return COUNTERS.get(color);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static void updateAll(@Nonnull MinecraftServer server) {
        currSyncTick = server.getTickCounter();
        for (HopperCounter counter : COUNTERS.values()) {
            counter.update();
        }
    }

    @Nonnull
    public static StatsRecord get_reliable_average(long ticks, long linearTotal, long squaredTotal) {
        double average;
        double error;
        if (ticks <= 1) {
            average = ticks == 1 ? linearTotal : Double.NaN;
            error = Double.NaN;
        } else {
            average = (double) linearTotal / ticks;
            double temp = squaredTotal - average * linearTotal;
            double divisor = ticks * (ticks - 1);
            error = Math.sqrt(temp / divisor);
        }
        average *= 72000;
        error *= 72000;
        return new StatsRecord(average, error);
    }

    public static double round_half_even(double value, int scale) {
        double rate = simplePow(10.0, scale);
        return Math.rint(value * rate) / rate;
    }

    /**
     * {@code O(n)}
     */
    public static double simplePow(double base, int exponent) {
        double pow = 1.0;
        if (exponent >= 0) {
            for (int i = 0; i < exponent; ++i) {
                pow *= base;
            }
        } else {
            for (int i = exponent; i < 0; ++i) {
                pow /= base;
            }
        }
        return pow;
    }

    /**
     * {@code O(n)}
     */
    public static long simplePow(long base, int exponent) {
        long pow = 1;
        if (exponent >= 0) {
            for (int i = 0; i < exponent; ++i) {
                pow *= base;
            }
        } else {
            for (int i = exponent; i < 0; ++i) {
                pow /= base;
            }
        }
        return pow;
    }

    private void update() {
        if (startTick >= 0) {
            long totalInc = 0;
            for (ItemWithMeta item : currentPartials.keySet()) {
                long partialInc = currentPartials.getLong(item);
                linearPartials.put(item, linearPartials.getLong(item) + partialInc);
                squaredPartials.put(item, squaredPartials.getLong(item) + partialInc * partialInc);
                totalInc += partialInc;
                currentPartials.put(item, 0);
            }
            linearTotal += totalInc;
            squaredTotal += totalInc * totalInc;
            if (currSyncTick % 900 == 0) {
                currentPartials.clear();
            }
            ++actualTicks;
        }
    }

    public void add(ItemStack stack) {
        if (startTick == -1) {
            startTick = currSyncTick;
            startMillis = MinecraftServer.getCurrentTimeMillis();
        }
        ItemWithMeta item = new ItemWithMeta(stack);
        long stackCount = stack.getCount();
        currentPartials.put(item, currentPartials.getLong(item) + stackCount);
        pubSubProvider.publish();
    }

    /**
     * @param instant {@code true} for "reset" and {@code false} for "stop"
     */
    public void reset(boolean instant) {
        currentPartials.clear();
        linearPartials.clear();
        linearTotal = 0;
        squaredPartials.clear();
        squaredTotal = 0;
        if (instant) {
            startTick = currSyncTick;
            startMillis = MinecraftServer.getCurrentTimeMillis();
        } else {
            startTick = -1;
            startMillis = 0;
        }
        actualTicks = 0;
        pubSubProvider.publish();
    }

    public List<ITextComponent> format(boolean realTime, boolean brief) {
        if (linearPartials.isEmpty()) {
            if (brief) {
                return Collections.singletonList(Messenger.m(null, "g " + name + ": -, -/h, - min "));
            }
            return Collections.singletonList(Messenger.s(null, String.format("No items for %s yet", name)));
        }
        long total = linearTotal;
        long ticks = Math.max(realTime ? (MinecraftServer.getCurrentTimeMillis() - startMillis) / 50 : currSyncTick - startTick, 1);
        if (total == 0) {
            if (brief) {
                return Collections.singletonList(Messenger.m(null,
                        String.format("c %s: 0, 0/h, %.1f min ", name, ticks / (20.0 * 60.0))));
            }
            return Collections.singletonList(Messenger.m(null,
                    String.format("w No items for %s yet (%.2f min.%s)",
                            name, ticks / (20.0 * 60.0), (realTime ? " - real time" : "")),
                    "nb  [X]", "^g reset", "!/counter " + name + " reset"));
        }
        if (!realTime) {
            return formatReliable(brief);
        }
        if (brief) {
            return Collections.singletonList(Messenger.m(null,
                    String.format("c %s: %d, %d/h, %.1f min ",
                            name, total, total * (20 * 60 * 60) / ticks, ticks / (20.0 * 60.0))));
        }
        List<ITextComponent> list = new ArrayList<>();
        if (ticks == actualTicks) {
            list.add(Messenger.m(null, "c Tick Counting Correct"));
        } else {
            list.add(Messenger.m(null,
                    "c Tick Counting FAILED!! " + "ticks = " + ticks + ", actualTicks = " + actualTicks));
        }
        //StringBuilder colorFullName = new StringBuilder(Messenger.color_by_enum(color)).append('b');
        StringBuilder colorFullName = new StringBuilder("w").append('b');
        if ("cactus".equalsIgnoreCase(name) || "all".equalsIgnoreCase(name)) {
            colorFullName.append('i');
        }
        colorFullName.append(' ').append(name);
        list.add(Messenger.c("w Counter ", colorFullName,
                "w  for ", String.format("wb %.2f", ticks * 1.0 / (20 * 60)), "w  min "
                        + (realTime ? "(real time)" : "(in game)")
        ));
        list.add(Messenger.c("w Total: " + total + ", Average: ",
                String.format("wb %.1f", total * 1.0 * (20 * 60 * 60) / ticks), "w /h ",
                "nb [X]", "^g reset", "!/counter " + name + " reset"
        ));
        list.addAll(linearPartials.entrySet().stream().map(e -> {
            String itemName = e.getKey().getDisplayName();
            long count = e.getValue();
            return Messenger.s(null, String.format(" - %s: %d, %.1f/h",
                    itemName, count, count * (20.0 * 60.0 * 60.0) / ticks));
        }).collect(Collectors.toList()));
        return list;
    }

    public List<ITextComponent> formatReliable(boolean brief) {
        StatsRecord stats = get_reliable_average(actualTicks, linearTotal, squaredTotal);
        double minutes;
        if (brief) {
            minutes = Math.rint(actualTicks / 120.0) / 10.0;
            return Collections.singletonList(Messenger.m(null,
                    String.format("c %s: %d, %s/h, %.1f min ", name, linearTotal, stats.getRoundedCombined(), minutes)));
        }
        List<ITextComponent> list = new ArrayList<>();
        StringBuilder colorFullName = new StringBuilder("w").append('b');
        if ("cactus".equalsIgnoreCase(name) || "all".equalsIgnoreCase(name)) {
            colorFullName.append('i');
        }
        colorFullName.append(' ').append(name);
        boolean realTime = false;
        minutes = Math.rint(actualTicks / 12.0) / 100.0;
        list.add(Messenger.c("w Counter ", colorFullName,
                "w  for ", String.format("wb %.2f", minutes), "w  min (in game)"
        ));
        list.add(Messenger.c("w Total: " + linearTotal + ", Average: ",
                String.format("wb %s", stats.getRoundedAverage()), "w /h (SE: "
                        + String.format("%s", stats.getRoundedError()) + "/h) ",
                "nb [X]", "^g reset", "!/counter " + name + " reset"
        ));
        list.addAll(linearPartials.entrySet().stream().map(e -> {
            ItemWithMeta item = e.getKey();
            String itemName = item.getDisplayName();
            long count = e.getValue();
            StatsRecord statsPartial = get_reliable_average(actualTicks, count, squaredPartials.getLong(item));
            return Messenger.s(null, String.format(" - %s: %d, %s/h (SE: %s/h)",
                    itemName, count,
                    statsPartial.getRoundedAverage(),
                    statsPartial.getRoundedError()
            ));
        }).collect(Collectors.toList()));
        return list;
    }

    public long getTotalItems() {
        return linearPartials.values().stream().mapToLong(Long::longValue).sum();
    }

    private static class StatsRecord {
        private static final SortedMap<Integer, String> PREFIXES_MAP;
        private static final int I_AVG = 0;
        private static final int I_ERR = I_AVG + 1;

        static {
            PREFIXES_MAP = new TreeMap<>();
            PREFIXES_MAP.put(0, "");
            PREFIXES_MAP.put(3, "k");
            PREFIXES_MAP.put(6, "M");
            PREFIXES_MAP.put(9, "G");
            PREFIXES_MAP.put(12, "T");
        }

        public final double average;
        public final double error;
        private boolean flagRounded = false;
        /**
         * Significant Figures
         */
        private int[] firstDigitIndices = null;
        /**
         * Rounds {@code average} according to {@code error}
         */
        private String roundedAverage = null;
        /**
         * Rounds {@code error} with 1 or 2 significant figures
         */
        private String roundedError = null;
        private String roundedCombined = null;

        private StatsRecord(double average, double error) {
            this.average = average;
            this.error = error;
        }

        /**
         * Rounds {@code average} according to {@code error}
         * <p>
         * Example: 187236.9(766.2) = 187.2(0.8)k
         * <p>
         * {@code error = 766.2}, and {@code floor(log10(766.2)) = 2} is the {@code firstDigitIndex} of {@code error}
         * <p>
         * {@code roundUp(2, 3) = 3}, using prefix "k"
         */
        public String getRoundedAverage() {
            if (!flagRounded) {
                setRoundedResults();
            }
            return roundedAverage;
        }

        public String getRoundedError() {
            if (!flagRounded) {
                setRoundedResults();
            }
            return roundedError;
        }

        /**
         * format: {@code <avg>\(<err>\)<unit>}
         * <p>
         * Example: 187.2(0.8)k
         */
        public String getRoundedCombined() {
            if (!flagRounded) {
                setRoundedResults();
            }
            return roundedCombined;
        }

        private void setRoundedResults() {
            if (firstDigitIndices == null) {
                setFirstDigitIndices();
            }
            int minimal = firstDigitIndices[I_ERR];
            int level = MathHelper.roundUp(minimal, 3);
            String prefix = PREFIXES_MAP.get(level);
            if (prefix == null) {
                level = MathHelper.clamp(level, PREFIXES_MAP.firstKey(), PREFIXES_MAP.lastKey());
            }
            int postfix = level - minimal;
            String format = "%." + postfix + 'f' + prefix;
            double rate = simplePow(10, level);
            double avgToDisplay = round_half_even(average / rate, postfix);
            roundedAverage = String.format(format, avgToDisplay);
            double errToDisplay = round_half_even(error / rate, postfix);
            roundedError = String.format(format, errToDisplay);
            format = "%." + postfix + "f(%." + postfix + "f)" + prefix;
            roundedCombined = String.format(format, avgToDisplay, errToDisplay);
            flagRounded = true;
        }

        private void setFirstDigitIndices() {
            firstDigitIndices = new int[I_ERR + 1];
            firstDigitIndices[I_AVG] = MathHelper.floor(Math.log10(average));
            firstDigitIndices[I_ERR] = MathHelper.floor(Math.log10(error));
        }
    }
}
