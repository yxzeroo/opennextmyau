package myau.module.modules;

import java.util.Objects;
import myau.Myau;
import myau.event.EventTarget;
import myau.event.events.LeftClickMouseEvent;
import myau.event.events.RightClickMouseEvent;
import myau.event.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.util.ItemUtil;
import myau.util.KeyBindUtil;
import myau.util.RandomUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.WorldSettings;

public class AutoClicker extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private boolean clickPending = false;
    private long attackDelay = 0L;
    private boolean releaseBlockPending = false;
    private long blockHoldRemaining = 0L;
    private long blockCooldownRemaining = 0L;

    public final IntProperty minCps = new IntProperty("min-cps", 10, 1, 20);
    public final IntProperty maxCps = new IntProperty("max-cps", 14, 1, 20);
    public final BooleanProperty blockHit = new BooleanProperty("block-hit", false);
    public final FloatProperty blockHitHold = new FloatProperty("block-hit-hold", 1.5F, 1.0F, 20.0F, this.blockHit::getValue);
    public final FloatProperty blockHitDelay = new FloatProperty("block-hit-delay", 0.0F, 0.0F, 20.0F, this.blockHit::getValue);
    public final IntProperty blockHitHurtTime = new IntProperty("block-hit-hurt-time", 6, 0, 10, this.blockHit::getValue);
    public final BooleanProperty weaponsOnly = new BooleanProperty("weapons-only", true);
    public final BooleanProperty allowTools = new BooleanProperty("allow-tools", false, this.weaponsOnly::getValue);
    public final BooleanProperty breakBlocks = new BooleanProperty("break-blocks", true);

    public AutoClicker() {
        super("AutoClicker", false, 0);
    }

    private long getClickDelay() {
        return 1000L / RandomUtil.nextLong(this.minCps.getValue(), this.maxCps.getValue());
    }

    private long getBlockHoldDuration() {
        return (long) (this.blockHitHold.getValue() * 50.0F);
    }

    private long getBlockDelayDuration() {
        return (long) (this.blockHitDelay.getValue() * 50.0F);
    }

    private boolean isLookingAtBlock() {
        return mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
    }

    private boolean canClick() {
        if (this.weaponsOnly.getValue()
                && !ItemUtil.isHoldingWeapon()
                && (!this.allowTools.getValue() || !ItemUtil.isHoldingTool())) {
            return false;
        }
        if (this.breakBlocks.getValue() && this.isLookingAtBlock()) {
            WorldSettings.GameType gameType = mc.playerController.getCurrentGameType();
            return gameType != WorldSettings.GameType.SURVIVAL && gameType != WorldSettings.GameType.CREATIVE;
        }
        return true;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.attackDelay > 0L) {
            this.attackDelay -= 50L;
        }
        if (this.blockHoldRemaining > 0L) {
            this.blockHoldRemaining -= 50L;
        }
        if (this.blockCooldownRemaining > 0L) {
            this.blockCooldownRemaining -= 50L;
        }
        if (mc.currentScreen != null) {
            this.clickPending = false;
            this.releaseBlockPending = false;
            return;
        }
        if (this.clickPending) {
            this.clickPending = false;
            KeyBindUtil.updateKeyState(mc.gameSettings.keyBindAttack.getKeyCode());
        }
        if (this.releaseBlockPending) {
            this.releaseBlockPending = false;
            KeyBindUtil.updateKeyState(mc.gameSettings.keyBindUseItem.getKeyCode());
        }
        if (!this.isEnabled() || !this.canClick() || !mc.gameSettings.keyBindAttack.isKeyDown()) {
            return;
        }
        if (!mc.thePlayer.isUsingItem()) {
            while (this.attackDelay <= 0L) {
                this.clickPending = true;
                this.attackDelay += this.getClickDelay();
                KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
                KeyBindUtil.pressKeyOnce(mc.gameSettings.keyBindAttack.getKeyCode());
            }
        }
        if (this.blockHit.getValue() && mc.gameSettings.keyBindUseItem.isKeyDown() && ItemUtil.isHoldingSword()) {
            boolean hurtDelayActive = Myau.hurtTimeTracker.getHurtTicks() > this.blockHitHurtTime.getValue();
            if (this.blockHoldRemaining <= 0L || hurtDelayActive) {
                this.releaseBlockPending = true;
                KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                if (this.blockCooldownRemaining > 0L || hurtDelayActive) {
                    return;
                }
            }
            if (this.blockHoldRemaining <= 0L && !mc.thePlayer.isUsingItem()) {
                this.blockHoldRemaining += this.getBlockHoldDuration();
                this.blockCooldownRemaining += this.getBlockHoldDuration() + 50L + this.getBlockDelayDuration();
                KeyBindUtil.pressKeyOnce(mc.gameSettings.keyBindUseItem.getKeyCode());
            }
        }
    }

    @EventTarget(priority = -100)
    public void onLeftClick(LeftClickMouseEvent event) {
        if (!this.isEnabled() || event.isCancelled()) {
            return;
        }
        if (!this.clickPending) {
            this.attackDelay += this.getClickDelay();
        }
    }

    @EventTarget(priority = -100)
    public void onRightClick(RightClickMouseEvent event) {
        if (!this.isEnabled() || event.isCancelled() || !ItemUtil.isHoldingSword()) {
            return;
        }
        if (!this.releaseBlockPending) {
            this.blockHoldRemaining += this.getBlockHoldDuration();
        }
    }

    @Override
    public void onEnabled() {
        this.attackDelay = 0L;
        this.blockHoldRemaining = 0L;
        this.blockCooldownRemaining = 0L;
    }

    @Override
    public void verifyValue(String propertyName) {
        if (this.minCps.getName().equals(propertyName)) {
            if (this.minCps.getValue() > this.maxCps.getValue()) {
                this.maxCps.setValue(this.minCps.getValue());
            }
        } else if (this.maxCps.getName().equals(propertyName)) {
            if (this.minCps.getValue() > this.maxCps.getValue()) {
                this.minCps.setValue(this.maxCps.getValue());
            }
        }
    }

    @Override
    public String[] getSuffix() {
        if (Objects.equals(this.minCps.getValue(), this.maxCps.getValue())) {
            return new String[]{this.minCps.getValue().toString()};
        }
        return new String[]{String.format("%d-%d", this.minCps.getValue(), this.maxCps.getValue())};
    }
}
