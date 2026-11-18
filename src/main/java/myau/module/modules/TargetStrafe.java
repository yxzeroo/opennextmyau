package myau.module.modules;

import java.awt.Color;
import java.util.ArrayList;
import myau.Myau;
import myau.data.Vec2d;
import myau.event.EventTarget;
import myau.event.events.Render3DEvent;
import myau.event.events.StrafeEvent;
import myau.event.events.UpdateEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.ColorUtil;
import myau.util.MoveUtil;
import myau.util.PlayerUtil;
import myau.util.RenderUtil;
import myau.util.RotationUtil;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;

public class TargetStrafe extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private EntityLivingBase target = null;
    private float targetYaw = Float.NaN;
    private int direction = 1;
    public final FloatProperty radius = new FloatProperty("radius", 1.0F, 0.0F, 6.0F);
    public final IntProperty points = new IntProperty("points", 6, 3, 24);
    public final BooleanProperty requirePress = new BooleanProperty("require-press", true);
    public final BooleanProperty speedOnly = new BooleanProperty("speed-only", true);
    public final ModeProperty showTarget = new ModeProperty("show-target", 1, new String[]{"NONE", "DEFAULT", "HUD"});

    public TargetStrafe() {
        super("TargetStrafe", false, 0);
    }

    private boolean canStrafe() {
        if (this.speedOnly.getValue()) {
            Speed speed = (Speed) Myau.moduleManager.modules.get(Speed.class);
            Fly fly = (Fly) Myau.moduleManager.modules.get(Fly.class);
            LongJump longJump = (LongJump) Myau.moduleManager.modules.get(LongJump.class);
            if (!speed.isEnabled() && !fly.isEnabled() && (!longJump.isEnabled() || !longJump.isJumping())) {
                return false;
            }
        }
        return !this.requirePress.getValue() || PlayerUtil.isJumping();
    }

    private EntityLivingBase findTarget() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killAura.isEnabled() && killAura.isAttackAllowed()) {
            EntityLivingBase auraTarget = killAura.getTarget();
            if (TeamUtil.isEntityLoaded(auraTarget)) {
                return auraTarget;
            }
        }
        return null;
    }

    private Color getTargetColor(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer) {
            if (TeamUtil.isFriend((EntityPlayer) entity)) {
                return Myau.friendManager.getColor();
            }
            if (TeamUtil.isEnemy((EntityPlayer) entity)) {
                return Myau.enemyManager.getColor();
            }
        }
        switch (this.showTarget.getValue()) {
            case 1:
                if (!(entity instanceof EntityPlayer)) {
                    return Color.WHITE;
                }
                return TeamUtil.getTeamColor((EntityPlayer) entity, 1.0F);
            case 2:
                int hudRgb = ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis()).getRGB();
                return new Color(hudRgb);
            default:
                return new Color(-1);
        }
    }

    private boolean isPointOverVoid(double x, double z) {
        return PlayerUtil.isOverVoid(
            new AxisAlignedBB(
                x - 0.015,
                mc.thePlayer.posY,
                z - 0.015,
                x + 0.015,
                mc.thePlayer.posY + (double) mc.thePlayer.height,
                z + 0.015
            )
        );
    }

    private int wrapIndex(int index, int size) {
        if (index < 0) {
            return size - 1;
        }
        return index >= size ? 0 : index;
    }

    public float getTargetYaw() {
        return this.targetYaw;
    }

    @EventTarget(priority = 100)
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        boolean movingLeft = PlayerUtil.isMovingLeft();
        boolean movingRight = PlayerUtil.isMovingRight();
        if (movingLeft != movingRight) {
            this.direction = movingLeft ? 1 : -1;
        }
        if (!this.canStrafe()) {
            this.target = null;
            this.targetYaw = Float.NaN;
            return;
        }
        this.target = this.findTarget();
        if (this.target == null) {
            this.targetYaw = Float.NaN;
            return;
        }
        ArrayList<Vec2d> circlePoints = new ArrayList<>();
        int pointCount = this.points.getValue();
        double circleRadius = this.radius.getValue();
        for (int i = 0; i < pointCount; ++i) {
            double angle = (double) i * ((Math.PI * 2.0) / (double) pointCount);
            circlePoints.add(new Vec2d(circleRadius * Math.cos(angle), circleRadius * Math.sin(angle)));
        }
        if (circlePoints.isEmpty()) {
            this.target = null;
            this.targetYaw = Float.NaN;
            return;
        }
        double closestDistance = 0.0;
        int closestIndex = -1;
        for (int i = 0; i < circlePoints.size(); ++i) {
            Vec2d point = circlePoints.get(i);
            double distance = mc.thePlayer.getDistance(this.target.posX + point.getX(), mc.thePlayer.posY, this.target.posZ + point.getY());
            if (closestIndex == -1 || distance < closestDistance) {
                closestDistance = distance;
                closestIndex = i;
            }
        }
        if (mc.thePlayer.isCollidedHorizontally) {
            this.direction *= -1;
        }
        int nextIndex = this.wrapIndex(closestIndex + this.direction, circlePoints.size());
        double nextX = this.target.posX + circlePoints.get(nextIndex).getX();
        double nextZ = this.target.posZ + circlePoints.get(nextIndex).getY();
        if (this.isPointOverVoid(nextX, nextZ)) {
            this.direction *= -1;
            nextIndex = this.wrapIndex(closestIndex + this.direction, circlePoints.size());
            nextX = this.target.posX + circlePoints.get(nextIndex).getX();
            nextZ = this.target.posZ + circlePoints.get(nextIndex).getY();
        }
        this.targetYaw = RotationUtil.getRotations(nextX - mc.thePlayer.posX, 0.0, nextZ - mc.thePlayer.posZ)[0];
        event.setMoveYaw(this.targetYaw, 10);
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (this.isEnabled() && !Float.isNaN(this.targetYaw) && MoveUtil.isForwardPressed()) {
            event.setStrafe(0.0F);
            event.setForward(1.0F);
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled() && TeamUtil.isEntityLoaded(this.target) && this.showTarget.getValue() != 0) {
            Color color = this.getTargetColor(this.target);
            RenderUtil.enableRenderState();
            RenderUtil.drawEntityCircle(
                this.target,
                (double) this.radius.getValue(),
                this.points.getValue(),
                3.0F,
                ColorUtil.darker(color, 0.2F).getRGB()
            );
            RenderUtil.drawEntityCircle(
                this.target,
                (double) this.radius.getValue(),
                this.points.getValue(),
                1.5F,
                color.getRGB()
            );
            RenderUtil.disableRenderState();
        }
    }

    @Override
    public void onDisabled() {
        this.target = null;
        this.targetYaw = Float.NaN;
    }
}
