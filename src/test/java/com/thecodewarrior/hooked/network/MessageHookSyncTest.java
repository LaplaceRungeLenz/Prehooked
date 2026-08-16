package com.thecodewarrior.hooked.network;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;

import com.thecodewarrior.hooked.common.HookAnchor;
import com.thecodewarrior.hooked.common.HookStatus;
import com.thecodewarrior.hooked.common.HookType;
import com.thecodewarrior.hooked.common.Vec3;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class MessageHookSyncTest {

    private static final double EPSILON = 1.0E-9D;

    @Test
    public void packetRoundTripPreservesHookState() throws Exception {
        MessageHookSync source = new MessageHookSync();
        set(source, "entityId", 42);
        set(source, "hookTypeOrdinal", HookType.RED.ordinal());
        set(source, "redVerticalOffset", 3.25D);

        HookAnchor hook = new HookAnchor(
            UUID.fromString("00112233-4455-6677-8899-aabbccddeeff"),
            new Vec3(1.0D, 2.0D, 3.0D),
            new Vec3(0.0D, -2.0D, 0.0D),
            HookStatus.EXTENDING);
        hook.plant(12, 70, -5, ForgeDirection.UP, new Vec3(12.25D, 71.0D, -4.75D));
        hook.setWeight(0.6D);
        source.getHooks()
            .add(hook);

        ByteBuf buffer = Unpooled.buffer();
        source.toBytes(buffer);
        MessageHookSync restored = new MessageHookSync();
        restored.fromBytes(buffer);

        assertEquals(42, restored.getEntityId());
        assertEquals(HookType.RED.ordinal(), restored.getHookTypeOrdinal());
        assertEquals(3.25D, restored.getRedVerticalOffset(), EPSILON);
        assertEquals(
            1,
            restored.getHooks()
                .size());
        HookAnchor decoded = restored.getHooks()
            .get(0);
        assertEquals(hook.getId(), decoded.getId());
        assertEquals(HookStatus.PLANTED, decoded.getStatus());
        assertEquals(12, decoded.getBlockX());
        assertEquals(70, decoded.getBlockY());
        assertEquals(-5, decoded.getBlockZ());
        assertEquals(ForgeDirection.UP, decoded.getSide());
        assertEquals(0.6D, decoded.getWeight(), EPSILON);
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void encoderCapsSnapshotsAtProtocolMaximum() {
        MessageHookSync source = new MessageHookSync();
        List<HookAnchor> hooks = source.getHooks();
        for (int index = 0; index < 20; index++) {
            hooks.add(
                new HookAnchor(
                    new UUID(0L, index),
                    new Vec3(index, 0.0D, 0.0D),
                    new Vec3(1.0D, 0.0D, 0.0D),
                    HookStatus.EXTENDING));
        }

        ByteBuf buffer = Unpooled.buffer();
        source.toBytes(buffer);
        MessageHookSync restored = new MessageHookSync();
        restored.fromBytes(buffer);

        assertEquals(
            16,
            restored.getHooks()
                .size());
        assertEquals(0, buffer.readableBytes());
    }

    private static void set(Object object, String name, Object value) throws Exception {
        Field field = object.getClass()
            .getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }
}
