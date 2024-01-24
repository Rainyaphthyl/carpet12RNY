package carpet.helpers.lifetime.trackeddata;

import carpet.helpers.lifetime.removal.RemovalReason;
import carpet.helpers.lifetime.spawning.SpawningReason;
import carpet.helpers.lifetime.utils.*;
import carpet.utils.Messenger;
import com.google.common.collect.Lists;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;
import java.util.*;

public class MultiSizeTrackedData extends BasicTrackedData {
    private final Map<MobSize, BasicTrackedData> sizedMultiTracker = new EnumMap<>(MobSize.class);

    /**
     * {@code - AAA: 50, (100/h) 25% / 12%}
     *
     * @param reason spawning reason or removal reason
     * @param parent count at the previous level
     * @param root   count at the top level
     */
    @Nonnull
    public static ITextComponent getReasonWithDualRate(@Nonnull AbstractReason reason, long ticks, long count, long parent, long root) {
        double cent = 100.0 * count;
        double percentA = cent / parent;
        double percentB = cent / root;
        return Messenger.c(
                "g - ",
                reason.toText(),
                "g : ",
                CounterUtil.ratePerHourText(count, ticks, "wgg"),
                "w  ",
                TextUtil.attachHoverText(Messenger.s(null, String.format("%.1f%%", percentB)), Messenger.s(null, String.format("%.6f%%", percentB))),
                "g /",
                "w ",
                TextUtil.attachHoverText(Messenger.s(null, String.format("%.1f%%", percentA)), Messenger.s(null, String.format("%.6f%%", percentA)))
        );
    }

    @Override
    public void updateSpawning(Entity entity, SpawningReason reason) {
        BasicTrackedData subTrack = getSubTrack(entity);
        subTrack.updateSpawning(entity, reason);
    }

    @Override
    public void updateRemoval(Entity entity, RemovalReason reason) {
        BasicTrackedData subTrack = getSubTrack(entity);
        subTrack.updateRemoval(entity, reason);
        lifeTimeStatistic.update(entity);
    }

    @Override
    public long getSpawningCount() {
        return sizedMultiTracker.values().stream().mapToLong(BasicTrackedData::getSpawningCount).sum();
    }

    @Override
    public long getRemovalCount() {
        return sizedMultiTracker.values().stream().mapToLong(BasicTrackedData::getRemovalCount).sum();
    }

    @Override
    public List<ITextComponent> getSpawningReasonsTexts(long ticks, boolean hoverMode) {
        List<ITextComponent> result = Lists.newArrayList();
        // Title for hover mode
        long rootCount = getSpawningCount();
        if (hoverMode && rootCount > 0) {
            result.add(Messenger.s(null, "Reasons for spawning", "e"));
        }
        sizedMultiTracker.forEach((mobSize, subTrack) -> {
            List<Map.Entry<SpawningReason, Long>> entryList = Lists.newArrayList(subTrack.spawningReasons.entrySet());
            if (!entryList.isEmpty()) {
                entryList.sort(Collections.reverseOrder(Comparator.comparingLong(Map.Entry::getValue)));
                // Title for mob sizes / ages
                if (hoverMode) {
                    result.add(Messenger.s(null, "\n"));
                }
                result.add(Messenger.c("e Size: ", "eb " + mobSize.toString(), "g :"));
                entryList.forEach(entry -> {
                    SpawningReason reason = entry.getKey();
                    Long statistic = entry.getValue();
                    // added to upper result which will be sent by Messenger.send
                    // so each element will be in a separate line
                    if (hoverMode) {
                        result.add(Messenger.s(null, "\n"));
                    }
                    result.add(getReasonWithDualRate(reason, ticks, statistic, subTrack.getSpawningCount(), rootCount));
                });
            }
        });
        return result;
    }

    @Override
    public List<ITextComponent> getRemovalReasonsTexts(long ticks, boolean hoverMode) {
        List<ITextComponent> result = Lists.newArrayList();
        // Title for hover mode
        if (hoverMode && getRemovalCount() > 0) {
            result.add(Messenger.s(null, "Reasons for removal", "r"));
        }
        sizedMultiTracker.forEach((mobSize, subTrack) -> {
            List<Map.Entry<RemovalReason, LifeTimeStatistic>> entryList = Lists.newArrayList(subTrack.removalReasons.entrySet());
            if (!entryList.isEmpty()) {
                entryList.sort(Collections.reverseOrder(Comparator.comparingLong(a -> a.getValue().count)));
                // Title for mob sizes / ages
                if (hoverMode) {
                    result.add(Messenger.s(null, "\n"));
                }
                result.add(Messenger.c("r Size: ", "rb " + mobSize.toString(), "g :"));
                entryList.forEach(entry -> {
                    RemovalReason reason = entry.getKey();
                    LifeTimeStatistic statistic = entry.getValue();
                    // added to upper result which will be sent by Messenger.send
                    // so each element will be in a separate line
                    if (hoverMode) {
                        result.add(Messenger.s(null, "\n"));
                    }
                    result.add(Messenger.c(
                            getReasonWithDualRate(
                                    reason, ticks, statistic.count,
                                    subTrack.getRemovalCount(), lifeTimeStatistic.count
                            ),
                            "w \n",
                            statistic.getResult("  ", hoverMode)
                    ));
                });
            }
        });
        return result;
    }

    @Nonnull
    private BasicTrackedData getSubTrack(Entity entity) {
        MobSize size = null;
        if (entity instanceof EntityZombie) {
            size = ((EntityZombie) entity).isChild() ? MobSize.BABY : MobSize.ADULT;
        } else if (entity instanceof EntitySlime) {
            switch (((EntitySlime) entity).getSlimeSize()) {
                case 1:
                    size = MobSize.SMALL;
                    break;
                case 2:
                    size = MobSize.MEDIUM;
                    break;
                case 4:
                    size = MobSize.LARGE;
            }
        }
        return sizedMultiTracker.computeIfAbsent(size, s -> new BasicTrackedData());
    }
}
