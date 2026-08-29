package com.thecodewarrior.hooked.common;

/** Shared motion calculations for releasing planted hooks. */
public final class HookMotion {

    public static final double VANILLA_JUMP_SPEED = 0.41999998688697815D;
    /** Reaches an apex of approximately 1 block under vanilla 1.7.10 gravity. */
    public static final double BASE_RELEASE_JUMP_SPEED = 0.37D;
    private static final double STALLED_HORIZONTAL_SPEED_SQUARED = 1.0E-6D;
    private static final double UNFINISHED_HORIZONTAL_PULL_SQUARED = 1.0E-4D;
    private static final double MOMENTUM_MULTIPLIER = 1.25D;
    private static final double MOMENTUM_LIFT_FACTOR = 0.30D;
    private static final double MAX_MOMENTUM_LIFT = 0.45D;
    private static final double MAX_UPWARD_SPEED = 2.50D;

    private HookMotion() {}

    public static Vec3 releaseVelocity(Vec3 suppliedMotion, double baseJumpSpeed) {
        return releaseVelocity(suppliedMotion, baseJumpSpeed, true);
    }

    public static Vec3 releaseVelocity(Vec3 suppliedMotion, double baseJumpSpeed, boolean preserveMomentum) {
        Vec3 motion = preserveMomentum && suppliedMotion != null ? suppliedMotion : Vec3.ZERO;
        double x = finiteOrZero(motion.x);
        double y = finiteOrZero(motion.y);
        double z = finiteOrZero(motion.z);
        double jump = Math.max(0.0D, finiteOrZero(baseJumpSpeed));
        double speed = Math.sqrt(x * x + y * y + z * z);
        double momentumLift = Math.min(MAX_MOMENTUM_LIFT, speed * MOMENTUM_LIFT_FACTOR);
        double upward = Math.max(jump + momentumLift, Math.max(0.0D, y) * MOMENTUM_MULTIPLIER);

        return new Vec3(x * MOMENTUM_MULTIPLIER, Math.min(MAX_UPWARD_SPEED, upward), z * MOMENTUM_MULTIPLIER);
    }

    static boolean isPullStalled(Vec3 actualMotion, Vec3 remainingPull, boolean collidedHorizontally) {
        if (!collidedHorizontally || actualMotion == null || remainingPull == null) {
            return false;
        }
        double horizontalSpeedSquared = actualMotion.x * actualMotion.x + actualMotion.z * actualMotion.z;
        double horizontalPullSquared = remainingPull.x * remainingPull.x + remainingPull.z * remainingPull.z;
        return horizontalSpeedSquared <= STALLED_HORIZONTAL_SPEED_SQUARED
            && horizontalPullSquared > UNFINISHED_HORIZONTAL_PULL_SQUARED;
    }

    private static double finiteOrZero(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) ? 0.0D : value;
    }
}
