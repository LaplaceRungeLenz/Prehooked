package com.thecodewarrior.hooked.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.thecodewarrior.hooked.common.Vec3;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class MessageFireHook implements IMessage {

    private Vec3 direction = Vec3.ZERO;

    public MessageFireHook() {}

    public MessageFireHook(Vec3 direction) {
        this.direction = direction == null ? Vec3.ZERO : direction;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        direction = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeDouble(direction.x);
        buffer.writeDouble(direction.y);
        buffer.writeDouble(direction.z);
    }

    public static final class Handler implements IMessageHandler<MessageFireHook, IMessage> {

        @Override
        public IMessage onMessage(MessageFireHook message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            ServerActionQueue.enqueueFire(player, message.direction);
            return null;
        }
    }
}
