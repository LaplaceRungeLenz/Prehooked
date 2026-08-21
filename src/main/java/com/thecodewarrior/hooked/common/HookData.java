package com.thecodewarrior.hooked.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.potion.Potion;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;

import com.thecodewarrior.hooked.HookedConfig;
import com.thecodewarrior.hooked.HookedMod;
import com.thecodewarrior.hooked.item.ItemHook;
import com.thecodewarrior.hooked.network.HookNetwork;
import com.thecodewarrior.hooked.network.MessageHookSync;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;

public final class HookData implements IExtendedEntityProperties {

    public static final String IDENTIFIER = HookedMod.MODID + ":hooks";
    private static final int SYNC_RANGE = 160;
    private static final double RED_MOVEMENT_SPEED = 0.25D;
    private static final double CLOSE_WALL_SIGHT_DISTANCE = 1.0D;
    private static final int LOCAL_RELEASE_GRACE_TICKS = 20;

    private final List<HookAnchor> hooks = new ArrayList<HookAnchor>();
    private EntityPlayer player;
    private HookType hookType;
    private Vec3 center;
    private double redVerticalOffset;
    private int cooldown;
    private int ticksSinceSync;
    private int localReleaseGraceTicks;
    private boolean pullStoppedByCollision;
    private boolean dirty;

    public static HookData get(EntityPlayer player) {
        HookData data = (HookData) player.getExtendedProperties(IDENTIFIER);
        if (data == null) {
            data = new HookData();
            player.registerExtendedProperties(IDENTIFIER, data);
            data.init(player, player.worldObj);
        }
        return data;
    }

    @Override
    public void init(net.minecraft.entity.Entity entity, World world) {
        player = (EntityPlayer) entity;
    }

    public List<HookAnchor> getHooks() {
        return Collections.unmodifiableList(hooks);
    }

    public HookType getHookType() {
        return hookType;
    }

    public Vec3 getCenter() {
        return center;
    }

    public int getCooldown() {
        return cooldown;
    }

    public boolean hasPlantedHooks() {
        for (HookAnchor hook : hooks) {
            if (hook.getStatus() == HookStatus.PLANTED) {
                return true;
            }
        }
        return false;
    }

    public void fire(Vec3 requestedDirection) {
        if (player == null || player.worldObj.isRemote || cooldown > 0) {
            return;
        }

        fireInternal(requestedDirection);
    }

    /** Immediately mirrors a fire action for the owning client while the server validates it. */
    public void predictFire(Vec3 requestedDirection) {
        if (player == null || !player.worldObj.isRemote || !HookedMod.proxy.isLocalPlayer(player) || cooldown > 0) {
            return;
        }

        localReleaseGraceTicks = 0;
        fireInternal(requestedDirection);
    }

