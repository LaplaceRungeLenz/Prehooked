package com.thecodewarrior.hooked.common;

public enum HookType {

    WOOD(1, 8.0D, 8.0D, 4.0D, 0.5D, 0x8B5A2B),
    // Vanilla free fall approaches 3.92 blocks/tick. Iron is slightly slower;
    // Diamond is deliberately faster so it can overtake a falling player.
    IRON(2, 16.0D, 72.0D, 8.0D, 0.5D, 0xC9CED3),
    DIAMOND(4, 24.0D, 100.0D, 20.0D, 0.5D, 0x55FFFF),
    RED(4, 24.0D, 24.0D, 20.0D, 0.5D, 0xFF3030),
    ENDER(1, 64.0D, 64.0D * 20.0D, 45.0D, 0.5D, 0x9D4EDD);

    private final int count;
    private final double range;
    private final double projectileSpeed;
    private final double pullStrength;
    private final double hookLength;
    private final int color;

    HookType(int count, double range, double blocksPerSecond, double reelBlocksPerSecond, double hookLength,
        int color) {
        this.count = count;
        this.range = range;
        this.projectileSpeed = blocksPerSecond / 20.0D;
        this.pullStrength = reelBlocksPerSecond / 20.0D;
        this.hookLength = hookLength;
        this.color = color;
    }

    public int getCount() {
        return count;
    }

    public double getRange() {
        return range;
    }

    public double getRangeSquared() {
        return range * range;
    }

    public double getProjectileSpeed() {
        return projectileSpeed;
    }

    public double getPullStrength() {
        return pullStrength;
    }

    public double getHookLength() {
        return hookLength;
    }

    public int getColor() {
        return color;
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
