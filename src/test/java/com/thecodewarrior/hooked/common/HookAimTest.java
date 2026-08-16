package com.thecodewarrior.hooked.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HookAimTest {

    private static final double EPSILON = 1.0E-9D;

    @Test
    public void chestLaunchConvergesOnTheEyeRay() {
        Vec3 chest = new Vec3(0.0D, 0.8D, 0.0D);
        Vec3 eye = new Vec3(0.0D, 1.6D, 0.0D);
        Vec3 look = new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 target = eye.add(look.scale(8.0D));

        Vec3 direction = HookAim.launchDirection(chest, eye, look, 8.0D, null);
        Vec3 reached = chest.add(
            direction.scale(
                target.subtract(chest)
                    .length()));

        assertTrue(direction.y > 0.0D);
        assertEquals(target.x, reached.x, EPSILON);
        assertEquals(target.y, reached.y, EPSILON);
        assertEquals(target.z, reached.z, EPSILON);
    }

    @Test
    public void closeBlockHitOverridesTheFallbackDistance() {
        Vec3 chest = new Vec3(0.0D, 0.8D, 0.0D);
        Vec3 eye = new Vec3(0.0D, 1.6D, 0.0D);
        Vec3 hit = new Vec3(0.0D, 1.6D, 0.5D);

        Vec3 direction = HookAim.launchDirection(chest, eye, new Vec3(0.0D, 0.0D, 1.0D), 8.0D, hit);
        Vec3 reached = chest.add(
            direction.scale(
                hit.subtract(chest)
                    .length()));

        assertEquals(hit.x, reached.x, EPSILON);
        assertEquals(hit.y, reached.y, EPSILON);
        assertEquals(hit.z, reached.z, EPSILON);
    }

    @Test
    public void identifiesOnlyHitsInsideTheCloseWallWindow() {
        Vec3 eye = new Vec3(0.5D, 66.62D, 0.5D);

        assertTrue(HookAim.isCloseSightHit(eye, new Vec3(0.5D, 66.62D, 1.0D), 1.0D));
        assertFalse(HookAim.isCloseSightHit(eye, new Vec3(0.5D, 66.62D, 1.5001D), 1.0D));
        assertFalse(HookAim.isCloseSightHit(eye, null, 1.0D));
    }
}
