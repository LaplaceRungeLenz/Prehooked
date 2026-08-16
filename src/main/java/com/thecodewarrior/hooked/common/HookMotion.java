package com.thecodewarrior.hooked.common;

/** Shared motion calculations for releasing planted hooks. */
public final class HookMotion {

    public static final double VANILLA_JUMP_SPEED = 0.41999998688697815D;
    /** Reaches an apex of approximately two blocks under vanilla 1.7.10 gravity. */
    public static final double BASE_RELEASE_JUMP_SPEED = 0.55D;
    private static final double MOMENTUM_MULTIPLIER = 1.25D;
    private static final double MOMENTUM_LIFT_FACTOR = 0.30D;
    private static final double MAX_MOMENTUM_LIFT = 0.45D;
    private static final double MAX_UPWARD_SPEED = 2.50D;

    private HookMotion() {}

    public static Vec3 releaseVelocity(Vec3 suppliedMotion, double baseJumpSpeed) {
        Vec3 motion = suppliedMotion == null ? Vec3.ZERO : suppliedMotion;
        double x = finiteOrZero(motion.x);
        double y = finiteOrZero(motion.y);
        double z = finiteOrZero(motion.z);
        double jump = Math.max(0.0D, finiteOrZero(baseJumpSpeed));
        double speed = Math.sqrt(x * x + y * y + z * z);
        double momentumLift = Math.min(MAX_MOMENTUM_LIFT, speed * MOMENTUM_LIFT_FACTOR);
        double upward = Math.max(jump + momentumLift, Math.max(0.0D, y) * MOMENTUM_MULTIPLIER);

        return new Vec3(x * MOMENTUM_MULTIPLIER, Math.min(MAX_UPWARD_SPEED, upward), z * MOMENTUM_MULTIPLIER);
    }

    private static double finiteOrZero(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) ? 0.0D : value;
    }
}
