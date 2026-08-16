package com.thecodewarrior.hooked.network;

import java.util.UUID;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class MessageRetractHook implements IMessage {

    private UUID id = new UUID(0L, 0L);

    public MessageRetractHook() {}

    public MessageRetractHook(UUID id) {
        this.id = id;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        id = new UUID(buffer.readLong(), buffer.readLong());
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeLong(id.getMostSignificantBits());
        buffer.writeLong(id.getLeastSignificantBits());
    }

    public static final class Handler implements IMessageHandler<MessageRetractHook, IMessage> {

        @Override
        public IMessage onMessage(MessageRetractHook message, MessageContext context) {
            ServerActionQueue.enqueueRetract(context.getServerHandler().playerEntity, message.id);
            return null;
        }
    }
}
