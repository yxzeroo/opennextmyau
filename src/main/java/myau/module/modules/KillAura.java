package myau.module.modules;

import com.google.common.base.CaseFormat;
import java.awt.Color;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import myau.Myau;
import myau.data.EntityBox;
import myau.enums.BlinkModules;
import myau.event.EventTarget;
import myau.event.events.CancelUseEvent;
import myau.event.events.HitBlockEvent;
import myau.event.events.LeftClickMouseEvent;
import myau.event.events.PostTickEvent;
import myau.event.events.PostUpdateEvent;
import myau.event.events.ReceivePacketEvent;
import myau.event.events.Render3DEvent;
import myau.event.events.RightClickDelayEvent;
import myau.event.events.RightClickMouseEvent;
import myau.event.events.SendPacketEvent;
import myau.event.events.TickEvent;
import myau.event.events.UpdateEvent;
import myau.management.RotationState;
import myau.mixin.IAccessorPlayerControllerMP;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.util.ChatUtil;
import myau.util.ItemUtil;
import myau.util.MoveUtil;
import myau.util.PacketUtil;
import myau.util.PlayerUtil;
import myau.util.RandomUtil;
import myau.util.RenderUtil;
import myau.util.RotationUtil;
import myau.util.TeamUtil;
import myau.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.DataWatcher.WatchableObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldSettings;

