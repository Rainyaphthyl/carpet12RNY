package carpet.logging.logHelpers;

import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import it.unimi.dsi.fastutil.floats.Float2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.floats.Float2ObjectSortedMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
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
    private final Object2IntMap<ImpactOnEntity> impactedEntities;
    private final Float2ObjectSortedMap<Object2ObjectMap<Block, List<BlockPos>>> blockDroppingChances;
    private boolean affectBlocks = false;

    {
        impactedEntities = new Object2IntOpenHashMap<>();
        impactedEntities.defaultReturnValue(0);
        blockDroppingChances = new Float2ObjectAVLTreeMap<>();
        blockDroppingChances.defaultReturnValue(null);
    }

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
                            (affectBlocks ? "m (affects blocks)" : "m (doesn't affect blocks)")));
                    break;
                case "compact":
                case "full":
                    messages.add(Messenger.c("d #" + explosionCountInCurrentGT, "gb ->",
                            Messenger.dblt("l", pos.x, pos.y, pos.z)));
                    messages.add(Messenger.c("w   affects blocks: ", "m " + affectBlocks));
                    messages.add(Messenger.c("w   creates fire: ", "m " + createFire));
                    messages.add(Messenger.c("w   power: ", "c " + power));
                    // blocks
                    if (blockDroppingChances.isEmpty()) {
                        messages.add(Messenger.c("w   affected blocks: ", "m None"));
                    } else {
                        messages.add(Messenger.c("w   affected blocks:"));
                        blockDroppingChances.float2ObjectEntrySet().forEach(object2ObjectMapEntry -> {
                            float chance = object2ObjectMapEntry.getFloatKey();
                            Object2ObjectMap<Block, List<BlockPos>> blockPosMap = object2ObjectMapEntry.getValue();
                            double percent = chance * 100.0;
                            blockPosMap.forEach((block, posList) -> {
                                StringBuilder titleBuilder = new StringBuilder();
                                String style;
                                if (block == Blocks.TNT) {
                                    style = "y";
                                } else if (chance == 1.0F) {
                                    style = "l";
                                } else {
                                    style = "r";
                                }
                                String name = Block.REGISTRY.getNameForObject(block).getPath();
                                titleBuilder.append(style).append("   - ").append(name);
                                String title = titleBuilder.toString();
                                posList.forEach(pos -> messages.add(Messenger.c(title,
                                        String.format("%s  [ %d, %d, %d ] %.1f%%", style,
                                                pos.getX(), pos.getY(), pos.getZ(), percent))));
                            });
                        });
                    }
                    // entities
                    if (impactedEntities.isEmpty()) {
                        messages.add(Messenger.c("w   affected entities: ", "m None"));
                    } else {
                        messages.add(Messenger.c("w   affected entities:"));
                        impactedEntities.forEach((k, v) -> {
                            StringBuilder nameBuilder = new StringBuilder();
                            Entity entity = k.entity;
                            boolean showingDamage = false;
                            if (entity instanceof EntityItem) {
                                nameBuilder.append("r ");
                                showingDamage = true;
                            } else if (entity.hasCustomName() || entity instanceof EntityPlayerMP) {
                                nameBuilder.append("c ");
                            } else {
                                nameBuilder.append("l ");
                            }
                            if (!showingDamage && !(entity instanceof EntityThrowable) && k.accel.length() == 0.0) {
                                showingDamage = true;
                            }
                            nameBuilder.append(entity.getName());
                            String title = k.pos.equals(pos) ? "r   - TNT" : "w   - ";
                            String posStyle = k.pos.equals(pos) ? "r" : "y";
                            if (showingDamage) {
                                messages.add(Messenger.c(title, nameBuilder.toString(), "w  ",
                                        Messenger.dblt(posStyle, k.pos.x, k.pos.y, k.pos.z),
                                        "w  damage: ", "r " + k.damage,
                                        "l " + (v > 1 ? ("(" + v + ")") : "")));
                            } else {
                                messages.add(Messenger.c(title, nameBuilder.toString(), "w  ",
                                        Messenger.dblt(posStyle, k.pos.x, k.pos.y, k.pos.z),
                                        "w  +", Messenger.dblt("d", k.accel.x, k.accel.y, k.accel.z),
                                        "l " + (v > 1 ? ("(" + v + ")") : "")));
                            }
                        });
                    }
                    break;
            }
            return messages.toArray(new ITextComponent[0]);
        });
    }

    public void onEntityImpacted(@Nonnull Entity entity, Vec3d accel, float damage) {
        ImpactOnEntity impactOnEntity = new ImpactOnEntity(entity, accel, damage);
        impactedEntities.put(impactOnEntity, impactedEntities.getInt(impactOnEntity) + 1);
    }

    /**
     * @param chance set to {@code -1} for TNT
     */
    public void onBlockDestroyed(BlockPos pos, Block block, float chance) {
        Object2ObjectMap<Block, List<BlockPos>> blockClasses = blockDroppingChances.get(chance);
        if (blockClasses == null) {
            blockClasses = new Object2ObjectOpenHashMap<>();
            blockClasses.defaultReturnValue(null);
            blockDroppingChances.put(chance, blockClasses);
        }
        List<BlockPos> blockPositions = blockClasses.get(block);
        if (blockPositions == null) {
            blockPositions = new ArrayList<>();
            blockClasses.put(block, blockPositions);
        }
        blockPositions.add(pos);
    }

    public static final class ImpactOnEntity {
        public final Entity entity;
        public final Vec3d pos;
        public final Vec3d accel;
        public final float damage;

        public ImpactOnEntity(@Nonnull Entity entity, Vec3d accel, float damage) {
            this.entity = entity;
            pos = entity.getPositionVector();
            this.accel = accel;
            this.damage = damage;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof ImpactOnEntity) {
                return entity == ((ImpactOnEntity) obj).entity;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return pos.hashCode() ^ accel.hashCode();
        }
    }
}
