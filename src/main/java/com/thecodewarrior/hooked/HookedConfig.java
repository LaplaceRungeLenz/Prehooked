package com.thecodewarrior.hooked;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import com.thecodewarrior.hooked.common.HookSettings;
import com.thecodewarrior.hooked.common.HookStats;
import com.thecodewarrior.hooked.common.HookType;

public final class HookedConfig {

    /** @deprecated Use the matching constants on {@link HookSettings}. */
    @Deprecated
    public static final int SEARCH_BAUBLES = HookSettings.SEARCH_BAUBLES;
    /** @deprecated Use the matching constants on {@link HookSettings}. */
    @Deprecated
    public static final int SEARCH_HAND = HookSettings.SEARCH_HAND;
    /** @deprecated Use the matching constants on {@link HookSettings}. */
    @Deprecated
    public static final int SEARCH_HOTBAR = HookSettings.SEARCH_HOTBAR;
    /** @deprecated Use the matching constants on {@link HookSettings}. */
    @Deprecated
    public static final int SEARCH_INVENTORY = HookSettings.SEARCH_INVENTORY;

    private static final String HOOK_CATEGORY_PREFIX = "hooks.";
    private static volatile HookSettings localSettings = HookSettings.defaults();
    private static volatile HookSettings activeSettings = localSettings;
    private static volatile boolean serverSettingsActive;
    private static int stateSyncInterval = 2;

    private HookedConfig() {}

    public static synchronized void load(File file) {
        Configuration config = new Configuration(file);
        config.load();

        int searchLocations = readInt(
            config,
            "searchLocations",
            Configuration.CATEGORY_GENERAL,
            HookSettings.SEARCH_BAUBLES,
            HookSettings.MIN_SEARCH_LOCATIONS,
            HookSettings.MAX_SEARCH_LOCATIONS,
            "Bit mask and search order: 1=Baubles, 2=held item, 4=hotbar, 8=main inventory.");
        stateSyncInterval = readInt(
            config,
            "stateSyncInterval",
            Configuration.CATEGORY_GENERAL,
            2,
            1,
            20,
            "Ticks between active hook state packets. Lower values make remote hooks smoother.");
        boolean enableRedHookFlight = config.getBoolean(
            "enableRedHookFlight",
            Configuration.CATEGORY_GENERAL,
            true,
            "Enable bounded flight inside the region defined by planted Red Hook anchors.");

        Map<HookType, HookStats> stats = new EnumMap<HookType, HookStats>(HookType.class);
        for (HookType type : HookType.values()) {
            String category = HOOK_CATEGORY_PREFIX + type.getSerializedName();
            HookStats defaults = type.getDefaultStats();
            int anchorLimit = type == HookType.RED ? HookStats.MAX_RED_ANCHORS : HookStats.MAX_ANCHORS;
            config.addCustomCategoryComment(
                category,
                "Gameplay statistics for the " + type.getSerializedName() + " hook. Changes require a restart.");
            stats.put(
                type,
                new HookStats(
                    readInt(
                        config,
                        "maxAnchors",
                        category,
                        defaults.getMaxAnchors(),
                        1,
                        anchorLimit,
                        "Maximum simultaneously active anchors."),
                    readDouble(
                        config,
                        "rangeBlocks",
                        category,
                        defaults.getRange(),
                        HookStats.MIN_RANGE,
                        HookStats.MAX_RANGE,
                        "Maximum firing distance and rope length, in blocks."),
                    readDouble(
                        config,
                        "projectileSpeedBlocksPerTick",
                        category,
                        defaults.getProjectileSpeed(),
                        HookStats.MIN_SPEED,
                        HookStats.MAX_SPEED,
                        "Hook-head extension speed, in blocks per tick."),
                    readDouble(
                        config,
                        "pullSpeedBlocksPerTick",
                        category,
                        defaults.getPullSpeed(),
                        HookStats.MIN_SPEED,
                        HookStats.MAX_SPEED,
                        "Maximum speed used to pull the player toward planted anchors, in blocks per tick."),
                    readDouble(
                        config,
                        "retractSpeedBlocksPerTick",
                        category,
                        defaults.getRetractSpeed(),
                        HookStats.MIN_SPEED,
                        HookStats.MAX_SPEED,
                        "Speed of a retracting hook head, in blocks per tick.")));
        }

        localSettings = new HookSettings(searchLocations, enableRedHookFlight, stats);
        activeSettings = localSettings;
        serverSettingsActive = false;

        if (config.hasChanged()) {
            config.save();
        }
    }

    public static int getSearchLocations() {
        return activeSettings.getSearchLocations();
    }

    public static boolean isRedHookFlightEnabled() {
        return activeSettings.isRedHookFlightEnabled();
    }

    public static HookStats getHookStats(HookType type) {
        return activeSettings.getStats(type);
    }

    public static int getStateSyncInterval() {
        return stateSyncInterval;
    }

    public static HookSettings getLocalSettings() {
        return localSettings;
    }

    public static double getMaximumRange() {
        return activeSettings.getMaximumRange();
    }

    public static void applyServerSettings(HookSettings settings) {
        activeSettings = settings == null ? localSettings : settings;
        serverSettingsActive = settings != null;
    }

    public static void restoreLocalSettings() {
        activeSettings = localSettings;
        serverSettingsActive = false;
    }

    public static boolean hasServerSettings() {
        return serverSettingsActive;
    }

    private static int readInt(Configuration config, String name, String category, int defaultValue, int minimum,
        int maximum, String comment) {
        Property property = config.get(category, name, defaultValue, comment, minimum, maximum);
        int supplied = property.getInt(defaultValue);
        int value = Math.max(minimum, Math.min(maximum, supplied));
        if (value != supplied) {
            HookedMod.LOG.warn("Clamped config value {}.{} from {} to {}", category, name, supplied, value);
            property.set(value);
        }
        return value;
    }

    private static double readDouble(Configuration config, String name, String category, double defaultValue,
        double minimum, double maximum, String comment) {
        Property property = config.get(category, name, defaultValue, comment, minimum, maximum);
        double supplied = property.getDouble(defaultValue);
        double finite = Double.isNaN(supplied) || Double.isInfinite(supplied) ? defaultValue : supplied;
        double value = Math.max(minimum, Math.min(maximum, finite));
        if (Double.compare(value, supplied) != 0) {
            HookedMod.LOG.warn("Clamped config value {}.{} from {} to {}", category, name, supplied, value);
            property.set(value);
        }
        return value;
    }
}