public class KillAura extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat HEALTH_FORMAT = new DecimalFormat("+0.0;-0.0", new DecimalFormatSymbols(Locale.US));

    private final TimerUtil switchTimer = new TimerUtil();
    private EntityBox target = null;
    private boolean targetInBlockRange = false;
    private int targetIndex = 0;
    private boolean attacked = false;
    private boolean blockingSent = false;
    private boolean blocking = false;
    private boolean fakeBlocking = false;
    private boolean blinkResetPending = false;
    private long attackDelay = 0L;
    private long blockHoldRemaining = 0L;
    private long blockCooldownRemaining = 0L;
    private int blockStage = 0;
    private int lastHealthLogTick = 0;

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"SINGLE", "SWITCH"});
    public final ModeProperty sort = new ModeProperty("sort", 0, new String[]{"DISTANCE", "HEALTH", "HURT_TIME", "FOV"});
    public final ModeProperty autoBlock = new ModeProperty(
            "auto-block",
            7,
            new String[]{"NONE", "VANILLA", "HYPIXEL", "BLINK", "INTERACT", "SPOOF", "SWAP", "LEGIT", "FAKE"}
    );
    public final BooleanProperty autoBlockRequirePress = new BooleanProperty(
            "auto-block-require-press",
            true,
            () -> this.autoBlock.getValue() != 0 && this.autoBlock.getValue() != 8
    );
    public final BooleanProperty autoBlockNoSlow = new BooleanProperty(
            "auto-block-no-slow",
            false,
            () -> this.autoBlock.getValue() != 0 && this.autoBlock.getValue() != 8
    );
    public final FloatProperty autoBlockHold = new FloatProperty(
            "auto-block-hold",
            1.5F,
            1.0F,
            20.0F,
            () -> this.autoBlock.getValue() != 0 && this.autoBlock.getValue() != 1 && this.autoBlock.getValue() != 8
    );
    public final FloatProperty autoBlockDelay = new FloatProperty(
            "auto-block-delay",
            0.0F,
            0.0F,
            20.0F,
            () -> this.autoBlock.getValue() != 0
                    && this.autoBlock.getValue() != 1
                    && this.autoBlock.getValue() != 5
                    && this.autoBlock.getValue() != 8
    );
    public final IntProperty autoBlockHurtTime = new IntProperty(
            "auto-block-hurt-time",
            6,
            0,
            10,
            () -> this.autoBlock.getValue() != 0 && this.autoBlock.getValue() != 8
    );
    public final FloatProperty autoBlockRange = new FloatProperty(
            "auto-block-range",
            4.0F,
            3.0F,
            8.0F,
            () -> this.autoBlock.getValue() != 0
    );
    public final FloatProperty swingRange = new FloatProperty("swing-range", 4.0F, 3.0F, 6.0F);
    public final FloatProperty attackRange = new FloatProperty("attack-range", 3.0F, 3.0F, 6.0F);
    public final IntProperty fov = new IntProperty("fov", 360, 30, 360);
    public final IntProperty minAps = new IntProperty("min-aps", 14, 1, 20);
    public final IntProperty maxAps = new IntProperty("max-aps", 14, 1, 20);
    public final IntProperty switchDelay = new IntProperty("switch-delay", 150, 0, 1000);
    public final ModeProperty rotations = new ModeProperty("rotations", 2, new String[]{"NONE", "LEGIT", "SILENT", "LOCK_VIEW"});
    public final ModeProperty moveFix = new ModeProperty("move-fix", 1, new String[]{"NONE", "SILENT", "STRICT"});
    public final PercentProperty smoothing = new PercentProperty("smoothing", 0);
    public final IntProperty angleStep = new IntProperty("angle-step", 90, 30, 180);
    public final BooleanProperty throughWalls = new BooleanProperty("through-walls", true);
    public final BooleanProperty requirePress = new BooleanProperty("require-press", false);
    public final BooleanProperty allowMining = new BooleanProperty("allow-mining", true);
    public final BooleanProperty weaponsOnly = new BooleanProperty("weapons-only", true);
    public final BooleanProperty allowTools = new BooleanProperty("allow-tools", false, this.weaponsOnly::getValue);
    public final BooleanProperty inventoryCheck = new BooleanProperty("inventory-check", true);
    public final BooleanProperty botCheck = new BooleanProperty("bot-check", true);
    public final BooleanProperty players = new BooleanProperty("players", true);
    public final BooleanProperty bosses = new BooleanProperty("bosses", false);
    public final BooleanProperty mobs = new BooleanProperty("mobs", false);
    public final BooleanProperty animals = new BooleanProperty("animals", false);
    public final BooleanProperty golems = new BooleanProperty("golems", false);
    public final BooleanProperty silverfish = new BooleanProperty("silverfish", false);
    public final BooleanProperty teams = new BooleanProperty("teams", true);
    public final ModeProperty showTarget = new ModeProperty("show-target", 0, new String[]{"NONE", "DEFAULT", "HUD"});
    public final ModeProperty debugLog = new ModeProperty("debug-log", 0, new String[]{"NONE", "HEALTH"});

    public KillAura() {
        super("KillAura", false, 0);
    }

    private long getAttackDelay() {
        return 1000L / RandomUtil.nextLong(this.minAps.getValue(), this.maxAps.getValue());
    }

    private long getBlockHoldDuration() {
        return (long) (this.autoBlockHold.getValue() * 50.0F);
    }

    private long getBlockDelayDuration() {
        return (long) (this.autoBlockDelay.getValue() * 50.0F);
    }

    private boolean performAttack(float yaw, float pitch) {
        if (Myau.packetStateTracker.digging || Myau.packetStateTracker.placing) {
            return false;
        }
        if (this.isUsingSword() && this.autoBlock.getValue() != 1) {
            return false;
        }
        if (this.attackDelay > 0L) {
            return false;
        }
        HitSelect hitSelect = (HitSelect) Myau.moduleManager.modules.get(HitSelect.class);
        if (hitSelect.isEnabled() && hitSelect.shouldCancelHit(this.target.entity)) {
            return false;
        }
        this.attackDelay += this.getAttackDelay();
        mc.thePlayer.swingItem();
        boolean canHit;
        if (this.rotations.getValue() == 0) {
            canHit = this.isInAttackRange(this.target.distance);
        } else {
            Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0F);
            double range = this.attackRange.getValue().doubleValue();
            canHit = this.throughWalls.getValue()
                    ? RotationUtil.interceptBox(this.target.boundingBox, eyePos, yaw, pitch, range) != null
                    : !RotationUtil.isRayBlocked(this.target.boundingBox, eyePos, yaw, pitch, range);
        }
        if (!canHit) {
            return false;
        }
        ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
        PacketUtil.sendPacket(new C02PacketUseEntity(this.target.entity, C02PacketUseEntity.Action.ATTACK));
        if (mc.playerController.getCurrentGameType() != WorldSettings.GameType.SPECTATOR) {
            PlayerUtil.attackEntity(this.target.entity);
        }
        this.attacked = true;
        return true;
    }

    private void startBlock() {
        ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
        this.sendBlockUse(mc.thePlayer.getHeldItem());
    }

    private void sendBlockUse(ItemStack stack) {
        PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(stack));
        mc.thePlayer.setItemInUse(stack, stack.getMaxItemUseDuration());
        this.blockingSent = true;
    }

    private void releaseBlock() {
        PacketUtil.sendPacket(new C07PacketPlayerDigging(Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        mc.thePlayer.stopUsingItem();
        this.blockingSent = false;
    }

    private void interactAndBlock(float yaw, float pitch) {
        if (this.target == null) {
            return;
        }
        MovingObjectPosition hit = RotationUtil.interceptBox(this.target.boundingBox, yaw, pitch, 8.0);
        if (hit == null) {
            return;
        }
        ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
        PacketUtil.sendPacket(new C02PacketUseEntity(
                this.target.entity,
                new Vec3(hit.hitVec.xCoord - this.target.x, hit.hitVec.yCoord - this.target.y, hit.hitVec.zCoord - this.target.z)
        ));
        PacketUtil.sendPacket(new C02PacketUseEntity(this.target.entity, C02PacketUseEntity.Action.INTERACT));
        PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
        mc.thePlayer.setItemInUse(mc.thePlayer.getHeldItem(), mc.thePlayer.getHeldItem().getMaxItemUseDuration());
        this.blockingSent = true;
    }

    private boolean canEngageTarget() {
        if (this.target == null) {
            return false;
        }
        if (this.inventoryCheck.getValue() && mc.currentScreen instanceof GuiContainer) {
            return false;
        }
        if (this.weaponsOnly.getValue()
                && !ItemUtil.isHoldingWeapon()
                && (!this.allowTools.getValue() || !ItemUtil.isHoldingTool())) {
            return false;
        }
        if ((ItemUtil.isEatingOrDrinking() || ItemUtil.isHoldingBow()) && PlayerUtil.isUsingItem()) {
            return false;
        }
        AutoHeal autoHeal = (AutoHeal) Myau.moduleManager.modules.get(AutoHeal.class);
        if (autoHeal.isEnabled() && autoHeal.isSwitching()) {
            return false;
        }
        BedNuker bedNuker = (BedNuker) Myau.moduleManager.modules.get(BedNuker.class);
        if (bedNuker.isEnabled() && bedNuker.isBreaking()) {
            return false;
        }
        Scaffold scaffold = (Scaffold) Myau.moduleManager.modules.get(Scaffold.class);
        if (scaffold.isEnabled()) {
            return false;
        }
        boolean attacking = PlayerUtil.isAttacking();
        if (this.allowMining.getValue() && attacking && mc.objectMouseOver.typeOfHit.equals(MovingObjectType.BLOCK)) {
            return false;
        }
        return !this.requirePress.getValue() || attacking;
    }

    private boolean canAutoBlock() {
        if (!ItemUtil.isHoldingSword()) {
            return false;
        }
        if (this.autoBlock.getValue() == 0) {
            return true;
        }
        if (this.autoBlock.getValue() == 8) {
            return this.targetInBlockRange;
        }
        return !this.autoBlockRequirePress.getValue() && this.targetInBlockRange || PlayerUtil.isUsingItem();
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
        if (entity instanceof EntityOtherPlayerMP) {
            EntityPlayer player = (EntityPlayer) entity;
            if (!this.players.getValue()) {
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
        if (entity instanceof EntityDragon || entity instanceof EntityWither) {
            return this.bosses.getValue();
        }
        if (entity instanceof EntityMob || entity instanceof EntitySlime) {
            if (entity instanceof EntitySilverfish) {
                if (!this.silverfish.getValue()) {
                    return false;
                }
                return !this.teams.getValue() || !TeamUtil.hasTeamArmorStand(entity);
            }
            return this.mobs.getValue();
        }
        if (entity instanceof EntityAnimal
                || entity instanceof EntityBat
                || entity instanceof EntitySquid
                || entity instanceof EntityVillager) {
            return this.animals.getValue();
        }
        if (entity instanceof EntityIronGolem) {
            if (!this.golems.getValue()) {
                return false;
            }
            return !this.teams.getValue() || !TeamUtil.hasTeamArmorStand(entity);
        }
        return false;
    }

    private boolean isInAnyRange(double distance) {
        return this.isInAutoBlockRange(distance) || this.isInSwingRange(distance) || this.isInAttackRange(distance);
    }

    private boolean isInAutoBlockRange(double distance) {
        if (this.autoBlock.getValue() == 0) {
            return false;
        }
        return distance <= this.autoBlockRange.getValue().doubleValue();
    }

    private boolean isInSwingRange(double distance) {
        return distance <= this.swingRange.getValue().doubleValue();
    }

    private boolean isInAttackRange(double distance) {
        return distance <= this.attackRange.getValue().doubleValue();
    }

    private boolean isEnemyPlayer(EntityLivingBase entity) {
        return entity instanceof EntityPlayer && TeamUtil.isEnemy((EntityPlayer) entity);
    }

    private int findSpoofSlot(int currentSlot) {
        for (int slot = 0; slot < 9; ++slot) {
            if (slot != currentSlot && mc.thePlayer.inventory.getStackInSlot(slot) == null) {
                return slot;
            }
        }
        for (int slot = 0; slot < 9; ++slot) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            if (slot != currentSlot && stack != null && !stack.hasDisplayName()) {
                return slot;
            }
        }
        return Math.floorMod(currentSlot - 1, 9);
    }

    private int findSecondarySwordSlot(int currentSlot) {
        for (int slot = 0; slot < 9; ++slot) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            if (slot != currentSlot && stack != null && stack.getItem() instanceof ItemSword) {
                return slot;
            }
        }
        return -1;
    }

    private int compareTargets(EntityBox first, EntityBox second) {
        int comparison = 0;
        switch (this.sort.getValue()) {
            case 1:
                comparison = Float.compare(TeamUtil.getEffectiveHealth(first.entity), TeamUtil.getEffectiveHealth(second.entity));
                break;
            case 2:
                comparison = Integer.compare(first.entity.hurtResistantTime, second.entity.hurtResistantTime);
                break;
            case 3:
                comparison = Float.compare(first.fov, second.fov);
                break;
        }
        return comparison != 0 ? comparison : Double.compare(first.distance, second.distance);
    }

    private void logHealthDelta(float healthDelta) {
        if (healthDelta != 0.0F && this.lastHealthLogTick != mc.thePlayer.ticksExisted) {
            this.lastHealthLogTick = mc.thePlayer.ticksExisted;
            String colorPrefix = healthDelta > 0.0F ? "&a" : "&c";
            ChatUtil.sendFormatted(String.format(
                    "%sHealth: %s&l%s&r (&otick: %d&r)&r",
                    Myau.clientName,
                    colorPrefix,
                    HEALTH_FORMAT.format((double) healthDelta),
                    mc.thePlayer.ticksExisted
            ));
        }
    }

    public EntityLivingBase getTarget() {
        return this.target != null ? this.target.entity : null;
    }

    public boolean isAttackAllowed() {
        if (this.inventoryCheck.getValue() && mc.currentScreen instanceof GuiContainer) {
            return false;
        }
        if (((IAccessorPlayerControllerMP) mc.playerController).getIsHittingBlock()) {
            return false;
        }
        Scaffold scaffold = (Scaffold) Myau.moduleManager.modules.get(Scaffold.class);
        if (scaffold.isEnabled()) {
            return false;
        }
        if (this.weaponsOnly.getValue()
                && !ItemUtil.isHoldingWeapon()
                && (!this.allowTools.getValue() || !ItemUtil.isHoldingTool())) {
            return false;
        }
        return !this.requirePress.getValue() || PlayerUtil.isAttacking();
    }

    public boolean shouldFakeBlock() {
        return this.fakeBlocking && ItemUtil.isHoldingSword();
    }

    public boolean isUsingSword() {
        return (mc.thePlayer.isUsingItem() || this.blockingSent) && ItemUtil.isHoldingSword();
    }

    public boolean isBlocking() {
        return this.blocking;
    }

    @EventTarget(priority = -50)
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        int syncedSlot = ((IAccessorPlayerControllerMP) mc.playerController).getCurrentPlayerItem();
        boolean canEngage = this.canEngageTarget();
        boolean canAttackThisTick = canEngage;
        boolean shouldBlock = canEngage && this.canAutoBlock();
        boolean hurtPauseBlock = this.autoBlock.getValue() != 0
                && this.autoBlock.getValue() != 8
                && shouldBlock
                && (Myau.hurtTimeTracker.getHurtTicks() > this.autoBlockHurtTime.getValue() || this.blockCooldownRemaining > 0L);

        if (this.attackDelay > 0L) {
            this.attackDelay -= 50L;
        }
        if (this.blockHoldRemaining > 0L) {
            this.blockHoldRemaining -= 50L;
        }
        if (this.blockCooldownRemaining > 0L) {
            this.blockCooldownRemaining -= 50L;
        }

        if (!shouldBlock || hurtPauseBlock) {
            if (this.blocking && this.isUsingSword() && !Myau.packetStateTracker.digging && !Myau.packetStateTracker.placing) {
                switch (this.autoBlock.getValue()) {
                    case 4:
                    case 5: {
                        int spoofSlot = this.findSpoofSlot(syncedSlot);
                        PacketUtil.sendPacket(new C09PacketHeldItemChange(spoofSlot));
                        ((IAccessorPlayerControllerMP) mc.playerController).setCurrentPlayerItem(spoofSlot);
                        canAttackThisTick = false;
                        break;
                    }
                    default: {
                        this.releaseBlock();
                        canAttackThisTick = false;
                        break;
                    }
                }
            }
            Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
            this.blocking = false;
            this.fakeBlocking = false;
            if (this.blockHoldRemaining > 0L) {
                this.blockHoldRemaining = 0L;
            }
            this.blockStage = 0;
        }

        if (!canEngage) {
            return;
        }

        if (hurtPauseBlock) {
            shouldBlock = false;
            this.blocking = true;
            this.fakeBlocking = true;
        }

        boolean shouldStartBlock = false;
        if (shouldBlock) {
            switch (this.autoBlock.getValue()) {
                case 0: {
                    Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                    this.blocking = false;
                    this.fakeBlocking = false;
                    if (PlayerUtil.isUsingItem()
                            && !this.isUsingSword()
                            && !Myau.packetStateTracker.digging
                            && !Myau.packetStateTracker.placing) {
                        shouldStartBlock = true;
                    }
                    break;
                }
                case 1: {
                    Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                    this.blocking = true;
                    this.fakeBlocking = false;
                    if (!this.isUsingSword() && !Myau.packetStateTracker.digging && !Myau.packetStateTracker.placing) {
                        shouldStartBlock = true;
                    }
                    break;
                }
                case 2: {
                    this.blocking = true;
                    this.fakeBlocking = true;
                    if (!Myau.packetStateTracker.digging && !Myau.packetStateTracker.placing) {
                        switch (this.blockStage) {
                            case 0:
                                if (!this.isUsingSword()) {
                                    shouldStartBlock = true;
                                    this.blockHoldRemaining += this.getBlockHoldDuration();
                                }
                                this.blinkResetPending = true;
                                this.blockStage = 1;
                                break;
                            case 1:
                                if (this.blockHoldRemaining <= 0L) {
                                    if (this.isUsingSword()) {
                                        this.releaseBlock();
                                        canAttackThisTick = false;
                                    }
                                    this.blockCooldownRemaining += this.getBlockDelayDuration();
                                    this.blockStage = 0;
                                } else {
                                    this.blinkResetPending = true;
                                }
                                break;
                            default:
                                this.blockStage = 0;
                                break;
                        }
                    }
                    break;
                }
                case 3: {
                    this.blocking = true;
                    this.fakeBlocking = true;
                    if (!Myau.packetStateTracker.digging && !Myau.packetStateTracker.placing) {
                        switch (this.blockStage) {
                            case 0:
                                if (!this.isUsingSword()) {
                                    shouldStartBlock = true;
                                    this.blockHoldRemaining += this.getBlockHoldDuration();
                                }
                                this.blinkResetPending = true;
                                this.blockStage = 1;
                                break;
                            case 1:
                                if (this.isUsingSword()) {
                                    this.releaseBlock();
                                    canAttackThisTick = false;
                                }
                                if (this.blockHoldRemaining <= 0L) {
                                    this.blockCooldownRemaining += this.getBlockDelayDuration();
                                    this.blockStage = 0;
                                }
                                break;
                            default:
                                this.blockStage = 0;
                                break;
                        }
                    }
                    break;
                }
                case 4: {
                    this.blocking = true;
                    this.fakeBlocking = true;
                    if (mc.thePlayer.inventory.currentItem == syncedSlot
                            && !Myau.packetStateTracker.digging
                            && !Myau.packetStateTracker.placing) {
                        switch (this.blockStage) {
                            case 0:
                                if (!this.isUsingSword()) {
                                    shouldStartBlock = true;
                                    this.blockHoldRemaining += this.getBlockHoldDuration();
                                }
                                this.blinkResetPending = true;
                                this.blockStage = 1;
                                break;
                            case 1:
                                if (this.isUsingSword()) {
                                    int spoofSlot = this.findSpoofSlot(syncedSlot);
                                    PacketUtil.sendPacket(new C09PacketHeldItemChange(spoofSlot));
                                    ((IAccessorPlayerControllerMP) mc.playerController).setCurrentPlayerItem(spoofSlot);
                                    canAttackThisTick = false;
                                }
                                if (this.blockHoldRemaining <= 0L) {
                                    this.blockCooldownRemaining += this.getBlockDelayDuration();
                                    this.blockStage = 0;
                                }
                                break;
                            default:
                                this.blockStage = 0;
                                break;
                        }
                    }
                    break;
                }
                case 5: {
                    Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                    this.blocking = true;
                    this.fakeBlocking = false;
                    if (!Myau.packetStateTracker.digging
                            && !Myau.packetStateTracker.placing
                            && mc.thePlayer.inventory.currentItem == syncedSlot
                            && (!this.isUsingSword() || this.blockHoldRemaining <= 0L)) {
                        int spoofSlot = this.findSpoofSlot(syncedSlot);
                        PacketUtil.sendPacket(new C09PacketHeldItemChange(spoofSlot));
                        PacketUtil.sendPacket(new C09PacketHeldItemChange(syncedSlot));
                        shouldStartBlock = true;
                        this.blockHoldRemaining += this.getBlockHoldDuration();
                    }
                    break;
                }
                case 6: {
                    Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                    this.blocking = true;
                    this.fakeBlocking = true;
                    if (mc.thePlayer.inventory.currentItem == syncedSlot
                            && !Myau.packetStateTracker.digging
                            && !Myau.packetStateTracker.placing) {
                        switch (this.blockStage) {
                            case 0:
                                if (!this.isUsingSword()) {
                                    shouldStartBlock = true;
                                    this.blockHoldRemaining += this.getBlockHoldDuration();
                                }
                                this.blockStage = 1;
                                break;
                            case 1:
                                if (this.blockHoldRemaining <= 0L) {
                                    if (this.isUsingSword()) {
                                        int swordSlot = this.findSecondarySwordSlot(syncedSlot);
                                        if (swordSlot != -1) {
                                            PacketUtil.sendPacket(new C09PacketHeldItemChange(swordSlot));
                                            ((IAccessorPlayerControllerMP) mc.playerController).setCurrentPlayerItem(swordSlot);
                                            this.sendBlockUse(mc.thePlayer.inventory.getStackInSlot(swordSlot));
                                        } else {
                                            this.releaseBlock();
                                        }
                                        canAttackThisTick = false;
                                    }
                                    this.blockCooldownRemaining += this.getBlockDelayDuration();
                                    this.blockStage = 0;
                                }
                                break;
                            default:
                                this.blockStage = 0;
                                break;
                        }
                    }
                    break;
                }
                case 7: {
                    Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                    this.blocking = true;
                    this.fakeBlocking = true;
                    if (!Myau.packetStateTracker.digging && !Myau.packetStateTracker.placing) {
                        switch (this.blockStage) {
                            case 0:
                                if (!this.isUsingSword()) {
                                    shouldStartBlock = true;
                                    this.blockHoldRemaining += this.getBlockHoldDuration();
                                }
                                this.blockStage = 1;
                                break;
                            case 1:
                                if (this.blockHoldRemaining <= 0L) {
                                    if (this.isUsingSword()) {
                                        this.releaseBlock();
                                        canAttackThisTick = false;
                                    }
                                    this.blockCooldownRemaining += this.getBlockDelayDuration();
                                    this.blockStage = 0;
                                }
                                break;
                            default:
                                this.blockStage = 0;
                                break;
                        }
                    }
                    break;
                }
                case 8: {
                    Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                    this.blocking = false;
                    this.fakeBlocking = true;
                    if (PlayerUtil.isUsingItem()
                            && !this.isUsingSword()
                            && !Myau.packetStateTracker.digging
                            && !Myau.packetStateTracker.placing) {
                        shouldStartBlock = true;
                    }
                    break;
                }
            }
        }

        boolean didAttack = false;
        if (this.target.visible && this.isInSwingRange(this.target.distance)) {
            if (this.rotations.getValue() == 2 || this.rotations.getValue() == 3) {
                float lastYaw = event.getLastYaw();
                float lastPitch = event.getLastPitch();
                float[] angleDelta = new float[]{
                        MathHelper.wrapAngleTo180_float(this.target.yaw - lastYaw),
                        this.target.pitch - lastPitch
                };
                if (Math.abs(angleDelta[0]) < 0.5F) {
                    angleDelta[0] = 0.0F;
                }
                if (Math.abs(angleDelta[1]) < 0.5F) {
                    angleDelta[1] = 0.0F;
                }
                angleDelta[0] = RotationUtil.clampAngle(angleDelta[0], this.angleStep.getValue().floatValue() * RandomUtil.nextFloat(0.75F, 1.0F));
                angleDelta[1] = RotationUtil.clampAngle(angleDelta[1], this.angleStep.getValue().floatValue() * RandomUtil.nextFloat(0.75F, 1.0F) * 0.5F);
                angleDelta[0] = RotationUtil.smoothAngle(angleDelta[0], this.smoothing.getValue().floatValue() / 100.0F + RandomUtil.nextFloat(-0.05F, 0.05F));
                angleDelta[1] = RotationUtil.smoothAngle(angleDelta[1], this.smoothing.getValue().floatValue() / 100.0F + RandomUtil.nextFloat(-0.05F, 0.05F));
                float sensitivityFactor = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
                float gcdScale = sensitivityFactor * sensitivityFactor * sensitivityFactor * 8.0F;
                if (angleDelta[0] == 0.0F && angleDelta[1] != 0.0F) {
                    angleDelta[0] += gcdScale * 0.15F * (RandomUtil.nextBoolean() ? 1.0F : -1.0F);
                }
                if (angleDelta[0] != 0.0F && angleDelta[1] == 0.0F) {
                    angleDelta[1] += gcdScale * 0.15F * (RandomUtil.nextBoolean() ? 1.0F : -1.0F);
                }
                float targetYaw = RotationUtil.quantizeAngle(lastYaw + angleDelta[0], lastYaw);
                float targetPitch = RotationUtil.quantizeAngle(lastPitch + angleDelta[1], lastPitch);
                targetPitch = MathHelper.clamp_float(targetPitch, -90.0F, 90.0F);
                event.setRotation(targetYaw, targetPitch, 1);
                if (this.rotations.getValue() == 3) {
                    Myau.rotationManager.setRotation(targetYaw, targetPitch, 1, true);
                }
                if (this.moveFix.getValue() != 0 || this.rotations.getValue() == 3) {
                    event.setMoveYaw(targetYaw, 1);
                }
            }
            if (canAttackThisTick) {
                didAttack = this.performAttack(event.getYaw(), event.getPitch());
            }
        }

        if (shouldStartBlock) {
            if (didAttack) {
                this.interactAndBlock(event.getYaw(), event.getPitch());
            } else {
                this.startBlock();
            }
        }
    }

    @EventTarget(priority = -50)
    public void onPostUpdate(PostUpdateEvent event) {
        if (this.blinkResetPending) {
            this.blinkResetPending = false;
            Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
            Myau.blinkManager.setBlinkState(true, BlinkModules.AUTO_BLOCK);
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0F);
        ArrayList<EntityBox> candidates = new ArrayList<>();
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityLivingBase) {
                EntityLivingBase living = (EntityLivingBase) entity;
                if (this.isValidTarget(living)) {
                    double borderSize = living.getCollisionBorderSize();
                    AxisAlignedBB boundingBox = living.getEntityBoundingBox().expand(borderSize, borderSize, borderSize);
                    double distance = RotationUtil.distanceToBox(boundingBox, eyePos);
                    if (this.isInAnyRange(distance)) {
                        float angle = RotationUtil.angleToBox(boundingBox, eyePos);
                        if (!(angle > this.fov.getValue().floatValue())) {
                            float[] targetRotations = RotationUtil.getRotationsToBox(boundingBox);
                            float yaw = targetRotations[0];
                            float pitch = targetRotations[1];
                            boolean visible = true;
                            if (!this.throughWalls.getValue() && RotationUtil.isRayBlocked(boundingBox, eyePos, yaw, pitch, 8.0)) {
                                if (RotationUtil.isRayBlocked(boundingBox, eyePos, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, 8.0)) {
                                    visible = false;
                                } else {
                                    yaw = mc.thePlayer.rotationYaw;
                                    pitch = mc.thePlayer.rotationPitch;
                                }
                            }
                            candidates.add(new EntityBox(
                                    living,
                                    boundingBox,
                                    living.posX,
                                    living.posY,
                                    living.posZ,
                                    yaw,
                                    pitch,
                                    distance,
                                    angle,
                                    visible
                            ));
                        }
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            this.target = null;
            this.targetInBlockRange = false;
            return;
        }

        this.targetInBlockRange = candidates.stream().anyMatch(box -> this.isInAutoBlockRange(box.distance));
        if (candidates.stream().anyMatch(box -> box.visible)) {
            candidates.removeIf(box -> !box.visible);
        }
        if (candidates.stream().anyMatch(box -> this.isInSwingRange(box.distance))) {
            candidates.removeIf(box -> !this.isInSwingRange(box.distance));
        }
        if (candidates.stream().anyMatch(box -> this.isInAttackRange(box.distance))) {
            candidates.removeIf(box -> !this.isInAttackRange(box.distance));
        }
        if (candidates.stream().anyMatch(box -> this.isEnemyPlayer(box.entity))) {
            candidates.removeIf(box -> !this.isEnemyPlayer(box.entity));
        }

        if (this.target != null && !this.switchTimer.hasTimeElapsed(this.switchDelay.getValue().longValue())) {
            Optional<EntityBox> currentTarget = candidates.stream()
                    .filter(box -> box.entity == this.target.entity)
                    .findFirst();
            if (currentTarget.isPresent()) {
                this.target = currentTarget.get();
                return;
            }
        }

        candidates.sort(this::compareTargets);
        if (this.mode.getValue() == 1 && this.attacked) {
            this.attacked = false;
            ++this.targetIndex;
        }
        if (this.mode.getValue() == 0 || this.targetIndex >= candidates.size()) {
            this.targetIndex = 0;
        }
        this.target = candidates.get(this.targetIndex);
        this.switchTimer.reset();
    }

    @EventTarget
    public void onPostTick(PostTickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.isUsingSword() && !mc.thePlayer.isBlocking()) {
            mc.thePlayer.setItemInUse(mc.thePlayer.getHeldItem(), mc.thePlayer.getHeldItem().getMaxItemUseDuration());
        }
    }

    @EventTarget(priority = -100)
    public void onSendPacket(SendPacketEvent event) {
        if (!this.isEnabled() || event.isCancelled()) {
            return;
        }
        if (event.getPacket() instanceof C07PacketPlayerDigging) {
            C07PacketPlayerDigging packet = (C07PacketPlayerDigging) event.getPacket();
            if (packet.getStatus() == Action.RELEASE_USE_ITEM) {
                this.blockingSent = false;
            }
        } else if (event.getPacket() instanceof C09PacketHeldItemChange) {
            this.blockingSent = false;
            if (this.blocking) {
                mc.thePlayer.stopUsingItem();
            }
        }
    }

    @EventTarget(priority = -100)
    public void onReceivePacket(ReceivePacketEvent event) {
        if (!this.isEnabled() || event.isCancelled() || this.debugLog.getValue() != 1) {
            return;
        }
        if (event.getPacket() instanceof S06PacketUpdateHealth) {
            S06PacketUpdateHealth packet = (S06PacketUpdateHealth) event.getPacket();
            this.logHealthDelta(packet.getHealth() - mc.thePlayer.getHealth());
        } else if (event.getPacket() instanceof S1CPacketEntityMetadata) {
            S1CPacketEntityMetadata packet = (S1CPacketEntityMetadata) event.getPacket();
            if (packet.getEntityId() == mc.thePlayer.getEntityId()) {
                for (WatchableObject watchable : packet.func_149376_c()) {
                    if (watchable.getDataValueId() == 6) {
                        this.logHealthDelta((Float) watchable.getObject() - mc.thePlayer.getHealth());
                    }
                }
            }
        }
    }

    @EventTarget
    public void onRightClickDelay(RightClickDelayEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.moveFix.getValue() == 1
                && this.rotations.getValue() != 3
                && RotationState.isRotated()
                && RotationState.getPriority() == 1.0F
                && MoveUtil.isForwardPressed()) {
            MoveUtil.fixStrafe(RotationState.getMoveYaw());
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.showTarget.getValue() != 0
                && this.target != null
                && TeamUtil.isEntityLoaded(this.target.entity)
                && this.isAttackAllowed()) {
            Color color = new Color(-1);
            switch (this.showTarget.getValue()) {
                case 1:
                    color = this.target.entity.hurtTime > 0 ? new Color(0xFF5555) : new Color(0x55FF55);
                    break;
                case 2:
                    color = ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis());
                    break;
            }
            RenderUtil.enableRenderState();
            RenderUtil.drawEntityBox(this.target.entity, color.getRed(), color.getGreen(), color.getBlue(), 63, 0.1F);
            RenderUtil.disableRenderState();
        }
    }

    @EventTarget
    public void onLeftClick(LeftClickMouseEvent event) {
        if (this.blocking) {
            event.setCancelled(true);
        } else if (this.isEnabled() && this.canEngageTarget() && this.isInSwingRange(this.target.distance)) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.blocking) {
            event.setCancelled(true);
        } else if (this.isEnabled() && this.canEngageTarget() && this.isInSwingRange(this.target.distance)) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onHitBlock(HitBlockEvent event) {
        if (this.blocking) {
            event.setCancelled(true);
        } else if (this.isEnabled() && this.canEngageTarget() && this.isInSwingRange(this.target.distance)) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onCancelUse(CancelUseEvent event) {
        if (this.blocking) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onEnabled() {
        this.target = null;
        this.targetInBlockRange = false;
        this.targetIndex = 0;
        this.attacked = false;
        this.attackDelay = 0L;
        this.blockHoldRemaining = 0L;
        this.blockCooldownRemaining = 0L;
        this.blockStage = 0;
    }

    @Override
    public void onDisabled() {
        Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
        this.blinkResetPending = false;
        this.blockingSent = false;
        this.blocking = false;
        this.fakeBlocking = false;
    }

    @Override
    public void verifyValue(String propertyName) {
        if (this.swingRange.getName().equals(propertyName)) {
            if (this.swingRange.getValue() < this.attackRange.getValue()) {
                this.attackRange.setValue(this.swingRange.getValue());
            }
        } else if (this.attackRange.getName().equals(propertyName)) {
            if (this.swingRange.getValue() < this.attackRange.getValue()) {
                this.swingRange.setValue(this.attackRange.getValue());
            }
        } else if (this.minAps.getName().equals(propertyName)) {
            if (this.minAps.getValue() > this.maxAps.getValue()) {
                this.maxAps.setValue(this.minAps.getValue());
            }
        } else if (this.maxAps.getName().equals(propertyName)) {
            if (this.minAps.getValue() > this.maxAps.getValue()) {
                this.minAps.setValue(this.maxAps.getValue());
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }
}
