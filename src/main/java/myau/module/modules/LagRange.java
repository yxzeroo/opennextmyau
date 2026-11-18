package myau.module.modules;

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;
import myau.Myau;
import myau.event.EventTarget;
import myau.event.events.PostTickEvent;
import myau.event.events.Render3DEvent;
import myau.event.events.SendPacketEvent;
import myau.event.events.TickEvent;
import myau.mixin.IAccessorPlayerControllerMP;
import myau.mixin.IAccessorRenderManager;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.ItemUtil;
import myau.util.MathUtil;
import myau.util.RenderUtil;
import myau.util.RotationUtil;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

public class LagRange extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int tickIndex = -1;
    private long delayAccumulator = 0L;
    private boolean hasTarget = false;
    private Vec3 prevLagPosition = null;
    private Vec3 currentLagPosition = null;

    public final IntProperty delay = new IntProperty("delay", 100, 0, 1000);
    public final FloatProperty range = new FloatProperty("range", 10.0f, 3.0f, 100.0f);
    public final BooleanProperty weaponsOnly = new BooleanProperty("weapons-only", true);
    public final BooleanProperty allowTools = new BooleanProperty("allow-tools", false, this.weaponsOnly::getValue);
    public final BooleanProperty botCheck = new BooleanProperty("bot-check", true);
    public final BooleanProperty teams = new BooleanProperty("teams", true);
    public final ModeProperty showPosition = new ModeProperty("show-position", 0, new String[]{"NONE", "DEFAULT", "HUD"});

    public LagRange() {
        super("LagRange", false, 0);
    }

    private boolean isValidTarget(EntityPlayer player) {
        if (player == mc.thePlayer || player == mc.thePlayer.ridingEntity) {
            return false;
        }
        if (player == mc.getRenderViewEntity() || player == mc.getRenderViewEntity().ridingEntity) {
            return false;
        }
        if (player.deathTime > 0) {
            return false;
        }
        if (TeamUtil.isFriend(player)) {
            return false;
        }
        if (TeamUtil.isEnemy(player)) {
            return true;
        }
        if (this.teams.getValue() && TeamUtil.isSameTeam(player)) {
            return false;
        }
        return !this.botCheck.getValue() || !TeamUtil.isBot(player);
    }

    private boolean shouldResetOnPacket(Packet<?> packet) {
        if (packet instanceof C02PacketUseEntity) {
            return true;
        }
        if (packet instanceof C07PacketPlayerDigging) {
            return ((C07PacketPlayerDigging) packet).getStatus() != C07PacketPlayerDigging.Action.RELEASE_USE_ITEM;
        }
        if (packet instanceof C08PacketPlayerBlockPlacement) {
            ItemStack stack = ((C08PacketPlayerBlockPlacement) packet).getStack();
            return stack == null || !(stack.getItem() instanceof ItemSword);
        }
        return false;
    }

    @EventTarget(priority = -50)
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        Myau.lagRangeManager.setDelay(0);
        this.hasTarget = false;

        BedNuker bedNuker = (BedNuker) Myau.moduleManager.modules.get(BedNuker.class);
        if ((bedNuker.isEnabled() && bedNuker.isBreaking())
                || ((IAccessorPlayerControllerMP) mc.playerController).getIsHittingBlock()
                || (mc.thePlayer.isUsingItem() && !mc.thePlayer.isBlocking())
                || (this.weaponsOnly.getValue() && !ItemUtil.isHoldingWeapon() && (!this.allowTools.getValue() || !ItemUtil.isHoldingTool()))) {
            this.tickIndex = -1;
            return;
        }

        List<EntityPlayer> targets = mc.theWorld.loadedEntityList.stream()
                .filter(entity -> entity instanceof EntityPlayer)
                .map(entity -> (EntityPlayer) entity)
                .filter(this::isValidTarget)
                .collect(Collectors.toList());
        if (targets.isEmpty()) {
            this.tickIndex = -1;
            return;
        }

        double eyeHeight = mc.thePlayer.getEyeHeight();
        Vec3 laggedEyePos = Myau.lagRangeManager.getLastPosition().addVector(0.0, eyeHeight, 0.0);
        Vec3 lastTickEyePos = new Vec3(mc.thePlayer.lastTickPosX, mc.thePlayer.lastTickPosY + eyeHeight, mc.thePlayer.lastTickPosZ);
        Vec3 currentEyePos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + eyeHeight, mc.thePlayer.posZ);

        for (EntityPlayer target : targets) {
            double currentDist = RotationUtil.distanceToEntity(target, currentEyePos);
            if (currentDist > (double) this.range.getValue()) {
                continue;
            }
            double lastTickDist = RotationUtil.distanceToEntity(target, lastTickEyePos);
            double laggedDist = RotationUtil.distanceToEntity(target, laggedEyePos);
            if (currentDist < lastTickDist || currentDist < laggedDist) {
                if (this.tickIndex < 0) {
                    this.tickIndex = 0;
                    this.delayAccumulator += (long) this.delay.getValue();
                    while (this.delayAccumulator > 0L) {
                        ++this.tickIndex;
                        this.delayAccumulator -= 50L;
                    }
                }
                Myau.lagRangeManager.setDelay(this.tickIndex);
                this.hasTarget = true;
                break;
            }
        }
    }

    @EventTarget(priority = -50)
    public void onPostTick(PostTickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        Vec3 lastPosition = Myau.lagRangeManager.getLastPosition();
        this.prevLagPosition = this.currentLagPosition == null ? lastPosition : this.currentLagPosition;
        this.currentLagPosition = lastPosition;
    }

    @EventTarget
    public void onSendPacket(SendPacketEvent event) {
        if (!this.isEnabled() || event.isCancelled()) {
            return;
        }
        if (this.shouldResetOnPacket(event.getPacket())) {
            Myau.lagRangeManager.setDelay(0);
            this.tickIndex = -1;
        }
    }

    @EventTarget(priority = 50)
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.showPosition.getValue() != 0
                && mc.gameSettings.thirdPersonView != 0
                && this.hasTarget
                && this.prevLagPosition != null
                && this.currentLagPosition != null) {
            Color renderColor = new Color(-1);
            switch (this.showPosition.getValue()) {
                case 1:
                    renderColor = TeamUtil.getTeamColor(mc.thePlayer, 1.0f);
                    break;
                case 2:
                    renderColor = ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis());
                    break;
            }
            double x = MathUtil.lerp(this.prevLagPosition.xCoord, this.currentLagPosition.xCoord, event.getPartialTicks());
            double y = MathUtil.lerp(this.prevLagPosition.yCoord, this.currentLagPosition.yCoord, event.getPartialTicks());
            double z = MathUtil.lerp(this.prevLagPosition.zCoord, this.currentLagPosition.zCoord, event.getPartialTicks());
            float borderSize = mc.thePlayer.getCollisionBorderSize();
            IAccessorRenderManager renderManager = (IAccessorRenderManager) mc.getRenderManager();
            AxisAlignedBB box = new AxisAlignedBB(
                    x - (double) mc.thePlayer.width / 2.0,
                    y,
                    z - (double) mc.thePlayer.width / 2.0,
                    x + (double) mc.thePlayer.width / 2.0,
                    y + (double) mc.thePlayer.height,
                    z + (double) mc.thePlayer.width / 2.0
            ).expand(borderSize, borderSize, borderSize).offset(
                    -renderManager.getRenderPosX(),
                    -renderManager.getRenderPosY(),
                    -renderManager.getRenderPosZ()
            );
            RenderUtil.enableRenderState();
            RenderUtil.drawFilledBox(box, renderColor.getRed(), renderColor.getGreen(), renderColor.getBlue(), 63);
            RenderUtil.disableRenderState();
        }
    }

    @Override
    public void onDisabled() {
        Myau.lagRangeManager.setDelay(0);
        this.tickIndex = -1;
        this.delayAccumulator = 0L;
        this.hasTarget = false;
        this.prevLagPosition = null;
        this.currentLagPosition = null;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{String.format("%dms", this.delay.getValue())};
    }
}
