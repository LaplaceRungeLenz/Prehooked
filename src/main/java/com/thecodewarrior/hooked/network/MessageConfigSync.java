package com.thecodewarrior.hooked.network;

import java.util.EnumMap;
import java.util.Map;

import com.thecodewarrior.hooked.HookedMod;
import com.thecodewarrior.hooked.common.HookSettings;
import com.thecodewarrior.hooked.common.HookStats;
import com.thecodewarrior.hooked.common.HookType;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Sends authoritative gameplay settings from a server to its clients. */
public final class MessageConfigSync implements IMessage {

    private static final int PROTOCOL_VERSION = 1;
    private static final int RECORD_BYTES = 34;

    private HookSettings settings = HookSettings.defaults();

    public MessageConfigSync() {}

    public MessageConfigSync(HookSettings settings) {
        this.settings = settings == null ? HookSettings.defaults() : settings;
    }

    public HookSettings getSettings() {
        return settings;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        if (!buffer.isReadable()) {
            settings = HookSettings.defaults();
            return;
        }
        int version = buffer.readUnsignedByte();
        if (version != PROTOCOL_VERSION || buffer.readableBytes() < 3) {
            buffer.skipBytes(buffer.readableBytes());
            settings = HookSettings.defaults();
            return;
        }

        int searchLocations = buffer.readUnsignedByte();
        boolean redHookFlightEnabled = buffer.readBoolean();
        int count = buffer.readUnsignedByte();
        Map<HookType, HookStats> stats = new EnumMap<HookType, HookStats>(HookType.class);
        for (int index = 0; index < count && buffer.readableBytes() >= RECORD_BYTES; index++) {
            HookType type = HookType.byOrdinal(buffer.readUnsignedByte());
            int maxAnchors = buffer.readUnsignedByte();
            HookStats decoded = new HookStats(
                maxAnchors,
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble());
            if (type != null) {
                stats.put(type, decoded);
            }
        }
        buffer.skipBytes(buffer.readableBytes());
        settings = new HookSettings(searchLocations, redHookFlightEnabled, stats);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeByte(PROTOCOL_VERSION);
        buffer.writeByte(settings.getSearchLocations());
        buffer.writeBoolean(settings.isRedHookFlightEnabled());
        HookType[] types = HookType.values();
        buffer.writeByte(types.length);
        for (HookType type : types) {
            HookStats stats = settings.getStats(type);
            buffer.writeByte(type.ordinal());
            buffer.writeByte(stats.getMaxAnchors());
            buffer.writeDouble(stats.getRange());
            buffer.writeDouble(stats.getProjectileSpeed());
            buffer.writeDouble(stats.getPullSpeed());
            buffer.writeDouble(stats.getRetractSpeed());
        }
    }

    public static final class Handler implements IMessageHandler<MessageConfigSync, IMessage> {

        @Override
        public IMessage onMessage(MessageConfigSync message, MessageContext context) {
            HookedMod.proxy.enqueueConfigSync(message.getSettings());
            return null;
        }
    }
}
