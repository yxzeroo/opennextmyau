package myau.module.modules;

import java.util.ArrayList;
import java.util.Comparator;
import myau.Myau;
import myau.data.BlockData;
import myau.event.EventTarget;
import myau.event.events.HitBlockEvent;
import myau.event.events.LeftClickMouseEvent;
import myau.event.events.MoveInputEvent;
import myau.event.events.Render2DEvent;
import myau.event.events.RightClickDelayEvent;
import myau.event.events.RightClickMouseEvent;
import myau.event.events.SafeWalkEvent;
import myau.event.events.StrafeEvent;
import myau.event.events.SwapItemEvent;
import myau.event.events.UpdateEvent;
import myau.management.RotationState;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.util.BlockUtil;
import myau.util.ItemUtil;
import myau.util.MoveUtil;
import myau.util.PacketUtil;
import myau.util.PlayerUtil;
import myau.util.RandomUtil;
import myau.util.RotationUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3i;
import net.minecraft.world.WorldSettings;

public class Scaffold extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final double[] PLACE_OFFSETS = new double[]{
        0.03125, 0.09375, 0.15625, 0.21875, 0.28125, 0.34375, 0.40625, 0.46875,
        0.53125, 0.59375, 0.65625, 0.71875, 0.78125, 0.84375, 0.90625, 0.96875
    };
    private int rotationTick = 0;
    private int lastSlot = -1;
    private int blockCount = -1;
    private float yaw = -180.0F;
    private float pitch = 0.0F;
    private boolean canRotate = false;
    private int towerTick = 0;
    private int towerMoveTick = 0;
    private int keepYStage = 0;
    private int startY = 256;
    private boolean shouldKeepY = false;
    private boolean tellyJumping = false;
    private EnumFacing targetFacing = null;

    public final ModeProperty rotations = new ModeProperty("rotations", 2, new String[]{"NONE", "DEFAULT", "BACKWARDS", "SIDEWAYS"});
    public final ModeProperty moveFix = new ModeProperty("move-fix", 1, new String[]{"NONE", "SILENT"});
    public final ModeProperty sprint = new ModeProperty("sprint", 0, new String[]{"NONE", "VANILLA"});
    public final PercentProperty groundMotion = new PercentProperty("ground-motion", 100);
    public final PercentProperty airMotion = new PercentProperty("air-motion", 100);
    public final PercentProperty speedMotion = new PercentProperty("speed-motion", 100);
    public final ModeProperty tower = new ModeProperty("tower", 0, new String[]{"NONE", "VANILLA", "EXTRA", "TELLY"});
    public final ModeProperty keepY = new ModeProperty("keep-y", 0, new String[]{"NONE", "VANILLA", "EXTRA", "TELLY"});
    public final BooleanProperty keepYOnPress = new BooleanProperty("keep-y-on-press", false, () -> this.keepY.getValue() != 0);
    public final BooleanProperty multiPlace = new BooleanProperty("multi-place", true);
    public final BooleanProperty safeWalk = new BooleanProperty("safe-walk", true);
    public final BooleanProperty swing = new BooleanProperty("swing", true);
    public final BooleanProperty itemSpoof = new BooleanProperty("item-spoof", false);
    public final BooleanProperty blockCounter = new BooleanProperty("block-counter", true);

    public Scaffold() {
        super("Scaffold", false, 0);
    }

    private boolean shouldStopSprint() {
        if (this.isTellyTick()) {
            return false;
        }
        boolean keepYActive = this.keepY.getValue() == 1 || this.keepY.getValue() == 2;
        if (keepYActive && this.keepYStage > 0) {
            return false;
        }
        return this.sprint.getValue() == 0;
    }

    private boolean canScaffold() {
        BedNuker bedNuker = (BedNuker) Myau.moduleManager.modules.get(BedNuker.class);
        if (bedNuker.isEnabled() && bedNuker.isBreaking()) {
            return false;
        }
        LongJump longJump = (LongJump) Myau.moduleManager.modules.get(LongJump.class);
        return !longJump.isEnabled() || !longJump.canStartJump() || longJump.isJumping();
    }

    private EnumFacing getBestFacing(BlockPos blockPos, BlockPos targetPos) {
        double minDistanceSq = 0.0;
        EnumFacing bestFacing = null;
        for (EnumFacing facing : EnumFacing.VALUES) {
            if (facing != EnumFacing.DOWN) {
                BlockPos offsetPos = blockPos.offset(facing);
                if (offsetPos.getY() <= targetPos.getY()) {
                    double distanceSq = offsetPos.distanceSqToCenter(
                        (double) targetPos.getX() + 0.5,
                        (double) targetPos.getY() + 0.5,
                        (double) targetPos.getZ() + 0.5
                    );
                    if (bestFacing == null || distanceSq < minDistanceSq || (distanceSq == minDistanceSq && facing == EnumFacing.UP)) {
                        minDistanceSq = distanceSq;
                        bestFacing = facing;
                    }
                }
            }
        }
        return bestFacing;
    }

    private BlockData findBlockData() {
        int playerY = MathHelper.floor_double(mc.thePlayer.posY);
        BlockPos targetPos = new BlockPos(
            MathHelper.floor_double(mc.thePlayer.posX),
            (this.keepYStage != 0 && !this.shouldKeepY ? Math.min(playerY, this.startY) : playerY) - 1,
            MathHelper.floor_double(mc.thePlayer.posZ)
        );
        if (!BlockUtil.isReplaceable(targetPos)) {
            return null;
        }
        ArrayList<BlockPos> positions = new ArrayList<>();
        for (int x = -4; x <= 4; ++x) {
            for (int y = -4; y <= 0; ++y) {
                for (int z = -4; z <= 4; ++z) {
                    BlockPos pos = targetPos.add(x, y, z);
                    if (!BlockUtil.isReplaceable(pos) && !BlockUtil.isInteractable(pos)) {
                        double distance = mc.thePlayer.getDistance(
                            (double) pos.getX() + 0.5,
                            (double) pos.getY() + 0.5,
                            (double) pos.getZ() + 0.5
                        );
                        if (distance <= (double) mc.playerController.getBlockReachDistance()
                            && (this.keepYStage == 0 || this.shouldKeepY || pos.getY() < this.startY)) {
                            for (EnumFacing facing : EnumFacing.VALUES) {
                                if (facing != EnumFacing.DOWN) {
                                    BlockPos neighbor = pos.offset(facing);
                                    if (BlockUtil.isReplaceable(neighbor)) {
                                        positions.add(pos);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (positions.isEmpty()) {
            return null;
        }
        positions.sort(
            Comparator.comparingDouble(
                pos -> pos.distanceSqToCenter(
                    (double) targetPos.getX() + 0.5,
                    (double) targetPos.getY() + 0.5,
                    (double) targetPos.getZ() + 0.5
                )
            )
        );
        BlockPos bestPos = positions.get(0);
        EnumFacing bestFacing = this.getBestFacing(bestPos, targetPos);
        return bestFacing == null ? null : new BlockData(bestPos, bestFacing);
    }

    private void placeBlock(BlockPos blockPos, EnumFacing facing, Vec3 hitVec) {
        if (!ItemUtil.isHoldingPlaceableBlock() || this.blockCount <= 0) {
            return;
        }
        if (mc.playerController.onPlayerRightClick(
            mc.thePlayer,
            mc.theWorld,
            mc.thePlayer.inventory.getCurrentItem(),
            blockPos,
            facing,
            hitVec
        )) {
            if (mc.playerController.getCurrentGameType() != WorldSettings.GameType.CREATIVE) {
                --this.blockCount;
            }
            if (this.swing.getValue()) {
                mc.thePlayer.swingItem();
            } else {
                PacketUtil.sendPacket(new C0APacketAnimation());
            }
        }
    }

    private EnumFacing getCardinalFacing(float yaw) {
        if (yaw < -135.0F || yaw > 135.0F) {
            return EnumFacing.NORTH;
        }
        if (yaw < -45.0F) {
            return EnumFacing.EAST;
        }
        if (yaw < 45.0F) {
            return EnumFacing.SOUTH;
        }
        return EnumFacing.WEST;
    }

    private double getEdgeDistance(EnumFacing facing) {
        switch (facing) {
            case NORTH:
                return mc.thePlayer.posZ - Math.floor(mc.thePlayer.posZ);
            case EAST:
                return Math.ceil(mc.thePlayer.posX) - mc.thePlayer.posX;
            case SOUTH:
                return Math.ceil(mc.thePlayer.posZ) - mc.thePlayer.posZ;
            default:
                return mc.thePlayer.posX - Math.floor(mc.thePlayer.posX);
        }
    }

    private float getMotionFactor() {
        if (!mc.thePlayer.onGround) {
            return (float) this.airMotion.getValue() / 100.0F;
        }
        return MoveUtil.getSpeedEffect() > 0
            ? (float) this.speedMotion.getValue() / 100.0F
            : (float) this.groundMotion.getValue() / 100.0F;
    }

    private double getRandomStepOffset() {
        return 0.2155 - RandomUtil.nextDouble(1.0E-4, 9.0E-4);
    }

    private float getMovementYaw() {
        return MoveUtil.getDirectionYaw(mc.thePlayer.rotationYaw, (float) MoveUtil.getForwardValue(), (float) MoveUtil.getLeftValue());
    }

    private boolean isDiagonal(float yaw) {
        float angle = Math.abs(yaw % 90.0F);
        return angle > 20.0F && angle < 70.0F;
    }

    private boolean isTellyTick() {
        if (mc.thePlayer.onGround && MoveUtil.isForwardPressed() && !PlayerUtil.isBlockAbove()) {
            boolean tellyKeepY = this.keepY.getValue() == 3;
            boolean tellyTower = this.tower.getValue() == 3;
            return (tellyKeepY && this.keepYStage > 0) || (tellyTower && mc.gameSettings.keyBindJump.isKeyDown());
        }
        return false;
    }

    public int getSpoofSlot() {
        return this.lastSlot;
    }

    @EventTarget(priority = 50)
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.rotationTick > 0) {
            --this.rotationTick;
        }
        if (mc.thePlayer.onGround) {
            if (this.keepYStage > 0) {
                --this.keepYStage;
            }
            if (this.keepYStage < 0) {
                ++this.keepYStage;
            }
            if (this.keepYStage == 0
                && this.keepY.getValue() != 0
                && (!this.keepYOnPress.getValue() || PlayerUtil.isUsingItem())
                && !mc.gameSettings.keyBindJump.isKeyDown()
                && !mc.thePlayer.isPotionActive(Potion.jump)) {
                this.keepYStage = 1;
            }
            this.startY = this.shouldKeepY ? this.startY : MathHelper.floor_double(mc.thePlayer.posY);
            this.shouldKeepY = false;
            this.tellyJumping = false;
        }
        if (!this.canScaffold()) {
            return;
        }
        ItemStack heldStack = mc.thePlayer.getHeldItem();
        int heldBlocks = ItemUtil.isBlock(heldStack) ? heldStack.stackSize : 0;
        this.blockCount = Math.min(this.blockCount, heldBlocks);
        if (this.blockCount <= 0) {
            int startSlot = mc.thePlayer.inventory.currentItem;
            if (this.blockCount == 0) {
                --startSlot;
            }
            for (int i = startSlot; i > startSlot - 9; --i) {
                int hotbarSlot = (i % 9 + 9) % 9;
                ItemStack slotStack = mc.thePlayer.inventory.getStackInSlot(hotbarSlot);
                if (ItemUtil.isBlock(slotStack)) {
                    mc.thePlayer.inventory.currentItem = hotbarSlot;
                    this.blockCount = slotStack.stackSize;
                    break;
                }
            }
        }
        float lastYaw = event.getLastYaw();
        float lastPitch = event.getLastPitch();
        float movementYaw = this.getMovementYaw();
        float backwardsYaw = movementYaw - 180.0F;
        float sidewaysYaw = this.isDiagonal(movementYaw)
            ? backwardsYaw
            : movementYaw - 135.0F * ((movementYaw + 180.0F) % 90.0F < 45.0F ? 1.0F : -1.0F);
        if (!this.canRotate) {
            if (this.yaw == -180.0F && this.pitch == 0.0F) {
                this.pitch = RotationUtil.quantizeAngle(RandomUtil.nextFloat(65.0F, 85.0F), lastPitch);
            }
            switch (this.rotations.getValue()) {
                case 2:
                    this.yaw = RotationUtil.quantizeAngle(backwardsYaw, lastYaw);
                    break;
                default:
                    this.yaw = RotationUtil.quantizeAngle(sidewaysYaw, lastYaw);
                    break;
            }
        }
        BlockData blockData = this.findBlockData();
        Vec3 hitVec = null;
        if (blockData != null) {
            double[] xOffsets = PLACE_OFFSETS;
            double[] yOffsets = PLACE_OFFSETS;
            double[] zOffsets = PLACE_OFFSETS;
            switch (blockData.getFacing()) {
                case DOWN:
                    yOffsets = new double[]{0.0};
                    break;
                case UP:
                    yOffsets = new double[]{1.0};
                    break;
                case NORTH:
                    zOffsets = new double[]{0.0};
                    break;
                case EAST:
                    xOffsets = new double[]{1.0};
                    break;
                case SOUTH:
                    zOffsets = new double[]{1.0};
                    break;
                case WEST:
                    xOffsets = new double[]{0.0};
                    break;
            }
            float bestYaw = -180.0F;
            float bestPitch = 0.0F;
            float bestDiff = 0.0F;
            for (double offsetX : xOffsets) {
                for (double offsetY : yOffsets) {
                    for (double offsetZ : zOffsets) {
                        double deltaX = (double) blockData.getBlockPos().getX() + offsetX - mc.thePlayer.posX;
                        double deltaY = (double) blockData.getBlockPos().getY() + offsetY - mc.thePlayer.posY - (double) mc.thePlayer.getEyeHeight();
                        double deltaZ = (double) blockData.getBlockPos().getZ() + offsetZ - mc.thePlayer.posZ;
                        float[] candidateRotations = RotationUtil.getRotations(deltaX, deltaY, deltaZ);
                        candidateRotations[0] = RotationUtil.quantizeAngle(candidateRotations[0], lastYaw);
                        candidateRotations[1] = RotationUtil.quantizeAngle(candidateRotations[1], lastPitch);
                        candidateRotations[1] = MathHelper.clamp_float(candidateRotations[1], -90.0F, 90.0F);
                        MovingObjectPosition traceResult = RotationUtil.rayTraceBlocks(
                            candidateRotations[0],
                            candidateRotations[1],
                            mc.playerController.getBlockReachDistance(),
                            1.0F
                        );
                        if (traceResult != null
                            && traceResult.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                            && traceResult.getBlockPos().equals(blockData.getBlockPos())
                            && traceResult.sideHit == blockData.getFacing()) {
                            float angleDiff = Math.abs(MathHelper.wrapAngleTo180_float(candidateRotations[0] - this.yaw))
                                + Math.abs(candidateRotations[1] - this.pitch);
                            if ((bestYaw == -180.0F && bestPitch == 0.0F) || angleDiff < bestDiff) {
                                bestYaw = candidateRotations[0];
                                bestPitch = candidateRotations[1];
                                bestDiff = angleDiff;
                                hitVec = traceResult.hitVec;
                            }
                        }
                    }
                }
            }
            if (bestYaw != -180.0F || bestPitch != 0.0F) {
                this.yaw = bestYaw;
                this.pitch = bestPitch;
                this.canRotate = true;
            }
        }
        if (this.canRotate && MoveUtil.isForwardPressed()) {
            float yawOffset = Math.abs(MathHelper.wrapAngleTo180_float(backwardsYaw - this.yaw));
            if (yawOffset < 90.0F) {
                switch (this.rotations.getValue()) {
                    case 2:
                        this.yaw = RotationUtil.quantizeAngle(backwardsYaw, lastYaw);
                        break;
                    case 3:
                        this.yaw = RotationUtil.quantizeAngle(sidewaysYaw, lastPitch);
                        break;
                }
            }
        }
        if (this.rotations.getValue() != 0) {
            float targetYaw = this.yaw;
            float targetPitch = this.pitch;
            if (this.isTellyTick()) {
                float yawDelta = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - event.getLastYaw());
                targetYaw = RotationUtil.quantizeAngle(lastYaw + yawDelta * RandomUtil.nextFloat(0.98F, 0.99F), lastYaw);
                targetPitch = RotationUtil.quantizeAngle(RandomUtil.nextFloat(30.0F, 80.0F), lastPitch);
                this.rotationTick = 3;
                this.tellyJumping = true;
            } else {
                float yawDelta = MathHelper.wrapAngleTo180_float(this.yaw - event.getLastYaw());
                float maxTurn = RandomUtil.nextFloat(80.0F, 90.0F);
                if (this.tellyJumping && mc.thePlayer.motionY > 0.0 && this.rotationTick <= 1) {
                    maxTurn = RandomUtil.nextFloat(30.0F, 35.0F);
                }
                if (Math.abs(yawDelta) > maxTurn) {
                    float clampedTurn = RotationUtil.clampAngle(yawDelta, maxTurn);
                    targetYaw = RotationUtil.quantizeAngle(lastYaw + clampedTurn, lastYaw);
                    this.rotationTick = Math.max(this.rotationTick, 1);
                }
            }
            event.setRotation(targetYaw, targetPitch, 3);
            if (this.moveFix.getValue() == 1) {
                event.setMoveYaw(targetYaw, 3);
            }
        }
        if (blockData != null && hitVec != null && this.rotationTick <= 0) {
            this.placeBlock(blockData.getBlockPos(), blockData.getFacing(), hitVec);
            if (this.multiPlace.getValue()) {
                for (int extra = 0; extra < 3; ++extra) {
                    blockData = this.findBlockData();
                    if (blockData == null) {
                        break;
                    }
                    MovingObjectPosition traceResult = RotationUtil.rayTraceBlocks(
                        this.yaw,
                        this.pitch,
                        mc.playerController.getBlockReachDistance(),
                        1.0F
                    );
                    if (traceResult != null
                        && traceResult.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                        && traceResult.getBlockPos().equals(blockData.getBlockPos())
                        && traceResult.sideHit == blockData.getFacing()) {
                        this.placeBlock(blockData.getBlockPos(), blockData.getFacing(), traceResult.hitVec);
                        continue;
                    }
                    hitVec = BlockUtil.getClickVec(blockData.getBlockPos(), blockData.getFacing());
                    double deltaX = hitVec.xCoord - mc.thePlayer.posX;
                    double deltaY = hitVec.yCoord - mc.thePlayer.posY - (double) mc.thePlayer.getEyeHeight();
                    double deltaZ = hitVec.zCoord - mc.thePlayer.posZ;
                    float[] clickRotations = RotationUtil.getRotations(deltaX, deltaY, deltaZ);
                    if (!(Math.abs(MathHelper.wrapAngleTo180_float(clickRotations[0] - this.yaw)) < 120.0F)) {
                        break;
                    }
                    if (!(Math.abs(clickRotations[1] - this.pitch) < 60.0F)) {
                        break;
                    }
                    traceResult = RotationUtil.rayTraceBlocks(
                        clickRotations[0],
                        clickRotations[1],
                        mc.playerController.getBlockReachDistance(),
                        1.0F
                    );
                    if (traceResult == null
                        || traceResult.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                        || !traceResult.getBlockPos().equals(blockData.getBlockPos())
                        || traceResult.sideHit != blockData.getFacing()) {
                        break;
                    }
                    this.placeBlock(blockData.getBlockPos(), blockData.getFacing(), traceResult.hitVec);
                }
            }
        }
        if (this.targetFacing != null) {
            if (this.rotationTick <= 0) {
                int playerX = MathHelper.floor_double(mc.thePlayer.posX);
                int playerY = MathHelper.floor_double(mc.thePlayer.posY);
                int playerZ = MathHelper.floor_double(mc.thePlayer.posZ);
                BlockPos belowPos = new BlockPos(playerX, playerY - 1, playerZ);
                hitVec = BlockUtil.getHitVec(belowPos, this.targetFacing, this.yaw, this.pitch);
                this.placeBlock(belowPos, this.targetFacing, hitVec);
            }
            this.targetFacing = null;
        } else if (this.keepY.getValue() == 2 && this.keepYStage > 0 && !mc.thePlayer.onGround) {
            int nextY = MathHelper.floor_double(mc.thePlayer.posY + mc.thePlayer.motionY);
            if (nextY <= this.startY && mc.thePlayer.posY > (double) (this.startY + 1)) {
                this.shouldKeepY = true;
                blockData = this.findBlockData();
                if (blockData != null && this.rotationTick <= 0) {
                    hitVec = BlockUtil.getHitVec(blockData.getBlockPos(), blockData.getFacing(), this.yaw, this.pitch);
                    this.placeBlock(blockData.getBlockPos(), blockData.getFacing(), hitVec);
                }
            }
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (!mc.thePlayer.isCollidedHorizontally
            && mc.thePlayer.hurtTime <= 5
            && !mc.thePlayer.isPotionActive(Potion.jump)
            && mc.gameSettings.keyBindJump.isKeyDown()
            && ItemUtil.isHoldingPlaceableBlock()) {
            int yFraction = (int) (mc.thePlayer.posY % 1.0 * 100.0);
            switch (this.tower.getValue()) {
                case 1:
                    switch (this.towerTick) {
                        case 0:
                            if (mc.thePlayer.onGround) {
                                this.towerTick = 1;
                                mc.thePlayer.motionY = -0.0784000015258789;
                            }
                            return;
                        case 1:
                            if (yFraction == 0 && PlayerUtil.isBlockUnder()) {
                                this.startY = MathHelper.floor_double(mc.thePlayer.posY);
                                this.towerTick = 2;
                                mc.thePlayer.motionY = 0.41999998688697815;
                                if (MoveUtil.isForwardPressed()) {
                                    MoveUtil.setSpeed(MoveUtil.getSpeed(), MoveUtil.getDirectionYaw());
                                } else {
                                    MoveUtil.setSpeed(0.0);
                                    event.setForward(0.0F);
                                    event.setStrafe(0.0F);
                                }
                                return;
                            }
                            this.towerTick = 0;
                            return;
                        case 2:
                            this.towerTick = 3;
                            mc.thePlayer.motionY = 0.75 - mc.thePlayer.posY % 1.0;
                            return;
                        case 3:
                            this.towerTick = 1;
                            mc.thePlayer.motionY = 1.0 - mc.thePlayer.posY % 1.0;
                            return;
                        default:
                            this.towerTick = 0;
                            return;
                    }
                case 2:
                    switch (this.towerTick) {
                        case 0:
                            if (mc.thePlayer.onGround) {
                                this.towerTick = 1;
                                mc.thePlayer.motionY = -0.0784000015258789;
                            }
                            return;
                        case 1:
                            if (yFraction == 0 && PlayerUtil.isBlockUnder()) {
                                this.startY = MathHelper.floor_double(mc.thePlayer.posY);
                                if (!MoveUtil.isForwardPressed()) {
                                    this.towerMoveTick = 2;
                                    MoveUtil.setSpeed(0.0);
                                    event.setForward(0.0F);
                                    event.setStrafe(0.0F);
                                    EnumFacing facing = this.getCardinalFacing(MathHelper.wrapAngleTo180_float(this.yaw - 180.0F));
                                    double edgeDistance = this.getEdgeDistance(facing);
                                    if (edgeDistance > 0.1) {
                                        if (mc.thePlayer.onGround) {
                                            Vec3i directionVec = facing.getDirectionVec();
                                            double stepOffset = Math.min(this.getRandomStepOffset(), edgeDistance - 0.05);
                                            double jitter = RandomUtil.nextDouble(0.02, 0.03);
                                            AxisAlignedBB nextBox = mc.thePlayer.getEntityBoundingBox().offset(
                                                (double) directionVec.getX() * (stepOffset - jitter),
                                                0.0,
                                                (double) directionVec.getZ() * (stepOffset - jitter)
                                            );
                                            if (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, nextBox).isEmpty()) {
                                                mc.thePlayer.motionY = -0.0784000015258789;
                                                mc.thePlayer.setPosition(
                                                    nextBox.minX + (nextBox.maxX - nextBox.minX) / 2.0,
                                                    nextBox.minY,
                                                    nextBox.minZ + (nextBox.maxZ - nextBox.minZ) / 2.0
                                                );
                                            }
                                        }
                                        return;
                                    }
                                    this.towerTick = 2;
                                    this.targetFacing = facing;
                                    mc.thePlayer.motionY = 0.41999998688697815;
                                    return;
                                }
                                this.towerTick = 2;
                                ++this.towerMoveTick;
                                mc.thePlayer.motionY = 0.41999998688697815;
                                MoveUtil.setSpeed(MoveUtil.getSpeed(), MoveUtil.getDirectionYaw());
                                return;
                            }
                            this.towerTick = 0;
                            this.towerMoveTick = 0;
                            return;
                        case 2:
                            this.towerTick = 3;
                            mc.thePlayer.motionY -= RandomUtil.nextDouble(0.00101, 0.00109);
                            return;
                        case 3:
                            if (this.towerMoveTick >= 4) {
                                this.towerTick = 4;
                                this.towerMoveTick = 0;
                            } else {
                                this.towerTick = 1;
                                mc.thePlayer.motionY = 1.0 - mc.thePlayer.posY % 1.0;
                            }
                            return;
                        case 4:
                            this.towerTick = 5;
                            return;
                        case 5:
                            if (!PlayerUtil.isBlockUnder()) {
                                this.towerTick = 0;
                            } else {
                                this.towerTick = 1;
                                mc.thePlayer.motionY -= 0.08;
                                mc.thePlayer.motionY *= 0.9800000190734863;
                                mc.thePlayer.motionY -= 0.08;
                                mc.thePlayer.motionY *= 0.9800000190734863;
                            }
                            return;
                        default:
                            this.towerTick = 0;
                            this.towerMoveTick = 0;
                            return;
                    }
                default:
                    this.towerTick = 0;
                    this.towerMoveTick = 0;
                    break;
            }
        } else {
            this.towerTick = 0;
            this.towerMoveTick = 0;
        }
    }

    @EventTarget
    public void onRightClickDelay(RightClickDelayEvent event) {
        if (this.isEnabled()) {
            if (this.moveFix.getValue() == 1
                && RotationState.isRotated()
                && RotationState.getPriority() == 3.0F
                && MoveUtil.isForwardPressed()) {
                MoveUtil.fixStrafe(RotationState.getMoveYaw());
            }
            if (mc.thePlayer.onGround && this.keepYStage > 0 && MoveUtil.isForwardPressed()) {
                mc.thePlayer.movementInput.jump = true;
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled()) {
            float motionFactor = this.getMotionFactor();
            if (motionFactor != 1.0F) {
                if (mc.thePlayer.movementInput.moveForward != 0.0F && mc.thePlayer.movementInput.moveStrafe != 0.0F) {
                    mc.thePlayer.movementInput.moveForward *= 1.0F / (float) Math.sqrt(2.0);
                    mc.thePlayer.movementInput.moveStrafe *= 1.0F / (float) Math.sqrt(2.0);
                }
                mc.thePlayer.movementInput.moveForward *= motionFactor;
                mc.thePlayer.movementInput.moveStrafe *= motionFactor;
            }
            if (this.shouldStopSprint()) {
                mc.thePlayer.setSprinting(false);
            }
        }
    }

    @EventTarget
    public void onSafeWalk(SafeWalkEvent event) {
        if (this.isEnabled() && this.safeWalk.getValue()) {
            if (mc.thePlayer.onGround
                && mc.thePlayer.motionY <= 0.0
                && PlayerUtil.canMove(mc.thePlayer.motionX, -1.0, mc.thePlayer.motionZ)) {
                event.setSafeWalk(true);
            }
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (this.isEnabled() && this.blockCounter.getValue()) {
            int totalBlocks = 0;
            for (int slot = 0; slot < 9; ++slot) {
                ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
                if (stack != null && stack.stackSize > 0) {
                    Item item = stack.getItem();
                    if (item instanceof ItemBlock) {
                        Block block = ((ItemBlock) item).getBlock();
                        if (!BlockUtil.isInteractable(block) && BlockUtil.isSolid(block)) {
                            totalBlocks += stack.stackSize;
                        }
                    }
                }
            }
            HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
            float scale = hud.scale.getValue();
            GlStateManager.pushMatrix();
            GlStateManager.scale(scale, scale, 0.0F);
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(770, 771);
            String counterText = String.format("%d block%s left", totalBlocks, totalBlocks != 1 ? "s" : "");
            ScaledResolution resolution = new ScaledResolution(mc);
            float drawX = ((float) resolution.getScaledWidth() / 2.0F + (float) mc.fontRendererObj.FONT_HEIGHT * 1.5F) / scale;
            float drawY = (float) resolution.getScaledHeight() / 2.0F / scale - (float) mc.fontRendererObj.FONT_HEIGHT / 2.0F + 1.0F;
            int baseColor = totalBlocks > 0 ? 16777215 : 16733525;
            mc.fontRendererObj.drawString(counterText, drawX, drawY, baseColor | -1090519040, hud.shadow.getValue());
            GlStateManager.disableBlend();
            GlStateManager.enableDepth();
            GlStateManager.popMatrix();
        }
    }

    @EventTarget
    public void onLeftClick(LeftClickMouseEvent event) {
        if (this.isEnabled()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onHitBlock(HitBlockEvent event) {
        if (this.isEnabled()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onSwapItem(SwapItemEvent event) {
        if (this.isEnabled()) {
            this.lastSlot = event.setSlot(this.lastSlot);
            event.setCancelled(true);
        }
    }

    @Override
    public void onEnabled() {
        if (mc.thePlayer != null) {
            this.lastSlot = mc.thePlayer.inventory.currentItem;
        } else {
            this.lastSlot = -1;
        }
        this.blockCount = -1;
        this.rotationTick = 0;
        this.yaw = -180.0F;
        this.pitch = 0.0F;
        this.canRotate = false;
        this.towerTick = 0;
        this.towerMoveTick = 0;
        this.tellyJumping = false;
    }

    @Override
    public void onDisabled() {
        if (mc.thePlayer != null && this.lastSlot != -1) {
            mc.thePlayer.inventory.currentItem = this.lastSlot;
        }
    }
}
