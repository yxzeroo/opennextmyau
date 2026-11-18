package myau.module.modules;

import com.google.common.base.CaseFormat;
import myau.Myau;
import myau.enums.BlinkModules;
import myau.event.EventTarget;
import myau.event.events.KeyEvent;
import myau.event.events.LivingUpdateEvent;
import myau.module.Module;
import myau.property.properties.FloatProperty;
import myau.property.properties.ModeProperty;
import myau.util.PlayerUtil;
import myau.util.RandomUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;
import net.minecraft.util.AxisAlignedBB;

public class AntiVoid extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private boolean isInVoid = false;
    private boolean wasInVoid = false;
    private double[] lastSafePosition = null;
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"BLINK"});
    public final FloatProperty distance = new FloatProperty("distance", 5.0F, 0.0F, 16.0F);

    public AntiVoid() {
        super("AntiVoid", false, 0);
    }

    private void resetBlink() {
        Myau.blinkManager.setBlinkState(false, BlinkModules.ANTI_VOID);
        this.lastSafePosition = null;
    }

    private boolean canBlink() {
        LongJump longJump = (LongJump) Myau.moduleManager.modules.get(LongJump.class);
        return !longJump.isJumping();
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled()) {
            this.isInVoid = !mc.thePlayer.capabilities.allowFlying && PlayerUtil.isOverVoid();
            if (this.mode.getValue() == 0) {
                if (!this.isInVoid) {
                    this.resetBlink();
                }
                if (this.lastSafePosition != null) {
                    float halfWidth = mc.thePlayer.width / 2.0F;
                    float height = mc.thePlayer.height;
                    AxisAlignedBB safeBox = new AxisAlignedBB(
                        this.lastSafePosition[0] - (double) halfWidth,
                        this.lastSafePosition[1],
                        this.lastSafePosition[2] - (double) halfWidth,
                        this.lastSafePosition[0] + (double) halfWidth,
                        this.lastSafePosition[1] + (double) height,
                        this.lastSafePosition[2] + (double) halfWidth
                    );
                    if (PlayerUtil.isOverVoid(safeBox)) {
                        this.resetBlink();
                    }
                }
                if (!this.wasInVoid && this.isInVoid && this.canBlink()) {
                    Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                    if (Myau.blinkManager.setBlinkState(true, BlinkModules.ANTI_VOID)) {
                        this.lastSafePosition = new double[]{
                            mc.thePlayer.prevPosX,
                            mc.thePlayer.prevPosY,
                            mc.thePlayer.prevPosZ
                        };
                    }
                }
                if (Myau.blinkManager.getBlinkingModule() == BlinkModules.ANTI_VOID
                    && this.lastSafePosition != null
                    && this.lastSafePosition[1] - (double) this.distance.getValue() > mc.thePlayer.posY) {
                    Myau.blinkManager.blinkedPackets.offerFirst(
                        new C04PacketPlayerPosition(
                            this.lastSafePosition[0],
                            this.lastSafePosition[1] - RandomUtil.nextDouble(10.0, 20.0),
                            this.lastSafePosition[2],
                            false
                        )
                    );
                    this.resetBlink();
                }
            }
            this.wasInVoid = this.isInVoid;
        }
    }

    @EventTarget
    public void onKey(KeyEvent event) {
        if (event.getKey() == mc.gameSettings.keyBindUseItem.getKeyCode()) {
            ItemStack heldItem = mc.thePlayer.inventory.getCurrentItem();
            if (heldItem != null && heldItem.getItem() instanceof ItemEnderPearl) {
                this.resetBlink();
            }
        }
    }

    @Override
    public void onEnabled() {
        this.isInVoid = false;
        this.wasInVoid = false;
        this.resetBlink();
    }

    @Override
    public void onDisabled() {
        Myau.blinkManager.setBlinkState(false, BlinkModules.ANTI_VOID);
    }

    @Override
    public void verifyValue(String propertyName) {
        if (this.isEnabled()) {
            this.onDisabled();
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }
}
