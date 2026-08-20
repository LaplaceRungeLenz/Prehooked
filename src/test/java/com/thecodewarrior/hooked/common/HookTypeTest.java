package com.thecodewarrior.hooked.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class HookTypeTest {

    private static final double EPSILON = 1.0E-9D;

    @Test
    public void matchesPortHookStatistics() {
        assertStats(HookType.WOOD, 1, 8.0D, 0.4D, 0.2D, 0.5D);
        assertStats(HookType.IRON, 2, 16.0D, 3.6D, 0.4D, 0.5D);
        assertStats(HookType.DIAMOND, 4, 24.0D, 5.0D, 1.0D, 1.0D);
        assertStats(HookType.RED, 4, 24.0D, 1.2D, 1.0D, 1.0D);
        assertStats(HookType.ENDER, 1, 64.0D, 64.0D, 2.25D, 2.25D);
    }

    @Test
    public void metadataWrapsAndNetworkOrdinalsRejectInvalidValues() {
        assertSame(HookType.WOOD, HookType.byMetadata(0));
        assertSame(HookType.ENDER, HookType.byMetadata(-1));
        assertSame(HookType.WOOD, HookType.byMetadata(HookType.values().length));
        assertNull(HookType.byOrdinal(-1));
        assertNull(HookType.byOrdinal(HookType.values().length));
    }

    private static void assertStats(HookType type, int count, double range, double projectileSpeed, double pullSpeed,
        double retractSpeed) {
        HookStats stats = type.getDefaultStats();
        assertEquals(count, stats.getMaxAnchors());
        assertEquals(range, stats.getRange(), EPSILON);
        assertEquals(projectileSpeed, stats.getProjectileSpeed(), EPSILON);
        assertEquals(pullSpeed, stats.getPullSpeed(), EPSILON);
        assertEquals(retractSpeed, stats.getRetractSpeed(), EPSILON);
        assertEquals(0.5D, type.getHookLength(), EPSILON);
    }
}
