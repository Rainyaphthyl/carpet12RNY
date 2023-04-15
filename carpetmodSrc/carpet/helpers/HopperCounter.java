package carpet.helpers;

import carpet.CarpetServer;
import carpet.pubsub.PubSubInfoProvider;
import carpet.utils.Messenger;
import it.unimi.dsi.fastutil.objects.Object2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
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
    public static final HopperCounter cactus = new HopperCounter(EnumDyeColor.GREEN, "cactus");
    public static final HopperCounter all = new HopperCounter(EnumDyeColor.GRAY, "all");
    public static final Map<String, HopperCounter> COUNTERS;
    private static boolean flag_curr_update = false;
    private static long currSyncTick = 0;

    static {
        COUNTERS = new HashMap<>();
        for (EnumDyeColor color : EnumDyeColor.values()) {
            COUNTERS.put(color.getName(), new HopperCounter(color, color.getName()));
        }
        COUNTERS.put("cactus", cactus);
        COUNTERS.put("all", all);
    }

    public final EnumDyeColor color;
    private final Object2LongMap<ItemWithMeta> linearCounter = new Object2LongLinkedOpenHashMap<>();
    private final Object2LongMap<ItemWithMeta> squaredCounter = new Object2LongLinkedOpenHashMap<>();
    private final PubSubInfoProvider<Long> pubSubProvider;
    private final String name;
    private final Object2LongMap<ItemWithMeta> counterCurrTick = new Object2LongLinkedOpenHashMap<>();
    private long totalSquaredCount = 0;
    private long startTick = 0;
    private long startMillis = 0;
    private volatile boolean updating = false;

    private HopperCounter(EnumDyeColor color, String name) {
        this.name = name;
        this.color = color;
        pubSubProvider = new PubSubInfoProvider<>(CarpetServer.PUBSUB, "carpet.counter." + name, 0, this::getTotalItems);
    }

    public static void resetAll(MinecraftServer server) {
        flag_curr_update = false;
        for (HopperCounter counter : COUNTERS.values()) {
            counter.reset(server);
        }
    }

    @Nonnull
    public static List<ITextComponent> formatAll(MinecraftServer server, boolean realtime) {
        List<ITextComponent> text = new ArrayList<>();

        for (HopperCounter counter : COUNTERS.values()) {
            List<ITextComponent> temp = counter.format(server, realtime, false);
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

    public static void updateAll(long currTick) {
        currSyncTick = currTick;
        if (flag_curr_update) {
            flag_curr_update = false;
            for (HopperCounter counter : COUNTERS.values()) {
                counter.update();
            }
        }
    }

    private void update() {
        if (startTick > 0) {
            updating = true;
            long sum = 0;
            for (ItemWithMeta itemKey : counterCurrTick.keySet()) {
                long increment = counterCurrTick.getLong(itemKey);
                if (increment != 0) {
                    sum += increment;
                    linearCounter.put(itemKey, linearCounter.getLong(itemKey) + increment);
                    long squared = squaredCounter.getLong(itemKey);
                    if (squared >= 0) {
                        squaredCounter.put(itemKey, squared + increment * increment);
                    }
                    counterCurrTick.put(itemKey, 0);
                }
            }
            if (totalSquaredCount >= 0) {
                totalSquaredCount += sum * sum;
            }
            updating = false;
        }
    }

    public void add(MinecraftServer server, ItemStack stack) {
        if (startTick == 0) {
            startTick = server.getTickCounter();
            startMillis = MinecraftServer.getCurrentTimeMillis();
        }
        ItemWithMeta item = new ItemWithMeta(stack);
        flag_curr_update = true;
        counterCurrTick.put(item, linearCounter.getLong(item) + stack.getCount());
        pubSubProvider.publish();
    }

    public void reset(@Nonnull MinecraftServer server) {
        counterCurrTick.clear();
        linearCounter.clear();
        squaredCounter.clear();
        totalSquaredCount = 0;
        startTick = currSyncTick;
        startMillis = MinecraftServer.getCurrentTimeMillis();
        pubSubProvider.publish();
    }

    public List<ITextComponent> format(MinecraftServer server, boolean realTime, boolean brief) {
        if (linearCounter.isEmpty()) {
            if (brief) {
                return Collections.singletonList(Messenger.m(null, "g " + name + ": -, -/h, - min "));
            }
            return Collections.singletonList(Messenger.s(null, String.format("No items for %s yet", name)));
        }
        long total = getTotalItems();
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
        if (brief) {
            return Collections.singletonList(Messenger.m(null,
                    String.format("c %s: %d, %d/h, %.1f min ",
                            name, total, total * (20 * 60 * 60) / ticks, ticks / (20.0 * 60.0))));
        }
        List<ITextComponent> list = new ArrayList<>();
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
        list.addAll(linearCounter.entrySet().stream().map(e -> {
            String itemName = e.getKey().getDisplayName();
            long count = e.getValue();
            return Messenger.s(null, String.format(" - %s: %d, %.1f/h",
                    itemName,
                    count,
                    count * (20.0 * 60.0 * 60.0) / ticks));
        }).collect(Collectors.toList()));
        return list;
    }

    public long getTotalItems() {
        return linearCounter.values().stream().mapToLong(Long::longValue).sum();
    }

    private StatBundle getStatisticBundle() {
        long ticks = currSyncTick - startTick;
        return null;
    }

    private static class StatBundle {
        public final Object2DoubleMap<ItemWithMeta> averagesPartial = new Object2DoubleLinkedOpenHashMap<>();
        public final Object2DoubleMap<ItemWithMeta> variancesPartial = new Object2DoubleLinkedOpenHashMap<>();
        public final Object2DoubleMap<ItemWithMeta> deviationsPartial = new Object2DoubleLinkedOpenHashMap<>();
        public final Object2DoubleMap<ItemWithMeta> errorsPartial = new Object2DoubleLinkedOpenHashMap<>();
        public double average = 0.0;
        public double variance = 0.0;
        public double deviation = 0.0;
        public double error = 0.0;
    }
}
