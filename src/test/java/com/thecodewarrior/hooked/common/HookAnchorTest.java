package com.thecodewarrior.hooked.common;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;

public class HookAnchorTest {

    private static final double EPSILON = 1.0E-9D;

    @Test
    public void nbtRoundTripPreservesAPlantedAnchor() {
        UUID id = UUID.fromString("10203040-5060-7080-90a0-b0c0d0e0f000");
        HookAnchor original = new HookAnchor(
            id,
            new Vec3(1.25D, 2.5D, -3.75D),
            new Vec3(4.0D, 0.0D, 3.0D),
            HookStatus.EXTENDING);
        original.plant(10, 64, -20, ForgeDirection.NORTH, new Vec3(9.5D, 64.25D, -19.0D));
        original.setWeight(0.375D);

        HookAnchor restored = HookAnchor.readFromNBT(original.writeToNBT());

        assertEquals(id, restored.getId());
        assertVector(restored.getPosition(), 9.5D, 64.25D, -19.0D);
        assertVector(restored.getDirection(), 0.8D, 0.0D, 0.6D);
        assertEquals(HookStatus.PLANTED, restored.getStatus());
        assertEquals(10, restored.getBlockX());
        assertEquals(64, restored.getBlockY());
        assertEquals(-20, restored.getBlockZ());
        assertEquals(ForgeDirection.NORTH, restored.getSide());
        assertEquals(0.375D, restored.getWeight(), EPSILON);
    }

    @Test
    public void malformedFieldsFallBackToSafeValues() {
        NBTTagCompound tag = new HookAnchor(
            UUID.randomUUID(),
            new Vec3(0.0D, 0.0D, 0.0D),
            new Vec3(0.0D, 1.0D, 0.0D),
            HookStatus.PLANTED).writeToNBT();
        tag.setByte("status", (byte) 120);
        tag.setByte("side", (byte) 120);
        tag.setDouble("weight", Double.NaN);

        HookAnchor restored = HookAnchor.readFromNBT(tag);

        assertEquals(HookStatus.RETRACTING, restored.getStatus());
        assertEquals(ForgeDirection.UNKNOWN, restored.getSide());
        assertEquals(1.0D, restored.getWeight(), EPSILON);
    }

    private static void assertVector(Vec3 vector, double x, double y, double z) {
        assertEquals(x, vector.x, EPSILON);
        assertEquals(y, vector.y, EPSILON);
        assertEquals(z, vector.z, EPSILON);
    }
}
