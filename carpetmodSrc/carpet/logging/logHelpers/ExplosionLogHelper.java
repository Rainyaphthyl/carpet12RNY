package carpet.logging.logHelpers;

import carpet.helpers.ItemWithMeta;
import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final Object2ObjectMap<BlockPos, BlockWithDrop> destroyedBlocks;
    private boolean affectBlocks = false;

    {
        impactedEntities = new Object2IntLinkedOpenHashMap<>();
        impactedEntities.defaultReturnValue(0);
        destroyedBlocks = new Object2ObjectLinkedOpenHashMap<>();
        destroyedBlocks.defaultReturnValue(null);
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
                    if (destroyedBlocks.isEmpty()) {
                        messages.add(Messenger.c("w   affected blocks: ", "m None"));
                    } else {
                        messages.add(Messenger.c("w   affected " + destroyedBlocks.size() + " blocks:"));
                        destroyedBlocks.forEach((pos, dropping) -> {
                            boolean isTNT = dropping.iBlockState.getBlock() == Blocks.TNT;
                            boolean isMovingPiston = dropping.iBlockState.getBlock() == Blocks.PISTON_EXTENSION;
                            boolean isHarvested = dropping.chance == 1.0F;
                            String title = isTNT ? "r   - " : "w   - ";
                            String harvestStyle = isTNT ? "r" : (isMovingPiston ? "l" : "d");
                            String chanceStyle = isTNT ? "r" : (isHarvested ? "l" : "r");
                            Block block = dropping.iBlockState.getBlock();
                            int meta = block.getMetaFromState(dropping.iBlockState);
                            String name, idMetaStr;
                            Item item = Item.getItemFromBlock(block);
                            if (item == Items.AIR) {
                                name = Block.REGISTRY.getNameForObject(block).getPath() + ':' + meta;
                                idMetaStr = "c (" + ItemWithMeta.get_display_ID(Block.getIdFromBlock(block), meta) + ')';
                            } else {
                                ItemWithMeta itemWithMeta = new ItemWithMeta(item, meta);
                                name = itemWithMeta.getDisplayName();
                                idMetaStr = (isTNT ? "r (" : "q (") + itemWithMeta.getDisplayID() + ')';
                            }
                            String positionStr = (isTNT ? "r" : "y") +
                                    " [ " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " ]";
                            float percent = dropping.chance * 100;
                            messages.add(Messenger.c(title, idMetaStr,
                                    harvestStyle + "  " + name, "w  ", positionStr, "w  ",
                                    String.format("%s %.1f%%", chanceStyle, percent), "^w " + percent + '%'));
                        });
                    }
                    // entities
                    if (impactedEntities.isEmpty()) {
                        messages.add(Messenger.c("w   affected entities: ", "m None"));
                    } else {
                        AtomicInteger entityCount = new AtomicInteger();
                        impactedEntities.object2IntEntrySet().forEach(entry -> entityCount.addAndGet(entry.getIntValue()));
                        messages.add(Messenger.c("w   affected " + entityCount.get() + " entities:"));
                        impactedEntities.object2IntEntrySet().forEach(entry -> {
                            StringBuilder nameBuilder = new StringBuilder();
                            ImpactOnEntity impact = entry.getKey();
                            int count = entry.getIntValue();
                            Entity entity = impact.entity;
                            boolean showingDamage = false;
                            if (entity instanceof EntityItem) {
                                nameBuilder.append("r ");
                                showingDamage = true;
                            } else if (entity.hasCustomName() || entity instanceof EntityPlayerMP) {
                                nameBuilder.append("c ");
                            } else {
                                nameBuilder.append("l ");
                            }
                            if (!showingDamage && !(entity instanceof EntityThrowable) && impact.accel.length() == 0.0) {
                                showingDamage = true;
                            }
                            nameBuilder.append(entity.getName());
                            String title = impact.pos.equals(pos) ? "r   - " : "w   - ";
                            String posStyle = impact.pos.equals(pos) ? "r" : "y";
                            if (showingDamage) {
                                messages.add(Messenger.c(title, nameBuilder.toString(), "w  ",
                                        Messenger.dblt(posStyle, impact.pos.x, impact.pos.y, impact.pos.z),
                                        "w  damage: ", "r " + impact.damage,
                                        "l " + (count > 1 ? ("(" + count + ")") : "")));
                            } else {
                                messages.add(Messenger.c(title, nameBuilder.toString(), "w  ",
                                        Messenger.dblt(posStyle, impact.pos.x, impact.pos.y, impact.pos.z),
                                        "w  +", Messenger.dblt("d", impact.accel.x, impact.accel.y, impact.accel.z),
                                        "l " + (count > 1 ? ("(" + count + ")") : "")));
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
    public void onBlockDestroyed(BlockPos pos, IBlockState blockState, float chance) {
        BlockWithDrop blockWithDrop = new BlockWithDrop(blockState, chance);
        destroyedBlocks.put(pos, blockWithDrop);
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

    public static final class BlockWithDrop {
        public final IBlockState iBlockState;
        public final float chance;

        public BlockWithDrop(IBlockState iBlockState, float chance) {
            this.iBlockState = iBlockState;
            this.chance = chance;
        }
    }
}
