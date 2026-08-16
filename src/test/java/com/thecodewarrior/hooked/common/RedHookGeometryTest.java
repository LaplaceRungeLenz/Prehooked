package com.thecodewarrior.hooked.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class RedHookGeometryTest {

    private static final double EPSILON = 1.0E-6D;

    @Test
    public void singleAnchorOnlyAllowsVerticalMovementBelowIt() {
        RedHookGeometry.Result result = RedHookGeometry
            .constrain(new Vec3(9.0D, 20.0D, -4.0D), Arrays.asList(new Vec3(1.0D, 10.0D, 2.0D)), 6.0D);

        assertVector(result.getPoint(), 1.0D, 10.0D, 2.0D);
        assertEquals(1.0D, result.getWeights()[0], EPSILON);
    }

    @Test
    public void twoAnchorsClampToTheirLineSegment() {
        List<Vec3> anchors = Arrays.asList(new Vec3(0.0D, 0.0D, 0.0D), new Vec3(10.0D, 0.0D, 0.0D));
        RedHookGeometry.Result result = RedHookGeometry.constrain(new Vec3(4.0D, 7.0D, 3.0D), anchors, 24.0D);

        assertVector(result.getPoint(), 4.0D, 0.0D, 0.0D);
        assertEquals(0.6D, result.getWeights()[0], EPSILON);
        assertEquals(0.4D, result.getWeights()[1], EPSILON);
    }

    @Test
    public void threeAnchorsClampToTriangleSurface() {
        List<Vec3> anchors = Arrays
            .asList(new Vec3(0.0D, 0.0D, 0.0D), new Vec3(10.0D, 0.0D, 0.0D), new Vec3(0.0D, 10.0D, 0.0D));
        RedHookGeometry.Result result = RedHookGeometry.constrain(new Vec3(8.0D, 8.0D, 5.0D), anchors, 24.0D);

        assertVector(result.getPoint(), 5.0D, 5.0D, 0.0D);
        assertNormalizedNonNegative(result.getWeights());
    }

    @Test
    public void fourAnchorsKeepInteriorPointInTetrahedron() {
        List<Vec3> anchors = Arrays.asList(
            new Vec3(0.0D, 0.0D, 0.0D),
            new Vec3(10.0D, 0.0D, 0.0D),
            new Vec3(0.0D, 10.0D, 0.0D),
            new Vec3(0.0D, 0.0D, 10.0D));
        RedHookGeometry.Result result = RedHookGeometry.constrain(new Vec3(2.0D, 2.0D, 2.0D), anchors, 24.0D);

        assertVector(result.getPoint(), 2.0D, 2.0D, 2.0D);
        assertNormalizedNonNegative(result.getWeights());
    }

    @Test
    public void maximumRopeLengthAppliesToEveryAnchor() {
        List<Vec3> anchors = Arrays.asList(new Vec3(0.0D, 0.0D, 0.0D), new Vec3(20.0D, 0.0D, 0.0D));
        RedHookGeometry.Result result = RedHookGeometry.constrain(new Vec3(1.0D, 4.0D, 0.0D), anchors, 11.0D);

        for (Vec3 anchor : anchors) {
            assertTrue(
                result.getPoint()
                    .subtract(anchor)
                    .length() <= 11.0D + EPSILON);
        }
        assertNormalizedNonNegative(result.getWeights());
    }

    private static void assertNormalizedNonNegative(double[] weights) {
        double sum = 0.0D;
        for (double weight : weights) {
            assertTrue(weight >= -EPSILON);
            sum += weight;
        }
        assertEquals(1.0D, sum, EPSILON);
    }

    private static void assertVector(Vec3 vector, double x, double y, double z) {
        assertEquals(x, vector.x, EPSILON);
        assertEquals(y, vector.y, EPSILON);
        assertEquals(z, vector.z, EPSILON);
    }
}
