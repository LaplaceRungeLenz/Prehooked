package com.thecodewarrior.hooked.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

/** Minecraft 1.7 renderer for the original four-pronged world hook model. */
final class HookHeadModel extends ModelBase {

    private static final float PIXEL = 1.0F / 16.0F;
    private final ModelRenderer center;
    private final ModelRenderer northArm;
    private final ModelRenderer eastArm;
    private final ModelRenderer southArm;
    private final ModelRenderer westArm;

    HookHeadModel() {
        textureWidth = 16;
        textureHeight = 16;

        center = new ModelRenderer(this, 0, 0);
        center.addBox(-0.5F, 0.0F, -0.5F, 1, 8, 1);

        northArm = new ModelRenderer(this, 4, 0);
        northArm.setRotationPoint(0.0F, 5.0F, -3.5F);
        northArm.addBox(-0.5F, 0.1414F, 0.1414F, 1, 4, 1);
        northArm.rotateAngleX = radians(45.0F);

        eastArm = new ModelRenderer(this, 8, 0);
        eastArm.setRotationPoint(2.5F, 4.5F, 0.0F);
        eastArm.addBox(-0.0707F, -0.2121F, -0.5F, 1, 4, 1);
        eastArm.rotateAngleZ = radians(45.0F);

        southArm = new ModelRenderer(this, 4, 6);
        southArm.setRotationPoint(0.0F, 4.25F, 2.25F);
        southArm.addBox(-0.5F, -0.2121F, 0.2828F, 1, 4, 1);
        southArm.rotateAngleX = radians(-45.0F);

        westArm = new ModelRenderer(this, 8, 6);
        westArm.setRotationPoint(-3.0F, 5.0F, 0.0F);
        westArm.addBox(-0.2121F, -0.2121F, -0.5F, 1, 4, 1);
        westArm.rotateAngleZ = radians(-45.0F);
    }

    void render() {
        center.render(PIXEL);
        northArm.render(PIXEL);
        eastArm.render(PIXEL);
        southArm.render(PIXEL);
        westArm.render(PIXEL);
    }

    private static float radians(float degrees) {
        return (float) Math.toRadians(degrees);
    }
}
