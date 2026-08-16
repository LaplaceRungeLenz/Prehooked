package com.thecodewarrior.hooked;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public final class HookedConfig {

    public static final int SEARCH_BAUBLES = 1;
    public static final int SEARCH_HAND = 2;
    public static final int SEARCH_HOTBAR = 4;
    public static final int SEARCH_INVENTORY = 8;

    public static int searchLocations = SEARCH_BAUBLES;
    public static int stateSyncInterval = 2;
    public static boolean enableRedHookFlight = true;

    private HookedConfig() {}

    public static void load(File file) {
        Configuration config = new Configuration(file);
        config.load();

        searchLocations = config.getInt(
            "searchLocations",
            Configuration.CATEGORY_GENERAL,
            SEARCH_BAUBLES,
            SEARCH_BAUBLES,
            SEARCH_BAUBLES | SEARCH_HAND | SEARCH_HOTBAR | SEARCH_INVENTORY,
            "Bit mask and search order: 1=Baubles, 2=held item, 4=hotbar, 8=main inventory.");
        stateSyncInterval = config.getInt(
            "stateSyncInterval",
            Configuration.CATEGORY_GENERAL,
            2,
            1,
            20,
            "Ticks between active hook state packets. Lower values make remote hooks smoother.");
        enableRedHookFlight = config.getBoolean(
            "enableRedHookFlight",
            Configuration.CATEGORY_GENERAL,
            true,
            "Enable bounded flight inside the region defined by planted Red Hook anchors.");

        if (config.hasChanged()) {
            config.save();
        }
    }
}