    private void fireInternal(Vec3 requestedDirection) {

        ItemStack stack = ItemHook.findUsableHook(player);
        HookType type = ItemHook.getType(stack);
        if (type == null) {
            clear(!player.worldObj.isRemote);
            return;
        }

        if (hookType != type) {
            clear(false);
            hookType = type;
        }

        int maximum = ItemHook.isInhibited(stack) ? 1 : type.getCount();
        int active = 0;
        for (HookAnchor hook : hooks) {
            if (hook.getStatus() != HookStatus.RETRACTING) {
                active++;
            }
        }
        if (active >= maximum) {
            retractOldest();
        }

        Vec3 serverLook = Vec3.fromMinecraft(player.getLookVec())
            .normalize();
        Vec3 direction = requestedDirection == null ? Vec3.ZERO : requestedDirection.normalize();
        // Keep the client's exact click-time aim when it agrees with the server's
        // tracked rotation, but reject forged directions.
        if (direction.lengthSquared() < 0.99D || direction.dot(serverLook) < 0.8660254037844386D) {
            direction = serverLook;
        }

        Vec3 start = Vec3.waist(player);
        Vec3 eye = new Vec3(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3 sightEnd = eye.add(direction.scale(type.getRange() + type.getHookLength()));
        MovingObjectPosition aimedHit = rayTrace(eye, sightEnd);
        Vec3 hitPosition = aimedHit != null && aimedHit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
            ? Vec3.fromMinecraft(aimedHit.hitVec)
            : null;
        direction = HookAim.launchDirection(start, eye, direction, type.getRange(), hitPosition);

        HookAnchor hook = new HookAnchor(UUID.randomUUID(), start, direction, HookStatus.EXTENDING);
        hooks.add(hook);
        // The eye ray is already an authoritative block trace. Reusing its exact
        // result for a touching wall avoids the 1.7.10 ray tracer occasionally
        // dropping a second, very short chest-to-face trace on a block boundary.
        // Other shots still use the hook body's normal first collision segment.
        if (HookAim.isCloseSightHit(eye, hitPosition, CLOSE_WALL_SIGHT_DISTANCE)) {
            plantAtHit(hook, aimedHit, start);
        } else {
            double initialContactReach = Math.min(type.getProjectileSpeed(), type.getRange()) + type.getHookLength();
            tryPlant(hook, start.add(direction.scale(initialContactReach)), start);
        }
        updateCenter();
        cooldown = 4;
        dirty = true;
        if (!player.worldObj.isRemote) {
            syncNow();
        }
    }

    public void retract(UUID id) {
        if (player == null || player.worldObj.isRemote || id == null) {
            return;
        }
        for (HookAnchor hook : hooks) {
            if (hook.getId()
                .equals(id)) {
                hook.setStatus(HookStatus.RETRACTING);
                dirty = true;
                updateCenter();
                syncNow();
                return;
            }
        }
    }

    public void retractAll(boolean boost) {
        if (player == null || player.worldObj.isRemote) {
            return;
        }
        if (boost && hasPlantedHooks()) {
            applyReleaseVelocity();
            player.velocityChanged = true;
            ForgeHooks.onLivingJump(player);
            if (player instanceof EntityPlayerMP) {
                ((EntityPlayerMP) player).playerNetServerHandler.sendPacket(new S12PacketEntityVelocity(player));
            }
        }
        clear(true);
    }

    /**
     * Removes the local anchors before vanilla movement runs, preventing an old
     * planted snapshot from tugging against the server's release velocity.
     */
    public void predictRetractAll(boolean boost) {
        if (player == null || !player.worldObj.isRemote || !HookedMod.proxy.isLocalPlayer(player)) {
            return;
        }
        if (boost && hasPlantedHooks()) {
            applyReleaseVelocity();
        }
        localReleaseGraceTicks = LOCAL_RELEASE_GRACE_TICKS;
        clear(false);
    }

    private void applyReleaseVelocity() {
        double jumpSpeed = HookMotion.BASE_RELEASE_JUMP_SPEED;
        if (player.isPotionActive(Potion.jump)) {
            jumpSpeed += (player.getActivePotionEffect(Potion.jump)
                .getAmplifier() + 1) * 0.1D;
        }
        Vec3 velocity = HookMotion.releaseVelocity(
            new Vec3(player.motionX, player.motionY, player.motionZ),
            jumpSpeed,
            !pullStoppedByCollision);
        player.motionX = velocity.x;
        player.motionY = velocity.y;
        player.motionZ = velocity.z;
        player.onGround = false;
        player.isAirBorne = true;
        player.fallDistance = 0.0F;
    }

    public void moveRedHook(double strafe, double forward, double vertical) {
        if (player == null || player.worldObj.isRemote && !HookedMod.proxy.isLocalPlayer(player)
            || !HookedConfig.isRedHookFlightEnabled()
            || hookType != HookType.RED
            || !hasPlantedHooks()
            || center == null) {
            return;
        }

        strafe = clampFinite(strafe);
        forward = clampFinite(forward);
        vertical = clampFinite(vertical);
        int planted = countPlanted();
        if (planted == 1) {
            strafe = 0.0D;
            forward = 0.0D;
        }

        double magnitude = Math.sqrt(strafe * strafe + forward * forward);
        if (magnitude > 1.0D) {
            strafe /= magnitude;
            forward /= magnitude;
        }
        double yaw = Math.toRadians(player.rotationYaw);
        Vec3 offset = new Vec3(
            (strafe * Math.cos(yaw) - forward * Math.sin(yaw)) * RED_MOVEMENT_SPEED,
            vertical * RED_MOVEMENT_SPEED,
            (forward * Math.cos(yaw) + strafe * Math.sin(yaw)) * RED_MOVEMENT_SPEED);
        if (offset.lengthSquared() < 1.0E-6D) {
            return;
        }

        // Advance from the synchronized suspension center, but run collision
        // against the complete player-to-target displacement. Testing only the
        // incremental offset incorrectly blocks a descending center whenever the
        // player's body has already reached the floor below it.
        Vec3 waist = Vec3.waist(player);
        Vec3 requested = waist.add(
            collide(
                center.add(offset)
                    .subtract(waist)));
        List<HookAnchor> plantedHooks = getPlantedHooks();
        List<Vec3> anchors = new ArrayList<Vec3>(plantedHooks.size());
        for (HookAnchor hook : plantedHooks) {
            anchors.add(hook.getPosition());
        }
        RedHookGeometry.Result result = RedHookGeometry
            .constrain(requested, anchors, hookType.getRange() - 1.0D / 16.0D);
        double[] weights = result.getWeights();
        if (plantedHooks.size() == 1) {
            redVerticalOffset = plantedHooks.get(0)
                .getPosition().y - result.getPoint().y;
            plantedHooks.get(0)
                .setWeight(1.0D);
        } else {
            redVerticalOffset = 0.0D;
            for (int index = 0; index < plantedHooks.size(); index++) {
                plantedHooks.get(index)
                    .setWeight(weights[index]);
            }
        }
        updateCenter();
        if (!player.worldObj.isRemote) {
            dirty = true;
        }
    }

    public void tick() {
        if (player == null) {
            return;
        }

        if (player.worldObj.isRemote && !HookedMod.proxy.isLocalPlayer(player)) {
            advanceRemoteHooks();
            updateCenter();
            return;
        }

        if (localReleaseGraceTicks > 0) {
            localReleaseGraceTicks--;
        }

        if (cooldown > 0) {
            cooldown--;
        }

        ItemStack stack = ItemHook.findUsableHook(player);
        HookType equippedType = ItemHook.getType(stack);
        if (equippedType == null || hookType != null && equippedType != hookType) {
            if (!hooks.isEmpty()) {
                clear(!player.worldObj.isRemote);
            }
            hookType = equippedType;
            return;
        }
        hookType = equippedType;
        if (hookType == null) {
            return;
        }

        int maximum = ItemHook.isInhibited(stack) ? 1 : hookType.getCount();
        while (countPlanted() > maximum) {
            retractOldestPlanted();
        }

        boolean changed = advanceHooks();
        updateCenter();
        applyMovement();

        if (!player.worldObj.isRemote) {
            ticksSinceSync++;
            if (changed || dirty || ticksSinceSync >= HookedConfig.getStateSyncInterval() && !hooks.isEmpty()) {
                syncNow();
            }
        }
    }

    private void advanceRemoteHooks() {
        if (hookType == null) {
            return;
        }
        Vec3 waist = Vec3.waist(player);
        Iterator<HookAnchor> iterator = hooks.iterator();
        while (iterator.hasNext()) {
            HookAnchor hook = iterator.next();
            if (hook.getStatus() == HookStatus.EXTENDING) {
                double distanceLeft = hookType.getRange() - Math.sqrt(
                    hook.getPosition()
                        .distanceSquared(waist));
                if (distanceLeft <= 0.0D) {
                    hook.setStatus(HookStatus.RETRACTING);
                } else {
                    double step = Math.min(hookType.getProjectileSpeed(), distanceLeft);
                    hook.setPosition(
                        hook.getPosition()
                            .add(
                                hook.getDirection()
                                    .scale(step)));
                }
            } else if (hook.getStatus() == HookStatus.RETRACTING) {
                Vec3 relative = waist.subtract(hook.getPosition());
                if (relative.length() <= hookType.getRetractSpeed()) {
                    iterator.remove();
                } else {
                    hook.setPosition(
                        hook.getPosition()
                            .add(
                                relative.normalize()
                                    .scale(hookType.getRetractSpeed())));
                }
            }
        }
    }

    private boolean advanceHooks() {
        boolean changed = false;
        com.thecodewarrior.hooked.common.Vec3 waist = com.thecodewarrior.hooked.common.Vec3.waist(player);
        Iterator<HookAnchor> iterator = hooks.iterator();
        while (iterator.hasNext()) {
            HookAnchor hook = iterator.next();
            if (hook.getStatus() == HookStatus.RETRACTING) {
                double speed = hookType.getRetractSpeed();
                com.thecodewarrior.hooked.common.Vec3 toPlayer = waist.subtract(hook.getPosition());
                if (toPlayer.length() <= speed) {
                    iterator.remove();
                    changed = true;
                    continue;
                }
                hook.setPosition(
                    hook.getPosition()
                        .add(
                            toPlayer.normalize()
                                .scale(speed)));
                changed = true;
                continue;
            }

            if (hook.getStatus() == HookStatus.EXTENDING) {
                double distanceLeft = hookType.getRange() - Math.sqrt(
                    hook.getPosition()
                        .distanceSquared(waist));
                if (distanceLeft <= 0.0D) {
                    hook.setStatus(HookStatus.RETRACTING);
                    changed = true;
                    continue;
                }

                double step = Math.min(hookType.getProjectileSpeed(), distanceLeft);
                com.thecodewarrior.hooked.common.Vec3 next = hook.getPosition()
                    .add(
                        hook.getDirection()
                            .scale(step));
                if (!player.worldObj.isRemote) {
                    Vec3 traceEnd = next.add(
                        hook.getDirection()
                            .scale(hookType.getHookLength()));
                    if (tryPlant(hook, traceEnd, waist)) {
                        changed = true;
                    } else {
                        hook.setPosition(next);
                        changed = true;
                    }
                } else {
                    hook.setPosition(next);
                }
            }

            if (hook.getPosition()
                .distanceSquared(waist) > hookType.getRangeSquared() * 1.05D) {
                hook.setStatus(HookStatus.RETRACTING);
                changed = true;
            }

            if (!player.worldObj.isRemote && hook.getStatus() == HookStatus.PLANTED && !isAnchorStillValid(hook)) {
                hook.setStatus(HookStatus.RETRACTING);
                changed = true;
            }
        }
        return changed;
    }

    private boolean tryPlant(HookAnchor hook, Vec3 traceEnd, Vec3 waist) {
        return plantAtHit(hook, rayTrace(hook.getPosition(), traceEnd), waist);
    }

    private boolean plantAtHit(HookAnchor hook, MovingObjectPosition hit, Vec3 waist) {
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return false;
        }
        ForgeDirection side = ForgeDirection.getOrientation(hit.sideHit);
        Vec3 hitPosition = Vec3.fromMinecraft(hit.hitVec)
            .subtract(
                hook.getDirection()
                    .scale(hookType.getHookLength()));
        hook.plant(hit.blockX, hit.blockY, hit.blockZ, side, hitPosition);
        if (hookType == HookType.RED && countPlanted() == 1) {
            // A single red anchor permits movement on the vertical rope below it.
            redVerticalOffset = Math
                .max(0.0D, Math.min(hookType.getRange() - 1.0D / 16.0D, hook.getPosition().y - waist.y));
        }
        return true;
    }

