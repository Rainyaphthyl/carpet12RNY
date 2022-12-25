package carpet.utils.portalcalculator;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.ints.IntBidirectionalIterator;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Stores the portal positions and provides faster searching, keeping the order of vanilla portal search.
 * <p>
 * Ranges from [~-256, 0, ~-256] to [~+256, 255, ~+256],<br>
 * OR from [~-128, 0, ~-128] to [~+128, 255, ~+128],<br>
 * "~" ranges with pattern size, so the map size may be larger than 513.
 * <p>
 * order: x+, z+, y-
 */
public class PortalMegaCache implements Set<BlockPos> {
    /**
     * Provides the vanilla searching order
     */
    public static final Comparator<BlockPos> BLOCK_POS_COMPARATOR = (pos1, pos2) -> {
        if (pos1 == pos2) {
            return 0;
        } else if (pos1 == null) {
            return -1;
        } else if (pos2 == null) {
            return 1;
        } else if (pos1.getX() != pos2.getX()) {
            return pos1.getX() < pos2.getX() ? -1 : 1;
        } else if (pos1.getZ() != pos2.getZ()) {
            return pos1.getZ() < pos2.getZ() ? -1 : 1;
        } else if (pos1.getY() != pos2.getY()) {
            return pos1.getY() > pos2.getY() ? -1 : 1;
        } else {
            return 0;
        }
    };
    public static final BlockPos POS_EXTREME_START = new BlockPos(Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE);
    public static final BlockPos POS_EXTREME_END = new BlockPos(Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
    private final Int2ObjectSortedMap<Int2ObjectSortedMap<Int2ObjectSortedMap<BlockPos>>> cache;
    private int count;
    private int xFront, yFront, zFront, xBack, yBack, zBack;
    private BlockPos first, last;

    public PortalMegaCache() {
        count = 0;
        cache = new Int2ObjectAVLTreeMap<>();
        cache.defaultReturnValue(null);
        xFront = POS_EXTREME_END.getX();
        yFront = POS_EXTREME_END.getY();
        zFront = POS_EXTREME_END.getZ();
        xBack = POS_EXTREME_START.getX();
        yBack = POS_EXTREME_START.getY();
        zBack = POS_EXTREME_START.getZ();
        first = null;
        last = null;
    }

    /**
     * Provides the vanilla searching order
     */
    public static int compare(int x1, int y1, int z1, int x2, int y2, int z2) {
        if (x1 != x2) {
            return x1 < x2 ? -1 : 1;
        } else if (z1 != z2) {
            return z1 < z2 ? -1 : 1;
        } else if (y1 != y2) {
            return y1 > y2 ? -1 : 1;
        } else {
            return 0;
        }
    }

    public static boolean posEquals(BlockPos pos, int x, int y, int z) {
        return pos != null && x == pos.getX() && z == pos.getZ() && y == pos.getY();
    }

    @Override
    public int size() {
        return count;
    }

    @Override
    public boolean isEmpty() {
        return count == 0;
    }

    @Override
    public boolean contains(Object o) {
        return (o instanceof BlockPos) && contains(((BlockPos) o).getX(), ((BlockPos) o).getY(), ((BlockPos) o).getZ());
    }

    @Override
    public Iterator<BlockPos> iterator() {
        return new PortalIterator(this);
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(@Nonnull T[] a) {
        int size = size();
        T[] result = a;
        if (a.length < size) {
            result = (T[]) new Object[size];
        }
        int i = 0;
        for (Iterator<BlockPos> iterator = iterator(); iterator.hasNext(); ++i) {
            BlockPos blockPos = iterator.next();
            result[i] = (T) blockPos;
        }
        for (; i < size; ++i) {
            result[i] = null;
        }
        return result;
    }

    @Override
    public boolean add(BlockPos pos) {
        if (pos == null) {
            return false;
        }
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        boolean flag = false;
        Int2ObjectSortedMap<Int2ObjectSortedMap<BlockPos>> face = cache.get(x);
        // default return value is set to null
        if (face == null) {
            face = new Int2ObjectAVLTreeMap<>();
            face.defaultReturnValue(null);
            cache.put(x, face);
        }
        Int2ObjectSortedMap<BlockPos> column = face.get(z);
        if (column == null) {
            column = new Int2ObjectAVLTreeMap<>();
            column.defaultReturnValue(null);
            face.put(z, column);
        }
        if (!column.containsKey(y)) {
            flag = column.put(y, pos) != pos;
        }
        if (flag) {
            if (compare(x, y, z, xFront, yFront, zFront) < 0) {
                first = null;
                first = first();
                xFront = x;
                yFront = y;
                zFront = z;
            }
            if (compare(x, y, z, xBack, yBack, zBack) > 0) {
                last = null;
                last = last();
                xBack = x;
                yBack = y;
                zBack = z;
            }
            ++count;
        }
        return flag;
    }

    @Override
    public boolean remove(Object o) {
        return (o instanceof BlockPos) && remove(((BlockPos) o).getX(), ((BlockPos) o).getY(), ((BlockPos) o).getZ());
    }

    @Override
    public boolean containsAll(@Nonnull Collection<?> c) {
        for (Object item : c) {
            if (!contains(item)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(@Nonnull Collection<? extends BlockPos> c) {
        boolean modified = false;
        for (BlockPos item : c) {
            modified |= (item != null) && add(item);
        }
        return modified;
    }

    @Override
    public boolean retainAll(@Nonnull Collection<?> c) {
        boolean modified = false;
        for (Iterator<BlockPos> iterator = iterator(); iterator.hasNext(); ) {
            BlockPos pos = iterator.next();
            if (!c.contains(pos)) {
                iterator.remove();
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean removeAll(@Nonnull Collection<?> c) {
        boolean modified = false;
        for (Iterator<BlockPos> iterator = iterator(); iterator.hasNext(); ) {
            BlockPos pos = iterator.next();
            if (c.contains(pos)) {
                iterator.remove();
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public void clear() {
        int x, z;
        for (IntIterator xIter = cache.keySet().iterator(); xIter.hasNext(); ) {
            x = xIter.nextInt();
            Int2ObjectSortedMap<Int2ObjectSortedMap<BlockPos>> face = cache.get(x);
            for (IntIterator zIter = face.keySet().iterator(); zIter.hasNext(); ) {
                z = zIter.nextInt();
                Int2ObjectSortedMap<BlockPos> column = face.get(z);
                column.clear();
            }
            face.clear();
        }
        cache.clear();
        count = 0;
    }

    public boolean add(int x, int y, int z) {
        return add(new BlockPos(x, y, z));
    }

    public boolean contains(BlockPos pos) {
        return pos != null && contains(pos.getX(), pos.getY(), pos.getZ());
    }

    public boolean contains(int x, int y, int z) {
        Int2ObjectSortedMap<Int2ObjectSortedMap<BlockPos>> face = cache.get(x);
        if (face == null) {
            return false;
        }
        Int2ObjectSortedMap<BlockPos> column = face.get(z);
        if (column == null) {
            return false;
        }
        BlockPos point = column.get(y);
        return posEquals(point, x, y, z);
    }

    public boolean remove(BlockPos pos) {
        return pos != null && remove(pos.getX(), pos.getY(), pos.getZ());
    }

    public boolean remove(int x, int y, int z) {
        Int2ObjectSortedMap<Int2ObjectSortedMap<BlockPos>> face = cache.get(x);
        if (face == null) {
            return false;
        }
        Int2ObjectSortedMap<BlockPos> column = face.get(z);
        if (column == null) {
            return false;
        }
        BlockPos point = column.remove(y);
        boolean modified = posEquals(point, x, y, z);
        if (modified) {
            if (column.isEmpty()) {
                face.remove(z);
            }
            if (face.isEmpty()) {
                cache.remove(x);
            }
            if (compare(x, y, z, xFront, yFront, zFront) == 0) {
                first = null;
                first = first();
                xFront = first.getX();
                yFront = first.getY();
                zFront = first.getZ();
            }
            if (compare(x, y, z, xBack, yBack, zBack) == 0) {
                last = null;
                last = last();
                xBack = last.getX();
                yBack = last.getY();
                zBack = last.getZ();
            }
            --count;
        }
        return modified;
    }

    public Comparator<? super BlockPos> comparator() {
        return BLOCK_POS_COMPARATOR;
    }

    public BlockPos first() {
        if (first != null) {
            return first;
        }
        if (isEmpty()) {
            return null;
        } else {
            Int2ObjectSortedMap<Int2ObjectSortedMap<BlockPos>> face = cache.get(cache.firstIntKey());
            Int2ObjectSortedMap<BlockPos> column = face.get(face.firstIntKey());
            return column.get(column.lastIntKey());
        }
    }

    public BlockPos last() {
        if (last != null) {
            return last;
        }
        if (isEmpty()) {
            return null;
        } else {
            Int2ObjectSortedMap<Int2ObjectSortedMap<BlockPos>> face = cache.get(cache.lastIntKey());
            Int2ObjectSortedMap<BlockPos> column = face.get(face.lastIntKey());
            return column.get(column.firstIntKey());
        }
    }

    private static class PortalIterator implements Iterator<BlockPos> {
        private final PortalMegaCache parent;
        private final IntBidirectionalIterator iterVolume;
        private Int2ObjectSortedMap<Int2ObjectSortedMap<BlockPos>> face;
        private Int2ObjectSortedMap<BlockPos> column;
        private IntBidirectionalIterator iterFace;
        private IntBidirectionalIterator iterColumn;
        private BlockPos pos;
        private int x;
        private int z;
        private int y;

        private PortalIterator(@Nonnull PortalMegaCache parent) throws NullPointerException {
            this.parent = Objects.requireNonNull(parent);
            iterVolume = Objects.requireNonNull(parent.cache).keySet().iterator();
            Objects.requireNonNull(iterVolume);
            face = null;
            column = null;
            pos = null;
            x = POS_EXTREME_START.getX();
            y = POS_EXTREME_START.getY();
            z = POS_EXTREME_START.getZ();
        }

        @Override
        public boolean hasNext() {
            return compare(x, y, z, parent.xBack, parent.yBack, parent.zBack) < 0;
        }

        @Override
        public BlockPos next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            // 0 for y--, 1 for z++, 2 for x++, 3 for error
            if (iterColumn != null && iterColumn.hasPrevious()) {
                y = iterColumn.previousInt();
                pos = column.get(y);
            } else if (iterFace != null && iterFace.hasNext()) {
                z = iterFace.nextInt();
                column = face.get(z);
                iterColumn = column.keySet().iterator();
                if (!iterColumn.hasPrevious()) {
                    throw new NoSuchElementException("wrong structure");
                }
                y = iterColumn.previousInt();
                pos = column.get(y);
            } else if (iterVolume != null && iterVolume.hasNext()) {
                x = iterVolume.nextInt();
                face = parent.cache.get(x);
                iterFace = face.keySet().iterator();
                if (!iterFace.hasNext()) {
                    throw new NoSuchElementException("wrong structure");
                }
                z = iterFace.nextInt();
                column = face.get(z);
                iterColumn = column.keySet().iterator();
                if (!iterColumn.hasPrevious()) {
                    throw new NoSuchElementException("wrong structure");
                }
                y = iterColumn.previousInt();
                pos = column.get(y);
            }
            return pos;
        }

        @Override
        public void remove() {
            parent.remove(x, y, z);
        }
    }
}
