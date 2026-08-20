package com.thecodewarrior.hooked.item;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.thecodewarrior.hooked.HookedConfig;
import com.thecodewarrior.hooked.HookedMod;
import com.thecodewarrior.hooked.common.HookSettings;
import com.thecodewarrior.hooked.common.HookType;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class ItemHook extends Item implements IBauble {

    private static final String INHIBITED_TAG = "hookedInhibited";
    private IIcon[] icons;

    public ItemHook() {
        setUnlocalizedName("hooked.hook");
        setCreativeTab(CreativeTabs.tabTransport);
        setHasSubtypes(true);
        setMaxDamage(0);
        setMaxStackSize(1);
        setTextureName(HookedMod.MODID + ":hook_wood");
    }

    public static HookType getType(ItemStack stack) {
        return stack != null && stack.getItem() == ModItems.hook ? HookType.byMetadata(stack.getItemDamage()) : null;
    }

    public static boolean isInhibited(ItemStack stack) {
        return stack != null && stack.hasTagCompound()
            && stack.getTagCompound()
                .getBoolean(INHIBITED_TAG);
    }

    public static ItemStack findUsableHook(EntityPlayer player) {
        int searchLocations = HookedConfig.getSearchLocations();
        if ((searchLocations & HookSettings.SEARCH_BAUBLES) != 0) {
            IInventory baubles = BaublesApi.getBaubles(player);
            ItemStack found = findInInventory(baubles, 0, baubles == null ? 0 : baubles.getSizeInventory());
            if (found != null) {
                return found;
            }
        }

        if ((searchLocations & HookSettings.SEARCH_HAND) != 0) {
            ItemStack held = player.getHeldItem();
            if (getType(held) != null) {
                return held;
            }
        }

        if ((searchLocations & HookSettings.SEARCH_HOTBAR) != 0) {
            ItemStack found = findInInventory(player.inventory, 0, 9);
            if (found != null) {
                return found;
            }
        }

        if ((searchLocations & HookSettings.SEARCH_INVENTORY) != 0) {
            return findInInventory(player.inventory, 9, 36);
        }
        return null;
    }

    private static ItemStack findInInventory(IInventory inventory, int from, int to) {
        if (inventory == null) {
            return null;
        }
        int maximum = Math.min(to, inventory.getSizeInventory());
        for (int slot = Math.max(0, from); slot < maximum; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (getType(stack) != null) {
                return stack;
            }
        }
        return null;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        HookType type = getType(stack);
        if (player.isSneaking() && type != null && type.getCount() > 1) {
            if (!stack.hasTagCompound()) {
                stack.setTagCompound(new NBTTagCompound());
            }
            stack.getTagCompound()
                .setBoolean(INHIBITED_TAG, !isInhibited(stack));
        }
        return stack;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return "item.hooked.hook_" + HookType.byMetadata(stack.getItemDamage())
            .getSerializedName();
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        for (HookType type : HookType.values()) {
            list.add(new ItemStack(this, 1, type.ordinal()));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        icons = new IIcon[HookType.values().length];
        for (HookType type : HookType.values()) {
            icons[type.ordinal()] = register.registerIcon(HookedMod.MODID + ":hook_" + type.getSerializedName());
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int metadata) {
        return icons[HookType.byMetadata(metadata)
            .ordinal()];
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        HookType type = getType(stack);
        if (type == null) {
            return;
        }
        tooltip.add(StatCollector.translateToLocal("tooltip.hooked.hook_" + type.getSerializedName() + ".info"));
        if (type.getCount() > 1 && isInhibited(stack)) {
            tooltip.add(StatCollector.translateToLocal("tooltip.hooked.hook.inhibited"));
        }
        String fireKey = HookedMod.proxy.getFireKeyName();
        tooltip.add(StatCollector.translateToLocalFormatted("tooltip.hooked.controls", fireKey, fireKey));
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.UNIVERSAL;
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase player) {}

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {}

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {}

    @Override
    public boolean canEquip(ItemStack itemstack, EntityLivingBase wearer) {
        if (!(wearer instanceof EntityPlayer)) {
            return false;
        }
        IInventory inventory = BaublesApi.getBaubles((EntityPlayer) wearer);
        if (inventory == null) {
            return false;
        }
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack equipped = inventory.getStackInSlot(slot);
            if (equipped != null && equipped != itemstack && equipped.getItem() == this) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean canUnequip(ItemStack itemstack, EntityLivingBase player) {
        return true;
    }
}
