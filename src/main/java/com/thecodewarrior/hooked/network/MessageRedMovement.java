package com.thecodewarrior.hooked.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class MessageRedMovement implements IMessage {

    private float strafe;
    private float forward;
    private float vertical;

    public MessageRedMovement() {}

    public MessageRedMovement(float strafe, float forward, float vertical) {
        this.strafe = strafe;
        this.forward = forward;
        this.vertical = vertical;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        strafe = buffer.readFloat();
        forward = buffer.readFloat();
        vertical = buffer.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeFloat(strafe);
        buffer.writeFloat(forward);
        buffer.writeFloat(vertical);
    }

    public static final class Handler implements IMessageHandler<MessageRedMovement, IMessage> {

        @Override
        public IMessage onMessage(MessageRedMovement message, MessageContext context) {
            ServerActionQueue.enqueueRedMovement(
                context.getServerHandler().playerEntity,
                message.strafe,
                message.forward,
                message.vertical);
            return null;
        }
    }
}
