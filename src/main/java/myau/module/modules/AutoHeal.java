package myau.module.modules;

import myau.event.EventTarget;
import myau.event.events.HitBlockEvent;
import myau.event.events.LeftClickMouseEvent;
import myau.event.events.PostTickEvent;
import myau.event.events.RightClickMouseEvent;
import myau.event.events.SwapItemEvent;
import myau.event.events.TickEvent;
import myau.mixin.IAccessorPlayerControllerMP;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.PercentProperty;
import myau.util.PacketUtil;
import myau.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemSoup;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.potion.Potion;

public class AutoHeal extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final TimerUtil timer = new TimerUtil();
    private boolean wasLowHealth = false;
    private int prevSlot = -1;
    public final PercentProperty health = new PercentProperty("health", 35);
    public final IntProperty delay = new IntProperty("delay", 4000, 0, 5000);
    public final BooleanProperty regenCheck = new BooleanProperty("regen-check", false);

    public AutoHeal() {
        super("AutoHeal", false, 0);
    }

    private int findHealSlot() {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            if (stack != null && stack.hasDisplayName()) {
                String displayName = stack.getDisplayName();
                if (stack.getItem() instanceof ItemSkull
                        && ((displayName.contains("\u00a76") && displayName.contains("Golden Head"))
                        || displayName.matches("\\S+\u00a7c's Head"))) {
                    return slot;
                }
                if (stack.getItem() instanceof ItemSoup
                        && ((displayName.contains("\u00a7a") && displayName.contains("Tasty Soup"))
                        || (displayName.contains("\u00a7a") && displayName.contains("Assist Soup")))) {
                    return slot;
                }
            }
        }
        return -1;
    }

    private boolean hasRegen() {
        return this.regenCheck.getValue() && mc.thePlayer.isPotionActive(Potion.regeneration);
    }

    public boolean isSwitching() {
        return this.prevSlot != -1;
    }

    @EventTarget(priority = 50)
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        float totalHealth = (float) Math.ceil(mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount());
        boolean lowHealth = totalHealth / mc.thePlayer.getMaxHealth() <= (float) this.health.getValue() / 100.0F;
        if (this.wasLowHealth && lowHealth && !this.hasRegen() && this.timer.hasTimeElapsed(this.delay.getValue())) {
            int healSlot = this.findHealSlot();
            if (healSlot != -1) {
                this.prevSlot = mc.thePlayer.inventory.currentItem;
                mc.thePlayer.inventory.currentItem = healSlot;
                ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
                PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                this.timer.reset();
            }
        }
        this.wasLowHealth = lowHealth;
    }

    @EventTarget
    public void onPostTick(PostTickEvent event) {
        if (this.prevSlot != -1) {
            mc.thePlayer.inventory.currentItem = this.prevSlot;
            this.prevSlot = -1;
        }
    }

    @EventTarget
    public void onLeftClick(LeftClickMouseEvent event) {
        if (this.isEnabled() && this.isSwitching()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled() && this.isSwitching()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onHitBlock(HitBlockEvent event) {
        if (this.isEnabled() && this.isSwitching()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onSwapItem(SwapItemEvent event) {
        if (this.isEnabled() && this.isSwitching()) {
            event.setCancelled(true);
        }
    }
}