    private MovingObjectPosition rayTrace(com.thecodewarrior.hooked.common.Vec3 start,
        com.thecodewarrior.hooked.common.Vec3 end) {
        return player.worldObj.rayTraceBlocks(start.toMinecraft(), end.toMinecraft(), false);
    }

    private boolean isAnchorStillValid(HookAnchor hook) {
        if (!player.worldObj.blockExists(hook.getBlockX(), hook.getBlockY(), hook.getBlockZ())) {
            return false;
        }
        Block block = player.worldObj.getBlock(hook.getBlockX(), hook.getBlockY(), hook.getBlockZ());
        return block != null && !block.isAir(player.worldObj, hook.getBlockX(), hook.getBlockY(), hook.getBlockZ());
    }

    private void updateCenter() {
        com.thecodewarrior.hooked.common.Vec3 sum = com.thecodewarrior.hooked.common.Vec3.ZERO;
        double total = 0.0D;
        int planted = 0;
        for (HookAnchor hook : hooks) {
            if (hook.getStatus() != HookStatus.PLANTED) {
                continue;
            }
            sum = sum.add(
                hook.getPosition()
                    .scale(hook.getWeight()));
            total += hook.getWeight();
            planted++;
        }
        center = planted == 0 || total <= 0.0D ? null : sum.scale(1.0D / total);
        if (hookType == HookType.RED && planted == 1 && center != null) {
            center = center.subtract(new com.thecodewarrior.hooked.common.Vec3(0.0D, redVerticalOffset, 0.0D));
        } else if (planted != 1) {
            redVerticalOffset = 0.0D;
        }
    }

