package carpet.helpers;

import carpet.CarpetServer;
import carpet.pubsub.PubSubInfoProvider;
import carpet.utils.Messenger;
import carpet.utils.StatsBundle;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
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
    public static StatsBundle get_reliable_average(long ticks, long linearTotal, long squaredTotal) {
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
        return new StatsBundle(average, error);
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
        //if (ticks == actualTicks) {
        //    list.add(Messenger.m(null, "c Tick Counting Correct"));
        //} else {
        //    list.add(Messenger.m(null,
        //            "c Tick Counting FAILED!! " + "ticks = " + ticks + ", actualTicks = " + actualTicks));
        //}
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
        StatsBundle stats = get_reliable_average(actualTicks, linearTotal, squaredTotal);
        double percent = 100.0 * stats.error / stats.average;
        String color = Messenger.stats_error_color(percent, true);
        StatsBundle.RoundedStatsBundle rounded = stats.getRoundedBundle();
        double minutes;
        if (brief) {
            minutes = Math.rint(actualTicks / 120.0) / 10.0;
            return Collections.singletonList(Messenger.m(null,
                    String.format("%s %s: %d, %s(%s)%s/h, %.1f min", Messenger.stats_error_color(percent, true),
                            name, linearTotal, rounded.average, rounded.error, rounded.unit, minutes)));
        }
        List<ITextComponent> list = new ArrayList<>();
        StringBuilder colorFullName = new StringBuilder("w").append('b');
        if ("cactus".equalsIgnoreCase(name) || "all".equalsIgnoreCase(name)) {
            colorFullName.append('i');
        }
        colorFullName.append(' ').append(name);
        boolean realTime = false;
        minutes = actualTicks / 1200.0;
        list.add(Messenger.c("w Counter ", colorFullName,
                "w  for ", String.format("wb %.2f", minutes), "w  min (in game) ",
                "nb [X]", "^g reset", "!/counter " + name + " reset"));
        list.add(Messenger.c("w Total: " + linearTotal + ", Average: ",
                "wb " + rounded.average, "w (" + rounded.error + ')', "wb " + rounded.unit, "w /h, E: ",
                color + ' ' + StatsBundle.round_to_sig_figs(percent, 3) + '%'));
        boolean flagColor = false;
        List<Integer> indexList = new IntArrayList();
        List<ItemWithMeta> itemList = new ArrayList<>(linearPartials.keySet());
        List<StatsBundle> statsList = linearPartials.object2LongEntrySet().stream().map(e -> {
            indexList.add(indexList.size());
            return get_reliable_average(actualTicks, e.getLongValue(), squaredPartials.getLong(e.getKey()));
        }).collect(Collectors.toList());
        List<Double> percentList = statsList.stream().map(e -> 100.0 * e.error / e.average)
                .collect(Collectors.toList());
        indexList.sort(Comparator.comparing(percentList::get));
        for (int i : indexList) {
            ItemWithMeta item = itemList.get(i);
            long itemCount = linearPartials.getLong(item);
            String itemName = item.getDisplayName();
            String itemID = item.getDisplayID();
            stats = statsList.get(i);
            rounded = stats.getRoundedBundle();
            percent = percentList.get(i);
            String percentDisplay = StatsBundle.round_to_sig_figs(percent, 3);
            color = Messenger.stats_error_color(percent, true);
            String colorCyan = flagColor ? "c" : "q";
            String colorWhite = flagColor ? "w" : "g";
            flagColor = !flagColor;
            list.add(Messenger.m(null, String.format("%s - %s, ", colorWhite, itemID),
                    String.format("%s %s", colorWhite, itemName), String.format("%s : %d, ", colorWhite, itemCount),
                    String.format("%sb %s", colorCyan, rounded.average),
                    String.format("%s (%s)", colorCyan, rounded.error),
                    String.format("%sb %s", colorCyan, rounded.unit), String.format("%s /h", colorCyan),
                    String.format("%s , E: ", colorWhite), String.format("%s %s%%", color, percentDisplay)));
        }
        return list;
    }

    public long getTotalItems() {
        return linearPartials.values().stream().mapToLong(Long::longValue).sum();
    }

}
