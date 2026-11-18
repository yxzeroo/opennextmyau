package myau.module.modules;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import myau.Myau;
import myau.enums.ChatColors;
import myau.event.EventTarget;
import myau.event.events.LoadWorldEvent;
import myau.event.events.PostTickEvent;
import myau.event.events.ReceivePacketEvent;
import myau.event.events.Render2DEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.TextProperty;
import myau.util.ChatUtil;
import myau.util.ColorUtil;
import myau.util.SoundUtil;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

public class BedTracker extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final LinkedHashMap<String, Long> lastAlertTimes = new LinkedHashMap<>();
    private final LinkedHashSet<EntityEnderPearl> trackedPearls = new LinkedHashSet<>();
    private final LinkedHashSet<String> whitelistedPlayers = new LinkedHashSet<>();
    private final Color whiteColor = new Color(ChatColors.WHITE.toRgb());
    private final Color redColor = new Color(ChatColors.RED.toRgb());
    private final Color yellowColor = new Color(ChatColors.YELLOW.toRgb());
    private final Color greenColor = new Color(ChatColors.GREEN.toRgb());
    private BlockPos bedPos = null;
    private long lastMacroTime = -1L;
    private boolean waitingForSpawn = false;

    public final BooleanProperty alerts = new BooleanProperty("alerts", true);
    public final IntProperty alertsRange = new IntProperty("alerts-range", 48, 8, 128, this.alerts::getValue);
    public final BooleanProperty alertsOnPearl = new BooleanProperty("alerts-on-pearl", true);
    public final ModeProperty alertsSound = new ModeProperty("alerts-sound", 1, new String[]{"NONE", "MEOW", "ANVIL"}, () -> this.alerts.getValue() || this.alertsOnPearl.getValue());
    public final IntProperty alertsFrequency = new IntProperty("alerts-frequency", 5, 1, 30, () -> this.alerts.getValue() || this.alertsOnPearl.getValue());
    public final BooleanProperty macro = new BooleanProperty("macro", false);
    public final IntProperty macroRange = new IntProperty("macro-range", 24, 8, 128, this.macro::getValue);
    public final BooleanProperty macroOnPearl = new BooleanProperty("macro-on-pearl", false);
    public final TextProperty macroText = new TextProperty("macro-text", "/lobby", () -> this.macro.getValue() || this.macroOnPearl.getValue());
    public final IntProperty macroDelay = new IntProperty("macro-delay", 1, 1, 10, () -> this.macro.getValue() || this.macroOnPearl.getValue());
    public final BooleanProperty hud = new BooleanProperty("hud", true);
    public final ModeProperty hudPositionX = new ModeProperty("hud-position-x", 0, new String[]{"LEFT", "MIDDLE", "RIGHT"}, this.hud::getValue);
    public final ModeProperty hudPositionY = new ModeProperty("hud-position-y", 0, new String[]{"TOP", "MIDDLE", "BOTTOM"}, this.hud::getValue);
    public final IntProperty hudOffsetX = new IntProperty("hud-offset-x", 2, 0, 255, this.hud::getValue);
    public final IntProperty hudOffsetY = new IntProperty("hud-offset-y", 2, 0, 255, this.hud::getValue);
    public final FloatProperty hudScale = new FloatProperty("hud-scale", 1.0f, 0.5f, 1.5f, this.hud::getValue);
    public final BooleanProperty hudShadow = new BooleanProperty("hud-shadow", true, this.hud::getValue);

    public BedTracker() {
        super("BedTracker", false, 0, true);
    }

    private void playAlertSound() {
        switch (this.alertsSound.getValue()) {
            case 1:
                SoundUtil.playSound("mob.cat.meow");
                break;
            case 2:
                SoundUtil.playSound("random.anvil_land");
                break;
        }
    }

    private Color getDistanceColor(int distance) {
        if (distance < 0) {
            return this.whiteColor;
        }
        if (distance <= 100) {
            return this.greenColor;
        }
        if (distance <= 114) {
            return ColorUtil.interpolate((float) (114 - distance) / 14.0f, this.yellowColor, this.greenColor);
        }
        if (distance <= 128) {
            return ColorUtil.interpolate((float) (128 - distance) / 14.0f, this.redColor, this.yellowColor);
        }
        return this.redColor;
    }

    private boolean isBedBlock(BlockPos pos) {
        return pos != null && mc.theWorld.getBlockState(pos).getBlock() == Blocks.bed;
    }

    @EventTarget
    public void onPostTick(PostTickEvent event) {
        if (!this.isEnabled() || !this.isBedBlock(this.bedPos)) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean alerted = false;
        boolean triggerMacro = false;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityEnderPearl) {
                EntityEnderPearl pearl = (EntityEnderPearl) entity;
                if (!this.trackedPearls.contains(pearl)) {
                    this.trackedPearls.add(pearl);
                    if (this.alertsOnPearl.getValue()) {
                        ChatUtil.sendFormatted(String.format("%s%s: &fDetected &5Ender Pearl&r &e&l\u26a0&r", Myau.clientName, this.getName()));
                        alerted = true;
                    }
                    if (this.macroOnPearl.getValue() && this.lastMacroTime + (long) this.macroDelay.getValue() * 1000L <= now) {
                        this.lastMacroTime = now;
                        triggerMacro = true;
                    }
                }
            }
        }

        List<EntityPlayer> players = mc.theWorld.loadedEntityList.stream()
                .filter(entity -> entity instanceof EntityPlayer)
                .map(entity -> (EntityPlayer) entity)
                .filter(player -> !TeamUtil.isBot(player) && !this.whitelistedPlayers.contains(player.getName()))
                .collect(Collectors.toList());

        for (EntityPlayer player : players) {
            if (TeamUtil.isSameTeam(player)) {
                this.whitelistedPlayers.add(player.getName());
                continue;
            }
            double distance = player.getDistance(
                    (double) this.bedPos.getX() + 0.5,
                    (double) this.bedPos.getY() + 0.5,
                    (double) this.bedPos.getZ() + 0.5
            );
            String playerName = player.getName();
            String displayName = player.getDisplayName().getFormattedText();
            ItemStack heldItem = player.getHeldItem();
            boolean hasPearl = heldItem != null && heldItem.getItem() instanceof ItemEnderPearl;

            if (this.alerts.getValue() && distance < (double) this.alertsRange.getValue()) {
                Long lastAlert = this.lastAlertTimes.get(playerName);
                if (lastAlert == null || lastAlert + (long) this.alertsFrequency.getValue() * 1000L <= now) {
                    this.lastAlertTimes.put(playerName, now);
                    ChatUtil.sendFormatted(String.format(
                            "%s%s: %s&r &fis %d blocks away from your bed &e&l\u26a0&r",
                            Myau.clientName,
                            this.getName(),
                            displayName,
                            (int) distance + 1
                    ));
                    alerted = true;
                }
            }

            if (this.alertsOnPearl.getValue() && hasPearl) {
                Long lastAlert = this.lastAlertTimes.get(playerName);
                if (lastAlert == null || lastAlert + (long) this.alertsFrequency.getValue() * 1000L <= now) {
                    this.lastAlertTimes.put(playerName, now);
                    ChatUtil.sendFormatted(String.format(
                            "%s%s: %s&r &fhas &5Ender Pearl&r &e&l\u26a0&r",
                            Myau.clientName,
                            this.getName(),
                            displayName
                    ));
                    alerted = true;
                }
            }

            if ((this.macro.getValue() && distance < (double) this.macroRange.getValue())
                    || (this.macroOnPearl.getValue() && hasPearl)) {
                if (this.lastMacroTime + (long) this.macroDelay.getValue() * 1000L <= now) {
                    this.lastMacroTime = now;
                    triggerMacro = true;
                }
            }
        }

        if (alerted) {
            this.playAlertSound();
        }
        if (triggerMacro) {
            ChatUtil.sendRaw(String.format(
                    ChatColors.formatColor("%s%s: &fRunning &6%s&r"),
                    ChatColors.formatColor(Myau.clientName),
                    this.getName(),
                    this.macroText.getValue()
            ));
            ChatUtil.sendMessage(this.macroText.getValue());
        }
    }

    @EventTarget(priority = -50)
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled() || !this.hud.getValue()) {
            return;
        }
        if (mc.theWorld == null || mc.thePlayer == null || mc.gameSettings.showDebugInfo) {
            return;
        }
        GuiScreen currentScreen = mc.currentScreen;
        if (currentScreen != null && !(currentScreen instanceof GuiChat)) {
            return;
        }

        int distance = 0;
        boolean hasBed = this.isBedBlock(this.bedPos);
        if (hasBed) {
            double diffX = mc.thePlayer.posX - (double) this.bedPos.getX();
            double diffZ = mc.thePlayer.posZ - (double) this.bedPos.getZ();
            distance = (int) Math.sqrt(diffX * diffX + diffZ * diffZ) + 1;
        }

        String bedStatus = hasBed ? "&atrue&r" : "&cfalse&r";
        String distanceSuffix = hasBed
                ? String.format(" &7| &fDistance: &r%d%s", distance, distance >= 128 ? " &c&l\u26a0&r" : "")
                : "";
        String hudText = ChatColors.formatColor(String.format("&fBed: %s%s", bedStatus, distanceSuffix));

        ScaledResolution scaledResolution = new ScaledResolution(mc);
        float textWidth = (float) mc.fontRendererObj.getStringWidth(hudText);
        float textHeight = (float) mc.fontRendererObj.FONT_HEIGHT - 1.0f;
        float scale = this.hudScale.getValue();

        float posX = (float) this.hudOffsetX.getValue() / scale;
        switch (this.hudPositionX.getValue()) {
            case 0:
                ++posX;
                break;
            case 1:
                posX += (float) scaledResolution.getScaledWidth() / scale / 2.0f - textWidth / 2.0f;
                break;
            case 2:
                ++posX;
                posX *= -1.0f;
                posX += (float) scaledResolution.getScaledWidth() / scale - textWidth;
                break;
        }

        float posY = (float) this.hudOffsetY.getValue() / scale;
        switch (this.hudPositionY.getValue()) {
            case 0:
                ++posY;
                break;
            case 1:
                posY += (float) scaledResolution.getScaledHeight() / scale / 2.0f - textHeight / 2.0f;
                break;
            case 2:
                ++posY;
                posY *= -1.0f;
                posY += (float) scaledResolution.getScaledHeight() / scale - textHeight;
                break;
        }

        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1.0f);
        GlStateManager.translate(posX, posY, 0.0f);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        mc.fontRendererObj.drawString(hudText, 0.0f, 0.0f, this.getDistanceColor(distance).getRGB(), this.hudShadow.getValue());
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.waitingForSpawn = false;
    }

    @EventTarget
    public void onReceivePacket(ReceivePacketEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (event.getPacket() instanceof S02PacketChat) {
            S02PacketChat packet = (S02PacketChat) event.getPacket();
            String message = packet.getChatComponent().getFormattedText();
            if (message.contains("\u00a7e\u00a7lProtect your bed and destroy the enemy bed")
                    || message.contains("\u00a7e\u00a7lDestroy the enemy bed and then eliminate them")
                    || message.contains("\u00a7e\u00a7lEvery few seconds brings a new surprise")) {
                this.lastAlertTimes.clear();
                this.trackedPearls.clear();
                this.whitelistedPlayers.clear();
                this.bedPos = null;
                this.waitingForSpawn = true;
            }
        }
        if (event.getPacket() instanceof S08PacketPlayerPosLook && this.waitingForSpawn) {
            this.waitingForSpawn = false;
            this.executor.schedule(() -> {
                int playerX = MathHelper.floor_double(mc.thePlayer.posX);
                int playerY = MathHelper.floor_double(mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight());
                int playerZ = MathHelper.floor_double(mc.thePlayer.posZ);
                BlockPos nearestBed = null;
                int nearestDist = -1;
                for (int x = playerX - 25; x <= playerX + 25; ++x) {
                    for (int y = playerY - 25; y <= playerY + 25; ++y) {
                        for (int z = playerZ - 25; z <= playerZ + 25; ++z) {
                            BlockPos pos = new BlockPos(x, y, z);
                            if (this.isBedBlock(pos)) {
                                int dist = Math.abs(x - playerX) + Math.abs(y - playerY) + Math.abs(z - playerZ);
                                if (nearestDist < 0 || nearestDist > dist) {
                                    nearestBed = pos;
                                    nearestDist = dist;
                                }
                            }
                        }
                    }
                }
                if (nearestBed != null) {
                    this.bedPos = nearestBed;
                    ChatUtil.sendFormatted(String.format(
                            "%s%s: &fWhitelisted your bed at (%d, %d, %d) &a&l\u2714&r",
                            Myau.clientName,
                            this.getName(),
                            this.bedPos.getX(),
                            this.bedPos.getY(),
                            this.bedPos.getZ()
                    ));
                    SoundUtil.playSound("note.pling");
                }
            }, 3L, TimeUnit.SECONDS);
        }
    }

    @Override
    public void onDisabled() {
        this.lastAlertTimes.clear();
        this.trackedPearls.clear();
        this.whitelistedPlayers.clear();
        this.bedPos = null;
    }
}
