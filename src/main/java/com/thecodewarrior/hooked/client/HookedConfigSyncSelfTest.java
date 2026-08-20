package com.thecodewarrior.hooked.client;

import net.minecraft.client.Minecraft;

import com.thecodewarrior.hooked.HookedConfig;
import com.thecodewarrior.hooked.HookedMod;
import com.thecodewarrior.hooked.common.HookStats;
import com.thecodewarrior.hooked.common.HookType;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Opt-in remote-server configuration synchronization and disconnect test. */
@SideOnly(Side.CLIENT)
public final class HookedConfigSyncSelfTest {

    private static final HookedConfigSyncSelfTest INSTANCE = new HookedConfigSyncSelfTest();
    private static final int TIMEOUT_TICKS = 600;
    private static final double EPSILON = 1.0E-9D;

    private int stage;
    private int ticks;
    private int shutdownTicks;
    private boolean shutdownRequested;

    private HookedConfigSyncSelfTest() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean("hooked.configSyncTest")) {
            return;
        }
        Minecraft.getMinecraft().gameSettings.pauseOnLostFocus = false;
        HookedMod.LOG.warn("HOOKED_CONFIG_SYNC_TEST enabled; a remote test server connection is required");
        FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (shutdownRequested) {
            if (++shutdownTicks >= 20) {
                minecraft.shutdown();
            }
            return;
        }
        if (++ticks > TIMEOUT_TICKS) {
            fail("timed out at stage " + stage);
            return;
        }

        try {
            if (stage == 0) {
                if (minecraft.theWorld == null || !HookedConfig.hasServerSettings()) {
                    return;
                }
                verifyServerSettings();
                HookedMod.LOG.info("HOOKED_CONFIG_SYNC_TEST authoritative settings received");
                minecraft.theWorld.sendQuittingDisconnectingPacket();
                minecraft.loadWorld(null);
                stage = 1;
                ticks = 0;
                return;
            }
            if (stage == 1) {
                if (HookedConfig.hasServerSettings()) {
                    return;
                }
                verifyLocalSettings();
                HookedMod.LOG.info(
                    "HOOKED_CONFIG_SYNC_TEST_PASS serverSearch={} localSearch={} disconnectRestore=true",
                    integerProperty("hooked.configSyncTest.serverSearchLocations", 15),
                    integerProperty("hooked.configSyncTest.localSearchLocations", 1));
                shutdownRequested = true;
            }
        } catch (Throwable throwable) {
            HookedMod.LOG.error("HOOKED_CONFIG_SYNC_TEST_FAIL stage {}", stage, throwable);
            shutdownRequested = true;
        }
    }

    private static void verifyServerSettings() {
        require(
            HookedConfig.getSearchLocations() == integerProperty("hooked.configSyncTest.serverSearchLocations", 15),
            "server searchLocations was not applied");
        require(
            HookedConfig.isRedHookFlightEnabled()
                == Boolean.parseBoolean(System.getProperty("hooked.configSyncTest.serverRedFlight", "false")),
            "server Red Hook flight setting was not applied");
        verifyWoodStats("server", new HookStats(3, 12.0D, 0.75D, 0.35D, 0.65D));
    }

    private static void verifyLocalSettings() {
        require(
            HookedConfig.getSearchLocations() == integerProperty("hooked.configSyncTest.localSearchLocations", 1),
            "local searchLocations was not restored");
        require(
            HookedConfig.isRedHookFlightEnabled()
                == Boolean.parseBoolean(System.getProperty("hooked.configSyncTest.localRedFlight", "true")),
            "local Red Hook flight setting was not restored");
        verifyWoodStats("local", HookType.WOOD.getDefaultStats());
    }

    private static void verifyWoodStats(String prefix, HookStats fallback) {
        HookStats actual = HookedConfig.getHookStats(HookType.WOOD);
        require(
            actual.getMaxAnchors()
                == integerProperty("hooked.configSyncTest." + prefix + "WoodAnchors", fallback.getMaxAnchors()),
            prefix + " Wood maxAnchors did not match");
        requireClose(
            actual.getRange(),
            doubleProperty("hooked.configSyncTest." + prefix + "WoodRange", fallback.getRange()),
            prefix + " Wood range did not match");
        requireClose(
            actual.getProjectileSpeed(),
            doubleProperty("hooked.configSyncTest." + prefix + "WoodProjectileSpeed", fallback.getProjectileSpeed()),
            prefix + " Wood projectile speed did not match");
        requireClose(
            actual.getPullSpeed(),
            doubleProperty("hooked.configSyncTest." + prefix + "WoodPullSpeed", fallback.getPullSpeed()),
            prefix + " Wood pull speed did not match");
        requireClose(
            actual.getRetractSpeed(),
            doubleProperty("hooked.configSyncTest." + prefix + "WoodRetractSpeed", fallback.getRetractSpeed()),
            prefix + " Wood retract speed did not match");
    }

    private static int integerProperty(String name, int fallback) {
        return Integer.parseInt(System.getProperty(name, Integer.toString(fallback)));
    }

    private static double doubleProperty(String name, double fallback) {
        return Double.parseDouble(System.getProperty(name, Double.toString(fallback)));
    }

    private void fail(String message) {
        HookedMod.LOG.error("HOOKED_CONFIG_SYNC_TEST_FAIL stage {}: {}", stage, message);
        shutdownRequested = true;
    }

    private static void requireClose(double actual, double expected, String message) {
        require(Math.abs(actual - expected) <= EPSILON, message + ": expected " + expected + ", got " + actual);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
