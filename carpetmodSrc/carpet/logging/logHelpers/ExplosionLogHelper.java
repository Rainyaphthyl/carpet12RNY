package carpet.logging.logHelpers;

import carpet.CarpetServer;
import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ExplosionLogHelper {

    // CARPET-SYLKOS
    // Some code yeeted from lntricarpet and gnembon 1.16+ fabric carpet

    // carpet12RNY
    // Modified by Naftalluvia

    private static long lastGametime = 0;
    private static long explosionCountInCurrentGT = 0;
    private static boolean newTick = false;
    private final Vec3d pos;
    private final float power;
    private final boolean createFire;
    private final Object2IntMap<EntityImpact> impactedEntities = new Object2IntOpenHashMap<>();
    //TODO: 2023/5/3,0003 To log the blocks with dropping rates
    private boolean affectBlocks = false;

    public ExplosionLogHelper(double x, double y, double z, float power, boolean createFire) {
        this.pos = new Vec3d(x, y, z);
        this.power = power;
        this.createFire = createFire;
    }

    public void setAffectBlocks(boolean affectBlocks) {
        this.affectBlocks = affectBlocks;
    }

    public void onExplosionDone(long gameTime) {
        newTick = false;
        if (lastGametime != gameTime) {
            explosionCountInCurrentGT = 0;
            lastGametime = gameTime;
            newTick = true;
        }
        ++explosionCountInCurrentGT;
        LoggerRegistry.getLogger("explosions").log((option) -> {
            List<ITextComponent> messages = new ArrayList<>();
            if (newTick) {
                messages.add(Messenger.c("wb tick : ", "d " + gameTime));
            }
            switch (option) {
                case "brief":
                    messages.add(Messenger.c("d #" + explosionCountInCurrentGT, "gb ->",
                            Messenger.dblt("l", pos.x, pos.y, pos.z),
                            (affectBlocks ? "m (affects blocks)" : "m  (doesn't affect blocks)")
                    ));
                    break;
                case "compact":
                case "full":
                    try {
                        messages.add(Messenger.c("d #" + explosionCountInCurrentGT, "gb ->",
                                Messenger.dblt("l", pos.x, pos.y, pos.z)));
                        messages.add(Messenger.c("w   affects blocks: ", "m " + affectBlocks));
                        messages.add(Messenger.c("w   creates fire: ", "m " + createFire));
                        messages.add(Messenger.c("w   power: ", "c " + power));
                        if (impactedEntities.isEmpty()) {
                            messages.add(Messenger.c("w   affected entities: ", "m None"));
                        } else {
                            messages.add(Messenger.c("w   affected entities:"));
                            impactedEntities.forEach((k, v) -> {
                                StringBuilder nameBuilder = new StringBuilder();
                                Entity entity = k.entity;
                                if (entity.hasCustomName() || entity instanceof EntityPlayerMP) {
                                    nameBuilder.append("c ");
                                } else if (entity instanceof EntityItem) {
                                    nameBuilder.append("r ");
                                } else {
                                    nameBuilder.append("w ");
                                }
                                nameBuilder.append(' ').append(entity.getName());
                                messages.add(Messenger.c(
                                        k.pos.equals(pos) ? "r   - TNT" : "w   - ",
                                        Messenger.dblt(k.pos.equals(pos) ? "r" : "y", k.pos.x, k.pos.y, k.pos.z),
                                        "w  dV",
                                        Messenger.dblt("d", k.accel.x, k.accel.y, k.accel.z),
                                        nameBuilder.toString(),
                                        "l " + (v > 1 ? ("(" + v + ")") : "")));
                            });
                        }

                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                        Messenger.print_server_message(CarpetServer.minecraft_server,
                                "The server should have crashed!");
                    }
                    break;
            }
            return messages.toArray(new ITextComponent[0]);
        });
    }

    public void onEntityImpacted(Entity entity, Vec3d accel) {
        EntityImpact impact = new EntityImpact(entity, accel);
        impactedEntities.put(impact, impactedEntities.getOrDefault(impact, 0) + 1);
    }

    public static final class EntityImpact {
        Entity entity;
        Vec3d pos;
        Vec3d accel;

        public EntityImpact(@Nonnull Entity entity, Vec3d accel) {
            this.entity = entity;
            pos = entity.getPositionVector();
            this.accel = accel;
        }
    }
}
