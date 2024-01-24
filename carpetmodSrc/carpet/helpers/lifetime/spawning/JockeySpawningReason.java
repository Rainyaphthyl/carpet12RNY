package carpet.helpers.lifetime.spawning;

import carpet.helpers.lifetime.utils.LifeTimeTrackerUtil;
import carpet.utils.Messenger;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.ITextComponent;

public class JockeySpawningReason extends SpawningReason {
    private final Class<? extends Entity> partnerClass;
    private final JockeyType type;

    public JockeySpawningReason(Entity partner, JockeyType type) {
        partnerClass = partner.getClass();
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JockeySpawningReason)) return false;
        JockeySpawningReason that = (JockeySpawningReason) o;
        if (!partnerClass.equals(that.partnerClass)) return false;
        return type == that.type;
    }

    @Override
    public int hashCode() {
        return 31 * partnerClass.hashCode() + (type != null ? type.hashCode() : 0);
    }

    @Override
    public ITextComponent toText() {
        switch (type) {
            case CHICKEN:
                return Messenger.c("w Chicken jockey",
                        "g  (with " + LifeTimeTrackerUtil.getEntityTypeDescriptor(partnerClass) + ")");
            case SPIDER:
                return Messenger.c("w Spider jockey");
            default:
                return Messenger.c("w Unknown jockey",
                        "g  (with " + LifeTimeTrackerUtil.getEntityTypeDescriptor(partnerClass) + ")");
        }
    }

    public enum JockeyType {
        CHICKEN,
        SPIDER
    }
}
