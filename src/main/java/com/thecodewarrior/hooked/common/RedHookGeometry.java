package com.thecodewarrior.hooked.common;

import java.util.ArrayList;
import java.util.List;

/** Geometry used by the Red Hook's bounded-flight controller. */
public final class RedHookGeometry {

    private static final double EPSILON = 1.0E-9D;

    private RedHookGeometry() {}

    public static Result constrain(Vec3 target, List<Vec3> suppliedAnchors, double maximumLength) {
        int count = Math.min(4, suppliedAnchors == null ? 0 : suppliedAnchors.size());
        if (count == 0) {
            return new Result(target == null ? Vec3.ZERO : target.copy(), new double[0]);
        }

        List<Vec3> anchors = new ArrayList<Vec3>(count);
        for (int index = 0; index < count; index++) {
            anchors.add(suppliedAnchors.get(index));
        }
        Vec3 requested = target == null ? anchors.get(0) : target;
        double range = Math.max(0.0D, maximumLength);

        if (count == 1) {
            Vec3 anchor = anchors.get(0);
            double y = clamp(requested.y, anchor.y - range, anchor.y);
            return new Result(new Vec3(anchor.x, y, anchor.z), new double[] { 1.0D });
        }

        Projection projection = projectToHull(requested, anchors);
        for (int pass = 0; pass < 16; pass++) {
            Vec3 limited = limitToHookLength(projection.point, anchors, range);
            Projection corrected = projectToHull(limited, anchors);
            if (corrected.point.distanceSquared(projection.point) < EPSILON) {
                projection = corrected;
                break;
            }
            projection = corrected;
        }
        return new Result(projection.point, normalize(projection.weights));
    }

