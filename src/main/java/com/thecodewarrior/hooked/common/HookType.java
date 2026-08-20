package com.thecodewarrior.hooked.common;

import com.thecodewarrior.hooked.HookedConfig;

public enum HookType {

    WOOD(1, 8.0D, 0.4D, 0.2D, 0.5D, 0.5D, 0x8B5A2B),
    // Vanilla free fall approaches 3.92 blocks/tick. Iron is slightly slower;
    // Diamond is deliberately faster so it can overtake a falling player.
    IRON(2, 16.0D, 3.6D, 0.4D, 0.5D, 0.5D, 0xC9CED3),
    DIAMOND(4, 24.0D, 5.0D, 1.0D, 1.0D, 0.5D, 0x55FFFF),
    RED(4, 24.0D, 1.2D, 1.0D, 1.0D, 0.5D, 0xFF3030),
    ENDER(1, 64.0D, 64.0D, 2.25D, 2.25D, 0.5D, 0x9D4EDD);

    private final HookStats defaultStats;
    private final double hookLength;
    private final int color;

    HookType(int count, double range, double projectileSpeed, double pullSpeed, double retractSpeed, double hookLength,
        int color) {
        this.defaultStats = new HookStats(count, range, projectileSpeed, pullSpeed, retractSpeed);
        this.hookLength = hookLength;
        this.color = color;
    }

    public int getCount() {
        return getStats().getMaxAnchors();
    }

    public double getRange() {
        return getStats().getRange();
    }

    public double getRangeSquared() {
        double range = getRange();
        return range * range;
    }

    public double getProjectileSpeed() {
        return getStats().getProjectileSpeed();
    }

    public double getPullSpeed() {
        return getStats().getPullSpeed();
    }

    /** @deprecated Since 1.1.0; use {@link #getPullSpeed()}. */
    @Deprecated
    public double getPullStrength() {
        return getPullSpeed();
    }

    public double getRetractSpeed() {
        return getStats().getRetractSpeed();
    }

    public HookStats getDefaultStats() {
        return defaultStats;
    }

    public double getHookLength() {
        return hookLength;
    }

    public int getColor() {
        return color;
    }

    private HookStats getStats() {
        return HookedConfig.getHookStats(this);
    }

    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static HookType byMetadata(int metadata) {
        HookType[] values = values();
        return values[Math.floorMod(metadata, values.length)];
    }

    public static HookType byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : null;
    }
}
