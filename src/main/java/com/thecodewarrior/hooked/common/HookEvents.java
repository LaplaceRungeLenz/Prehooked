package com.thecodewarrior.hooked.common;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.minecraftforge.event.entity.player.PlayerEvent.StartTracking;

import com.thecodewarrior.hooked.HookedConfig;
import com.thecodewarrior.hooked.network.HookNetwork;
import com.thecodewarrior.hooked.network.MessageConfigSync;
import com.thecodewarrior.hooked.network.ServerActionQueue;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public final class HookEvents {

    @SubscribeEvent
    public void onEntityConstructing(EntityConstructing event) {
        if (event.entity instanceof EntityPlayer && event.entity.getExtendedProperties(HookData.IDENTIFIER) == null) {
            event.entity.registerExtendedProperties(HookData.IDENTIFIER, new HookData());
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            HookData.get(event.player)
                .tick();
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ServerActionQueue.drain();
        }
    }

    @SubscribeEvent
    public void onBreakSpeed(BreakSpeed event) {
        if (!event.entityPlayer.onGround && HookData.get(event.entityPlayer)
            .hasPlantedHooks()) {
            event.newSpeed *= 5.0F;
        }
    }

    @SubscribeEvent
    public void onStartTracking(StartTracking event) {
        if (event.target instanceof EntityPlayer && event.entityPlayer instanceof EntityPlayerMP) {
            HookData.get((EntityPlayer) event.target)
                .syncTo((EntityPlayerMP) event.entityPlayer);
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            syncSettingsTo((EntityPlayerMP) event.player);
            HookData.get(event.player)
                .syncTo((EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onRespawn(PlayerRespawnEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            syncSettingsTo((EntityPlayerMP) event.player);
            HookData.get(event.player)
                .syncTo((EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onChangedDimension(PlayerChangedDimensionEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            HookData.get(event.player)
                .retractAll(false);
        }
    }

    private static void syncSettingsTo(EntityPlayerMP player) {
        HookNetwork.CHANNEL.sendTo(new MessageConfigSync(HookedConfig.getLocalSettings()), player);
    }
}
