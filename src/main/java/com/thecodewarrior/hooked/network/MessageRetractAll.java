package com.thecodewarrior.hooked.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class MessageRetractAll implements IMessage {

    private boolean boost;

    public MessageRetractAll() {}

    public MessageRetractAll(boolean boost) {
        this.boost = boost;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        boost = buffer.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeBoolean(boost);
    }

    public static final class Handler implements IMessageHandler<MessageRetractAll, IMessage> {

        @Override
        public IMessage onMessage(MessageRetractAll message, MessageContext context) {
            ServerActionQueue.enqueueRetractAll(context.getServerHandler().playerEntity, message.boost);
            return null;
        }
    }
}
