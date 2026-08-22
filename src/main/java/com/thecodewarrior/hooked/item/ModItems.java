package com.thecodewarrior.hooked.item;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;

import com.thecodewarrior.hooked.common.HookType;

import cpw.mods.fml.common.registry.GameRegistry;

public final class ModItems {

    public static final ItemHook hook = new ItemHook();
    public static final ItemHookComponent component = new ItemHookComponent();

    private ModItems() {}

    public static void registerItems() {
        GameRegistry.registerItem(hook, "hook");
        GameRegistry.registerItem(component, "component");
    }

    public static void registerRecipes() {
        ItemStack rope = new ItemStack(component, 1, ItemHookComponent.ROPE);
        ItemStack chainLink = new ItemStack(component, 3, ItemHookComponent.IRON_CHAIN_LINK);
        ItemStack chain = new ItemStack(component, 1, ItemHookComponent.IRON_CHAIN);

        GameRegistry.addShapelessRecipe(rope, ropeIngredients());
        GameRegistry.addRecipe(new ShapedOreRecipe(chainLink, " II", "I I", "II ", 'I', "ingotIron"));
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                chain,
                "  L",
                " L ",
                "L  ",
                'L',
                new ItemStack(component, 1, ItemHookComponent.IRON_CHAIN_LINK)));

        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                hook(HookType.WOOD),
                "SSP",
                " RS",
                "R S",
                'S',
                "stickWood",
                'P',
                new ItemStack(Items.wooden_pickaxe, 1, Short.MAX_VALUE),
                'R',
                rope));
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                hook(HookType.IRON),
                "IIP",
                " CI",
                "C I",
                'I',
                "ingotIron",
                'P',
                new ItemStack(Items.iron_pickaxe, 1, Short.MAX_VALUE),
                'C',
                chain));
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                hook(HookType.DIAMOND),
                " DD",
                " HD",
                "D  ",
                'D',
                "gemDiamond",
                'H',
                hook(HookType.IRON)));
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                hook(HookType.RED),
                "PDR",
                " HD",
                "C P",
                'P',
                Blocks.piston,
                'D',
                "dustRedstone",
                'R',
                Blocks.redstone_block,
                'C',
                Items.comparator,
                'H',
                hook(HookType.DIAMOND)));
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                hook(HookType.ENDER),
                "PRE",
                " HR",
                "B P",
                'P',
                Items.ender_pearl,
                'R',
                Items.blaze_rod,
                'E',
                Items.ender_eye,
                'B',
                Items.blaze_powder,
                'H',
                hook(HookType.DIAMOND)));
    }

    private static ItemStack hook(HookType type) {
        return new ItemStack(hook, 1, type.ordinal());
    }

    static Object[] ropeIngredients() {
        return new Object[] { Items.string, Items.string };
    }
}
