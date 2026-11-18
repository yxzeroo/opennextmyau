package myau.module.modules;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import myau.Myau;
import myau.event.EventTarget;
import myau.event.events.KeyEvent;
import myau.event.events.PostTickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.PercentProperty;
import myau.util.ItemUtil;
import myau.util.PlayerUtil;
import myau.util.RandomUtil;
import myau.util.RotationUtil;
import myau.util.TeamUtil;
import myau.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;

public class AimAssist extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final TimerUtil clickTimer = new TimerUtil();
    private final float[] quantizedRotations = new float[]{-180.0F, 0.0F};
    private final float[] rawRotations = new float[]{-180.0F, 0.0F};

    public final FloatProperty horizontalSpeed = new FloatProperty("horizontal-speed", 3.0F, 0.0F, 30.0F);
    public final FloatProperty verticalSpeed = new FloatProperty("vertical-speed", 0.0F, 0.0F, 30.0F);
    public final PercentProperty smoothing = new PercentProperty("smoothing", 50);
    public final FloatProperty range = new FloatProperty("range", 4.5F, 3.0F, 8.0F);
    public final IntProperty fov = new IntProperty("fov", 90, 30, 360);
    public final BooleanProperty requirePress = new BooleanProperty("require-press", true);
    public final BooleanProperty allowMining = new BooleanProperty("allow-mining", true);
    public final BooleanProperty weaponsOnly = new BooleanProperty("weapons-only", true);
    public final BooleanProperty allowTools = new BooleanProperty("allow-tools", false, this.weaponsOnly::getValue);
    public final BooleanProperty botCheck = new BooleanProperty("bot-check", true);
    public final BooleanProperty teams = new BooleanProperty("teams", true);

    public AimAssist() {
        super("AimAssist", false, 0);
    }

    private boolean isValidTarget(EntityLivingBase entity) {
        if (entity == mc.thePlayer || entity == mc.thePlayer.ridingEntity) {
            return false;
        }
        if (entity == mc.getRenderViewEntity() || entity == mc.getRenderViewEntity().ridingEntity) {
            return false;
        }
        if (entity.deathTime > 0) {
            return false;
        }
        if (RotationUtil.distanceToEntity(entity) > this.range.getValue().doubleValue()) {
            return false;
        }
        if (RotationUtil.angleToEntity(entity) > this.fov.getValue().floatValue()) {
            return false;
        }
        if (RotationUtil.rayTraceEntityBlocks(entity) != null) {
            return false;
        }
        if (!(entity instanceof EntityPlayer)) {
            return true;
        }
        EntityPlayer player = (EntityPlayer) entity;
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

    private boolean isInReach(EntityPlayer player) {
        Reach reach = (Reach) Myau.moduleManager.modules.get(Reach.class);
        double maxReach = reach.isEnabled() ? reach.range.getValue().doubleValue() : 3.0;
        return RotationUtil.distanceToEntity(player) <= maxReach;
    }

    private EntityLivingBase findTarget() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killAura.isEnabled()) {
            EntityLivingBase target = killAura.getTarget();
            if (target != null && this.isValidTarget(target)) {
                return target;
            }
        }
        List<EntityPlayer> players = mc.theWorld.loadedEntityList.stream()
                .filter(entity -> entity instanceof EntityPlayer)
                .map(entity -> (EntityPlayer) entity)
                .filter(this::isValidTarget)
                .sorted(Comparator.comparingDouble(RotationUtil::distanceToEntity))
                .collect(Collectors.toList());
        if (players.isEmpty()) {
            return null;
        }
        if (players.stream().anyMatch(this::isInReach)) {
            players.removeIf(player -> !this.isInReach(player));
        }
        return players.get(0);
    }

    private boolean isLookingAtBlock() {
        return mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
    }

    @EventTarget
    public void onPostTick(PostTickEvent event) {
        if (!this.isEnabled() || mc.currentScreen != null) {
            return;
        }
        if (this.weaponsOnly.getValue()
                && !ItemUtil.isHoldingWeapon()
                && (!this.allowTools.getValue() || !ItemUtil.isHoldingTool())) {
            return;
        }
        boolean attacking = PlayerUtil.isAttacking();
        if (this.allowMining.getValue() && attacking && this.isLookingAtBlock()) {
            return;
        }
        if (this.requirePress.getValue() && !attacking && this.clickTimer.hasTimeElapsed(350L)) {
            return;
        }
        EntityLivingBase target = this.findTarget();
        if (target == null || RotationUtil.distanceToEntity(target) <= 0.0) {
            return;
        }
        if (this.quantizedRotations[0] != mc.thePlayer.rotationYaw) {
            this.quantizedRotations[0] = mc.thePlayer.rotationYaw;
            this.rawRotations[0] = mc.thePlayer.rotationYaw;
        }
        if (this.quantizedRotations[1] != mc.thePlayer.rotationPitch) {
            this.quantizedRotations[1] = mc.thePlayer.rotationPitch;
            this.rawRotations[1] = mc.thePlayer.rotationPitch;
        }
        AxisAlignedBB boundingBox = target.getEntityBoundingBox();
        double borderSize = target.getCollisionBorderSize();
        float[] targetRotations = RotationUtil.getRotationsToBox(boundingBox.expand(borderSize, borderSize, borderSize));
        float[] angleDelta = new float[]{
                MathHelper.wrapAngleTo180_float(targetRotations[0] - this.rawRotations[0]),
                targetRotations[1] - this.rawRotations[1]
        };
        if (Math.abs(angleDelta[0]) < 0.5F) {
            angleDelta[0] = 0.0F;
        }
        if (Math.abs(angleDelta[1]) < 0.5F) {
            angleDelta[1] = 0.0F;
        }
        angleDelta[0] = RotationUtil.clampAngle(angleDelta[0], this.horizontalSpeed.getValue() * 6.0F * RandomUtil.nextFloat(0.75F, 1.0F));
        angleDelta[1] = RotationUtil.clampAngle(angleDelta[1], this.verticalSpeed.getValue() * 6.0F * RandomUtil.nextFloat(0.75F, 1.0F) * 0.5F);
        angleDelta[0] = RotationUtil.smoothAngle(angleDelta[0], this.smoothing.getValue().floatValue() / 100.0F + RandomUtil.nextFloat(-0.05F, 0.05F));
        angleDelta[1] = RotationUtil.smoothAngle(angleDelta[1], this.smoothing.getValue().floatValue() / 100.0F + RandomUtil.nextFloat(-0.05F, 0.05F));
        if (angleDelta[0] == 0.0F && angleDelta[1] != 0.0F) {
            angleDelta[0] += RandomUtil.nextFloat(0.1F, 0.2F) * (RandomUtil.nextBoolean() ? 1.0F : -1.0F);
        }
        if (angleDelta[0] != 0.0F && angleDelta[1] == 0.0F) {
            angleDelta[1] += RandomUtil.nextFloat(0.1F, 0.2F) * (RandomUtil.nextBoolean() ? 1.0F : -1.0F);
        }
        this.rawRotations[0] += angleDelta[0];
        this.rawRotations[1] += angleDelta[1];
        this.rawRotations[1] = MathHelper.clamp_float(this.rawRotations[1], -90.0F, 90.0F);
        this.quantizedRotations[0] = RotationUtil.quantizeAngle(this.rawRotations[0], this.quantizedRotations[0]);
        this.quantizedRotations[1] = RotationUtil.quantizeAngle(this.rawRotations[1], this.quantizedRotations[1]);
        this.quantizedRotations[1] = MathHelper.clamp_float(this.quantizedRotations[1], -90.0F, 90.0F);
        Myau.rotationManager.setRotation(this.quantizedRotations[0], this.quantizedRotations[1], 0, false);
    }

    @EventTarget
    public void onKey(KeyEvent event) {
        if (event.getKey() == mc.gameSettings.keyBindAttack.getKeyCode()) {
            AutoClicker autoClicker = (AutoClicker) Myau.moduleManager.modules.get(AutoClicker.class);
            if (!autoClicker.isEnabled()) {
                this.clickTimer.reset();
            }
        }
    }
}
