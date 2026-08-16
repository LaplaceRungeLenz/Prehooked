package com.thecodewarrior.hooked.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HookMotionTest {

    private static final double EPSILON = 1.0E-9D;

    @Test
    public void stationaryReleaseReachesApproximatelyTwoBlocks() {
        Vec3 result = HookMotion.releaseVelocity(Vec3.ZERO, HookMotion.BASE_RELEASE_JUMP_SPEED);

        assertEquals(0.0D, result.x, EPSILON);
        assertEquals(HookMotion.BASE_RELEASE_JUMP_SPEED, result.y, EPSILON);
        assertEquals(0.0D, result.z, EPSILON);

        double height = 0.0D;
        double velocity = result.y;
        while (velocity > 0.0D) {
            height += velocity;
            velocity = (velocity - 0.08D) * 0.98D;
        }
        assertTrue(height >= 1.95D && height <= 2.10D);
    }

    @Test
    public void existingMomentumRaisesAndCarriesThePlayer() {
        Vec3 forward = HookMotion.releaseVelocity(new Vec3(0.0D, 0.0D, 0.8D), HookMotion.BASE_RELEASE_JUMP_SPEED);
        Vec3 falling = HookMotion.releaseVelocity(new Vec3(0.0D, -1.0D, 0.0D), HookMotion.BASE_RELEASE_JUMP_SPEED);
        Vec3 rising = HookMotion.releaseVelocity(new Vec3(0.0D, 0.8D, 0.0D), HookMotion.BASE_RELEASE_JUMP_SPEED);

        assertEquals(1.0D, forward.z, EPSILON);
        assertTrue(forward.y > HookMotion.BASE_RELEASE_JUMP_SPEED);
        assertTrue(falling.y > HookMotion.BASE_RELEASE_JUMP_SPEED);
        assertEquals(1.0D, rising.y, EPSILON);
    }

    @Test
    public void extremeAndInvalidMotionRemainBoundedAndFinite() {
        Vec3 extreme = HookMotion.releaseVelocity(new Vec3(0.0D, 100.0D, 0.0D), HookMotion.BASE_RELEASE_JUMP_SPEED);
        Vec3 invalid = HookMotion.releaseVelocity(new Vec3(Double.NaN, Double.POSITIVE_INFINITY, 0.0D), Double.NaN);

        assertEquals(2.5D, extreme.y, EPSILON);
        assertEquals(0.0D, invalid.x, EPSILON);
        assertEquals(0.0D, invalid.y, EPSILON);
    }
}
