package myau.module.modules;

import myau.Myau;
import myau.enums.FloatModules;
import myau.event.EventTarget;
import myau.event.events.LivingUpdateEvent;
import myau.event.events.MoveInputEvent;
import myau.event.events.RightClickMouseEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.util.BlockUtil;
import myau.util.ItemUtil;
import myau.util.PlayerUtil;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.BlockPos;

public class NoSlow extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int lastFloatSlot = -1;
    public final ModeProperty swordMode = new ModeProperty("sword-mode", 1, new String[]{"NONE", "VANILLA"});
    public final PercentProperty swordMotion = new PercentProperty("sword-motion", 20, () -> this.swordMode.getValue() != 0);
    public final BooleanProperty swordSprint = new BooleanProperty("sword-sprint", true, () -> this.swordMode.getValue() != 0);
    public final ModeProperty foodMode = new ModeProperty("food-mode", 0, new String[]{"NONE", "VANILLA", "FLOAT"});
    public final PercentProperty foodMotion = new PercentProperty("food-motion", 20, () -> this.foodMode.getValue() != 0);
    public final BooleanProperty foodSprint = new BooleanProperty("food-sprint", true, () -> this.foodMode.getValue() != 0);
    public final ModeProperty bowMode = new ModeProperty("bow-mode", 0, new String[]{"NONE", "VANILLA", "FLOAT"});
    public final PercentProperty bowMotion = new PercentProperty("bow-motion", 20, () -> this.bowMode.getValue() != 0);
    public final BooleanProperty bowSprint = new BooleanProperty("bow-sprint", true, () -> this.bowMode.getValue() != 0);

    public NoSlow() {
        super("NoSlow", false, 0);
    }

    public boolean isSwordActive() {
        return this.swordMode.getValue() != 0 && ItemUtil.isHoldingSword();
    }

    public boolean isFoodActive() {
        return this.foodMode.getValue() != 0 && ItemUtil.isEatingOrDrinking();
    }

    public boolean isBowActive() {
        return this.bowMode.getValue() != 0 && ItemUtil.isHoldingBow();
    }

    public boolean isFloatMode() {
        return (this.foodMode.getValue() == 2 && ItemUtil.isEatingOrDrinking())
                || (this.bowMode.getValue() == 2 && ItemUtil.isHoldingBow());
    }

    public boolean isAnyActive() {
        if (!mc.thePlayer.isUsingItem()) {
            return false;
        }
        return this.isSwordActive() || this.isFoodActive() || this.isBowActive();
    }

    public boolean canSprint() {
        return (this.isSwordActive() && this.swordSprint.getValue())
                || (this.isFoodActive() && this.foodSprint.getValue())
                || (this.isBowActive() && this.bowSprint.getValue());
    }

    public int getMotionMultiplier() {
        if (ItemUtil.isHoldingSword()) {
            return this.swordMotion.getValue();
        }
        if (ItemUtil.isEatingOrDrinking()) {
            return this.foodMotion.getValue();
        }
        if (ItemUtil.isHoldingBow()) {
            return this.bowMotion.getValue();
        }
        return 100;
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (!this.isEnabled() || !this.isAnyActive()) {
            return;
        }
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killAura.isBlocking() && killAura.autoBlockNoSlow.getValue()) {
            return;
        }
        float multiplier = (float) this.getMotionMultiplier() / 100.0F;
        mc.thePlayer.movementInput.moveForward *= multiplier;
        mc.thePlayer.movementInput.moveStrafe *= multiplier;
        if (!this.canSprint()) {
            mc.thePlayer.setSprinting(false);
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (!this.isEnabled() || !this.isFloatMode()) {
            this.lastFloatSlot = -1;
            Myau.floatManager.setFloatState(false, FloatModules.NO_SLOW);
            return;
        }
        int currentSlot = mc.thePlayer.inventory.currentItem;
        if (this.lastFloatSlot != currentSlot && PlayerUtil.isUsingItem()) {
            this.lastFloatSlot = currentSlot;
            Myau.floatManager.setFloatState(true, FloatModules.NO_SLOW);
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (mc.objectMouseOver != null) {
            switch (mc.objectMouseOver.typeOfHit) {
                case BLOCK: {
                    BlockPos blockPos = mc.objectMouseOver.getBlockPos();
                    if (BlockUtil.isInteractable(blockPos) && !PlayerUtil.isSneaking()) {
                        return;
                    }
                    break;
                }
                case ENTITY: {
                    Entity entity = mc.objectMouseOver.entityHit;
                    if (entity instanceof EntityVillager) {
                        return;
                    }
                    if (entity instanceof EntityLivingBase && TeamUtil.isShopNPC((EntityLivingBase) entity)) {
                        return;
                    }
                    break;
                }
            }
        }
        if (this.isFloatMode() && !Myau.floatManager.isFloating() && mc.thePlayer.onGround) {
            event.setCancelled(true);
            mc.thePlayer.motionY = 0.42F;
        }
    }
}
