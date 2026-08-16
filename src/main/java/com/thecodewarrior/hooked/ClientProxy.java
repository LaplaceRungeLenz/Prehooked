package com.thecodewarrior.hooked;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;

import com.thecodewarrior.hooked.client.ClientInputHandler;
import com.thecodewarrior.hooked.client.HookRenderHandler;
import com.thecodewarrior.hooked.client.HookedSelfTest;
import com.thecodewarrior.hooked.common.HookData;
import com.thecodewarrior.hooked.network.MessageHookSync;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public final class ClientProxy extends CommonProxy {

    private final Queue<MessageHookSync> pendingSync = new ConcurrentLinkedQueue<MessageHookSync>();

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        ClientInputHandler.register();
        MinecraftForge.EVENT_BUS.register(new HookRenderHandler());
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @Override
    public void loadComplete(FMLLoadCompleteEvent event) {
        HookedSelfTest.registerIfEnabled();
    }

    @Override
    public boolean isLocalPlayer(EntityPlayer player) {
        return player == Minecraft.getMinecraft().thePlayer;
    }

    @Override
    public String getFireKeyName() {
        return ClientInputHandler.getFireKeyName();
    }

    @Override
    public void enqueueSync(MessageHookSync message) {
        pendingSync.offer(message);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        MessageHookSync message;
        while ((message = pendingSync.poll()) != null) {
            if (minecraft.theWorld == null) {
                continue;
            }
            Entity entity = minecraft.theWorld.getEntityByID(message.getEntityId());
            if (entity instanceof EntityPlayer) {
                HookData.get((EntityPlayer) entity)
                    .applySnapshot(message);
            }
        }
    }
}
