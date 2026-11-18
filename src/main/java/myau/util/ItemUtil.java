package myau.util;

import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemFireball;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;

public class ItemUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final ArrayList<Integer> GOOD_POTIONS = new BadPotions();

    public static boolean isTrash(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        Item item = stack.getItem();
        if (item instanceof ItemBlock) {
            return !isPlaceableBlock((ItemBlock) item);
        }
        if (item instanceof ItemPotion) {
            List<PotionEffect> effects = ((ItemPotion) item).getEffects(stack);
            return effects.stream().map(PotionEffect::getPotionID).noneMatch(GOOD_POTIONS::contains);
        }
        return !(item instanceof ItemFishingRod)
            && !(item instanceof ItemSnowball)
            && !(item instanceof ItemEgg)
            && !(item instanceof ItemBucket)
            && !(item instanceof ItemEnderPearl)
            && (!(item instanceof ItemFood) || item == Items.spider_eye)
            && item != Items.nether_star;
    }

    public static boolean isBlock(ItemStack stack) {
        if (stack != null && stack.stackSize >= 1) {
            Item item = stack.getItem();
            return item instanceof ItemBlock && isPlaceableBlock((ItemBlock) item);
        }
        return false;
    }

    public static boolean isPlaceableBlock(ItemBlock itemBlock) {
        Block block = itemBlock.getBlock();
        return !BlockUtil.isInteractable(block) && BlockUtil.isSolid(block);
    }

    public static double getAttackBonus(ItemStack stack) {
        double bonus = 0.0;
        if (stack == null) {
            return bonus;
        }
        Multimap<String, AttributeModifier> modifiers = stack.getAttributeModifiers();
        for (String key : modifiers.keySet()) {
            if (key.equals("generic.attackDamage")) {
                Collection<AttributeModifier> values = modifiers.get(key);
                Iterator<AttributeModifier> iterator = values.iterator();
                if (iterator.hasNext()) {
                    bonus += iterator.next().getAmount();
                }
                break;
            }
        }
        if (stack.isItemEnchanted()) {
            bonus += EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack);
            bonus += (double) EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack) * 1.25;
        }
        return bonus;
    }

    public static float getToolEfficiency(ItemStack stack) {
        float efficiency = 1.0f;
        if (stack != null && stack.getItem() instanceof ItemTool) {
            efficiency *= ((ItemTool) stack.getItem()).getToolMaterial().getEfficiencyOnProperMaterial();
            if (efficiency > 1.0f) {
                int level = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, stack);
                if (level > 0) {
                    efficiency += (float) (level * level + 1);
                }
            }
        }
        return efficiency;
    }

    public static double getArmorProtection(ItemStack stack) {
        double protection = 0.0;
        if (stack != null && stack.getItem() instanceof ItemArmor) {
            protection += ((ItemArmor) stack.getItem()).damageReduceAmount;
            if (stack.isItemEnchanted()) {
                protection += (double) EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack) * 0.25;
            }
        }
        return protection;
    }

    public static int findSwordSlot(int startSlot, boolean checkDurability) {
        int bestSlot = -1;
        double bestBonus = 0.0;
        for (int offset = 0; offset < 36; ++offset) {
            int slot = ((startSlot + offset) % 36 + 36) % 36;
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            if (stack != null && stack.getItem() instanceof ItemSword) {
                if (checkDurability && stack.isItemDamaged() && stack.getMaxDamage() - stack.getItemDamage() < 30) {
                    continue;
                }
                double bonus = getAttackBonus(stack);
                if (bonus > bestBonus) {
                    bestSlot = slot;
                    bestBonus = bonus;
                }
            }
        }
        return bestSlot;
    }

    public static int findToolSlot(String toolClass, int startSlot, boolean checkDurability) {
        int bestSlot = -1;
        float bestEfficiency = 1.0f;
        for (int offset = 0; offset < 36; ++offset) {
            int slot = ((startSlot + offset) % 36 + 36) % 36;
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            if (stack != null && stack.getItem() instanceof ItemTool && stack.getItem().getToolClasses(stack).contains(toolClass)) {
                if (checkDurability && stack.isItemDamaged() && stack.getMaxDamage() - stack.getItemDamage() < 30) {
                    continue;
                }
                float efficiency = getToolEfficiency(stack);
                if (efficiency > bestEfficiency) {
                    bestSlot = slot;
                    bestEfficiency = efficiency;
                }
            }
        }
        return bestSlot;
    }

    public static int findBestToolSlot(int currentSlot, Block block) {
        ItemStack currentStack = mc.thePlayer.inventory.getStackInSlot(currentSlot);
        int bestSlot = currentSlot;
        float bestStrength = currentStack != null ? currentStack.getStrVsBlock(block) : 1.0f;
        for (int slot = 0; slot < 9; ++slot) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            if (stack != null) {
                float strength = stack.getStrVsBlock(block);
                if (strength > bestStrength) {
                    bestSlot = slot;
                    bestStrength = strength;
                }
            }
        }
        return bestSlot;
    }

    public static int findArmorSlot(int armorType, boolean checkDurability) {
        int bestSlot = -1;
        double bestProtection = 0.0;
        for (int slot = 0; slot < 40; ++slot) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            if (stack != null && stack.getItem() instanceof ItemArmor && ((ItemArmor) stack.getItem()).armorType == armorType) {
                if (checkDurability && stack.isItemDamaged() && stack.getMaxDamage() - stack.getItemDamage() < 30) {
                    continue;
                }
                double protection = getArmorProtection(stack);
                if (protection >= bestProtection) {
                    bestSlot = slot;
                    bestProtection = protection;
                }
            }
        }
        return bestSlot;
    }

    public static int findBlockSlot(int startSlot) {
        int bestSlot = -1;
        int largestStack = 0;
        for (int offset = 0; offset < 36; ++offset) {
            int slot = ((startSlot + offset) % 36 + 36) % 36;
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            if (stack != null && isBlock(stack) && largestStack < stack.stackSize) {
                bestSlot = slot;
                largestStack = stack.stackSize;
            }
        }
        return bestSlot;
    }

    public static boolean isHoldingWeapon() {
        ItemStack stack = mc.thePlayer.getHeldItem();
        if (stack == null) {
            return false;
        }
        if (stack.hasTagCompound()) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag.hasKey("ExtraAttributes")) {
                NBTTagCompound extra = tag.getCompoundTag("ExtraAttributes");
                if (extra.hasKey("UHCid")) {
                    long uhcId = extra.getLong("UHCid");
                    if (uhcId == 50006L || uhcId == 50009L) {
                        return true;
                    }
                }
            }
            if (tag.hasKey("HideFlags") && stack.getItem() instanceof ItemSpade) {
                if (((ItemSpade) stack.getItem()).getToolMaterial() == ToolMaterial.EMERALD) {
                    return true;
                }
            }
            if (tag.hasKey("ItemModel") && tag.getString("ItemModel").equals("minecraft:mace")) {
                return true;
            }
        }
        if (stack.getItem() instanceof ItemEnchantedBook) {
            return false;
        }
        Map<Integer, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        return enchantments.containsKey(19) || stack.getItem() instanceof ItemSword;
    }

    public static boolean isHoldingSword() {
        ItemStack stack = mc.thePlayer.getHeldItem();
        return stack != null && stack.getItem() instanceof ItemSword;
    }

    public static boolean isHoldingTool() {
        ItemStack stack = mc.thePlayer.getHeldItem();
        return stack != null && stack.getItem() instanceof ItemTool;
    }

    public static boolean isEatingOrDrinking() {
        ItemStack stack = mc.thePlayer.getHeldItem();
        if (stack == null || ItemPotion.isSplash(stack.getItem().getMetadata(stack))) {
            return false;
        }
        return stack.getItemUseAction() == EnumAction.EAT || stack.getItemUseAction() == EnumAction.DRINK;
    }

    public static boolean isHoldingBow() {
        ItemStack stack = mc.thePlayer.getHeldItem();
        return stack != null && stack.getItem() instanceof ItemBow;
    }

    public static boolean isHoldingBlock() {
        ItemStack stack = mc.thePlayer.getHeldItem();
        return stack != null && stack.stackSize >= 1 && stack.getItem() instanceof ItemBlock;
    }

    public static boolean isHoldingPlaceableBlock() {
        return isBlock(mc.thePlayer.getHeldItem());
    }

    public static boolean isHoldingFireball() {
        ItemStack stack = mc.thePlayer.getHeldItem();
        return stack != null && stack.stackSize >= 1 && stack.getItem() instanceof ItemFireball;
    }
}
