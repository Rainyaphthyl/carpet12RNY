package carpet.utils;

import carpet.CarpetSettings;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CarpetProfiler {
    private static final String[] DIM_KEYS = new String[]{"overworld", "the_nether", "the_end"};
    private static final String[] DIM_NAMES = new String[]{"Overworld", "Nether", "End"};
    private static final String[] PREFIXES = new String[]{"", " - ", "   - "};
    private static final HashMap<String, Long> time_repo = new HashMap<>();
    public static int tick_health_requested = 0;
    private static int tick_health_elapsed = 0;
    private static int test_type = 0; //1 for ticks, 2 for entities;
    private static String current_section = null;
    private static long current_section_start = 0;
    private static String current_dimension = null;
    private static long current_dimension_start = 0;
    private static long current_tick_start = 0;

    @Nullable
    public static String get_dim_key(int ordinal) {
        if (ordinal >= 0 && ordinal < DIM_KEYS.length) {
            return DIM_KEYS[ordinal];
        } else {
            return null;
        }
    }

    public static void prepare_tick_report(int ticks) {
        //maybe add so it only spams the sending player, but honestly - all may want to see it
        time_repo.clear();
        test_type = 1;
        time_repo.put("tick", 0L);
        time_repo.put("network", 0L);
        time_repo.put("autosave", 0L);
        time_repo.put("carpet", 0L);

        for (String dimKey : DIM_KEYS) {
            time_repo.put(dimKey, 0L);
            time_repo.put(dimKey + ".spawning", 0L);
            time_repo.put(dimKey + ".tile_ticks", 0L);
            time_repo.put(dimKey + ".chunk_ticks", 0L);
            time_repo.put(dimKey + ".block_events", 0L);
            time_repo.put(dimKey + ".entities", 0L);
            time_repo.put(dimKey + ".tile_entities", 0L);
        }

        tick_health_elapsed = ticks;
        tick_health_requested = ticks;
        current_tick_start = 0L;
        current_section_start = 0L;
        current_section = null;
        current_dimension_start = 0L;
        current_dimension = null;
    }

    public static void start_section(String dimension, String name) {
        if (tick_health_requested == 0L || test_type != 1) {
            return;
        }
        if (current_tick_start == 0L) {
            return;
        }
        if (current_section != null) {
            end_current_section();
        }
        String key = name;
        if (dimension != null) {
            key = dimension + "." + name;
        }
        current_section = key;
        current_section_start = System.nanoTime();
    }

    public static void start_dimension(String dimension) {
        if (tick_health_requested == 0L || test_type != 1 || current_tick_start == 0L) {
            return;
        }
        if (current_dimension != null) {
            end_current_dimension();
        }
        current_dimension = dimension;
        current_dimension_start = System.nanoTime();
    }

    public static void start_entity_section(String dimension, Entity e) {
        if (tick_health_requested == 0L || test_type != 2) {
            return;
        }
        if (current_tick_start == 0L) {
            return;
        }
        if (current_section != null) {
            end_current_section();
        }
        current_section = dimension + "." + e.cm_name();
        current_section_start = System.nanoTime();
    }

    public static void start_tileentity_section(String dimension, TileEntity e) {
        if (tick_health_requested == 0L || test_type != 2) {
            return;
        }
        if (current_tick_start == 0L) {
            return;
        }
        if (current_section != null) {
            end_current_section();
        }
        current_section = dimension + "." + e.cm_name();
        current_section_start = System.nanoTime();
    }

    public static void end_current_section() {
        if (tick_health_requested == 0L || test_type != 1) {
            return;
        }
        long end_time = System.nanoTime();
        if (current_tick_start == 0L) {
            return;
        }
        if (current_section == null) {
            CarpetSettings.LOG.error("finishing section that hasn't started");
            return;
        }
        //CarpetSettings.LOG.error("finishing section "+current_section);
        time_repo.put(current_section, time_repo.get(current_section) + end_time - current_section_start);
        current_section = null;
        current_section_start = 0;
    }

    public static void end_current_dimension() {
        if (tick_health_requested == 0L || test_type != 1 || current_tick_start == 0L) {
            return;
        }
        if (current_dimension == null) {
            CarpetSettings.LOG.error("finishing dimension that hasn't started");
            return;
        }
        long end_time = System.nanoTime();
        long prev_time = time_repo.get(current_dimension);
        time_repo.put(current_dimension, prev_time + end_time - current_dimension_start);
        current_dimension = null;
        current_dimension_start = 0;
    }

    public static void end_current_entity_section() {
        if (tick_health_requested == 0L || test_type != 2) {
            return;
        }
        long end_time = System.nanoTime();
        if (current_tick_start == 0L) {
            return;
        }
        if (current_section == null) {
            CarpetSettings.LOG.error("finishing section that hasn't started");
            return;
        }
        //CarpetSettings.LOG.error("finishing section "+current_section);
        String time_section = "t." + current_section;
        String count_section = "c." + current_section;
        time_repo.put(time_section, time_repo.getOrDefault(time_section, 0L) + end_time - current_section_start);
        time_repo.put(count_section, time_repo.getOrDefault(count_section, 0L) + 1);
        current_section = null;
        current_section_start = 0;
    }

    public static void start_tick_profiling() {
        if (CarpetProfiler.tick_health_requested != 0L) {
            current_tick_start = System.nanoTime();
        }
    }

    public static void end_tick_profiling(MinecraftServer server) {
        if (current_tick_start == 0L) {
            return;
        }
        if (CarpetProfiler.tick_health_requested != 0L) {
            time_repo.put("tick", time_repo.get("tick") + System.nanoTime() - current_tick_start);
            tick_health_elapsed--;
            //CarpetSettings.LOG.error("tick count current at "+time_repo.get("tick"));
            if (tick_health_elapsed <= 0) {
                finalize_tick_report(server);
            }
        }
    }

    public static void finalize_tick_report(MinecraftServer server) {
        if (test_type == 1) {
            finalize_tick_report_for_time(server);
        }
        if (test_type == 2) {
            finalize_tick_report_for_entities(server);
        }
        cleanup_tick_report();
    }

    public static void cleanup_tick_report() {
        time_repo.clear();
        test_type = 0;
        tick_health_elapsed = 0;
        tick_health_requested = 0;
        current_tick_start = 0L;
        current_section_start = 0L;
        current_section = null;

    }

    public static void finalize_tick_report_for_time(MinecraftServer server) {
        //print stats
        final long total_tick_time = time_repo.get("tick");
        final double divider = 1.0D / tick_health_requested / 1000000;
        print_stat_line(server, 0, "Average tick time", divider * total_tick_time);
        long accumulated_total = 0L;
        long value;

        value = time_repo.get("carpet");
        accumulated_total += value;
        print_stat_line(server, 1, "Carpet", divider * value);

        for (int i = 0; i < DIM_KEYS.length; ++i) {
            final long value_dim = time_repo.get(DIM_KEYS[i]);
            accumulated_total += value_dim;
            print_stat_line(server, 1, DIM_NAMES[i], divider * value_dim);

            long accumulated_partial = 0L;

            value = time_repo.get(DIM_KEYS[i] + ".spawning");
            accumulated_partial += value;
            print_stat_line(server, 2, "Spawning", divider * value);

            value = time_repo.get(DIM_KEYS[i] + ".tile_ticks");
            accumulated_partial += value;
            print_stat_line(server, 2, "Tile Ticks", divider * value);

            value = time_repo.get(DIM_KEYS[i] + ".chunk_ticks");
            accumulated_partial += value;
            print_stat_line(server, 2, "Chunk Ticks", divider * value);

            value = time_repo.get(DIM_KEYS[i] + ".block_events");
            accumulated_partial += value;
            print_stat_line(server, 2, "Block Events", divider * value);

            value = time_repo.get(DIM_KEYS[i] + ".entities");
            accumulated_partial += value;
            print_stat_line(server, 2, "Entities", divider * value);

            value = time_repo.get(DIM_KEYS[i] + ".tile_entities");
            accumulated_partial += value;
            print_stat_line(server, 2, "Tile Entities", divider * value);

            final long rest_dim = value_dim - accumulated_partial;
            print_stat_line(server, 2, "Rest", divider * rest_dim);
        }

        value = time_repo.get("network");
        accumulated_total += value;
        print_stat_line(server, 1, "Network", divider * value);

        value = time_repo.get("autosave");
        accumulated_total += value;
        print_stat_line(server, 1, "Autosave", divider * value);

        final long rest = total_tick_time - accumulated_total;
        print_stat_line(server, 1, "Rest", divider * rest);
    }

    public static void finalize_tick_report_for_entities(MinecraftServer server) {
        //print stats
        long total_tick_time = time_repo.get("tick");
        double divider = 1.0D / tick_health_requested / 1000000;
        Messenger.print_server_message(server, String.format("Average tick time: %.3fms", divider * total_tick_time));
        time_repo.remove("tick");
        Messenger.print_server_message(server, "Top 10 counts:");
        int total = 0;
        for (Map.Entry<String, Long> entry : time_repo.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toList())) {
            if (entry.getKey().startsWith("t.")) {
                continue;
            }
            total++;
            if (total > 10) {
                continue;
            }
            String[] parts = entry.getKey().split("\\.");
            String dim = parts[1];
            String name = parts[2];
            Messenger.print_server_message(server, String.format(" - %s in %s: %.3f", name, dim, 1.0D * entry.getValue() / tick_health_requested));
        }
        Messenger.print_server_message(server, "Top 10 grossing:");
        total = 0;
        for (Map.Entry<String, Long> entry : time_repo.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toList())) {
            if (entry.getKey().startsWith("c.")) {
                continue;
            }
            total++;
            if (total > 10) {
                continue;
            }
            String[] parts = entry.getKey().split("\\.");
            String dim = parts[1];
            String name = parts[2];
            Messenger.print_server_message(server, String.format(" - %s in %s: %.3fms", name, dim, divider * entry.getValue()));
        }

    }

    public static void prepare_entity_report(int ticks) {
        //maybe add so it only spams the sending player, but honestly - all may want to see it
        time_repo.clear();
        time_repo.put("tick", 0L);
        test_type = 2;
        tick_health_elapsed = ticks;
        tick_health_requested = ticks;
        current_tick_start = 0L;
        current_section_start = 0L;
        current_section = null;
    }

    private static void print_stat_line(MinecraftServer server, int layer, String name, double result) {
        String literal = String.format("%.3f", result);
        if (!"0.000".equals(literal)) {
            String prefix;
            if (layer >= 0 && layer < PREFIXES.length) {
                prefix = PREFIXES[layer];
            } else {
                StringBuilder prefixBuilder = new StringBuilder();
                for (int i = 1; i < layer; ++i) {
                    prefixBuilder.append("   ");
                }
                if (layer > 0) {
                    prefixBuilder.append(" - ");
                }
                prefix = prefixBuilder.toString();
            }
            Messenger.print_server_message(server, prefix + name + ": " + literal + "ms");
        }
    }
}