    private List<HookAnchor> getPlantedHooks() {
        List<HookAnchor> planted = new ArrayList<HookAnchor>(4);
        for (HookAnchor hook : hooks) {
            if (hook.getStatus() == HookStatus.PLANTED) {
                planted.add(hook);
            }
        }
        return planted;
    }

    @SuppressWarnings("rawtypes")
    private Vec3 collide(Vec3 requested) {
        double x = requested.x;
        double y = requested.y;
        double z = requested.z;
        net.minecraft.util.AxisAlignedBB box = player.boundingBox;
        List collisions = player.worldObj.getCollidingBoundingBoxes(player, box.addCoord(x, y, z));
        for (Object collision : collisions) {
            y = ((net.minecraft.util.AxisAlignedBB) collision).calculateYOffset(box, y);
        }
        net.minecraft.util.AxisAlignedBB moved = box.getOffsetBoundingBox(0.0D, y, 0.0D);
        for (Object collision : collisions) {
            x = ((net.minecraft.util.AxisAlignedBB) collision).calculateXOffset(moved, x);
        }
        moved = moved.getOffsetBoundingBox(x, 0.0D, 0.0D);
        for (Object collision : collisions) {
            z = ((net.minecraft.util.AxisAlignedBB) collision).calculateZOffset(moved, z);
        }
        return new Vec3(x, y, z);
    }

