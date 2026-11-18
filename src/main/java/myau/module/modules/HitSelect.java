package myau.module.modules;

import java.util.HashMap;
import java.util.Map;
import myau.event.EventTarget;
import myau.event.events.LeftClickMouseEvent;
import myau.event.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.util.PlayerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;

public class HitSelect extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final Map<EntityLivingBase, Integer> hurtTimeMap = new HashMap<>();
    private final Map<EntityLivingBase, Float> attackDamageMap = new HashMap<>();

    public final IntProperty hurtTime = new IntProperty("hurt-time", 4, 0, 10);
    public final BooleanProperty swing = new BooleanProperty("swing", true);

    public HitSelect() {
        super("HitSelect", false, 0);
    }

    public boolean shouldCancelHit(EntityLivingBase target) {
        float currentDamage = PlayerUtil.getAttackDamage(target);
        if (this.hurtTimeMap.getOrDefault(target, 0) <= 0) {
            this.hurtTimeMap.put(target, target.hurtTime);
            this.attackDamageMap.put(target, currentDamage);
            return false;
        }
        if (this.hurtTimeMap.getOrDefault(target, 0) <= this.hurtTime.getValue()) {
            return false;
        }
        if (this.attackDamageMap.getOrDefault(target, 0.0F) > currentDamage) {
            this.attackDamageMap.put(target, currentDamage);
            return false;
        }
        if (mc.thePlayer.hurtTime > 0 || !mc.thePlayer.isSprinting()) {
            return false;
        }
        if (this.swing.getValue()) {
            PlayerUtil.swingItem();
        }
        return true;
    }

    @EventTarget(priority = -100)
    public void onLeftClick(LeftClickMouseEvent event) {
        if (!this.isEnabled() || event.isCancelled()) {
            return;
        }
        if (mc.objectMouseOver == null
                || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY
                || !(mc.objectMouseOver.entityHit instanceof EntityLivingBase)) {
            return;
        }
        EntityLivingBase target = (EntityLivingBase) mc.objectMouseOver.entityHit;
        if (this.shouldCancelHit(target)) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        this.hurtTimeMap.replaceAll((entity, ticks) -> ticks > 0 ? ticks - 1 : ticks);
    }

    @Override
    public void onDisabled() {
        this.hurtTimeMap.clear();
        this.attackDamageMap.clear();
    }
}
