package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.events.UpdateEvent;
import myau.event.events.WindowClickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.util.ChatUtil;
import myau.util.ItemUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.world.WorldSettings;
import org.apache.commons.lang3.RandomUtils;

public class ChestStealer extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int clickDelay = 0;
    private int openDelayTicks = 0;
    private boolean inChest = false;
    private boolean warnedFull = false;
    public final IntProperty minDelay = new IntProperty("min-delay", 1, 0, 20);
    public final IntProperty maxDelay = new IntProperty("max-delay", 2, 0, 20);
    public final IntProperty openDelay = new IntProperty("open-delay", 1, 0, 20);
    public final BooleanProperty autoClose = new BooleanProperty("auto-close", false);
    public final BooleanProperty nameCheck = new BooleanProperty("name-check", true);
    public final BooleanProperty skipTrash = new BooleanProperty("skip-trash", true);

    public ChestStealer() {
        super("ChestStealer", false, 0);
    }

    private boolean isValidGameMode() {
        WorldSettings.GameType gameType = mc.playerController.getCurrentGameType();
        return gameType == WorldSettings.GameType.SURVIVAL || gameType == WorldSettings.GameType.ADVENTURE;
    }

    private void shiftClick(int windowId, int slotId) {
        mc.playerController.windowClick(windowId, slotId, 0, 1, mc.thePlayer);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.clickDelay > 0) {
            this.clickDelay--;
        }
        if (this.openDelayTicks > 0) {
            this.openDelayTicks--;
        }
        if (!(mc.currentScreen instanceof GuiChest)) {
            this.inChest = false;
            return;
        }
        Container container = ((GuiChest) mc.currentScreen).inventorySlots;
        if (!(container instanceof ContainerChest)) {
            this.inChest = false;
            return;
        }
        if (!this.inChest) {
            this.inChest = true;
            this.warnedFull = false;
            this.openDelayTicks = this.openDelay.getValue() + 1;
        }
        if (this.openDelayTicks > 0 || this.clickDelay > 0) {
            return;
        }
        if (!this.isEnabled() || !this.isValidGameMode()) {
            return;
        }
        IInventory chestInventory = ((ContainerChest) container).getLowerChestInventory();
        if (this.nameCheck.getValue()) {
            String inventoryName = chestInventory.getName();
            if (!inventoryName.isEmpty()
                    && !inventoryName.equals(I18n.format("container.chest"))
                    && !inventoryName.equals(I18n.format("container.chestDouble"))) {
                return;
            }
        }
        if (mc.thePlayer.inventory.getFirstEmptyStack() == -1) {
            if (!this.warnedFull) {
                ChatUtil.sendFormatted(String.format("%s%s: &cYour inventory is full!&r", Myau.clientName, this.getName()));
                this.warnedFull = true;
            }
            if (this.autoClose.getValue()) {
                mc.thePlayer.closeScreen();
            }
            return;
        }
        if (this.skipTrash.getValue()) {
            int bestSwordSlot = -1;
            double bestSwordDamage = 0.0;
            int[] bestArmorSlots = new int[]{-1, -1, -1, -1};
            double[] bestArmorProtections = new double[]{0.0, 0.0, 0.0, 0.0};
            int bestPickaxeSlot = -1;
            float bestPickaxeEfficiency = 1.0F;
            int bestShovelSlot = -1;
            float bestShovelEfficiency = 1.0F;
            int bestAxeSlot = -1;
            float bestAxeEfficiency = 1.0F;

            for (int slot = 0; slot < chestInventory.getSizeInventory(); slot++) {
                if (!container.getSlot(slot).getHasStack()) {
                    continue;
                }
                ItemStack stack = container.getSlot(slot).getStack();
                Item item = stack.getItem();
                if (item instanceof ItemSword) {
                    double damage = ItemUtil.getAttackBonus(stack);
                    if (bestSwordSlot == -1 || damage > bestSwordDamage) {
                        bestSwordSlot = slot;
                        bestSwordDamage = damage;
                    }
                } else if (item instanceof ItemArmor) {
                    int armorType = ((ItemArmor) item).armorType;
                    double protection = ItemUtil.getArmorProtection(stack);
                    if (bestArmorSlots[armorType] == -1 || protection > bestArmorProtections[armorType]) {
                        bestArmorSlots[armorType] = slot;
                        bestArmorProtections[armorType] = protection;
                    }
                } else if (item instanceof ItemPickaxe) {
                    float efficiency = ItemUtil.getToolEfficiency(stack);
                    if (bestPickaxeSlot == -1 || efficiency > bestPickaxeEfficiency) {
                        bestPickaxeSlot = slot;
                        bestPickaxeEfficiency = efficiency;
                    }
                } else if (item instanceof ItemSpade) {
                    float efficiency = ItemUtil.getToolEfficiency(stack);
                    if (bestShovelSlot == -1 || efficiency > bestShovelEfficiency) {
                        bestShovelSlot = slot;
                        bestShovelEfficiency = efficiency;
                    }
                } else if (item instanceof ItemAxe) {
                    float efficiency = ItemUtil.getToolEfficiency(stack);
                    if (bestAxeSlot == -1 || efficiency > bestAxeEfficiency) {
                        bestAxeSlot = slot;
                        bestAxeEfficiency = efficiency;
                    }
                }
            }

            int currentSwordSlot = ItemUtil.findSwordSlot(0, true);
            double currentSwordDamage = currentSwordSlot != -1 ? ItemUtil.getAttackBonus(mc.thePlayer.inventory.getStackInSlot(currentSwordSlot)) : 0.0;
            if (bestSwordDamage > currentSwordDamage) {
                this.shiftClick(container.windowId, bestSwordSlot);
                return;
            }
            for (int armorType = 0; armorType < 4; armorType++) {
                int currentArmorSlot = ItemUtil.findArmorSlot(armorType, true);
                double currentProtection = currentArmorSlot != -1 ? ItemUtil.getArmorProtection(mc.thePlayer.inventory.getStackInSlot(currentArmorSlot)) : 0.0;
                if (bestArmorProtections[armorType] > currentProtection) {
                    this.shiftClick(container.windowId, bestArmorSlots[armorType]);
                    return;
                }
            }
            int currentPickaxeSlot = ItemUtil.findToolSlot("pickaxe", 0, true);
            float currentPickaxeEfficiency = currentPickaxeSlot != -1 ? ItemUtil.getToolEfficiency(mc.thePlayer.inventory.getStackInSlot(currentPickaxeSlot)) : 1.0F;
            if (bestPickaxeEfficiency > currentPickaxeEfficiency) {
                this.shiftClick(container.windowId, bestPickaxeSlot);
                return;
            }
            int currentShovelSlot = ItemUtil.findToolSlot("shovel", 0, true);
            float currentShovelEfficiency = currentShovelSlot != -1 ? ItemUtil.getToolEfficiency(mc.thePlayer.inventory.getStackInSlot(currentShovelSlot)) : 1.0F;
            if (bestShovelEfficiency > currentShovelEfficiency) {
                this.shiftClick(container.windowId, bestShovelSlot);
                return;
            }
            int currentAxeSlot = ItemUtil.findToolSlot("axe", 0, true);
            float currentAxeEfficiency = currentAxeSlot != -1 ? ItemUtil.getToolEfficiency(mc.thePlayer.inventory.getStackInSlot(currentAxeSlot)) : 1.0F;
            if (bestAxeEfficiency > currentAxeEfficiency) {
                this.shiftClick(container.windowId, bestAxeSlot);
                return;
            }
        }
        for (int slot = 0; slot < chestInventory.getSizeInventory(); slot++) {
            if (container.getSlot(slot).getHasStack()) {
                ItemStack stack = container.getSlot(slot).getStack();
                if (!this.skipTrash.getValue() || !ItemUtil.isTrash(stack)) {
                    this.shiftClick(container.windowId, slot);
                    return;
                }
            }
        }
        if (this.autoClose.getValue()) {
            mc.thePlayer.closeScreen();
        }
    }

    @EventTarget
    public void onWindowClick(WindowClickEvent event) {
        this.clickDelay = RandomUtils.nextInt(this.minDelay.getValue() + 1, this.maxDelay.getValue() + 2);
    }

    @Override
    public void verifyValue(String propertyName) {
        switch (propertyName) {
            case "min-delay":
                if (this.minDelay.getValue() > this.maxDelay.getValue()) {
                    this.maxDelay.setValue(this.minDelay.getValue());
                }
                break;
            case "max-delay":
                if (this.minDelay.getValue() > this.maxDelay.getValue()) {
                    this.minDelay.setValue(this.maxDelay.getValue());
                }
                break;
        }
    }
}
