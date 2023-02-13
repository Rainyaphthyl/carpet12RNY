package carpet.helpers.endermelon;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

/**
 * Tracks ONE enderman in the endermelon farm
 */
public class EndermelonTracker {
    public static boolean enabled = false;
    public boolean running = false;
    /**
     * {@code N/48}
     */
    public DoubleArrayList melonDensityList = new DoubleArrayList();
    /**
     * {@code 1/20 or 1/57}
     */
    public DoubleArrayList blockTakingChanceList = new DoubleArrayList();

    public EndermelonTracker() throws NullPointerException {
    }

    public void startTracking() {
        if (!running) {
            melonDensityList.clear();
            blockTakingChanceList.clear();
            running = true;
        }
    }

}
