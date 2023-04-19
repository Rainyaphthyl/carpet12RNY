package carpet.logging.logHelpers;

import carpet.helpers.lifetime.utils.TextUtil;
import carpet.logging.Logger;
import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

public class ItemLogHelper {
    private boolean doLog;
    private Logger logger;

    private ArrayList<Vec3d> positions = new ArrayList<>();
    private ArrayList<Vec3d> motions = new ArrayList<>();
    private int sentLogs;

    public ItemLogHelper(String logName) {
        this.logger = LoggerRegistry.getLogger(logName);
        this.doLog = this.logger.hasSubscribers();
        sentLogs = 0;
    }

    @Nonnull
    @ParametersAreNonnullByDefault
    private static ITextComponent getDetailedRecord(Vec3d pos, Vec3d mot, int tick) {
        return Messenger.m(null,
                String.format("w tick: %d pos", tick),
                Messenger.dblt("w", pos.x, pos.y, pos.z),
                "w   mot", Messenger.dblt("w", mot.x, mot.y, mot.z),
                Messenger.m(null, "w  [tp]", "/tp " + pos.x + " " + pos.y + " " + pos.z));
    }

    @Nonnull
    @ParametersAreNonnullByDefault
    private static ITextComponent getRepeatedReport(String itemName, String idMetaText, int itemCount, int logCount) {
        return Messenger.m(null,
                String.format("q %s (%s) *%d ", itemName, idMetaText, itemCount),
                String.format("g ... %d repeated loggings ...", logCount));
    }

    public void onTick(double x, double y, double z, double motionX, double motionY, double motionZ) {
        if (!doLog) return;
        positions.add(new Vec3d(x, y, z));
        motions.add(new Vec3d(motionX, motionY, motionZ));
    }

    public void onFinish(String type, EntityItem entityIn) {
        if (!doLog) return;
        sentLogs = 0;
        sendUpdateLogs(true, type, entityIn);
        doLog = false;
    }

    private void sendUpdateLogs(boolean finished, String type, EntityItem entityIn) {
        logger.logNoCommand((option) -> {
            List<ITextComponent> comp = new ArrayList<>();
            int age = entityIn.getAge();
            ItemStack stack = entityIn.getItem();
            String name = stack.getDisplayName();
            int count = stack.getCount();
            String idMetaText;
            int id = Item.getIdFromItem(stack.getItem());
            if (stack.getItem().getHasSubtypes()) {
                int meta = stack.getMetadata();
                idMetaText = String.format("#%04d/%d", id, meta);
            } else {
                idMetaText = String.format("#%04d", id);
            }
            switch (option) {
                case "brief":
                    Vec3d p = new Vec3d(0, 0, 0);
                    if (positions.size() > 0) {
                        p = positions.get(positions.size() - 1);
                    } else {
                        p = entityIn.getPositionVector();
                    }
                    comp.add(Messenger.m(null,
                            String.format("w --%s-- t: %d  pos: ", type, age),
                            Messenger.dblt("w", p.x, p.y, p.z), "w  ",
                            TextUtil.getFancyText("q",
                                    Messenger.s(null, idMetaText),
                                    Messenger.s(null, String.format("%s (%s)", name, idMetaText)),
                                    null),
                            String.format("w  *%d", count)));
                    break;
                case "full":
                    comp.add(Messenger.m(null,
                            String.format("w ----%s---- t: %d ", type, age),
                            String.format("q %s (%s)", name, idMetaText),
                            String.format("w  *%d", count)));
                    Vec3d prevPos = null, prevMot = null;
                    int repeatCount = 1;
                    if (positions.size() <= 0) {
                        Vec3d pos = entityIn.getPositionVector();
                        Vec3d mot = new Vec3d(entityIn.motionX, entityIn.motionY, entityIn.motionZ);
                        comp.add(getDetailedRecord(pos, mot, 0));
                    }
                    for (int i = sentLogs, maxIndex = positions.size() - 1; i <= maxIndex; i++) {
                        sentLogs++;
                        Vec3d pos = positions.get(i);
                        Vec3d mot = motions.get(i);
                        if (prevPos != null && prevPos.equals(pos)) {
                            ++repeatCount;
                            // merge repeated loggings
                            if (i == maxIndex) {
                                if (repeatCount > 1) {
                                    if (repeatCount > 2) {
                                        comp.add(getRepeatedReport(name, idMetaText, count, repeatCount));
                                    }
                                    comp.add(getDetailedRecord(prevPos, prevMot, i + 1));
                                    repeatCount = 1;
                                }
                            }
                        } else {
                            if (repeatCount > 1) {
                                if (repeatCount > 2) {
                                    comp.add(getRepeatedReport(name, idMetaText, count, repeatCount));
                                }
                                comp.add(getDetailedRecord(prevPos, prevMot, i));
                                repeatCount = 1;
                            }
                            comp.add(getDetailedRecord(pos, mot, i + 1));
                        }
                        prevPos = pos;
                        prevMot = mot;
                    }
                    break;
            }
            return comp.toArray(new ITextComponent[0]);
        });
    }
}
