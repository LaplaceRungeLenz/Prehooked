package com.thecodewarrior.hooked.item;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import com.thecodewarrior.hooked.HookedMod;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class ItemHookComponent extends Item {

    public static final int PLANT_FIBER = 0;
    public static final int ROPE = 1;
    public static final int IRON_CHAIN_LINK = 2;
    public static final int IRON_CHAIN = 3;
    private static final String[] NAMES = { "plant_fiber", "rope", "iron_chain_link", "iron_chain" };
    private IIcon[] icons;

    public ItemHookComponent() {
        setUnlocalizedName("hooked.component");
        setCreativeTab(CreativeTabs.tabMaterials);
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return "item.hooked." + NAMES[Math.floorMod(stack.getItemDamage(), NAMES.length)];
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        // Metadata 0 remains readable for existing worlds, but obsolete plant
        // fiber is no longer exposed now that rope uses vanilla string directly.
        for (int metadata = ROPE; metadata < NAMES.length; metadata++) {
            list.add(new ItemStack(this, 1, metadata));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        icons = new IIcon[NAMES.length];
        for (int metadata = 0; metadata < NAMES.length; metadata++) {
            icons[metadata] = register.registerIcon(HookedMod.MODID + ":" + NAMES[metadata]);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int metadata) {
        return icons[Math.floorMod(metadata, icons.length)];
    }
}
