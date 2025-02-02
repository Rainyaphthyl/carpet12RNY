package carpet.helpers;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class ItemWithMeta {
    public final Item item;
    public final int metadata;

    public ItemWithMeta(ItemStack stack) {
        this(stack.getItem(), stack.getMetadata());
    }

    public ItemWithMeta(Item item, int metadata) {
        this.item = item;
        this.metadata = item.getHasSubtypes() ? metadata : 0;
    }

    @Nonnull
    public static String get_display_ID(int id, int meta) {
        return '#' + String.format("%04d", id) + '/' + meta;
    }

    public String getDisplayName() {
        return new ItemStack(item, 1, metadata).getDisplayName();
    }

    public String getDisplayID() {
        StringBuilder display = new StringBuilder();
        int id = Item.getIdFromItem(item);
        display.append('#').append(String.format("%04d", id));
        if (item.getHasSubtypes()) {
            display.append('/').append(metadata);
        }
        return display.toString();
    }

    @Override
    public int hashCode() {
        return (item.hashCode() << 4) | metadata;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj.getClass() == ItemWithMeta.class
                && ((ItemWithMeta) obj).item == this.item
                && ((ItemWithMeta) obj).metadata == this.metadata;
    }
}
