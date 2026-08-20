package com.thecodewarrior.hooked.network;

import com.thecodewarrior.hooked.HookedMod;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class HookNetwork {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(HookedMod.MODID);

    private HookNetwork() {}

    public static void init() {
        int discriminator = 0;
        CHANNEL.registerMessage(MessageFireHook.Handler.class, MessageFireHook.class, discriminator++, Side.SERVER);
        CHANNEL
            .registerMessage(MessageRetractHook.Handler.class, MessageRetractHook.class, discriminator++, Side.SERVER);
        CHANNEL.registerMessage(MessageRetractAll.Handler.class, MessageRetractAll.class, discriminator++, Side.SERVER);
        CHANNEL
            .registerMessage(MessageRedMovement.Handler.class, MessageRedMovement.class, discriminator++, Side.SERVER);
        CHANNEL.registerMessage(MessageHookSync.Handler.class, MessageHookSync.class, discriminator++, Side.CLIENT);
        CHANNEL.registerMessage(MessageConfigSync.Handler.class, MessageConfigSync.class, discriminator, Side.CLIENT);
    }
}
