package com.thecodewarrior.hooked.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.ForgeDirection;

import com.thecodewarrior.hooked.HookedMod;
import com.thecodewarrior.hooked.common.HookAnchor;
import com.thecodewarrior.hooked.common.HookData;
import com.thecodewarrior.hooked.common.HookStats;
import com.thecodewarrior.hooked.common.HookStatus;
import com.thecodewarrior.hooked.common.Vec3;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class MessageHookSync implements IMessage {

    private static final int MAX_HOOKS = HookStats.MAX_ANCHORS;

    private int entityId;
    private int hookTypeOrdinal = -1;
    private double redVerticalOffset;
    private final List<HookAnchor> hooks = new ArrayList<HookAnchor>();

    public MessageHookSync() {}

    public static MessageHookSync fromPlayer(EntityPlayer player, HookData data) {
        MessageHookSync message = new MessageHookSync();
        message.entityId = player.getEntityId();
        message.hookTypeOrdinal = data.getHookType() == null ? -1
            : data.getHookType()
                .ordinal();
        message.redVerticalOffset = data.getRedVerticalOffset();
        for (HookAnchor hook : data.getHooks()) {
            HookAnchor copy = new HookAnchor(
                hook.getId(),
                hook.getPosition()
                    .copy(),
                hook.getDirection()
                    .copy(),
                hook.getStatus());
            copy.plant(
                hook.getBlockX(),
                hook.getBlockY(),
                hook.getBlockZ(),
                hook.getSide(),
                hook.getPosition()
                    .copy());
            copy.setStatus(hook.getStatus());
            copy.setWeight(hook.getWeight());
            message.hooks.add(copy);
        }
        return message;
    }

    public int getEntityId() {
        return entityId;
    }

    public int getHookTypeOrdinal() {
        return hookTypeOrdinal;
    }

    public List<HookAnchor> getHooks() {
        return hooks;
    }

    public double getRedVerticalOffset() {
        return redVerticalOffset;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        entityId = buffer.readInt();
        hookTypeOrdinal = buffer.readByte();
        redVerticalOffset = buffer.readDouble();
        hooks.clear();
        int count = Math.min(buffer.readUnsignedByte(), MAX_HOOKS);
        for (int index = 0; index < count; index++) {
            UUID id = new UUID(buffer.readLong(), buffer.readLong());
            Vec3 position = readVec(buffer);
            Vec3 direction = readVec(buffer);
            HookStatus status = HookStatus.byOrdinal(buffer.readUnsignedByte());
            HookAnchor hook = new HookAnchor(id, position, direction, status);
            int blockX = buffer.readInt();
            int blockY = buffer.readInt();
            int blockZ = buffer.readInt();
            ForgeDirection side = ForgeDirection.getOrientation(buffer.readByte());
            hook.plant(blockX, blockY, blockZ, side, position);
            hook.setStatus(status);
            hook.setWeight(buffer.readDouble());
            hooks.add(hook);
        }
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(entityId);
        buffer.writeByte(hookTypeOrdinal);
        buffer.writeDouble(redVerticalOffset);
        int count = Math.min(MAX_HOOKS, hooks.size());
        buffer.writeByte(count);
        for (int index = 0; index < count; index++) {
            HookAnchor hook = hooks.get(index);
            buffer.writeLong(
                hook.getId()
                    .getMostSignificantBits());
            buffer.writeLong(
                hook.getId()
                    .getLeastSignificantBits());
            writeVec(buffer, hook.getPosition());
            writeVec(buffer, hook.getDirection());
            buffer.writeByte(
                hook.getStatus()
                    .ordinal());
            buffer.writeInt(hook.getBlockX());
            buffer.writeInt(hook.getBlockY());
            buffer.writeInt(hook.getBlockZ());
            buffer.writeByte(
                hook.getSide()
                    .ordinal());
            buffer.writeDouble(hook.getWeight());
        }
    }

    private static Vec3 readVec(ByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    private static void writeVec(ByteBuf buffer, Vec3 vector) {
        buffer.writeDouble(vector.x);
        buffer.writeDouble(vector.y);
        buffer.writeDouble(vector.z);
    }

    public static final class Handler implements IMessageHandler<MessageHookSync, IMessage> {

        @Override
        public IMessage onMessage(MessageHookSync message, MessageContext context) {
            HookedMod.proxy.enqueueSync(message);
            return null;
        }
    }
}
