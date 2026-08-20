package com.thecodewarrior.hooked.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import java.util.EnumMap;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

import com.thecodewarrior.hooked.HookedConfig;

public class HookSettingsTest {

    private static final double EPSILON = 1.0E-9D;

    @After
    public void restoreLocalConfiguration() {
        HookedConfig.restoreLocalSettings();
    }

    @Test
    public void validatesNetworkAndConfigurationBoundaries() {
        Map<HookType, HookStats> stats = new EnumMap<HookType, HookStats>(HookType.class);
        stats.put(
            HookType.WOOD,
            new HookStats(99, Double.NaN, Double.POSITIVE_INFINITY, -5.0D, HookStats.MAX_SPEED + 1.0D));
        stats.put(HookType.RED, new HookStats(12, 80.0D, 2.0D, 3.0D, 4.0D));

        HookSettings settings = new HookSettings(99, false, stats);
        HookStats wood = settings.getStats(HookType.WOOD);
        assertEquals(HookSettings.MAX_SEARCH_LOCATIONS, settings.getSearchLocations());
        assertFalse(settings.isRedHookFlightEnabled());
        assertEquals(HookStats.MAX_ANCHORS, wood.getMaxAnchors());
        assertEquals(
            HookType.WOOD.getDefaultStats()
                .getRange(),
            wood.getRange(),
            EPSILON);
        assertEquals(
            HookType.WOOD.getDefaultStats()
                .getProjectileSpeed(),
            wood.getProjectileSpeed(),
            EPSILON);
        assertEquals(HookStats.MIN_SPEED, wood.getPullSpeed(), EPSILON);
        assertEquals(HookStats.MAX_SPEED, wood.getRetractSpeed(), EPSILON);
        assertEquals(
            HookStats.MAX_RED_ANCHORS,
            settings.getStats(HookType.RED)
                .getMaxAnchors());
    }

    @Test
    public void appliesServerOverrideAndRestoresLocalSettings() {
        HookSettings local = HookedConfig.getLocalSettings();
        Map<HookType, HookStats> stats = new EnumMap<HookType, HookStats>(HookType.class);
        stats.put(HookType.IRON, new HookStats(7, 72.0D, 8.0D, 3.0D, 4.0D));
        HookSettings server = new HookSettings(HookSettings.MAX_SEARCH_LOCATIONS, false, stats);

        HookedConfig.applyServerSettings(server);
        assertEquals(true, HookedConfig.hasServerSettings());
        assertEquals(HookSettings.MAX_SEARCH_LOCATIONS, HookedConfig.getSearchLocations());
        assertEquals(7, HookType.IRON.getCount());
        assertEquals(72.0D, HookType.IRON.getRange(), EPSILON);
        assertEquals(8.0D, HookType.IRON.getProjectileSpeed(), EPSILON);
        assertEquals(3.0D, HookType.IRON.getPullSpeed(), EPSILON);
        assertEquals(4.0D, HookType.IRON.getRetractSpeed(), EPSILON);

        HookedConfig.restoreLocalSettings();
        assertFalse(HookedConfig.hasServerSettings());
        assertSame(local, HookedConfig.getLocalSettings());
        assertEquals(local.getSearchLocations(), HookedConfig.getSearchLocations());
        assertEquals(local.getStats(HookType.IRON), HookedConfig.getHookStats(HookType.IRON));
    }
}
