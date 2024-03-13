package carpet.utils.counter;

public enum TimeUnit
{
    gametick("gt", 1),
    second("s", 20),
    minute("min", 1200),
    hour("h", 72000),
    day("d", 72000 * 24),
    week("week", 72000 * 24 * 7);
    public final String symbol;
    public final long scale;

    TimeUnit(String symbol, long scale)
    {
        this.symbol = symbol;
        this.scale = scale;
    }
}
