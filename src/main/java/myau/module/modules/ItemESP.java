package myau.module.modules;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import myau.data.TrackedBed;
import myau.enums.ChatColors;
import myau.event.EventTarget;
import myau.event.events.Render3DEvent;
import myau.mixin.IAccessorRenderManager;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.PercentProperty;
import myau.util.MathUtil;
import myau.util.RenderUtil;
import myau.util.TeamUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;

public class ItemESP extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final PercentProperty opacity = new PercentProperty("opacity", 25);
    public final BooleanProperty outline = new BooleanProperty("outline", false);
    public final BooleanProperty itemCount = new BooleanProperty("item-count", true);
    public final BooleanProperty autoScale = new BooleanProperty("auto-scale", true);
    public final BooleanProperty emeralds = new BooleanProperty("emeralds", true);
    public final BooleanProperty diamonds = new BooleanProperty("diamonds", true);
    public final BooleanProperty gold = new BooleanProperty("gold", true);
    public final BooleanProperty iron = new BooleanProperty("iron", true);

    public ItemESP() {
        super("ItemESP", false, 0);
    }

    private boolean shouldHighlightItem(int itemId) {
        if (this.emeralds.getValue() && this.isEmeraldItem(itemId)) {
            return true;
        }
        if (this.diamonds.getValue() && this.isDiamondItem(itemId)) {
            return true;
        }
        if (this.gold.getValue() && this.isGoldItem(itemId)) {
            return true;
        }
        return this.iron.getValue() && this.isIronItem(itemId);
    }

    private boolean isEmeraldItem(int itemId) {
        Item item = Item.getItemById(itemId);
        Block block = Block.getBlockFromItem(item);
        return item == Items.emerald || block == Blocks.emerald_block || block == Blocks.emerald_ore;
    }

    private boolean isDiamondItem(int itemId) {
        Item item = Item.getItemById(itemId);
        Block block = Block.getBlockFromItem(item);
        return item == Items.diamond
                || item == Items.diamond_sword
                || item == Items.diamond_pickaxe
                || item == Items.diamond_shovel
                || item == Items.diamond_axe
                || item == Items.diamond_hoe
                || item == Items.diamond_helmet
                || item == Items.diamond_chestplate
                || item == Items.diamond_leggings
                || item == Items.diamond_boots
                || block == Blocks.diamond_block
                || block == Blocks.diamond_ore;
    }

    private boolean isGoldItem(int itemId) {
        Item item = Item.getItemById(itemId);
        Block block = Block.getBlockFromItem(item);
        return item == Items.gold_ingot
                || item == Items.gold_nugget
                || item == Items.golden_apple
                || block == Blocks.gold_block
                || block == Blocks.gold_ore;
    }

    private boolean isIronItem(int itemId) {
        Item item = Item.getItemById(itemId);
        Block block = Block.getBlockFromItem(item);
        return item == Items.iron_ingot || block == Blocks.iron_block || block == Blocks.iron_ore;
    }

    private Color getItemColor(int itemId) {
        if (this.isEmeraldItem(itemId)) {
            return new Color(ChatColors.GREEN.toRgb());
        }
        if (this.isDiamondItem(itemId)) {
            return new Color(ChatColors.AQUA.toRgb());
        }
        if (this.isGoldItem(itemId)) {
            return new Color(ChatColors.YELLOW.toRgb());
        }
        if (this.isIronItem(itemId)) {
            return new Color(ChatColors.WHITE.toRgb());
        }
        return new Color(ChatColors.GRAY.toRgb());
    }

    private int getItemPriority(int itemId) {
        if (this.isEmeraldItem(itemId)) {
            return 4;
        }
        if (this.isDiamondItem(itemId)) {
            return 3;
        }
        if (this.isGoldItem(itemId)) {
            return 2;
        }
        if (this.isIronItem(itemId)) {
            return 1;
        }
        return 0;
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        LinkedHashMap<TrackedBed, Integer> groupedItems = new LinkedHashMap<>();
        for (Entity entity : TeamUtil.getLoadedEntitiesSorted()) {
            if (entity.ticksExisted < 3) {
                continue;
            }
            if (!entity.ignoreFrustumCheck && !RenderUtil.isInViewFrustum(entity, 0.125)) {
                continue;
            }
            if (!(entity instanceof EntityItem)) {
                continue;
            }
            EntityItem entityItem = (EntityItem) entity;
            ItemStack stack = entityItem.getEntityItem();
            if (stack.stackSize <= 0) {
                continue;
            }
            int itemId = Item.getIdFromItem(stack.getItem());
            if (!this.shouldHighlightItem(itemId)) {
                continue;
            }
            double interpX = MathUtil.lerp(entityItem.lastTickPosX, entityItem.posX, event.getPartialTicks());
            double interpY = MathUtil.lerp(entityItem.lastTickPosY, entityItem.posY, event.getPartialTicks());
            double interpZ = MathUtil.lerp(entityItem.lastTickPosZ, entityItem.posZ, event.getPartialTicks());
            TrackedBed itemGroupKey = new TrackedBed(itemId, interpX, interpY, interpZ);
            Integer existingCount = groupedItems.get(itemGroupKey);
            groupedItems.put(new TrackedBed(itemId, interpX, interpY, interpZ), stack.stackSize + (existingCount == null ? 0 : existingCount));
        }
        List<Map.Entry<TrackedBed, Integer>> sortedEntries = groupedItems.entrySet().stream()
                .sorted((first, second) -> Integer.compare(
                        this.getItemPriority(first.getKey().itemKey),
                        this.getItemPriority(second.getKey().itemKey)
                ))
                .collect(Collectors.toList());
        IAccessorRenderManager renderManager = (IAccessorRenderManager) mc.getRenderManager();
        for (Map.Entry<TrackedBed, Integer> entry : sortedEntries) {
            TrackedBed itemPos = entry.getKey();
            Color itemColor = this.getItemColor(itemPos.itemKey);
            double renderX = itemPos.x - renderManager.getRenderPosX();
            double renderY = itemPos.y - renderManager.getRenderPosY();
            double renderZ = itemPos.z - renderManager.getRenderPosZ();
            double distance = mc.getRenderViewEntity().getDistance(itemPos.x, itemPos.y, itemPos.z);
            double boxSize = 0.5 + 0.375 * ((Math.max(6.0, this.autoScale.getValue() ? distance : 6.0) - 6.0) / 28.0);
            AxisAlignedBB boundingBox = new AxisAlignedBB(
                    renderX - boxSize * 0.5,
                    renderY,
                    renderZ - boxSize * 0.5,
                    renderX + boxSize * 0.5,
                    renderY + boxSize,
                    renderZ + boxSize * 0.5
            );
            RenderUtil.enableRenderState();
            if (this.opacity.getValue() > 0) {
                int fillAlpha = (int) ((float) this.opacity.getValue() / 100.0f * 255.0f);
                RenderUtil.drawFilledBox(boundingBox, itemColor.getRed(), itemColor.getGreen(), itemColor.getBlue(), fillAlpha);
                GlStateManager.resetColor();
            }
            if (this.outline.getValue()) {
                RenderUtil.drawBoundingBox(boundingBox, itemColor.getRed(), itemColor.getGreen(), itemColor.getBlue(), 255, 1.5f);
                GlStateManager.resetColor();
            }
            RenderUtil.disableRenderState();
            if (this.itemCount.getValue()) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(renderX, renderY + boxSize * 0.5, renderZ);
                GlStateManager.rotate(mc.getRenderManager().playerViewY * -1.0f, 0.0f, 1.0f, 0.0f);
                GlStateManager.rotate(mc.getRenderManager().playerViewX, mc.gameSettings.thirdPersonView == 2 ? -1.0f : 1.0f, 0.0f, 0.0f);
                double fontScale = -0.04375 - 0.0328125 * ((Math.max(6.0, this.autoScale.getValue() ? distance : 6.0) - 6.0) / 28.0);
                GlStateManager.scale(fontScale, fontScale, 1.0);
                GlStateManager.disableDepth();
                String countText = String.format("%d", entry.getValue());
                float textX = ((float) mc.fontRendererObj.getStringWidth(countText) / 2.0f - 0.5f) * -1.0f;
                float textY = ((float) (mc.fontRendererObj.FONT_HEIGHT / 2) - 0.5f) * -1.0f;
                RenderUtil.drawOutlinedString(countText, textX, textY, itemColor.getRGB());
                GlStateManager.enableDepth();
                GlStateManager.resetColor();
                GlStateManager.popMatrix();
            }
        }
    }
}
