package myau.module.modules;

import myau.event.EventTarget;
import myau.event.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.util.ItemUtil;
import myau.util.KeyBindUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;

public class AutoTool extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int currentToolSlot = -1;
    private int prevSlot = -1;
    private int miningTicks = 0;
    public final IntProperty delay = new IntProperty("delay", 0, 0, 5);
    public final BooleanProperty switchBack = new BooleanProperty("switch-back", true);
    public final BooleanProperty sneakOnly = new BooleanProperty("sneak-only", true);

    public AutoTool() {
        super("AutoTool", false, 0);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.currentToolSlot != -1 && this.currentToolSlot != mc.thePlayer.inventory.currentItem) {
            this.currentToolSlot = -1;
            this.prevSlot = -1;
        }
        if (mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                && mc.gameSettings.keyBindAttack.isKeyDown()
                && !mc.thePlayer.isUsingItem()) {
            if (this.miningTicks >= this.delay.getValue()
                    && (!this.sneakOnly.getValue() || KeyBindUtil.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()))) {
                int bestSlot = ItemUtil.findBestToolSlot(
                        mc.thePlayer.inventory.currentItem,
                        mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock()
                );
                if (mc.thePlayer.inventory.currentItem != bestSlot) {
                    if (this.prevSlot == -1) {
                        this.prevSlot = mc.thePlayer.inventory.currentItem;
                    }
                    mc.thePlayer.inventory.currentItem = this.currentToolSlot = bestSlot;
                }
            }
            this.miningTicks++;
            return;
        }
        if (this.switchBack.getValue() && this.prevSlot != -1) {
            mc.thePlayer.inventory.currentItem = this.prevSlot;
        }
        this.currentToolSlot = -1;
        this.prevSlot = -1;
        this.miningTicks = 0;
    }

    @Override
    public void onDisabled() {
        this.currentToolSlot = -1;
        this.prevSlot = -1;
        this.miningTicks = 0;
    }
}
