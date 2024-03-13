package carpet.utils.counter;

public enum ItemUnit
{
    item("", 1, false),
    stack("Stack", 1, true),
    box("Box", 27, true),
    largeChestBox("LCB", 27 * 54, true);
    public final String symbol;
    public final long scale;
    public final boolean stackDependent;

    ItemUnit(String symbol, long scale, boolean stackDependent)
    {
        this.symbol = symbol;
        this.scale = scale;
        this.stackDependent = stackDependent;
    }
}