    private static double clampFinite(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) ? 0.0D : Math.max(-1.0D, Math.min(1.0D, value));
    }

    private void applyMovement() {
        if (center == null || hookType == null || localReleaseGraceTicks > 0) {
            pullStoppedByCollision = false;
            return;
        }
        com.thecodewarrior.hooked.common.Vec3 pull = center
            .subtract(com.thecodewarrior.hooked.common.Vec3.waist(player));
        pullStoppedByCollision = HookMotion.isPullStalled(
            new Vec3(player.motionX, player.motionY, player.motionZ),
            pull,
            player.isCollidedHorizontally);
        double distance = pull.length();
        if (distance < 1.0E-5D) {
            player.motionX = 0.0D;
            player.motionY = 0.0D;
            player.motionZ = 0.0D;
            player.fallDistance = 0.0F;
            return;
        }
        double pullSpeed = hookType.getPullSpeed();
        com.thecodewarrior.hooked.common.Vec3 target = pull.clampLength(pullSpeed);
        if (distance <= pullSpeed) {
            player.motionX = target.x;
            player.motionY = target.y;
            player.motionZ = target.z;
        } else {
            cancelMotionAwayFromAnchors();
            double force = 0.5D;
            player.motionX = approach(player.motionX, target.x, force);
            player.motionY = approach(player.motionY, target.y, force);
            player.motionZ = approach(player.motionZ, target.z, force);
        }
        player.fallDistance = 0.0F;
    }

    private void cancelMotionAwayFromAnchors() {
        Vec3 motion = new Vec3(player.motionX, player.motionY, player.motionZ);
        Vec3 waist = Vec3.waist(player);
        for (HookAnchor hook : hooks) {
            if (hook.getStatus() != HookStatus.PLANTED) {
                continue;
            }
            Vec3 towardAnchor = hook.getPosition()
                .subtract(waist)
                .normalize();
            double projection = motion.dot(towardAnchor);
            if (projection < 0.0D) {
                motion = motion.subtract(towardAnchor.scale(projection));
            }
        }
        player.motionX = motion.x;
        player.motionY = motion.y;
        player.motionZ = motion.z;
    }

    private static double approach(double current, double target, double factor) {
        if (Math.abs(current) >= Math.abs(target) && Math.signum(current) == Math.signum(target)) {
            return current;
        }
        double adjusted = current + target * factor;
        return Math.abs(adjusted) > Math.abs(target) ? target : adjusted;
    }

    private int countPlanted() {
        int count = 0;
        for (HookAnchor hook : hooks) {
            if (hook.getStatus() == HookStatus.PLANTED) {
                count++;
            }
        }
        return count;
    }

    private void retractOldest() {
        for (HookAnchor hook : hooks) {
            if (hook.getStatus() != HookStatus.RETRACTING) {
                hook.setStatus(HookStatus.RETRACTING);
                dirty = true;
                return;
            }
        }
    }

    private void retractOldestPlanted() {
        for (HookAnchor hook : hooks) {
            if (hook.getStatus() == HookStatus.PLANTED) {
                hook.setStatus(HookStatus.RETRACTING);
                dirty = true;
                return;
            }
        }
    }

    private void clear(boolean synchronize) {
        hooks.clear();
        center = null;
        redVerticalOffset = 0.0D;
        pullStoppedByCollision = false;
        dirty = true;
        if (synchronize && player != null && !player.worldObj.isRemote) {
            syncNow();
        }
    }

    public void syncTo(EntityPlayerMP recipient) {
        HookNetwork.CHANNEL.sendTo(MessageHookSync.fromPlayer(player, this), recipient);
    }

    private void syncNow() {
        if (player == null || player.worldObj.isRemote) {
            return;
        }
        MessageHookSync packet = MessageHookSync.fromPlayer(player, this);
        HookNetwork.CHANNEL.sendToAllAround(
            packet,
            new TargetPoint(
                player.dimension,
                player.posX,
                player.posY,
                player.posZ,
                Math.max(SYNC_RANGE, HookedConfig.getMaximumRange() + 16.0D)));
        ticksSinceSync = 0;
        dirty = false;
    }

    public void applySnapshot(MessageHookSync snapshot) {
        if (player != null && player.worldObj.isRemote
            && HookedMod.proxy.isLocalPlayer(player)
            && localReleaseGraceTicks > 0
            && !snapshot.getHooks()
                .isEmpty()) {
            // This snapshot was already in flight when the client predicted a
            // release. Keep the anchors gone until the server's empty snapshot
            // acknowledges the queued retract action.
            return;
        }
        HookType snapshotType = HookType.byOrdinal(snapshot.getHookTypeOrdinal());
        boolean localRedPrediction = player != null && player.worldObj.isRemote
            && HookedMod.proxy.isLocalPlayer(player)
            && hookType == HookType.RED
            && snapshotType == HookType.RED;
        Map<UUID, Double> predictedWeights = new HashMap<UUID, Double>();
        double predictedVerticalOffset = redVerticalOffset;
        if (localRedPrediction) {
            for (HookAnchor hook : hooks) {
                if (hook.getStatus() == HookStatus.PLANTED) {
                    predictedWeights.put(hook.getId(), hook.getWeight());
                }
            }
        }

        hookType = snapshotType;
        redVerticalOffset = snapshot.getRedVerticalOffset();
        hooks.clear();
        for (HookAnchor anchor : snapshot.getHooks()) {
            hooks.add(anchor);
        }
        if (localRedPrediction && predictedWeights.size() == countPlanted()) {
            boolean sameAnchors = true;
            for (HookAnchor hook : hooks) {
                if (hook.getStatus() == HookStatus.PLANTED && !predictedWeights.containsKey(hook.getId())) {
                    sameAnchors = false;
                    break;
                }
            }
            if (sameAnchors) {
                redVerticalOffset = predictedVerticalOffset;
                for (HookAnchor hook : hooks) {
                    Double weight = predictedWeights.get(hook.getId());
                    if (weight != null) {
                        hook.setWeight(weight);
                    }
                }
            }
        }
        updateCenter();
        if (snapshot.getHooks()
            .isEmpty()) {
            localReleaseGraceTicks = 0;
        }
    }

    public double getRedVerticalOffset() {
        return redVerticalOffset;
    }

    @Override
    public void saveNBTData(NBTTagCompound compound) {
        NBTTagCompound data = new NBTTagCompound();
        data.setByte("type", (byte) (hookType == null ? -1 : hookType.ordinal()));
        data.setDouble("redVerticalOffset", redVerticalOffset);
        NBTTagList list = new NBTTagList();
        for (HookAnchor hook : hooks) {
            if (hook.getStatus() == HookStatus.PLANTED) {
                list.appendTag(hook.writeToNBT());
            }
        }
        data.setTag("anchors", list);
        compound.setTag(IDENTIFIER, data);
    }

    @Override
    public void loadNBTData(NBTTagCompound compound) {
        hooks.clear();
        if (!compound.hasKey(IDENTIFIER, Constants.NBT.TAG_COMPOUND)) {
            return;
        }
        NBTTagCompound data = compound.getCompoundTag(IDENTIFIER);
        hookType = HookType.byOrdinal(data.getByte("type"));
        redVerticalOffset = data.getDouble("redVerticalOffset");
        NBTTagList list = data.getTagList("anchors", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            HookAnchor anchor = HookAnchor.readFromNBT(list.getCompoundTagAt(i));
            if (anchor.getStatus() == HookStatus.PLANTED) {
                hooks.add(anchor);
            }
        }
        updateCenter();
    }
}
