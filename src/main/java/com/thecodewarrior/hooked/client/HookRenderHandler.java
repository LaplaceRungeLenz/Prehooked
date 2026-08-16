package com.thecodewarrior.hooked.client;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.opengl.GL11;

import com.thecodewarrior.hooked.HookedMod;
import com.thecodewarrior.hooked.common.HookAnchor;
import com.thecodewarrior.hooked.common.HookData;
import com.thecodewarrior.hooked.common.HookStatus;
import com.thecodewarrior.hooked.common.HookType;
import com.thecodewarrior.hooked.common.Vec3;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public final class HookRenderHandler {

    private static final double CHAIN_HALF_WIDTH = 0.5D;
    private static final double FIRST_PERSON_CAMERA_CLEARANCE = 1.25D;
    private static final Random PARTICLE_RANDOM = new Random();
    private static final HookHeadModel HOOK_HEAD_MODEL = new HookHeadModel();

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.theWorld == null || minecraft.renderViewEntity == null) {
            return;
        }
        double partial = event.partialTicks;
        double cameraX = interpolate(minecraft.renderViewEntity.lastTickPosX, minecraft.renderViewEntity.posX, partial);
        double cameraY = interpolate(minecraft.renderViewEntity.lastTickPosY, minecraft.renderViewEntity.posY, partial);
        double cameraZ = interpolate(minecraft.renderViewEntity.lastTickPosZ, minecraft.renderViewEntity.posZ, partial);

        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT
                | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_TEXTURE_BIT);
        GL11.glPushMatrix();
        GL11.glTranslated(-cameraX, -cameraY, -cameraZ);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        for (Object object : minecraft.theWorld.playerEntities) {
            renderPlayerHooks(minecraft, (EntityPlayer) object, partial);
        }

        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    private static void renderPlayerHooks(Minecraft minecraft, EntityPlayer player, double partial) {
        HookData data = HookData.get(player);
        HookType type = data.getHookType();
        if (type == null || data.getHooks()
            .isEmpty()) {
            return;
        }

        Vec3 waist = new Vec3(
            interpolate(player.lastTickPosX, player.posX, partial),
            interpolate(player.lastTickPosY, player.posY, partial) + player.getEyeHeight() * 0.5D,
            interpolate(player.lastTickPosZ, player.posZ, partial));

        for (HookAnchor hook : data.getHooks()) {
            float alpha = hook.getStatus() == HookStatus.RETRACTING ? 0.55F : 1.0F;
            Vec3 chainStart = waist;
            if (player == minecraft.thePlayer && minecraft.gameSettings.thirdPersonView == 0) {
                Vec3 eye = new Vec3(
                    interpolate(player.lastTickPosX, player.posX, partial),
                    interpolate(player.lastTickPosY, player.posY, partial) + player.getEyeHeight(),
                    interpolate(player.lastTickPosZ, player.posZ, partial));
                chainStart = clipPastCamera(waist, hook.getPosition(), eye);
            }
            renderChain(minecraft, type, chainStart, hook.getPosition(), alpha);
            renderHookHead(minecraft, type, hook, alpha);
            if (type == HookType.ENDER) {
                spawnEnderParticles(minecraft, waist, hook.getPosition());
            }
        }
    }

    private static Vec3 clipPastCamera(Vec3 start, Vec3 end, Vec3 camera) {
        Vec3 segment = end.subtract(start);
        double length = segment.length();
        if (length < 1.0E-5D) {
            return end;
        }
        Vec3 normal = segment.scale(1.0D / length);
        Vec3 relative = start.subtract(camera);
        double projection = relative.dot(normal);
        double discriminant = projection * projection - relative.lengthSquared()
            + FIRST_PERSON_CAMERA_CLEARANCE * FIRST_PERSON_CAMERA_CLEARANCE;
        if (discriminant <= 0.0D) {
            return start;
        }
        double exitDistance = -projection + Math.sqrt(discriminant);
        return start.add(normal.scale(Math.max(0.0D, Math.min(length, exitDistance))));
    }

    private static void spawnEnderParticles(Minecraft minecraft, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length <= 2.0D) {
            return;
        }
        Vec3 normal = delta.scale(1.0D / length);
        // The original emits dense portal particles along the whole rope. A render-time
        // probability keeps the same effect without tying it to the server tick rate.
        for (double distance = 1.0D; distance < length; distance += 0.25D) {
            if (PARTICLE_RANDOM.nextInt(8) != 0) {
                continue;
            }
            Vec3 position = start.add(normal.scale(distance));
            double jitterX = PARTICLE_RANDOM.nextDouble() * 0.1D - 0.05D;
            double jitterY = PARTICLE_RANDOM.nextDouble() * 0.1D - 0.05D;
            double jitterZ = PARTICLE_RANDOM.nextDouble() * 0.1D - 0.05D;
            double direction = PARTICLE_RANDOM.nextBoolean() ? 1.0D : -1.0D;
            minecraft.theWorld.spawnParticle(
                "portal",
                position.x + jitterX,
                position.y + jitterY + 0.1D,
                position.z + jitterZ,
                normal.x * direction + jitterX,
                normal.y * direction - 0.65D + jitterY,
                normal.z * direction + jitterZ);
        }
    }

    private static void renderChain(Minecraft minecraft, HookType type, Vec3 start, Vec3 end, float alpha) {
        Vec3 axis = end.subtract(start);
        double length = axis.length();
        if (length < 1.0E-5D) {
            return;
        }
        Vec3 normal = axis.scale(1.0D / length);
        Vec3 reference = Math.abs(normal.y) > 0.9D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 firstWidth = normal.cross(reference)
            .normalize()
            .scale(CHAIN_HALF_WIDTH);
        Vec3 secondWidth = normal.cross(firstWidth)
            .normalize()
            .scale(CHAIN_HALF_WIDTH);

        GL11.glColor4f(1.0F, 1.0F, 1.0F, alpha);
        minecraft.getTextureManager()
            .bindTexture(texture(type, "chain1"));
        drawRibbon(start, end, firstWidth, length);
        minecraft.getTextureManager()
            .bindTexture(texture(type, "chain2"));
        drawRibbon(start, end, secondWidth, length);
    }

    private static void drawRibbon(Vec3 start, Vec3 end, Vec3 width, double length) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(start.x + width.x, start.y + width.y, start.z + width.z, 0.0D, length);
        tessellator.addVertexWithUV(start.x - width.x, start.y - width.y, start.z - width.z, 1.0D, length);
        tessellator.addVertexWithUV(end.x - width.x, end.y - width.y, end.z - width.z, 1.0D, 0.0D);
        tessellator.addVertexWithUV(end.x + width.x, end.y + width.y, end.z + width.z, 0.0D, 0.0D);
        tessellator.draw();
    }

    private static void renderHookHead(Minecraft minecraft, HookType type, HookAnchor hook, float alpha) {
        minecraft.getTextureManager()
            .bindTexture(texture(type, "hook"));
        GL11.glColor4f(1.0F, 1.0F, 1.0F, alpha);
        GL11.glPushMatrix();
        Vec3 position = hook.getPosition();
        Vec3 direction = hook.getDirection()
            .normalize();
        GL11.glTranslated(position.x, position.y, position.z);
        GL11.glRotatef((float) Math.toDegrees(Math.atan2(direction.x, direction.z)), 0.0F, 1.0F, 0.0F);
        double clampedY = Math.max(-1.0D, Math.min(1.0D, direction.y));
        GL11.glRotatef((float) Math.toDegrees(Math.acos(clampedY)), 1.0F, 0.0F, 0.0F);
        HOOK_HEAD_MODEL.render();
        GL11.glPopMatrix();
    }

    private static ResourceLocation texture(HookType type, String name) {
        return new ResourceLocation(
            HookedMod.MODID,
            "textures/hooks/" + type.getSerializedName() + "/" + name + ".png");
    }

    private static double interpolate(double previous, double current, double partial) {
        return previous + (current - previous) * partial;
    }
}