    private static Vec3 limitToHookLength(Vec3 point, List<Vec3> anchors, double range) {
        Vec3 result = point;
        for (int pass = 0; pass < 16; pass++) {
            boolean changed = false;
            for (Vec3 anchor : anchors) {
                Vec3 relative = result.subtract(anchor);
                double length = relative.length();
                if (length > range + 1.0E-7D && length > EPSILON) {
                    result = anchor.add(relative.scale(range / length));
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
        return result;
    }

    private static Projection projectToHull(Vec3 point, List<Vec3> anchors) {
        switch (anchors.size()) {
            case 2:
                return projectToSegment(point, anchors.get(0), anchors.get(1));
            case 3:
                return projectToTriangle(point, anchors.get(0), anchors.get(1), anchors.get(2));
            case 4:
                return projectToTetrahedron(point, anchors.get(0), anchors.get(1), anchors.get(2), anchors.get(3));
            default:
                return new Projection(anchors.get(0), new double[] { 1.0D });
        }
    }

    private static Projection projectToSegment(Vec3 point, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double denominator = ab.lengthSquared();
        if (denominator < EPSILON) {
            return new Projection(a, new double[] { 1.0D, 0.0D });
        }
        double t = clamp(
            point.subtract(a)
                .dot(ab) / denominator,
            0.0D,
            1.0D);
        return new Projection(a.add(ab.scale(t)), new double[] { 1.0D - t, t });
    }

    private static Projection projectToTriangle(Vec3 point, Vec3 a, Vec3 b, Vec3 c) {
        Vec3 ab = b.subtract(a);
        Vec3 ac = c.subtract(a);
        if (ab.cross(ac)
            .lengthSquared() < EPSILON) {
            return closestDegenerateTriangle(point, a, b, c);
        }

        Vec3 ap = point.subtract(a);
        double d1 = ab.dot(ap);
        double d2 = ac.dot(ap);
        if (d1 <= 0.0D && d2 <= 0.0D) {
            return new Projection(a, new double[] { 1.0D, 0.0D, 0.0D });
        }

        Vec3 bp = point.subtract(b);
        double d3 = ab.dot(bp);
        double d4 = ac.dot(bp);
        if (d3 >= 0.0D && d4 <= d3) {
            return new Projection(b, new double[] { 0.0D, 1.0D, 0.0D });
        }

        double vc = d1 * d4 - d3 * d2;
        if (vc <= 0.0D && d1 >= 0.0D && d3 <= 0.0D) {
            double v = d1 / (d1 - d3);
            return new Projection(a.add(ab.scale(v)), new double[] { 1.0D - v, v, 0.0D });
        }

        Vec3 cp = point.subtract(c);
        double d5 = ab.dot(cp);
        double d6 = ac.dot(cp);
        if (d6 >= 0.0D && d5 <= d6) {
            return new Projection(c, new double[] { 0.0D, 0.0D, 1.0D });
        }

        double vb = d5 * d2 - d1 * d6;
        if (vb <= 0.0D && d2 >= 0.0D && d6 <= 0.0D) {
            double w = d2 / (d2 - d6);
            return new Projection(a.add(ac.scale(w)), new double[] { 1.0D - w, 0.0D, w });
        }

        double va = d3 * d6 - d5 * d4;
        if (va <= 0.0D && d4 - d3 >= 0.0D && d5 - d6 >= 0.0D) {
            double w = (d4 - d3) / (d4 - d3 + d5 - d6);
            return new Projection(
                b.add(
                    c.subtract(b)
                        .scale(w)),
                new double[] { 0.0D, 1.0D - w, w });
        }

        double denominator = 1.0D / (va + vb + vc);
        double v = vb * denominator;
        double w = vc * denominator;
        double u = 1.0D - v - w;
        return new Projection(
            a.scale(u)
                .add(b.scale(v))
                .add(c.scale(w)),
            new double[] { u, v, w });
    }

    private static Projection closestDegenerateTriangle(Vec3 point, Vec3 a, Vec3 b, Vec3 c) {
        Projection ab = projectToSegment(point, a, b);
        Projection bc = projectToSegment(point, b, c);
        Projection ca = projectToSegment(point, c, a);
        Projection best = new Projection(ab.point, new double[] { ab.weights[0], ab.weights[1], 0.0D });
        double distance = best.point.distanceSquared(point);
        if (bc.point.distanceSquared(point) < distance) {
            best = new Projection(bc.point, new double[] { 0.0D, bc.weights[0], bc.weights[1] });
            distance = best.point.distanceSquared(point);
        }
        if (ca.point.distanceSquared(point) < distance) {
            best = new Projection(ca.point, new double[] { ca.weights[1], 0.0D, ca.weights[0] });
        }
        return best;
    }

    private static Projection projectToTetrahedron(Vec3 point, Vec3 a, Vec3 b, Vec3 c, Vec3 d) {
        double[] barycentric = tetrahedralWeights(point, a, b, c, d);
        if (barycentric != null && allNonNegative(barycentric)) {
            return new Projection(point, barycentric);
        }

        Projection best = mapTriangle(projectToTriangle(point, a, b, c), 0, 1, 2);
        double distance = best.point.distanceSquared(point);
        Projection candidate = mapTriangle(projectToTriangle(point, a, b, d), 0, 1, 3);
        if (candidate.point.distanceSquared(point) < distance) {
            best = candidate;
            distance = candidate.point.distanceSquared(point);
        }
        candidate = mapTriangle(projectToTriangle(point, a, c, d), 0, 2, 3);
        if (candidate.point.distanceSquared(point) < distance) {
            best = candidate;
            distance = candidate.point.distanceSquared(point);
        }
        candidate = mapTriangle(projectToTriangle(point, b, c, d), 1, 2, 3);
        if (candidate.point.distanceSquared(point) < distance) {
            best = candidate;
        }
        return best;
    }

    private static Projection mapTriangle(Projection triangle, int first, int second, int third) {
        double[] weights = new double[4];
        weights[first] = triangle.weights[0];
        weights[second] = triangle.weights[1];
        weights[third] = triangle.weights[2];
        return new Projection(triangle.point, weights);
    }

    private static double[] tetrahedralWeights(Vec3 point, Vec3 a, Vec3 b, Vec3 c, Vec3 d) {
        Vec3 ab = b.subtract(a);
        Vec3 ac = c.subtract(a);
        Vec3 ad = d.subtract(a);
        Vec3 ap = point.subtract(a);
        double denominator = scalarTriple(ab, ac, ad);
        if (Math.abs(denominator) < EPSILON) {
            return null;
        }
        double bWeight = scalarTriple(ap, ac, ad) / denominator;
        double cWeight = scalarTriple(ab, ap, ad) / denominator;
        double dWeight = scalarTriple(ab, ac, ap) / denominator;
        return new double[] { 1.0D - bWeight - cWeight - dWeight, bWeight, cWeight, dWeight };
    }

    private static double scalarTriple(Vec3 a, Vec3 b, Vec3 c) {
        return a.dot(b.cross(c));
    }

    private static boolean allNonNegative(double[] values) {
        for (double value : values) {
            if (value < -1.0E-7D) {
                return false;
            }
        }
        return true;
    }

    private static double[] normalize(double[] weights) {
        double total = 0.0D;
        for (int index = 0; index < weights.length; index++) {
            weights[index] = Math.max(0.0D, weights[index]);
            total += weights[index];
        }
        if (total < EPSILON && weights.length > 0) {
            weights[0] = 1.0D;
            total = 1.0D;
        }
        for (int index = 0; index < weights.length; index++) {
            weights[index] /= total;
        }
        return weights;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static final class Projection {

        private final Vec3 point;
        private final double[] weights;

        private Projection(Vec3 point, double[] weights) {
            this.point = point;
            this.weights = weights;
        }
    }

    public static final class Result {

        private final Vec3 point;
        private final double[] weights;

        private Result(Vec3 point, double[] weights) {
            this.point = point;
            this.weights = weights;
        }

        public Vec3 getPoint() {
            return point;
        }

        public double[] getWeights() {
            return weights.clone();
        }
    }
}
