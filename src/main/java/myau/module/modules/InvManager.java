package myau.module.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import myau.event.EventTarget;
import myau.event.events.UpdateEvent;
import myau.event.events.WindowClickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.util.ItemUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings;
import org.apache.commons.lang3.RandomUtils;

public class InvManager extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int clickDelay = 0;
    private int openDelayTicks = 0;
    private boolean inInventory = false;
    public final IntProperty minDelay = new IntProperty("min-delay", 1, 0, 20);
    public final IntProperty maxDelay = new IntProperty("max-delay", 2, 0, 20);
    public final IntProperty openDelay = new IntProperty("open-delay", 1, 0, 20);
    public final BooleanProperty autoArmor = new BooleanProperty("auto-armor", true);
    public final BooleanProperty dropTrash = new BooleanProperty("drop-trash", false);
    public final IntProperty swordSlot = new IntProperty("sword-slot", 1, 0, 9);
    public final IntProperty pickaxeSlot = new IntProperty("pickaxe-slot", 3, 0, 9);
    public final IntProperty shovelSlot = new IntProperty("shovel-slot", 4, 0, 9);
    public final IntProperty axeSlot = new IntProperty("axe-slot", 5, 0, 9);
    public final IntProperty blocksSlot = new IntProperty("blocks-slot", 2, 0, 9);
    public final IntProperty blocks = new IntProperty("blocks", 128, 64, 2304);

    public InvManager() {
        super("InvManager", false, 0);
    }

    private boolean isValidGameMode() {
        WorldSettings.GameType gameType = mc.playerController.getCurrentGameType();
        return gameType == WorldSettings.GameType.SURVIVAL || gameType == WorldSettings.GameType.ADVENTURE;
    }

    private int convertSlotIndex(int slot) {
        if (slot >= 36) {
            return 8 - (slot - 36);
        }
        return slot <= 8 ? slot + 36 : slot;
    }

    private void clickSlot(int windowId, int slotId, int mouseButtonClicked, int mode) {
        mc.playerController.windowClick(windowId, slotId, mouseButtonClicked, mode, mc.thePlayer);
    }

    private int getStackSize(int slot) {
        if (slot == -1) {
            return 0;
        }
        ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
        return stack != null ? stack.stackSize : 0;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.clickDelay > 0) {
            this.clickDelay--;
        }
        if (this.openDelayTicks > 0) {
            this.openDelayTicks--;
        }
        if (!(mc.currentScreen instanceof GuiInventory)) {
            this.inInventory = false;
            return;
        }
        if (!(((GuiInventory) mc.currentScreen).inventorySlots instanceof ContainerPlayer)) {
            this.inInventory = false;
            return;
        }
        if (!this.inInventory) {
            this.inInventory = true;
            this.openDelayTicks = this.openDelay.getValue() + 1;
        }
        if (this.openDelayTicks > 0 || this.clickDelay > 0) {
            return;
        }
        if (!this.isEnabled() || !this.isValidGameMode()) {
            return;
        }

        ArrayList<Integer> bestArmorHighDurability = new ArrayList<>(Arrays.asList(-1, -1, -1, -1));
        ArrayList<Integer> bestArmorAny = new ArrayList<>(Arrays.asList(-1, -1, -1, -1));
        for (int armorType = 0; armorType < 4; armorType++) {
            bestArmorHighDurability.set(armorType, ItemUtil.findArmorSlot(armorType, true));
            bestArmorAny.set(armorType, ItemUtil.findArmorSlot(armorType, false));
        }

        int preferredSwordHotbar = this.swordSlot.getValue() - 1;
        int bestSwordHighDurability = ItemUtil.findSwordSlot(preferredSwordHotbar, true);
        int bestSwordAny = ItemUtil.findSwordSlot(preferredSwordHotbar, false);

        int preferredPickaxeHotbar = this.pickaxeSlot.getValue() - 1;
        int bestPickaxeHighDurability = ItemUtil.findToolSlot("pickaxe", preferredPickaxeHotbar, true);
        int bestPickaxeAny = ItemUtil.findToolSlot("pickaxe", preferredPickaxeHotbar, false);

        int preferredShovelHotbar = this.shovelSlot.getValue() - 1;
        int bestShovelHighDurability = ItemUtil.findToolSlot("shovel", preferredShovelHotbar, true);
        int bestShovelAny = ItemUtil.findToolSlot("shovel", preferredShovelHotbar, false);

        int preferredAxeHotbar = this.axeSlot.getValue() - 1;
        int bestAxeHighDurability = ItemUtil.findToolSlot("axe", preferredAxeHotbar, true);
        int bestAxeAny = ItemUtil.findToolSlot("axe", preferredAxeHotbar, false);

        int preferredBlocksHotbar = this.blocksSlot.getValue() - 1;
        int bestBlockSlot = ItemUtil.findBlockSlot(preferredBlocksHotbar);

        int windowId = mc.thePlayer.inventoryContainer.windowId;

        if (this.autoArmor.getValue()) {
            for (int armorType = 0; armorType < 4; armorType++) {
                int highDurabilitySlot = bestArmorHighDurability.get(armorType);
                int anySlot = bestArmorAny.get(armorType);
                if (highDurabilitySlot == -1 && anySlot == -1) {
                    continue;
                }
                int equippedArmorSlot = 39 - armorType;
                if (highDurabilitySlot == equippedArmorSlot || anySlot == equippedArmorSlot) {
                    continue;
                }
                if (mc.thePlayer.inventory.getStackInSlot(equippedArmorSlot) != null) {
                    if (mc.thePlayer.inventory.getFirstEmptyStack() != -1) {
                        this.clickSlot(windowId, this.convertSlotIndex(equippedArmorSlot), 0, 1);
                    } else {
                        this.clickSlot(windowId, this.convertSlotIndex(equippedArmorSlot), 1, 4);
                    }
                } else {
                    int targetArmorSlot = highDurabilitySlot != -1 ? highDurabilitySlot : anySlot;
                    this.clickSlot(windowId, this.convertSlotIndex(targetArmorSlot), 0, 1);
                }
                return;
            }
        }

        LinkedHashSet<Integer> reservedHotbarSlots = new LinkedHashSet<>();

        if (preferredSwordHotbar >= 0 && preferredSwordHotbar <= 8 && (bestSwordHighDurability != -1 || bestSwordAny != -1)) {
            reservedHotbarSlots.add(preferredSwordHotbar);
            if (bestSwordHighDurability != preferredSwordHotbar && bestSwordAny != preferredSwordHotbar) {
                int sourceSlot = bestSwordHighDurability != -1 ? bestSwordHighDurability : bestSwordAny;
                this.clickSlot(windowId, this.convertSlotIndex(sourceSlot), preferredSwordHotbar, 2);
                return;
            }
        }

        if (preferredPickaxeHotbar >= 0
                && preferredPickaxeHotbar <= 8
                && !reservedHotbarSlots.contains(preferredPickaxeHotbar)
                && (bestPickaxeHighDurability != -1 || bestPickaxeAny != -1)) {
            reservedHotbarSlots.add(preferredPickaxeHotbar);
            if (bestPickaxeHighDurability != preferredPickaxeHotbar && bestPickaxeAny != preferredPickaxeHotbar) {
                int sourceSlot = bestPickaxeHighDurability != -1 ? bestPickaxeHighDurability : bestPickaxeAny;
                this.clickSlot(windowId, this.convertSlotIndex(sourceSlot), preferredPickaxeHotbar, 2);
                return;
            }
        }

        if (preferredShovelHotbar >= 0
                && preferredShovelHotbar <= 8
                && !reservedHotbarSlots.contains(preferredShovelHotbar)
                && (bestShovelHighDurability != -1 || bestShovelAny != -1)) {
            reservedHotbarSlots.add(preferredShovelHotbar);
            if (bestShovelHighDurability != preferredShovelHotbar && bestShovelAny != preferredShovelHotbar) {
                int sourceSlot = bestShovelHighDurability != -1 ? bestShovelHighDurability : bestShovelAny;
                this.clickSlot(windowId, this.convertSlotIndex(sourceSlot), preferredShovelHotbar, 2);
                return;
            }
        }

        if (preferredAxeHotbar >= 0
                && preferredAxeHotbar <= 8
                && !reservedHotbarSlots.contains(preferredAxeHotbar)
                && (bestAxeHighDurability != -1 || bestAxeAny != -1)) {
            reservedHotbarSlots.add(preferredAxeHotbar);
            if (bestAxeHighDurability != preferredAxeHotbar && bestAxeAny != preferredAxeHotbar) {
                int sourceSlot = bestAxeHighDurability != -1 ? bestAxeHighDurability : bestAxeAny;
                this.clickSlot(windowId, this.convertSlotIndex(sourceSlot), preferredAxeHotbar, 2);
                return;
            }
        }

        if (preferredBlocksHotbar >= 0
                && preferredBlocksHotbar <= 8
                && !reservedHotbarSlots.contains(preferredBlocksHotbar)
                && bestBlockSlot != -1) {
            reservedHotbarSlots.add(preferredBlocksHotbar);
            if (bestBlockSlot != preferredBlocksHotbar) {
                this.clickSlot(windowId, this.convertSlotIndex(bestBlockSlot), preferredBlocksHotbar, 2);
                return;
            }
        }

        if (this.dropTrash.getValue()) {
            int totalBlocks = this.getStackSize(bestBlockSlot);
            for (int slot = 0; slot < 36; slot++) {
                if (bestArmorHighDurability.contains(slot)
                        || bestArmorAny.contains(slot)
                        || bestSwordHighDurability == slot
                        || bestSwordAny == slot
                        || bestPickaxeHighDurability == slot
                        || bestPickaxeAny == slot
                        || bestShovelHighDurability == slot
                        || bestShovelAny == slot
                        || bestAxeHighDurability == slot
                        || bestAxeAny == slot
                        || bestBlockSlot == slot) {
                    continue;
                }
                ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
                if (stack == null) {
                    continue;
                }
                boolean isBlock = ItemUtil.isBlock(stack);
                if (ItemUtil.isTrash(stack) || (isBlock && totalBlocks >= this.blocks.getValue())) {
                    this.clickSlot(windowId, this.convertSlotIndex(slot), 1, 4);
                    return;
                }
                if (isBlock) {
                    totalBlocks += stack.stackSize;
                }
            }
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
