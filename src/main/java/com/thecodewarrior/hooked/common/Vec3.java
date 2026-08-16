package com.thecodewarrior.hooked.common;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;

public final class Vec3 {

    public static final Vec3 ZERO = new Vec3(0.0D, 0.0D, 0.0D);

    public double x;
    public double y;
    public double z;

    public Vec3(double x, double y, double z) {
        this.x = finite(x);
        this.y = finite(y);
        this.z = finite(z);
    }

    public static Vec3 fromMinecraft(net.minecraft.util.Vec3 vector) {
        return new Vec3(vector.xCoord, vector.yCoord, vector.zCoord);
    }

    public static Vec3 waist(Entity entity) {
        return new Vec3(entity.posX, entity.posY + entity.getEyeHeight() * 0.5D, entity.posZ);
    }

    public Vec3 add(Vec3 other) {
        return new Vec3(x + other.x, y + other.y, z + other.z);
    }

    public Vec3 subtract(Vec3 other) {
        return new Vec3(x - other.x, y - other.y, z - other.z);
    }

    public Vec3 scale(double scalar) {
        return new Vec3(x * scalar, y * scalar, z * scalar);
    }

    public double dot(Vec3 other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public Vec3 cross(Vec3 other) {
        return new Vec3(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x);
    }

    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public double distanceSquared(Vec3 other) {
        return subtract(other).lengthSquared();
    }

    public Vec3 normalize() {
        double length = length();
        return length < 1.0E-8D ? ZERO : scale(1.0D / length);
    }

    public Vec3 clampLength(double maximum) {
        double length = length();
        if (length <= maximum || length < 1.0E-8D) {
            return this;
        }
        return scale(maximum / length);
    }

    public Vec3 copy() {
        return new Vec3(x, y, z);
    }

    public net.minecraft.util.Vec3 toMinecraft() {
        return net.minecraft.util.Vec3.createVectorHelper(x, y, z);
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setDouble("x", x);
        tag.setDouble("y", y);
        tag.setDouble("z", z);
        return tag;
    }

    public static Vec3 readFromNBT(NBTTagCompound tag) {
        return new Vec3(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"));
    }

    private static double finite(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) ? 0.0D : value;
    }
}
