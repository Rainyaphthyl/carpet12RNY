package carpet.helpers;

import carpet.CarpetServer;
import carpet.pubsub.PubSubInfoProvider;
import carpet.utils.Messenger;
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
    //private final Object2LongMap<ItemWithMeta> squaredPartials = new Object2LongLinkedOpenHashMap<>();
    private final Object2LongMap<ItemWithMeta> currentPartials = new Object2LongLinkedOpenHashMap<>();
    private final PubSubInfoProvider<Long> pubSubProvider;
    private final String name;
    private long startTick = 0;
    private long startMillis = 0;
    private long linearTotal = 0;
    /**
     * used for debug
     */
    private long actualTicks = 0;
    //private long squaredTotal = 0;

    private HopperCounter(EnumDyeColor color, String name) {
        this.name = name;
        this.color = color;
        pubSubProvider = new PubSubInfoProvider<>(CarpetServer.PUBSUB, "carpet.counter." + name, 0, this::getTotalItems);
    }

    public static void resetAll() {
        for (HopperCounter counter : COUNTERS.values()) {
            counter.reset();
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

    public void update() {
        if (startTick > 0) {
            long totalInc = 0;
            for (ItemWithMeta item : currentPartials.keySet()) {
                long partialInc = currentPartials.getLong(item);
                linearPartials.put(item, linearPartials.getLong(item) + partialInc);
                totalInc += partialInc;
                currentPartials.put(item, 0);
            }
            linearTotal += totalInc;
            if (currSyncTick % 900 == 0) {
                currentPartials.clear();
            }
            ++actualTicks;
        }
    }

    public void add(ItemStack stack) {
        if (startTick == 0) {
            startTick = currSyncTick;
            startMillis = MinecraftServer.getCurrentTimeMillis();
        }
        ItemWithMeta item = new ItemWithMeta(stack);
        long stackCount = stack.getCount();
        currentPartials.put(item, currentPartials.getLong(item) + stackCount);
        pubSubProvider.publish();
    }

    public void reset() {
        linearPartials.clear();
        currentPartials.clear();
        linearTotal = 0;
        startTick = currSyncTick;
        actualTicks = 0;
        startMillis = MinecraftServer.getCurrentTimeMillis();
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
        if (brief) {
            return Collections.singletonList(Messenger.m(null,
                    String.format("c %s: %d, %d/h, %.1f min ",
                            name, total, total * (20 * 60 * 60) / ticks, ticks / (20.0 * 60.0))));
        }
        List<ITextComponent> list = new ArrayList<>();
        if (ticks == actualTicks) {
            list.add(Messenger.s(null, "Tick Counting Correct"));
        } else {
            list.add(Messenger.s(null,
                    "Tick Counting FAILED!! " + "ticks = " + ticks + ", actualTicks = " + actualTicks));
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
                    itemName,
                    count,
                    count * (20.0 * 60.0 * 60.0) / ticks));
        }).collect(Collectors.toList()));
        return list;
    }

    public long getTotalItems() {
        return linearPartials.values().stream().mapToLong(Long::longValue).sum();
    }
}
