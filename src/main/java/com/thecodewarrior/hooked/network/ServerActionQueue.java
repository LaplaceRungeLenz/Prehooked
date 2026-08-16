package com.thecodewarrior.hooked.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.entity.player.EntityPlayerMP;

import com.thecodewarrior.hooked.common.HookData;
import com.thecodewarrior.hooked.common.Vec3;

/** Moves Netty callbacks onto the logical server thread and bounds packet work per tick. */
public final class ServerActionQueue {

    private static final int MAX_QUEUED_ACTIONS = 512;
    private static final int MAX_ACTIONS_PER_TICK = 256;
    private static final ConcurrentLinkedQueue<Action> ACTIONS = new ConcurrentLinkedQueue<Action>();
    private static final AtomicInteger ACTION_COUNT = new AtomicInteger();
    private static final Map<UUID, RedMovement> RED_MOVEMENTS = new ConcurrentHashMap<UUID, RedMovement>();

    private ServerActionQueue() {}

    public static void enqueueFire(EntityPlayerMP player, Vec3 direction) {
        enqueue(new Fire(player, direction == null ? Vec3.ZERO : direction.copy()));
    }

    public static void enqueueRetract(EntityPlayerMP player, UUID id) {
        enqueue(new Retract(player, id));
    }

    public static void enqueueRetractAll(EntityPlayerMP player, boolean boost) {
        enqueue(new RetractAll(player, boost));
    }

    public static void enqueueRedMovement(EntityPlayerMP player, float strafe, float forward, float vertical) {
        if (isUsable(player)) {
            RED_MOVEMENTS.put(player.getUniqueID(), new RedMovement(player, strafe, forward, vertical));
        }
    }

    private static void enqueue(Action action) {
        if (!isUsable(action.player)) {
            return;
        }
        int queued = ACTION_COUNT.incrementAndGet();
        if (queued > MAX_QUEUED_ACTIONS) {
            ACTION_COUNT.decrementAndGet();
            return;
        }
        ACTIONS.offer(action);
    }

    public static void drain() {
        int processed = 0;
        Action action;
        while (processed < MAX_ACTIONS_PER_TICK && (action = ACTIONS.poll()) != null) {
            ACTION_COUNT.decrementAndGet();
            if (isUsable(action.player)) {
                action.apply();
            }
            processed++;
        }

        for (Map.Entry<UUID, RedMovement> entry : RED_MOVEMENTS.entrySet()) {
            RedMovement movement = entry.getValue();
            if (RED_MOVEMENTS.remove(entry.getKey(), movement) && isUsable(movement.player)) {
                movement.apply();
            }
        }
    }

    private static boolean isUsable(EntityPlayerMP player) {
        return player != null && !player.isDead && player.worldObj != null && !player.worldObj.isRemote;
    }

    private abstract static class Action {

        protected final EntityPlayerMP player;

        private Action(EntityPlayerMP player) {
            this.player = player;
        }

        protected abstract void apply();
    }

    private static final class Fire extends Action {

        private final Vec3 direction;

        private Fire(EntityPlayerMP player, Vec3 direction) {
            super(player);
            this.direction = direction;
        }

        @Override
        protected void apply() {
            HookData.get(player)
                .fire(direction);
        }
    }

    private static final class Retract extends Action {

        private final UUID id;

        private Retract(EntityPlayerMP player, UUID id) {
            super(player);
            this.id = id;
        }

        @Override
        protected void apply() {
            HookData.get(player)
                .retract(id);
        }
    }

    private static final class RetractAll extends Action {

        private final boolean boost;

        private RetractAll(EntityPlayerMP player, boolean boost) {
            super(player);
            this.boost = boost;
        }

        @Override
        protected void apply() {
            HookData.get(player)
                .retractAll(boost);
        }
    }

    private static final class RedMovement extends Action {

        private final float strafe;
        private final float forward;
        private final float vertical;

        private RedMovement(EntityPlayerMP player, float strafe, float forward, float vertical) {
            super(player);
            this.strafe = strafe;
            this.forward = forward;
            this.vertical = vertical;
        }

        @Override
        protected void apply() {
            HookData.get(player)
                .moveRedHook(strafe, forward, vertical);
        }
    }
}
