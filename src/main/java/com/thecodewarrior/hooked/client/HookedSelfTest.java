package com.thecodewarrior.hooked.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.thecodewarrior.hooked.HookedMod;
import com.thecodewarrior.hooked.common.HookAnchor;
import com.thecodewarrior.hooked.common.HookData;
import com.thecodewarrior.hooked.common.HookMotion;
import com.thecodewarrior.hooked.common.HookStatus;
import com.thecodewarrior.hooked.common.HookType;
import com.thecodewarrior.hooked.common.Vec3;
import com.thecodewarrior.hooked.item.ItemHook;
import com.thecodewarrior.hooked.item.ModItems;
import com.thecodewarrior.hooked.network.HookNetwork;
import com.thecodewarrior.hooked.network.MessageRedMovement;

import baubles.api.BaublesApi;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Opt-in, end-to-end validation for release engineering. It is inert unless the
 * {@code hooked.selfTest} JVM property is true.
 */
@SideOnly(Side.CLIENT)
public final class HookedSelfTest {

    private static final HookedSelfTest INSTANCE = new HookedSelfTest();
    private static final String CONFIGURED_WORLD = System.getProperty("hooked.selfTest.world", "")
        .trim();
    private static final String WORLD_NAME = CONFIGURED_WORLD.isEmpty()
        ? "Hooked-QA-" + Long.toHexString(System.currentTimeMillis())
        : CONFIGURED_WORLD;
    private static final int TIMEOUT_TICKS = 600;
    private static final int MIN_RED_PREDICTION_STEPS = 3;
    private static final int MAX_RED_PREDICTION_STEPS = 20;
    private static final boolean OPEN_TO_LAN = Boolean.getBoolean("hooked.selfTest.openLan");

    private boolean worldRequested;
    private volatile boolean finished;
    private volatile boolean shutdownRequested;
    private int shutdownTicks;
    private int step;
    private int stepTicks;
    private int baubleSlot = -1;
    private boolean closeWallChecked;
    private double startingZ;
    private double woodAimY;
    private double maxPullDistance;
    private double maxPullVelocity;
    private double redCenterY;
    private volatile double expectedClientReleaseY;
    private volatile boolean clientSawReleaseBoost;
    private volatile boolean verifyClientRedPrediction;
    private volatile boolean clientRedPredictionPassed;
    private volatile boolean clientRedMovementSent;
    private double clientRedMaximumStep;
    private double clientRedPredictedTravel;
    private int clientRedMovementSteps;
    private boolean clientRedMovementPrimed;
    private boolean lanOpened;
    private String lanPort = "closed";

