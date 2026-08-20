package com.thecodewarrior.hooked.common;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Immutable, validated gameplay configuration shared by the server and clients. */
public final class HookSettings {

    public static final int SEARCH_BAUBLES = 1;
    public static final int SEARCH_HAND = 2;
    public static final int SEARCH_HOTBAR = 4;
    public static final int SEARCH_INVENTORY = 8;
    public static final int MIN_SEARCH_LOCATIONS = SEARCH_BAUBLES;
    public static final int MAX_SEARCH_LOCATIONS = SEARCH_BAUBLES | SEARCH_HAND | SEARCH_HOTBAR | SEARCH_INVENTORY;

    private final int searchLocations;
    private final boolean redHookFlightEnabled;
    private final Map<HookType, HookStats> hookStats;

    public HookSettings(int searchLocations, boolean redHookFlightEnabled, Map<HookType, HookStats> suppliedStats) {
        this.searchLocations = clamp(searchLocations, MIN_SEARCH_LOCATIONS, MAX_SEARCH_LOCATIONS);
        this.redHookFlightEnabled = redHookFlightEnabled;

        EnumMap<HookType, HookStats> validated = new EnumMap<HookType, HookStats>(HookType.class);
        for (HookType type : HookType.values()) {
            HookStats defaults = type.getDefaultStats();
            HookStats supplied = suppliedStats == null ? null : suppliedStats.get(type);
            validated.put(type, validate(type, supplied, defaults));
        }
        hookStats = Collections.unmodifiableMap(validated);
    }

    public static HookSettings defaults() {
        EnumMap<HookType, HookStats> stats = new EnumMap<HookType, HookStats>(HookType.class);
        for (HookType type : HookType.values()) {
            stats.put(type, type.getDefaultStats());
        }
        return new HookSettings(SEARCH_BAUBLES, true, stats);
    }

    public int getSearchLocations() {
        return searchLocations;
    }

    public boolean isRedHookFlightEnabled() {
        return redHookFlightEnabled;
    }

    public HookStats getStats(HookType type) {
        HookStats stats = hookStats.get(type);
        return stats == null ? HookType.WOOD.getDefaultStats() : stats;
    }

    public double getMaximumRange() {
        double maximum = 0.0D;
        for (HookStats stats : hookStats.values()) {
            maximum = Math.max(maximum, stats.getRange());
        }
        return maximum;
    }

    private static HookStats validate(HookType type, HookStats supplied, HookStats defaults) {
        HookStats source = supplied == null ? defaults : supplied;
        int maximumAnchors = type == HookType.RED ? HookStats.MAX_RED_ANCHORS : HookStats.MAX_ANCHORS;
        return new HookStats(
            clamp(source.getMaxAnchors(), 1, maximumAnchors),
            finiteClamp(source.getRange(), defaults.getRange(), HookStats.MIN_RANGE, HookStats.MAX_RANGE),
            finiteClamp(
                source.getProjectileSpeed(),
                defaults.getProjectileSpeed(),
                HookStats.MIN_SPEED,
                HookStats.MAX_SPEED),
            finiteClamp(source.getPullSpeed(), defaults.getPullSpeed(), HookStats.MIN_SPEED, HookStats.MAX_SPEED),
            finiteClamp(
                source.getRetractSpeed(),
                defaults.getRetractSpeed(),
                HookStats.MIN_SPEED,
                HookStats.MAX_SPEED));
    }

    private static double finiteClamp(double value, double fallback, double minimum, double maximum) {
        double finite = Double.isNaN(value) || Double.isInfinite(value) ? fallback : value;
        return Math.max(minimum, Math.min(maximum, finite));
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof HookSettings)) {
            return false;
        }
        HookSettings other = (HookSettings) object;
        return searchLocations == other.searchLocations && redHookFlightEnabled == other.redHookFlightEnabled
            && hookStats.equals(other.hookStats);
    }

    @Override
    public int hashCode() {
        int result = searchLocations;
        result = 31 * result + (redHookFlightEnabled ? 1 : 0);
        return 31 * result + hookStats.hashCode();
    }
}
