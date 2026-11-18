package myau.module.modules;

import myau.event.EventTarget;
import myau.event.events.LoadWorldEvent;
import myau.event.events.ReceivePacketEvent;
import myau.module.Module;
import myau.util.PacketUtil;
import myau.util.RandomUtil;
import myau.util.RotationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.MathHelper;

public class NoRotate extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private boolean ignoreNextReset = false;

    public NoRotate() {
        super("NoRotate", false, 0);
    }

    public boolean handlePosLook(EntityPlayer player, double x, double y, double z, float yaw, float pitch) {
        if (this.ignoreNextReset) {
            this.ignoreNextReset = false;
            return false;
        }
        if (mc.thePlayer.rotationYaw == -180.0f && mc.thePlayer.rotationPitch == 0.0f) {
            return false;
        }
        float sensitivity = mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
        float gcd = sensitivity * sensitivity * sensitivity * 8.0f;
        float newYaw = RotationUtil.quantizeAngle(
                mc.thePlayer.rotationYaw + gcd * 0.15f * (RandomUtil.nextBoolean() ? 1.0f : -1.0f),
                yaw % 360.0f
        );
        float newPitch = MathHelper.clamp_float(
                RotationUtil.quantizeAngle(
                        mc.thePlayer.rotationPitch + gcd * 0.15f * (RandomUtil.nextBoolean() ? 1.0f : -1.0f),
                        pitch % 360.0f
                ),
                -90.0f,
                90.0f
        );
        player.setPositionAndRotation(x, y, z, yaw, pitch);
        PacketUtil.sendPacket(new C03PacketPlayer.C06PacketPlayerPosLook(
                mc.thePlayer.posX,
                mc.thePlayer.getEntityBoundingBox().minY,
                mc.thePlayer.posZ,
                yaw % 360.0f,
                pitch % 360.0f,
                false
        ));
        mc.thePlayer.rotationYaw = newYaw;
        mc.thePlayer.rotationPitch = newPitch;
        return true;
    }

    @EventTarget
    public void onReceivePacket(ReceivePacketEvent event) {
        if (!this.isEnabled() || event.isCancelled()) {
            return;
        }
        if (event.getPacket() instanceof S02PacketChat) {
            S02PacketChat packet = (S02PacketChat) event.getPacket();
            String message = packet.getChatComponent().getFormattedText();
            if (message.contains("\u00a7e\u00a7lProtect your bed and destroy the enemy beds.")
                    || message.contains("\u00a7eYou will respawn in \u00a7r\u00a7c1 \u00a7r\u00a7esecond!")) {
                this.ignoreNextReset = true;
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.ignoreNextReset = false;
    }

    @Override
    public void onDisabled() {
        this.ignoreNextReset = false;
    }
}
