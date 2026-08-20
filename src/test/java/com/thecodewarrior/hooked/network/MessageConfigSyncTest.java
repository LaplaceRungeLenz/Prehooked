package com.thecodewarrior.hooked.network;

import static org.junit.Assert.assertEquals;

import java.util.EnumMap;
import java.util.Map;

import org.junit.Test;

import com.thecodewarrior.hooked.common.HookSettings;
import com.thecodewarrior.hooked.common.HookStats;
import com.thecodewarrior.hooked.common.HookType;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class MessageConfigSyncTest {

    @Test
    public void packetRoundTripPreservesAuthoritativeSettings() {
        Map<HookType, HookStats> stats = new EnumMap<HookType, HookStats>(HookType.class);
        int index = 1;
        for (HookType type : HookType.values()) {
            stats.put(type, new HookStats(index, index * 10.0D, index, index + 0.25D, index + 0.5D));
            index++;
        }
        HookSettings source = new HookSettings(HookSettings.MAX_SEARCH_LOCATIONS, false, stats);

        ByteBuf buffer = Unpooled.buffer();
        new MessageConfigSync(source).toBytes(buffer);
        MessageConfigSync restored = new MessageConfigSync();
        restored.fromBytes(buffer);

        assertEquals(source, restored.getSettings());
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void unknownProtocolFallsBackSafelyAndConsumesPayload() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(99);
        buffer.writeLong(123L);
        MessageConfigSync restored = new MessageConfigSync();

        restored.fromBytes(buffer);

        assertEquals(HookSettings.defaults(), restored.getSettings());
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void emptyPayloadFallsBackSafely() {
        ByteBuf buffer = Unpooled.buffer();
        MessageConfigSync restored = new MessageConfigSync();

        restored.fromBytes(buffer);

        assertEquals(HookSettings.defaults(), restored.getSettings());
        assertEquals(0, buffer.readableBytes());
    }
}