    private HookedSelfTest() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean("hooked.selfTest")) {
            return;
        }
        Minecraft.getMinecraft().gameSettings.pauseOnLostFocus = false;
        HookedMod.LOG.warn(
            "HOOKED_SELF_TEST enabled; {} temporary world {}",
            CONFIGURED_WORLD.isEmpty() ? "creating" : "reusing",
            WORLD_NAME);
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
        if (expectedClientReleaseY > 0.0D && minecraft.thePlayer != null
            && minecraft.thePlayer.motionY > HookMotion.VANILLA_JUMP_SPEED * 0.5D) {
            clientSawReleaseBoost = true;
        }
        if (verifyClientRedPrediction && !clientRedPredictionPassed && minecraft.thePlayer != null) {
            verifyClientRedPrediction(minecraft);
        }
        if (worldRequested) {
            return;
        }
        if (minecraft.theWorld != null) {
            worldRequested = true;
            HookedMod.LOG.info("HOOKED_SELF_TEST using the already-open world");
            return;
        }
        worldRequested = true;
        WorldSettings settings = new WorldSettings(
            0x484F4F4B45445141L,
            WorldSettings.GameType.CREATIVE,
            true,
            false,
            WorldType.FLAT);
        minecraft.launchIntegratedServer(WORLD_NAME, WORLD_NAME, settings);
    }

    @SubscribeEvent
    @SuppressWarnings("rawtypes")
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !worldRequested || finished) {
            return;
        }
        try {
            MinecraftServer server = MinecraftServer.getServer();
            if (server == null || !server.isServerRunning()) {
                return;
            }
            if (OPEN_TO_LAN && !lanOpened) {
                require(server instanceof IntegratedServer, "LAN test did not start an integrated server");
                lanPort = server.shareToLAN(WorldSettings.GameType.CREATIVE, true);
                require(lanPort != null, "integrated server failed to open to LAN");
                lanOpened = true;
                HookedMod.LOG.info("HOOKED_SELF_TEST LAN host opened on port {}", lanPort);
            }
            List players = server.getConfigurationManager().playerEntityList;
            if (players.isEmpty() || !(players.get(0) instanceof EntityPlayerMP)) {
                return;
            }
            runStep((EntityPlayerMP) players.get(0));
        } catch (Throwable throwable) {
            HookedMod.LOG.error("HOOKED_SELF_TEST_FAIL step {}", step, throwable);
            finished = true;
            shutdownRequested = true;
        }
    }

    private void runStep(EntityPlayerMP player) {
        stepTicks++;
        if (stepTicks > TIMEOUT_TICKS) {
            fail("timed out at step " + step);
            return;
        }

        HookData data = HookData.get(player);
        switch (step) {
            case 0:
                if (stepTicks < 20) {
                    return;
                }
                if (stepTicks == 20) {
                    preparePlayer(player);
                    return;
                }
                if (stepTicks < 25) {
                    return;
                }
                data.retractAll(false);
                removeExistingHooks(player);
                ItemStack woodenHook = new ItemStack(ModItems.hook, 1, HookType.WOOD.ordinal());
                baubleSlot = equipBauble(player, woodenHook);
                require(baubleSlot >= 0, "no Baubles Expanded slot accepted the universal hook");
                require(ItemHook.findUsableHook(player) == woodenHook, "equipped hook was not discovered");
                require(
                    !ModItems.hook.canEquip(new ItemStack(ModItems.hook, 1, HookType.IRON.ordinal()), player),
                    "a second hook was accepted while one was equipped");
                if (!closeWallChecked) {
                    setCloseWall(player, true);
                    data.fire(new Vec3(0.0D, 0.0D, 1.0D));
                    require(data.hasPlantedHooks(), "wood hook did not plant against the close wall immediately");
                    require(
                        data.getHooks()
                            .get(0)
                            .getBlockZ() == 1,
                        "close-wall hook planted on an unexpected block");
                    data.retractAll(false);
                    setCloseWall(player, false);
                    closeWallChecked = true;
                    HookedMod.LOG.info("HOOKED_SELF_TEST close-wall fire passed");
                    return;
                }
                if (data.getCooldown() > 0) {
                    return;
                }
                startingZ = player.posZ;
                woodAimY = player.posY + player.getEyeHeight();
                data.fire(new Vec3(0.0D, 0.0D, 1.0D));
                require(
                    data.getHooks()
                        .size() == 1,
                    "wood hook was not created");
                HookedMod.LOG.info("HOOKED_SELF_TEST step 0 passed: Baubles slot {} and wood fire", baubleSlot);
                HookAnchor fired = data.getHooks()
                    .get(0);
                double expectedChestY = player.posY + player.getEyeHeight() * 0.5D;
                require(
                    Math.abs(fired.getPosition().y - expectedChestY) < 1.0E-6D,
                    "hook did not originate at the player's chest");
                nextStep();
                break;
            case 1:
                if (!data.hasPlantedHooks()) {
                    return;
                }
                HookAnchor planted = data.getHooks()
                    .get(0);
                require(planted.getStatus() == HookStatus.PLANTED, "wood hook did not enter PLANTED state");
                require(planted.getBlockZ() == 6, "wood hook planted on an unexpected block");
                Vec3 plantedTip = planted.getPosition()
                    .add(
                        planted.getDirection()
                            .scale(HookType.WOOD.getHookLength()));
                require(
                    Math.abs(plantedTip.y - woodAimY) < 1.0E-3D,
                    "chest-fired hook tip did not land at crosshair height");
                HookedMod.LOG.info("HOOKED_SELF_TEST step 1 passed: wood hook planted");
                nextStep();
                break;
            case 2:
                maxPullDistance = Math.max(maxPullDistance, player.posZ - startingZ);
                maxPullVelocity = Math.max(maxPullVelocity, player.motionZ);
                if (maxPullDistance <= 0.05D && maxPullVelocity <= 0.01D) {
                    return;
                }
                Vec3 beforeRelease = new Vec3(player.motionX, player.motionY, player.motionZ);
                double expectedReleaseY = HookMotion
                    .releaseVelocity(beforeRelease, HookMotion.BASE_RELEASE_JUMP_SPEED).y;
                data.retractAll(true);
                require(
                    Math.abs(player.motionY - expectedReleaseY) < 1.0E-6D,
                    "jump retract did not preserve its momentum-scaled upward boost");
                require(
                    player.motionY > HookMotion.BASE_RELEASE_JUMP_SPEED,
                    "moving release did not jump higher than a stationary release");
                expectedClientReleaseY = player.motionY;
                HookedMod.LOG
                    .info("HOOKED_SELF_TEST step 2 passed: momentum release serverY={}", expectedClientReleaseY);
                nextStep();
                break;
            case 3:
                if (!clientSawReleaseBoost) {
                    return;
                }
                if (!data.getHooks()
                    .isEmpty()) {
                    return;
                }
                prepareRedPlayer(player);
                equipInKnownSlot(player, new ItemStack(ModItems.hook, 1, HookType.RED.ordinal()));
                HookedMod.LOG.info("HOOKED_SELF_TEST step 3 passed: red hook equipped");
                nextStep();
                break;
            case 4:
                if (data.getHookType() != HookType.RED) {
                    return;
                }
                data.fire(new Vec3(0.0D, 0.0D, 1.0D));
                require(
                    data.getHooks()
                        .size() == 1,
                    "red hook was not created");
                HookedMod.LOG.info("HOOKED_SELF_TEST step 4 passed: red hook fired");
                nextStep();
                break;
            case 5:
                if (!data.hasPlantedHooks() || data.getCenter() == null) {
                    return;
                }
                if (!verifyClientRedPrediction) {
                    redCenterY = data.getCenter().y;
                    verifyClientRedPrediction = true;
                    return;
                }
                if (!clientRedPredictionPassed) {
                    return;
                }
                if (!clientRedMovementSent) {
                    return;
                }
                if (data.getCenter() == null || data.getRedVerticalOffset() <= 0.1D
                    || data.getCenter().y >= redCenterY - 0.1D) {
                    // The client flag becomes visible before the packet necessarily
                    // reaches the integrated server's next action-drain phase.
                    return;
                }
                require(data.getCenter() != null, "red hook lost its bounded-flight center");
                require(data.getRedVerticalOffset() > 0.1D, "red hook did not initialize vertical suspension");
                require(data.getCenter().y < redCenterY - 0.1D, "red hook did not move its bounded-flight center");
                require(
                    data.getRedVerticalOffset() < HookType.RED.getRange(),
                    "red hook moved its center beyond the rope range");
                double redVerticalDelta = redCenterY - data.getCenter().y;
                data.retractAll(false);
                HookedMod.LOG.info(
                    "HOOKED_SELF_TEST_PASS baubleSlot={} woodPlanted=true pullDistance={} pullVelocity={} "
                        + "clientReleaseY={} redVerticalDelta={} maxRedCenterStep={} lanPort={}",
                    baubleSlot,
                    maxPullDistance,
                    maxPullVelocity,
                    expectedClientReleaseY,
                    redVerticalDelta,
                    clientRedMaximumStep,
                    lanPort);
                finished = true;
                shutdownRequested = true;
                break;
            default:
                fail("invalid step " + step);
        }
    }

    private void verifyClientRedPrediction(Minecraft minecraft) {
        HookData clientData = HookData.get(minecraft.thePlayer);
        if (clientData.getHookType() != HookType.RED || !clientData.hasPlantedHooks()
            || clientData.getCenter() == null) {
            return;
        }
        double previousY = clientData.getCenter().y;
        // The red hook is tested downward from several blocks above the floor.
        // Testing at floor level would correctly collide and yield zero movement.
        clientData.moveRedHook(0.0D, 0.0D, -1.0D);
        if (clientData.getCenter() == null) {
            return;
        }
        if (!clientRedMovementPrimed) {
            // Let the player/controller settle for one input tick before measuring
            // continuous movement. HookData also initializes a newly planted red
            // anchor at the player's waist so this priming step stays small.
            clientRedMovementPrimed = true;
            return;
        }
        double predictedStep = previousY - clientData.getCenter().y;
        if (Math.abs(predictedStep) < 1.0E-6D) {
            // A preceding server teleport can briefly leave the client body on
            // the floor, where collision correctly rejects downward motion. Do
            // not spend the smooth-movement sample budget on those zero steps.
            return;
        }
        clientRedMaximumStep = Math.max(clientRedMaximumStep, Math.abs(predictedStep));
        clientRedPredictedTravel += Math.max(0.0D, predictedStep);
        clientRedMovementSteps++;
        if (clientRedMaximumStep > 0.251D) {
            fail(
                "red hook client prediction exceeded its smooth step limit: travel=" + clientRedPredictedTravel
                    + " maxStep="
                    + clientRedMaximumStep);
            shutdownRequested = true;
            return;
        }
        if (clientRedMovementSteps >= MIN_RED_PREDICTION_STEPS && clientRedPredictedTravel > 0.5D) {
            clientRedPredictionPassed = true;
            HookNetwork.CHANNEL.sendToServer(new MessageRedMovement(0.0F, 0.0F, -1.0F));
            clientRedMovementSent = true;
        } else if (clientRedMovementSteps >= MAX_RED_PREDICTION_STEPS) {
            if (!clientRedPredictionPassed) {
                fail(
                    "red hook client prediction did not advance: travel=" + clientRedPredictedTravel
                        + " maxStep="
                        + clientRedMaximumStep);
                shutdownRequested = true;
            }
        }
    }

    private static void preparePlayer(EntityPlayerMP player) {
        player.playerNetServerHandler.setPlayerLocation(0.5D, 65.0D, 0.5D, 0.0F, 0.0F);
        player.motionX = 0.0D;
        player.motionY = 0.0D;
        player.motionZ = 0.0D;
        for (int x = -4; x <= 4; x++) {
            for (int z = -3; z <= 5; z++) {
                player.worldObj.setBlock(x, 64, z, Blocks.stone);
            }
        }
        for (int x = -2; x <= 2; x++) {
            for (int y = 64; y <= 68; y++) {
                player.worldObj.setBlock(x, y, 6, Blocks.stone);
            }
        }
    }

    private static void prepareRedPlayer(EntityPlayerMP player) {
        preparePlayer(player);
        // Leave enough air below the player for the single-anchor red hook's
        // downward bounded-flight path to exercise collision-free prediction.
        player.playerNetServerHandler.setPlayerLocation(0.5D, 68.0D, 0.5D, 0.0F, 0.0F);
    }

    private static void setCloseWall(EntityPlayerMP player, boolean present) {
        for (int x = -2; x <= 2; x++) {
            for (int y = 65; y <= 68; y++) {
                if (present) {
                    player.worldObj.setBlock(x, y, 1, Blocks.stone);
                } else {
                    player.worldObj.setBlockToAir(x, y, 1);
                }
            }
        }
    }

    private static void removeExistingHooks(EntityPlayerMP player) {
        IInventory inventory = BaublesApi.getBaubles(player);
        require(inventory != null, "Baubles inventory was unavailable");
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            if (ItemHook.getType(inventory.getStackInSlot(slot)) != null) {
                inventory.setInventorySlotContents(slot, null);
            }
        }
        inventory.markDirty();
    }

    private int equipBauble(EntityPlayerMP player, ItemStack stack) {
        IInventory inventory = BaublesApi.getBaubles(player);
        require(inventory != null, "Baubles inventory was unavailable");
        require(ModItems.hook.canEquip(stack, player), "hook rejected an empty Baubles inventory");
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            if (inventory.getStackInSlot(slot) == null && inventory.isItemValidForSlot(slot, stack)) {
                inventory.setInventorySlotContents(slot, stack);
                inventory.markDirty();
                return slot;
            }
        }
        return -1;
    }

    private void equipInKnownSlot(EntityPlayerMP player, ItemStack stack) {
        IInventory inventory = BaublesApi.getBaubles(player);
        require(inventory != null && baubleSlot >= 0, "Baubles inventory disappeared");
        inventory.setInventorySlotContents(baubleSlot, null);
        inventory.markDirty();
        require(ModItems.hook.canEquip(stack, player), "red hook rejected the emptied Baubles inventory");
        require(inventory.isItemValidForSlot(baubleSlot, stack), "red hook was rejected by the validated slot");
        inventory.setInventorySlotContents(baubleSlot, stack);
        inventory.markDirty();
    }

    private void nextStep() {
        step++;
        stepTicks = 0;
    }

    private void fail(String message) {
        HookedMod.LOG.error("HOOKED_SELF_TEST_FAIL step {}: {}", step, message);
        finished = true;
        shutdownRequested = true;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
