package com.thecodewarrior.hooked;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;

import com.thecodewarrior.hooked.common.HookEvents;
import com.thecodewarrior.hooked.common.HookSettings;
import com.thecodewarrior.hooked.item.ModItems;
import com.thecodewarrior.hooked.network.HookNetwork;
import com.thecodewarrior.hooked.network.MessageHookSync;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        HookedConfig.load(event.getSuggestedConfigurationFile());
        ModItems.registerItems();
        HookNetwork.init();

        HookEvents events = new HookEvents();
        MinecraftForge.EVENT_BUS.register(events);
        FMLCommonHandler.instance()
            .bus()
            .register(events);

        HookedMod.LOG.info("Prehooked {} initialized for Minecraft 1.7.10", Tags.VERSION);
    }

    public void init(FMLInitializationEvent event) {
        ModItems.registerRecipes();
    }

    public void postInit(FMLPostInitializationEvent event) {}

    public void loadComplete(FMLLoadCompleteEvent event) {}

    public boolean isLocalPlayer(EntityPlayer player) {
        return false;
    }

    public String getFireKeyName() {
        return "C";
    }

    public void enqueueSync(MessageHookSync message) {}

    public void enqueueConfigSync(HookSettings settings) {}
}
