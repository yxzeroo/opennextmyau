package myau.module.modules;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import myau.event.EventTarget;
import myau.event.events.TickEvent;
import myau.mixin.IAccessorMinecraft;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.util.RotationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;

public class FastPlace extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat DELAY_FORMAT = new DecimalFormat("0.0#", new DecimalFormatSymbols(Locale.US));
    private long delayMs = 0L;
    public final FloatProperty delay = new FloatProperty("delay", 1.0F, 1.0F, 3.0F);
    public final BooleanProperty blocksOnly = new BooleanProperty("blocks-only", true);
    public final BooleanProperty placeFix = new BooleanProperty("place-fix", true);

    public FastPlace() {
        super("FastPlace", false, 0);
    }

    private boolean canFastPlace() {
        ItemStack stack = mc.thePlayer.getHeldItem();
        if (stack != null) {
            Item item = stack.getItem();
            if (item instanceof ItemFishingRod) {
                return false;
            }
            if (item instanceof ItemBlock) {
                if (!this.placeFix.getValue()) {
                    return true;
                }
                MovingObjectPosition mop = RotationUtil.rayTraceBlocks(
                        mc.thePlayer.rotationYaw,
                        mc.thePlayer.rotationPitch,
                        (double) mc.playerController.getBlockReachDistance(),
                        1.0F
                );
                return mop != null
                        && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                        && ((ItemBlock) item).canPlaceBlockOnSide(mc.theWorld, mop.getBlockPos(), mop.sideHit, mc.thePlayer, stack);
            }
        }
        return !this.blocksOnly.getValue();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        int rightClickDelay = ((IAccessorMinecraft) mc).getRightClickDelayTimer();
        if (rightClickDelay == 4) {
            this.delayMs += (long) (50.0F * this.delay.getValue());
        }
        if (this.delayMs > 0L) {
            this.delayMs -= 50L;
        }
        if (this.delayMs <= 0L && rightClickDelay > 1 && this.canFastPlace()) {
            ((IAccessorMinecraft) mc).setRightClickDelayTimer(0);
        }
    }

    @Override
    public void onDisabled() {
        this.delayMs = 0L;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{DELAY_FORMAT.format(this.delay.getValue())};
    }
}
