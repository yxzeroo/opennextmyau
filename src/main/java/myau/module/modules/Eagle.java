package myau.module.modules;

import java.util.Objects;
import myau.event.EventTarget;
import myau.event.events.RightClickDelayEvent;
import myau.event.events.RightClickMouseEvent;
import myau.event.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.PercentProperty;
import myau.util.ItemUtil;
import myau.util.MathUtil;
import myau.util.MoveUtil;
import myau.util.PlayerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import org.apache.commons.lang3.RandomUtils;

public class Eagle extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int sneakDelay = 0;
    public final IntProperty minDelay = new IntProperty("min-delay", 2, 0, 10);
    public final IntProperty maxDelay = new IntProperty("max-delay", 3, 0, 10);
    public final PercentProperty offset = new PercentProperty("offset", 100);
    public final BooleanProperty align = new BooleanProperty("align", true);
    public final BooleanProperty directionCheck = new BooleanProperty("direction-check", true);
    public final BooleanProperty pitchCheck = new BooleanProperty("pitch-check", true);
    public final BooleanProperty blocksOnly = new BooleanProperty("blocks-only", true);

    public Eagle() {
        super("Eagle", false, 0);
    }

    private boolean shouldSneakAtEdge() {
        double[] predicted = MoveUtil.predictMovement();
        return PlayerUtil.canMove(
            mc.thePlayer.motionX + predicted[0],
            -1.0,
            mc.thePlayer.motionZ + predicted[1],
            (float) this.offset.getValue() / 100.0F
        );
    }

    private boolean canEagle() {
        if (this.directionCheck.getValue() && mc.gameSettings.keyBindForward.isKeyDown()) {
            return false;
        }
        if (this.pitchCheck.getValue() && mc.thePlayer.rotationPitch < 69.0F) {
            return false;
        }
        if (this.blocksOnly.getValue() && !ItemUtil.isHoldingPlaceableBlock()) {
            return false;
        }
        return mc.thePlayer.onGround;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.sneakDelay > 0) {
            --this.sneakDelay;
        }
        if (this.sneakDelay == 0 && this.shouldSneakAtEdge()) {
            this.sneakDelay = RandomUtils.nextInt(this.minDelay.getValue(), this.maxDelay.getValue() + 1);
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (!this.isEnabled()
            || mc.currentScreen != null
            || !this.align.getValue()
            || mc.gameSettings.keyBindSneak.isKeyDown()) {
            return;
        }
        if (this.canEagle() && (this.sneakDelay > 0 || this.shouldSneakAtEdge())) {
            float yawRadians = MathUtil.toRadians(
                MoveUtil.getDirectionYaw(mc.thePlayer.rotationYaw, MoveUtil.getForwardValue(), MoveUtil.getLeftValue()) - 180.0F
            );
            float dirX = -MathHelper.sin(yawRadians);
            float dirZ = MathHelper.cos(yawRadians);
            if (Math.abs(dirX) < 0.1F || Math.abs(dirZ) < 0.1F) {
                return;
            }
            AxisAlignedBB boundingBox = mc.thePlayer.getEntityBoundingBox();
            double edgeX = dirX >= 0.0F ? boundingBox.maxX : boundingBox.minX;
            double edgeZ = dirZ >= 0.0F ? boundingBox.maxZ : boundingBox.minZ;
            double distX = Math.abs(edgeX - Math.rint(edgeX));
            double distZ = Math.abs(edgeZ - Math.rint(edgeZ));
            if (Math.abs(distX - distZ) > 0.125) {
                event.setCancelled(true);
            }
        }
    }

    @EventTarget(priority = -100)
    public void onRightClickDelay(RightClickDelayEvent event) {
        if (!this.isEnabled() || mc.currentScreen != null || mc.thePlayer.movementInput.sneak) {
            return;
        }
        if (this.canEagle() && (this.sneakDelay > 0 || this.shouldSneakAtEdge())) {
            mc.thePlayer.movementInput.sneak = true;
            mc.thePlayer.movementInput.moveStrafe *= 0.3F;
            mc.thePlayer.movementInput.moveForward *= 0.3F;
        }
    }

    @Override
    public void onDisabled() {
        this.sneakDelay = 0;
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

    @Override
    public String[] getSuffix() {
        if (Objects.equals(this.minDelay.getValue(), this.maxDelay.getValue())) {
            return new String[]{this.minDelay.getValue().toString()};
        }
        return new String[]{String.format("%d-%d", this.minDelay.getValue(), this.maxDelay.getValue())};
    }
}
