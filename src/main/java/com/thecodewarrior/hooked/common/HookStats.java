package com.thecodewarrior.hooked.common;

/** Immutable gameplay statistics for one hook tier. Values use blocks and ticks. */
public final class HookStats {

    public static final int MAX_ANCHORS = 16;
    public static final int MAX_RED_ANCHORS = 4;
    public static final double MIN_RANGE = 1.0D;
    public static final double MAX_RANGE = 256.0D;
    public static final double MIN_SPEED = 0.01D;
    public static final double MAX_SPEED = 128.0D;

    private final int maxAnchors;
    private final double range;
    private final double projectileSpeed;
    private final double pullSpeed;
    private final double retractSpeed;

    public HookStats(int maxAnchors, double range, double projectileSpeed, double pullSpeed, double retractSpeed) {
        this.maxAnchors = maxAnchors;
        this.range = range;
        this.projectileSpeed = projectileSpeed;
        this.pullSpeed = pullSpeed;
        this.retractSpeed = retractSpeed;
    }

    public int getMaxAnchors() {
        return maxAnchors;
    }

    public double getRange() {
        return range;
    }

    public double getProjectileSpeed() {
        return projectileSpeed;
    }

    public double getPullSpeed() {
        return pullSpeed;
    }

    public double getRetractSpeed() {
        return retractSpeed;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof HookStats)) {
            return false;
        }
        HookStats other = (HookStats) object;
        return maxAnchors == other.maxAnchors && Double.compare(range, other.range) == 0
            && Double.compare(projectileSpeed, other.projectileSpeed) == 0
            && Double.compare(pullSpeed, other.pullSpeed) == 0
            && Double.compare(retractSpeed, other.retractSpeed) == 0;
    }

    @Override
    public int hashCode() {
        int result = maxAnchors;
        long bits = Double.doubleToLongBits(range);
        result = 31 * result + (int) (bits ^ bits >>> 32);
        bits = Double.doubleToLongBits(projectileSpeed);
        result = 31 * result + (int) (bits ^ bits >>> 32);
        bits = Double.doubleToLongBits(pullSpeed);
        result = 31 * result + (int) (bits ^ bits >>> 32);
        bits = Double.doubleToLongBits(retractSpeed);
        return 31 * result + (int) (bits ^ bits >>> 32);
    }
}
