package myau.module.modules;

import com.google.common.base.CaseFormat;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import myau.Myau;
import myau.enums.ChatColors;
import myau.enums.DelayModules;
import myau.event.EventTarget;
import myau.event.events.HitBlockEvent;
import myau.event.events.KnockbackEvent;
import myau.event.events.LeftClickMouseEvent;
import myau.event.events.LivingUpdateEvent;
import myau.event.events.LoadWorldEvent;
import myau.event.events.ReceivePacketEvent;
import myau.event.events.Render2DEvent;
import myau.event.events.Render3DEvent;
import myau.event.events.RightClickDelayEvent;
import myau.event.events.RightClickMouseEvent;
import myau.event.events.SwapItemEvent;
import myau.event.events.TickEvent;
import myau.event.events.UpdateEvent;
import myau.management.RotationState;
import myau.mixin.IAccessorPlayerControllerMP;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.util.BlockUtil;
import myau.util.ColorUtil;
import myau.util.ItemUtil;
import myau.util.MoveUtil;
import myau.util.PacketUtil;
import myau.util.PlayerUtil;
import myau.util.RenderUtil;
import myau.util.RotationUtil;
import myau.util.TimerUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockBed.EnumPartType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class BedNuker extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final TimerUtil breakCooldownTimer = new TimerUtil();
    private final Color redColor = new Color(ChatColors.RED.toRgb());
    private final Color yellowColor = new Color(ChatColors.YELLOW.toRgb());
    private final Color greenColor = new Color(ChatColors.GREEN.toRgb());
    private BlockPos targetPos = null;
    private BlockPos spawnBedPos = null;
    private int breakStage = 0;
    private int breakTicks = 0;
    private float breakProgress = 0.0F;
    private boolean targetIsBed = false;
    private int savedSlot = -1;
    private boolean rotating = false;
    private boolean miningTick = false;
    private boolean waitingForGameStart = false;

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"LEGIT", "SWAP"});
    public final FloatProperty range = new FloatProperty("range", 4.5F, 3.0F, 6.0F);
    public final PercentProperty speed = new PercentProperty("speed", 0);
    public final BooleanProperty groundSpoof = new BooleanProperty("ground-spoof", false);
    public final ModeProperty ignoreVelocity = new ModeProperty("ignore-velocity", 0, new String[]{"NONE", "CANCEL", "DELAY"});
    public final BooleanProperty surroundings = new BooleanProperty("surroundings", true);
    public final BooleanProperty toolCheck = new BooleanProperty("tool-check", true);
    public final BooleanProperty whitelist = new BooleanProperty("whitelist", true);
    public final BooleanProperty swing = new BooleanProperty("swing", true);
    public final ModeProperty moveFix = new ModeProperty("move-fix", 1, new String[]{"NONE", "SILENT", "STRICT"});
    public final ModeProperty showTarget = new ModeProperty("show-target", 1, new String[]{"NONE", "DEFAULT", "HUD"});
    public final ModeProperty showProgress = new ModeProperty("show-progress", 1, new String[]{"NONE", "DEFAULT", "HUD"});

    public BedNuker() {
        super("BedNuker", false, 0);
    }

    private void resetTarget() {
        if (this.targetPos != null) {
            mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), this.targetPos, -1);
        }
        this.targetPos = null;
        this.breakStage = 0;
        this.breakTicks = 0;
        this.breakProgress = 0.0F;
        this.targetIsBed = false;
        this.rotating = false;
        this.miningTick = false;
    }

    private float getBreakProgressRatio() {
        if (this.targetPos == null) {
            return 0.0F;
        }
        float progress = this.breakProgress;
        if (this.groundSpoof.getValue()) {
            int slot = ItemUtil.findBestToolSlot(mc.thePlayer.inventory.currentItem, mc.theWorld.getBlockState(this.targetPos).getBlock());
            progress = (float) this.breakTicks * this.getPlayerRelativeBlockHardness(mc.theWorld.getBlockState(this.targetPos), this.targetPos, slot, true);
        }
        return Math.min(1.0F, progress / (1.0F - 0.3F * ((float) this.speed.getValue() / 100.0F)));
    }

    private void restoreSlot() {
        if (this.savedSlot != -1) {
            mc.thePlayer.inventory.currentItem = this.savedSlot;
            this.syncHeldItem();
            this.savedSlot = -1;
        }
    }

    private void syncHeldItem() {
        if (((IAccessorPlayerControllerMP) mc.playerController).getCurrentPlayerItem() != mc.thePlayer.inventory.currentItem) {
            mc.thePlayer.stopUsingItem();
        }
        ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
    }

    private boolean hasProperTool(Block block) {
        Material material = block.getMaterial();
        if (material != Material.iron && material != Material.anvil && material != Material.rock) {
            return true;
        }
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            if (stack != null && stack.getItem() instanceof ItemPickaxe) {
                return true;
            }
        }
        return false;
    }

    private EnumFacing getHitFacing(BlockPos blockPos) {
        double deltaX = (double) blockPos.getX() + 0.5 - mc.thePlayer.posX;
        double deltaY = (double) blockPos.getY() + 0.25 - mc.thePlayer.posY - (double) mc.thePlayer.getEyeHeight();
        double deltaZ = (double) blockPos.getZ() + 0.5 - mc.thePlayer.posZ;
        float[] rotations = RotationUtil.getRotations(deltaX, deltaY, deltaZ);
        MovingObjectPosition mop = RotationUtil.rayTraceBlocks(rotations[0], rotations[1], 8.0, 1.0F);
        return mop == null ? EnumFacing.UP : mop.sideHit;
    }

    private float getToolDigEfficiency(IBlockState state, int slot, boolean onGround) {
        ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
        float digSpeed = stack == null ? 1.0F : stack.getItem().getDigSpeed(stack, state);
        if (digSpeed > 1.0F) {
            int efficiencyLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, stack);
            if (efficiencyLevel > 0) {
                digSpeed += (float) (efficiencyLevel * efficiencyLevel + 1);
            }
        }
        if (mc.thePlayer.isPotionActive(Potion.digSpeed)) {
            digSpeed *= 1.0F + (float) (mc.thePlayer.getActivePotionEffect(Potion.digSpeed).getAmplifier() + 1) * 0.2F;
        }
        if (mc.thePlayer.isPotionActive(Potion.digSlowdown)) {
            switch (mc.thePlayer.getActivePotionEffect(Potion.digSlowdown).getAmplifier()) {
                case 0:
                    digSpeed *= 0.3F;
                    break;
                case 1:
                    digSpeed *= 0.09F;
                    break;
                case 2:
                    digSpeed *= 0.0027F;
                    break;
                default:
                    digSpeed *= 8.1E-4F;
            }
        }
        if (mc.thePlayer.isInsideOfMaterial(Material.water) && !EnchantmentHelper.getAquaAffinityModifier(mc.thePlayer)) {
            digSpeed /= 5.0F;
        }
        if (!onGround) {
            digSpeed /= 5.0F;
        }
        return digSpeed;
    }

    public boolean canHarvestBlock(Block block, int slot) {
        if (block.getMaterial().isToolNotRequired()) {
            return true;
        }
        ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
        return stack != null && stack.canHarvestBlock(block);
    }

    private float getPlayerRelativeBlockHardness(IBlockState state, BlockPos pos, int slot, boolean onGround) {
        Block block = state.getBlock();
        float hardness = block.getBlockHardness(mc.theWorld, pos);
        if (hardness < 0.0F) {
            return 0.0F;
        }
        float divisor = this.canHarvestBlock(block, slot) ? 30.0F : 100.0F;
        return this.getToolDigEfficiency(state, slot, onGround) / hardness / divisor;
    }

    private float getBlockBreakSpeed(BlockPos pos) {
        IBlockState state = mc.theWorld.getBlockState(pos);
        int slot = ItemUtil.findBestToolSlot(mc.thePlayer.inventory.currentItem, state.getBlock());
        return this.getPlayerRelativeBlockHardness(state, pos, slot, mc.thePlayer.onGround);
    }

    private BlockPos getBestSurroundingBlock(BlockPos bedPos) {
        IBlockState state = mc.theWorld.getBlockState(bedPos);
        if (state.getBlock() instanceof BlockBed) {
            ArrayList<BlockPos> surroundingPositions = new ArrayList<>();
            EnumPartType part = state.getValue(BlockBed.PART);
            EnumFacing facing = state.getValue(BlockBed.FACING);
            BlockPos otherHalf = bedPos.offset(part == EnumPartType.HEAD ? facing.getOpposite() : facing);
            for (BlockPos partPos : Arrays.asList(bedPos, otherHalf)) {
                for (EnumFacing side : Arrays.asList(EnumFacing.UP, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST)) {
                    BlockPos neighborPos = partPos.offset(side);
                    Block neighborBlock = mc.theWorld.getBlockState(neighborPos).getBlock();
                    if (BlockUtil.isReplaceable(neighborBlock)) {
                        return null;
                    }
                    if (!(neighborBlock instanceof BlockBed)) {
                        surroundingPositions.add(neighborPos);
                    }
                }
            }
            if (!surroundingPositions.isEmpty()) {
                surroundingPositions.sort((pos1, pos2) -> {
                    int speedComparison = Float.compare(this.getBlockBreakSpeed(pos2), this.getBlockBreakSpeed(pos1));
                    if (speedComparison != 0) {
                        return speedComparison;
                    }
                    double eyeY = mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight();
                    return Double.compare(
                            pos1.distanceSqToCenter(mc.thePlayer.posX, eyeY, mc.thePlayer.posZ),
                            pos2.distanceSqToCenter(mc.thePlayer.posX, eyeY, mc.thePlayer.posZ)
                    );
                });
                return surroundingPositions.get(0);
            }
        }
        return null;
    }

    private BlockPos findTargetBlock() {
        return this.findTargetBlock(mc.thePlayer.posX, mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
    }

    private BlockPos findTargetBlock(double eyeX, double eyeY, double eyeZ) {
        ArrayList<BlockPos> bedPositions = new ArrayList<>();
        int centerX = MathHelper.floor_double(eyeX);
        int centerY = MathHelper.floor_double(eyeY);
        int centerZ = MathHelper.floor_double(eyeZ);
        for (int x = centerX - 6; x <= centerX + 6; x++) {
            for (int y = centerY - 6; y <= centerY + 6; y++) {
                for (int z = centerZ - 6; z <= centerZ + 6; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!this.whitelist.getValue() || this.spawnBedPos == null || !(this.spawnBedPos.distanceSq(pos) < 10.0)) {
                        IBlockState state = mc.theWorld.getBlockState(pos);
                        Block block = state.getBlock();
                        if (block instanceof BlockBed && PlayerUtil.canReach(pos, eyeX, eyeY, eyeZ, this.range.getValue().doubleValue())) {
                            bedPositions.add(pos);
                        }
                    }
                }
            }
        }
        if (bedPositions.isEmpty()) {
            return null;
        }
        bedPositions.sort(Comparator.comparingDouble(
                pos -> pos.distanceSqToCenter(mc.thePlayer.posX, mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ)
        ));
        for (BlockPos bedPos : bedPositions) {
            if (this.surroundings.getValue()) {
                BlockPos surroundingPos = this.getBestSurroundingBlock(bedPos);
                if (surroundingPos != null) {
                    Block surroundingBlock = mc.theWorld.getBlockState(surroundingPos).getBlock();
                    if (this.toolCheck.getValue() && !this.hasProperTool(surroundingBlock)) {
                        continue;
                    }
                    return surroundingPos;
                }
            }
            return bedPos;
        }
        return null;
    }

    private void swingArm() {
        if (this.swing.getValue()) {
            mc.thePlayer.swingItem();
        } else {
            PacketUtil.sendPacket(new C0APacketAnimation());
        }
    }

    private Color getDisplayColor(int colorMode) {
        switch (colorMode) {
            case 1: {
                float progress = this.getBreakProgressRatio();
                if (progress <= 0.5F) {
                    return ColorUtil.interpolate(progress / 0.5F, this.redColor, this.yellowColor);
                }
                return ColorUtil.interpolate((progress - 0.5F) / 0.5F, this.yellowColor, this.greenColor);
            }
            case 2:
                return ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis());
            default:
                return new Color(-1);
        }
    }

    public boolean isBreaking() {
        return this.targetPos != null && this.rotating;
    }

    public boolean isReadyToBreak() {
        return this.targetPos != null && this.miningTick;
    }

    @EventTarget(priority = 50)
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.targetPos != null) {
            if (!mc.theWorld.isAirBlock(this.targetPos) && PlayerUtil.canReach(this.targetPos, this.range.getValue().doubleValue())) {
                if (!this.targetIsBed) {
                    BlockPos nextTarget = this.findTargetBlock();
                    if (nextTarget != null && mc.theWorld.getBlockState(nextTarget).getBlock() instanceof BlockBed) {
                        this.resetTarget();
                    }
                }
            } else {
                this.restoreSlot();
                this.resetTarget();
            }
        }

        if (this.targetPos != null) {
            int bestSlot = ItemUtil.findBestToolSlot(mc.thePlayer.inventory.currentItem, mc.theWorld.getBlockState(this.targetPos).getBlock());
            if (this.mode.getValue() == 0 && this.savedSlot == -1) {
                this.savedSlot = mc.thePlayer.inventory.currentItem;
                mc.thePlayer.inventory.currentItem = bestSlot;
                this.syncHeldItem();
            }

            switch (this.breakStage) {
                case 0: {
                    if (!mc.thePlayer.isUsingItem()) {
                        this.swingArm();
                        PacketUtil.sendPacket(new C07PacketPlayerDigging(Action.START_DESTROY_BLOCK, this.targetPos, this.getHitFacing(this.targetPos)));
                        this.swingArm();
                        mc.effectRenderer.addBlockHitEffects(this.targetPos, this.getHitFacing(this.targetPos));
                        this.breakStage = 1;
                    }
                    break;
                }
                case 1: {
                    if (this.mode.getValue() == 1) {
                        this.rotating = false;
                    }
                    this.miningTick = true;
                    this.breakTicks++;
                    this.breakProgress += this.getPlayerRelativeBlockHardness(mc.theWorld.getBlockState(this.targetPos), this.targetPos, bestSlot, mc.thePlayer.onGround);
                    float spoofedProgress = (float) this.breakTicks * this.getPlayerRelativeBlockHardness(
                            mc.theWorld.getBlockState(this.targetPos),
                            this.targetPos,
                            bestSlot,
                            mc.thePlayer.onGround && this.groundSpoof.getValue()
                    );
                    mc.effectRenderer.addBlockHitEffects(this.targetPos, this.getHitFacing(this.targetPos));
                    float threshold = 1.0F - 0.3F * ((float) this.speed.getValue() / 100.0F);
                    if (this.breakProgress >= threshold || spoofedProgress >= threshold) {
                        if (this.mode.getValue() == 1) {
                            this.rotating = true;
                            this.savedSlot = mc.thePlayer.inventory.currentItem;
                            mc.thePlayer.inventory.currentItem = bestSlot;
                            this.syncHeldItem();
                            if (mc.thePlayer.isUsingItem()) {
                                this.savedSlot = mc.thePlayer.inventory.currentItem;
                                mc.thePlayer.inventory.currentItem = (mc.thePlayer.inventory.currentItem + 1) % 9;
                                this.syncHeldItem();
                            }
                        }
                        this.miningTick = false;
                        PacketUtil.sendPacket(new C07PacketPlayerDigging(Action.STOP_DESTROY_BLOCK, this.targetPos, this.getHitFacing(this.targetPos)));
                        this.swingArm();
                        IBlockState brokenState = mc.theWorld.getBlockState(this.targetPos);
                        Block brokenBlock = brokenState.getBlock();
                        if (brokenBlock.getMaterial() != Material.air) {
                            mc.theWorld.playAuxSFX(2001, this.targetPos, Block.getStateId(brokenState));
                            mc.theWorld.setBlockToAir(this.targetPos);
                        }
                        if (brokenBlock instanceof BlockBed) {
                            this.breakCooldownTimer.reset();
                        }
                        this.breakStage = 2;
                    }
                    break;
                }
                case 2: {
                    this.restoreSlot();
                    this.resetTarget();
                    break;
                }
            }

            if (this.targetPos != null) {
                return;
            }
        }

        if (mc.thePlayer.capabilities.allowEdit && this.breakCooldownTimer.hasTimeElapsed(500L)) {
            this.targetPos = this.findTargetBlock();
            this.breakStage = 0;
            this.breakTicks = 0;
            this.breakProgress = 0.0F;
            this.targetIsBed = this.targetPos != null && mc.theWorld.getBlockState(this.targetPos).getBlock() instanceof BlockBed;
            this.restoreSlot();
            if (this.targetPos != null) {
                this.rotating = true;
            }
        }

        if (this.targetPos == null) {
            Myau.lagManager.setDelayState(false, DelayModules.BED_NUKER);
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.isBreaking()) {
            double deltaX = (double) this.targetPos.getX() + 0.5 - mc.thePlayer.posX;
            double deltaY = (double) this.targetPos.getY() + 0.5 - mc.thePlayer.posY - (double) mc.thePlayer.getEyeHeight();
            double deltaZ = (double) this.targetPos.getZ() + 0.5 - mc.thePlayer.posZ;
            float lastYaw = event.getLastYaw();
            float lastPitch = event.getLastPitch();
            float[] rotations = RotationUtil.getRotations(deltaX, deltaY, deltaZ);
            rotations[0] = RotationUtil.quantizeAngle(rotations[0], lastYaw);
            rotations[1] = RotationUtil.quantizeAngle(rotations[1], lastPitch);
            rotations[1] = MathHelper.clamp_float(rotations[1], -90.0F, 90.0F);
            event.setRotation(rotations[0], rotations[1], 5);
            if (this.moveFix.getValue() != 0) {
                event.setMoveYaw(rotations[0], 5);
            }
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.isReadyToBreak()
                && !Myau.packetStateTracker.attacked
                && !Myau.packetStateTracker.digging
                && !Myau.packetStateTracker.placing
                && !Myau.packetStateTracker.swung) {
            this.swingArm();
        }
    }

    @EventTarget
    public void onRightClickDelay(RightClickDelayEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.moveFix.getValue() == 1
                && RotationState.isRotated()
                && RotationState.getPriority() == 5.0F
                && MoveUtil.isForwardPressed()) {
            MoveUtil.fixStrafe(RotationState.getMoveYaw());
        }
    }

    @EventTarget(priority = 50)
    public void onKnockback(KnockbackEvent event) {
        if (!this.isEnabled() || event.isCancelled() || event.getY() <= 0.0) {
            return;
        }
        if (this.ignoreVelocity.getValue() == 1 && this.targetPos != null) {
            event.setCancelled(true);
            event.setX(mc.thePlayer.motionX);
            event.setY(mc.thePlayer.motionY);
            event.setZ(mc.thePlayer.motionZ);
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.targetPos != null && (!this.targetIsBed || !this.surroundings.getValue()) && this.showProgress.getValue() != 0) {
            HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
            float scale = hud.scale.getValue();
            String progressText = String.format("%d%%", (int) (this.getBreakProgressRatio() * 100.0F));
            GlStateManager.pushMatrix();
            GlStateManager.scale(scale, scale, 0.0F);
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(770, 771);
            int textWidth = mc.fontRendererObj.getStringWidth(progressText);
            ScaledResolution scaledResolution = new ScaledResolution(mc);
            float x = (float) scaledResolution.getScaledWidth() / 2.0F / scale - (float) textWidth / 2.0F;
            float y = (float) scaledResolution.getScaledHeight() / 5.0F * 2.0F / scale;
            int color = (this.getDisplayColor(this.showProgress.getValue()).getRGB() & 0xFFFFFF) | -1090519040;
            mc.fontRendererObj.drawString(progressText, x, y, color, hud.shadow.getValue());
            GlStateManager.disableBlend();
            GlStateManager.enableDepth();
            GlStateManager.popMatrix();
        }
    }

    @EventTarget(priority = -50)
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled() && this.targetPos != null && !mc.theWorld.isAirBlock(this.targetPos)) {
            mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), this.targetPos, (int) (this.getBreakProgressRatio() * 10.0F) - 1);
            if (this.showTarget.getValue() != 0) {
                BedESP bedESP = (BedESP) Myau.moduleManager.modules.get(BedESP.class);
                Color color = this.getDisplayColor(this.showTarget.getValue());
                RenderUtil.enableRenderState();
                double height = this.targetIsBed ? bedESP.getBoxHeight() : 1.0;
                RenderUtil.drawBlockBox(this.targetPos, height, color.getRed(), color.getGreen(), color.getBlue(), 63);
                RenderUtil.disableRenderState();
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.waitingForGameStart = false;
    }

    @EventTarget
    public void onReceivePacket(ReceivePacketEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (event.getPacket() instanceof S02PacketChat) {
            S02PacketChat chatPacket = (S02PacketChat) event.getPacket();
            String text = chatPacket.getChatComponent().getFormattedText();
            if (text.contains("\u00a7e\u00a7lProtect your bed and destroy the enemy bed")
                    || text.contains("\u00a7e\u00a7lDestroy the enemy bed and then eliminate them")
                    || text.contains("\u00a7e\u00a7lEvery few seconds brings a new surprise")) {
                this.waitingForGameStart = true;
            }
        }
        if (event.getPacket() instanceof S08PacketPlayerPosLook && this.waitingForGameStart) {
            this.waitingForGameStart = false;
            this.spawnBedPos = null;
            this.scheduler.schedule(() -> {
                int playerX = MathHelper.floor_double(mc.thePlayer.posX);
                int playerY = MathHelper.floor_double(mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight());
                int playerZ = MathHelper.floor_double(mc.thePlayer.posZ);
                BlockPos closestBed = null;
                int minManhattanDist = -1;
                for (int x = playerX - 25; x <= playerX + 25; x++) {
                    for (int y = playerY - 25; y <= playerY + 25; y++) {
                        for (int z = playerZ - 25; z <= playerZ + 25; z++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            IBlockState state = mc.theWorld.getBlockState(pos);
                            if (state.getBlock() instanceof BlockBed) {
                                int manhattanDist = Math.abs(x - playerX) + Math.abs(y - playerY) + Math.abs(z - playerZ);
                                if (minManhattanDist < 0 || minManhattanDist > manhattanDist) {
                                    closestBed = pos;
                                    minManhattanDist = manhattanDist;
                                }
                            }
                        }
                    }
                }
                if (closestBed != null) {
                    this.spawnBedPos = closestBed;
                }
            }, 1L, TimeUnit.SECONDS);
        }
        if (this.isEnabled()
                && this.targetPos != null
                && this.ignoreVelocity.getValue() == 2
                && Myau.lagManager.getDelayModule() != DelayModules.BED_NUKER) {
            if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity velocityPacket = (S12PacketEntityVelocity) event.getPacket();
                if (velocityPacket.getEntityID() == mc.thePlayer.getEntityId() && velocityPacket.getMotionY() > 0) {
                    Myau.lagManager.setDelayModule(DelayModules.BED_NUKER);
                    Myau.lagManager.delayedPackets.offer(velocityPacket);
                    event.setCancelled(true);
                }
            }
            if (event.getPacket() instanceof S27PacketExplosion) {
                S27PacketExplosion explosionPacket = (S27PacketExplosion) event.getPacket();
                if (explosionPacket.func_149149_c() != 0.0F
                        || explosionPacket.func_149144_d() != 0.0F
                        || explosionPacket.func_149147_e() != 0.0F) {
                    Myau.lagManager.setDelayModule(DelayModules.BED_NUKER);
                    Myau.lagManager.delayedPackets.offer(explosionPacket);
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventTarget
    public void onLeftClick(LeftClickMouseEvent event) {
        if (this.isEnabled()
                && (this.isBreaking() || (this.targetPos != null && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK))) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled() && this.isBreaking()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onHitBlock(HitBlockEvent event) {
        if (this.isEnabled()
                && (this.isBreaking() || (this.targetPos != null && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK))) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onSwapItem(SwapItemEvent event) {
        if (this.isEnabled() && this.savedSlot != -1) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onDisabled() {
        this.resetTarget();
        this.savedSlot = -1;
        Myau.lagManager.setDelayState(false, DelayModules.BED_NUKER);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }
}
