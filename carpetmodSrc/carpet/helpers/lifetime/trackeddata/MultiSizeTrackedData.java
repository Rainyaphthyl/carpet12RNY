package carpet.helpers.lifetime.trackeddata;

import carpet.helpers.lifetime.removal.RemovalReason;
import carpet.helpers.lifetime.spawning.SpawningReason;
import carpet.helpers.lifetime.utils.LifeTimeStatistic;
import carpet.helpers.lifetime.utils.MobSize;
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

    @Override
    public void updateSpawning(Entity entity, SpawningReason reason) {
        BasicTrackedData subTrack = getSubTrack(entity);
        subTrack.updateSpawning(entity, reason);
    }

    @Override
    public void updateRemoval(Entity entity, RemovalReason reason) {
        BasicTrackedData subTrack = getSubTrack(entity);
        subTrack.updateRemoval(entity, reason);
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
        if (hoverMode && getSpawningCount() > 0) {
            result.add(Messenger.s(null, "Reasons for spawning", "e"));
        }
        sizedMultiTracker.forEach((mobSize, subTrack) -> {
            List<Map.Entry<SpawningReason, Long>> entryList = Lists.newArrayList(subTrack.spawningReasons.entrySet());
            if (!entryList.isEmpty()) {
                entryList.sort(Collections.reverseOrder(Comparator.comparingLong(Map.Entry::getValue)));
                // Title for mob sizes / ages
                result.add(Messenger.c("d Size: ", "y " + mobSize.toString()));
                if (hoverMode) {
                    result.add(Messenger.s(null, "\n"));
                }
                entryList.forEach(entry -> {
                    SpawningReason reason = entry.getKey();
                    Long statistic = entry.getValue();
                    // added to upper result which will be sent by Messenger.send
                    // so each element will be in a separate line
                    if (hoverMode) {
                        result.add(Messenger.s(null, "\n"));
                    }
                    result.add(subTrack.getSpawningReasonWithRate(reason, ticks, statistic, subTrack.getSpawningCount()));
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
                result.add(Messenger.c("d Size: ", "y " + mobSize.toString()));
                if (hoverMode) {
                    result.add(Messenger.s(null, "\n"));
                }
                entryList.forEach(entry -> {
                    RemovalReason reason = entry.getKey();
                    LifeTimeStatistic statistic = entry.getValue();
                    // added to upper result which will be sent by Messenger.send
                    // so each element will be in a separate line
                    if (hoverMode) {
                        result.add(Messenger.s(null, "\n"));
                    }
                    result.add(Messenger.c(
                            subTrack.getRemovalReasonWithRate(reason, ticks, statistic.count, subTrack.lifeTimeStatistic.count),
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
                case 0:
                    size = MobSize.SMALL;
                    break;
                case 1:
                    size = MobSize.MEDIUM;
                    break;
                case 2:
                    size = MobSize.LARGE;
            }
        }
        return sizedMultiTracker.computeIfAbsent(size, s -> new BasicTrackedData());
    }
}
