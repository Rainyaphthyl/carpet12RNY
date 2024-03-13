package carpet.utils;

import it.unimi.dsi.fastutil.ints.Int2DoubleArrayMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import net.minecraft.util.math.MathHelper;

public class StatsBundle {
    private static final Int2ObjectSortedMap<String> PREFIXES_MAP;
    private static final Int2DoubleMap LEVEL_RATES_MAP;

    static {
        PREFIXES_MAP = new Int2ObjectAVLTreeMap<>();
        PREFIXES_MAP.put(0, "");
        PREFIXES_MAP.put(3, "k");
        PREFIXES_MAP.put(6, "M");
        PREFIXES_MAP.put(9, "G");
        PREFIXES_MAP.put(12, "T");

        LEVEL_RATES_MAP = new Int2DoubleArrayMap();
        double rate = 1.0;
        for (int i = 0; i <= 12; i += 3) {
            LEVEL_RATES_MAP.put(i, rate);
            rate *= 1000.0;
        }
    }

    public final double average;
    public final double error;
    /**
     * Rounds {@code average} according to {@code error}
     */
    private String roundedAverage = null;
    /**
     * Rounds {@code error} with 1 or 2 significant figures
     */
    private String roundedError = null;

    public StatsBundle(double average, double error) {
        this.average = average;
        this.error = error;
    }

    /**
     * {@code O(n)}
     */
    public static long simplePow(long base, int exponent) {
        long pow = 1;
        if (exponent >= 0) {
            for (int i = 0; i < exponent; ++i) {
                pow *= base;
            }
        } else {
            for (int i = exponent; i < 0; ++i) {
                pow /= base;
            }
        }
        return pow;
    }

    public static double round_half_even(double value, int scale) {
        double rate = simplePow(10.0, scale);
        return Math.rint(value * rate) / rate;
    }

    /**
     * {@code O(n)}
     */
    public static double simplePow(double base, int exponent) {
        double pow = 1.0;
        if (exponent >= 0) {
            for (int i = 0; i < exponent; ++i) {
                pow *= base;
            }
        } else {
            for (int i = exponent; i < 0; ++i) {
                pow /= base;
            }
        }
        return pow;
    }

    /**
     * Round to significant figures
     */
    public static String round_to_sig_figs(double value, int digits) {
        int firstIndex = MathHelper.floor(Math.log10(value));
        int decimalPlaces = digits - 1 - firstIndex;
        String format = "%." + Math.max(decimalPlaces, 0) + 'f';
        return String.format(format, value);
    }

    public String getRoundedAverage() {
        setRoundedResults();
        return roundedAverage;
    }

    public String getRoundedError() {
        setRoundedResults();
        return roundedError;
    }

    public RoundedStatsBundle getRoundedBundle() {
        int minimal = MathHelper.floor(Math.log10(error));
        if (error / simplePow(10.0, minimal) < 2.95) {
            --minimal;
        }
        int level = minimal > 0 ? MathHelper.roundUp(minimal, 3) : 0;
        if (!PREFIXES_MAP.containsKey(level)) {
            level = MathHelper.clamp(level, PREFIXES_MAP.firstIntKey(), PREFIXES_MAP.lastIntKey());
        }
        String unitPrefix = PREFIXES_MAP.get(level);
        int decimalPlaces = level - minimal;
        double rate = LEVEL_RATES_MAP.get(level);
        double displayedAverage = average / rate;
        double displayedError = error / rate;
        String format = "%." + Math.max(decimalPlaces, 0) + 'f';
        String roundedAverage = String.format(format, displayedAverage);
        String roundedError = String.format(format, displayedError);
        return new RoundedStatsBundle(roundedAverage, roundedError, unitPrefix);
    }

    /**
     * @return The format String, used as the first param of String.format()
     */
    public String getRoundedPercent() {
        double value = 100.0 * error / average;
        return round_to_sig_figs(value, 3);
    }

    private void setRoundedResults() {
        int minimal = MathHelper.floor(Math.log10(error));
        if (error / simplePow(10.0, minimal) < 2.95) {
            --minimal;
        }
        int level = MathHelper.roundUp(minimal, 3);
        String prefix = PREFIXES_MAP.get(level);
        if (prefix == null) {
            level = MathHelper.clamp(level, PREFIXES_MAP.firstKey(), PREFIXES_MAP.lastKey());
            prefix = PREFIXES_MAP.get(level);
        }
        int postfix = level - minimal;
        String format = "%." + postfix + 'f' + prefix;
        double rate = simplePow(10, level);
        double avgToDisplay = round_half_even(average / rate, postfix);
        roundedAverage = String.format(format, avgToDisplay);
        double errToDisplay = round_half_even(error / rate, postfix);
        roundedError = String.format(format, errToDisplay);
        format = "%." + postfix + "f(%." + postfix + "f)" + prefix;
        String roundedCombined = String.format(format, avgToDisplay, errToDisplay);
    }

    /**
     * Rounds {@code average} according to {@code error}
     * <p>
     * Example: 187236.9(766.2) = 187.2(0.8)k
     * <p>
     * {@code error = 766.2}, and {@code floor(log10(766.2)) = 2} is the {@code firstDigitIndex} of {@code error}
     * <p>
     * {@code roundUp(2, 3) = 3}, using prefix "k"
     */
    public static class RoundedStatsBundle {
        public final String average;
        public final String error;
        public final String unit;


        public RoundedStatsBundle(String average, String error, String unit) {
            this.average = average;
            this.error = error;
            this.unit = unit;
        }
    }

}
