package com.thecodewarrior.hooked.common;

/** Shared launch geometry that makes a chest-fired hook converge on the crosshair. */
public final class HookAim {

    private HookAim() {}

    /**
     * Returns the direction from {@code launchPosition} to the point selected by
     * the eye ray. When the eye ray has no block hit, it converges on a point at
     * {@code aimDistance} instead of remaining parallel below the crosshair.
     */
    public static Vec3 launchDirection(Vec3 launchPosition, Vec3 eyePosition, Vec3 lookDirection, double aimDistance,
        Vec3 hitPosition) {
        Vec3 launch = launchPosition == null ? Vec3.ZERO : launchPosition;
        Vec3 eye = eyePosition == null ? launch : eyePosition;
        Vec3 look = lookDirection == null ? Vec3.ZERO : lookDirection.normalize();
        if (look.lengthSquared() < 0.99D) {
            return Vec3.ZERO;
        }

        double distance = Double.isNaN(aimDistance) || Double.isInfinite(aimDistance) ? 0.0D
            : Math.max(0.0D, aimDistance);
        Vec3 target = hitPosition == null ? eye.add(look.scale(distance)) : hitPosition;
        Vec3 launchDirection = target.subtract(launch)
            .normalize();
        return launchDirection.lengthSquared() < 0.99D ? look : launchDirection;
    }

    /** Returns whether an eye-ray hit is close enough to be treated as a touching wall. */
    public static boolean isCloseSightHit(Vec3 eyePosition, Vec3 hitPosition, double closeSightDistance) {
        double closeDistance = finiteNonNegative(closeSightDistance);
        return eyePosition != null && hitPosition != null
            && hitPosition.distanceSquared(eyePosition) <= closeDistance * closeDistance;
    }

    private static double finiteNonNegative(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) ? 0.0D : Math.max(0.0D, value);
    }
}
