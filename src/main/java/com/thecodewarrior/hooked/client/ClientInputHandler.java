package com.thecodewarrior.hooked.client;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.input.Keyboard;

import com.thecodewarrior.hooked.common.HookAnchor;
import com.thecodewarrior.hooked.common.HookData;
import com.thecodewarrior.hooked.common.HookStatus;
import com.thecodewarrior.hooked.common.HookType;
import com.thecodewarrior.hooked.common.Vec3;
import com.thecodewarrior.hooked.item.ItemHook;
import com.thecodewarrior.hooked.network.HookNetwork;
import com.thecodewarrior.hooked.network.MessageFireHook;
import com.thecodewarrior.hooked.network.MessageRedMovement;
import com.thecodewarrior.hooked.network.MessageRetractAll;
import com.thecodewarrior.hooked.network.MessageRetractHook;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public final class ClientInputHandler {

    private static final ClientInputHandler INSTANCE = new ClientInputHandler();
    private static final KeyBinding FIRE = new KeyBinding("key.hooked.fire", Keyboard.KEY_C, "key.categories.movement");
    private boolean jumpWasDown;
    private int lastJumpTick = -100;
    private int clientTicks;

    private ClientInputHandler() {}

    public static void register() {
        ClientRegistry.registerKeyBinding(FIRE);
        FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
    }

    public static String getFireKeyName() {
        return Keyboard.getKeyName(FIRE.getKeyCode());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            handleJumpInput();
            return;
        }
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.thePlayer;
        if (player == null || minecraft.currentScreen != null || ItemHook.findUsableHook(player) == null) {
            return;
        }

        while (FIRE.isPressed()) {
            if (player.isSneaking()) {
                UUID target = findLookedAtHook(player, HookData.get(player));
                if (target != null) {
                    HookNetwork.CHANNEL.sendToServer(new MessageRetractHook(target));
                }
            } else {
                Vec3 look = Vec3.fromMinecraft(player.getLookVec());
                HookData.get(player)
                    .predictFire(look);
                HookNetwork.CHANNEL.sendToServer(new MessageFireHook(look));
            }
        }

        HookData currentData = HookData.get(player);
        if (currentData.getHookType() == HookType.RED && currentData.hasPlantedHooks()) {
            float vertical = (minecraft.gameSettings.keyBindJump.getIsKeyPressed() ? 1.0F : 0.0F)
                - (minecraft.gameSettings.keyBindSneak.getIsKeyPressed() ? 1.0F : 0.0F);
            float strafe = player.moveStrafing;
            float forward = player.moveForward;
            if (minecraft.gameSettings.keyBindSneak.getIsKeyPressed()) {
                strafe /= 0.3F;
                forward /= 0.3F;
            }
            if (vertical != 0.0F || strafe != 0.0F || forward != 0.0F) {
                // Apply the bounded movement immediately on the owning client. The
                // server performs the same constrained calculation authoritatively.
                currentData.moveRedHook(strafe, forward, vertical);
                HookNetwork.CHANNEL.sendToServer(new MessageRedMovement(strafe, forward, vertical));
            }
        }

    }

    private void handleJumpInput() {
        clientTicks++;
        Minecraft minecraft = Minecraft.getMinecraft();
        boolean jumpDown = minecraft.gameSettings.keyBindJump.getIsKeyPressed();
        EntityPlayer player = minecraft.thePlayer;
        if (player == null || minecraft.currentScreen != null || ItemHook.findUsableHook(player) == null) {
            jumpWasDown = jumpDown;
            return;
        }

        HookData currentData = HookData.get(player);
        if (jumpDown && !jumpWasDown) {
            boolean red = currentData.getHookType() == HookType.RED;
            if (!red || clientTicks - lastJumpTick <= 7) {
                currentData.predictRetractAll(true);
                HookNetwork.CHANNEL.sendToServer(new MessageRetractAll(true));
            }
            lastJumpTick = clientTicks;
        }
        jumpWasDown = jumpDown;
    }

    private static UUID findLookedAtHook(EntityPlayer player, HookData data) {
        Vec3 eye = new Vec3(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3 look = Vec3.fromMinecraft(player.getLookVec())
            .normalize();
        double minimumCosine = Math.cos(Math.toRadians(10.0D));
        HookAnchor best = null;
        double bestCosine = minimumCosine;
        for (HookAnchor hook : data.getHooks()) {
            if (hook.getStatus() == HookStatus.RETRACTING) {
                continue;
            }
            double cosine = hook.getPosition()
                .subtract(eye)
                .normalize()
                .dot(look);
            if (cosine > bestCosine) {
                bestCosine = cosine;
                best = hook;
            }
        }
        return best == null ? null : best.getId();
    }
}
