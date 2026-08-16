package com.thecodewarrior.hooked.common;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

public final class HookAnchor {

    private final UUID id;
    private Vec3 position;
    private final Vec3 direction;
    private HookStatus status;
    private int blockX;
    private int blockY;
    private int blockZ;
    private ForgeDirection side = ForgeDirection.UNKNOWN;
    private double weight = 1.0D;

    public HookAnchor(UUID id, Vec3 position, Vec3 direction, HookStatus status) {
        this.id = id;
        this.position = position;
        this.direction = direction.normalize();
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public Vec3 getPosition() {
        return position;
    }

    public void setPosition(Vec3 position) {
        this.position = position;
    }

    public Vec3 getDirection() {
        return direction;
    }

    public HookStatus getStatus() {
        return status;
    }

    public void setStatus(HookStatus status) {
        this.status = status;
    }

    public void plant(int x, int y, int z, ForgeDirection side, Vec3 position) {
        blockX = x;
        blockY = y;
        blockZ = z;
        this.side = side == null ? ForgeDirection.UNKNOWN : side;
        this.position = position;
        this.status = HookStatus.PLANTED;
    }

    public int getBlockX() {
        return blockX;
    }

    public int getBlockY() {
        return blockY;
    }

    public int getBlockZ() {
        return blockZ;
    }

    public ForgeDirection getSide() {
        return side;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = Double.isNaN(weight) || Double.isInfinite(weight) ? 1.0D : Math.max(0.0D, weight);
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("idMost", id.getMostSignificantBits());
        tag.setLong("idLeast", id.getLeastSignificantBits());
        tag.setTag("position", position.writeToNBT());
        tag.setTag("direction", direction.writeToNBT());
        tag.setByte("status", (byte) status.ordinal());
        tag.setInteger("blockX", blockX);
        tag.setInteger("blockY", blockY);
        tag.setInteger("blockZ", blockZ);
        tag.setByte("side", (byte) side.ordinal());
        tag.setDouble("weight", weight);
        return tag;
    }

    public static HookAnchor readFromNBT(NBTTagCompound tag) {
        HookAnchor anchor = new HookAnchor(
            new UUID(tag.getLong("idMost"), tag.getLong("idLeast")),
            Vec3.readFromNBT(tag.getCompoundTag("position")),
            Vec3.readFromNBT(tag.getCompoundTag("direction")),
            HookStatus.byOrdinal(tag.getByte("status")));
        anchor.blockX = tag.getInteger("blockX");
        anchor.blockY = tag.getInteger("blockY");
        anchor.blockZ = tag.getInteger("blockZ");
        anchor.side = ForgeDirection.getOrientation(tag.getByte("side"));
        anchor.setWeight(tag.hasKey("weight") ? tag.getDouble("weight") : 1.0D);
        return anchor;
    }
}
