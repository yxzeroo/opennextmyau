package myau.module.modules;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import myau.Myau;
import myau.enums.BlinkModules;
import myau.enums.ChatColors;
import myau.event.EventTarget;
import myau.event.events.Render2DEvent;
import myau.mixin.IAccessorGuiChat;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.util.ColorUtil;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

public class HUD extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final ModeProperty color = new ModeProperty("color", 3, new String[]{"RAINBOW", "CHROMA", "ASTOLFO", "CUSTOM1", "CUSTOM12", "CUSTOM123"});
    public final FloatProperty colorSpeed = new FloatProperty("color-speed", 1.0f, 0.5f, 1.5f);
    public final PercentProperty colorSaturation = new PercentProperty("color-saturation", 50);
    public final PercentProperty colorBrightness = new PercentProperty("color-brightness", 100);
    public final ColorProperty customColor1 = new ColorProperty("custom-color-1", 0xFFFFFF, () -> this.color.getValue() == 3 || this.color.getValue() == 4 || this.color.getValue() == 5);
    public final ColorProperty customColor2 = new ColorProperty("custom-color-2", 0xFFFFFF, () -> this.color.getValue() == 4 || this.color.getValue() == 5);
    public final ColorProperty customColor3 = new ColorProperty("custom-color-3", 0xFFFFFF, () -> this.color.getValue() == 5);
    public final ModeProperty positionX = new ModeProperty("position-x", 0, new String[]{"LEFT", "RIGHT"});
    public final ModeProperty positionY = new ModeProperty("position-y", 0, new String[]{"TOP", "BOTTOM"});
    public final IntProperty offsetX = new IntProperty("offset-x", 2, 0, 255);
    public final IntProperty offsetY = new IntProperty("offset-y", 2, 0, 255);
    public final FloatProperty scale = new FloatProperty("scale", 1.0f, 0.5f, 1.5f);
    public final PercentProperty background = new PercentProperty("background", 25);
    public final BooleanProperty bar = new BooleanProperty("bar", true);
    public final BooleanProperty shadow = new BooleanProperty("shadow", true);
    public final BooleanProperty suffixes = new BooleanProperty("suffixes", true);
    public final BooleanProperty lowerCase = new BooleanProperty("lower-case", false);
    public final BooleanProperty chatOutline = new BooleanProperty("chat-outline", true);
    public final BooleanProperty blinkTimer = new BooleanProperty("blink-timer", true);
    public final BooleanProperty toggleSounds = new BooleanProperty("toggle-sounds", true);
    public final BooleanProperty toggleAlerts = new BooleanProperty("toggle-alerts", true);

    public HUD() {
        super("HUD", true, 0, true);
    }

    private String getModuleName(Module module) {
        String moduleName = module.getName();
        if (this.lowerCase.getValue()) {
            moduleName = moduleName.toLowerCase(Locale.ROOT);
        }
        return moduleName;
    }

    private String[] getModuleSuffixes(Module module) {
        String[] moduleSuffixes = module.getSuffix();
        if (this.lowerCase.getValue()) {
            for (int i = 0; i < moduleSuffixes.length; ++i) {
                moduleSuffixes[i] = moduleSuffixes[i].toLowerCase();
            }
        }
        return moduleSuffixes;
    }

    private int getModuleWidth(Module module) {
        return this.calculateStringWidth(this.getModuleName(module), this.getModuleSuffixes(module));
    }

    private int calculateStringWidth(String moduleName, String[] moduleSuffixes) {
        int width = mc.fontRendererObj.getStringWidth(moduleName);
        if (this.suffixes.getValue()) {
            for (String suffix : moduleSuffixes) {
                width += 3 + mc.fontRendererObj.getStringWidth(suffix);
            }
        }
        return width;
    }

    private float getColorCycle(long timeMillis, long offset) {
        long speed = (long) (3000.0 / Math.pow(Math.min(Math.max(0.5f, this.colorSpeed.getValue()), 1.5f), 3.0));
        return 1.0f - (float) (Math.abs(timeMillis - offset * 300L) % speed) / (float) speed;
    }

    public Color getColor(long timeMillis) {
        return this.getColor(timeMillis, 0L);
    }

    public Color getColor(long timeMillis, long offset) {
        Color baseColor = Color.white;
        switch (this.color.getValue()) {
            case 0:
                baseColor = ColorUtil.fromHSB(this.getColorCycle(timeMillis, offset), 1.0f, 1.0f);
                break;
            case 1:
                baseColor = ColorUtil.fromHSB(this.getColorCycle(timeMillis / 3L, 0L), 1.0f, 1.0f);
                break;
            case 2: {
                float cycle = this.getColorCycle(timeMillis, offset);
                if (cycle % 1.0f < 0.5f) {
                    cycle = 1.0f - cycle % 1.0f;
                }
                baseColor = ColorUtil.fromHSB(cycle, 1.0f, 1.0f);
                break;
            }
            case 3:
                baseColor = new Color(this.customColor1.getValue());
                break;
            case 4: {
                double cycle = this.getColorCycle(timeMillis, offset);
                float factor = (float) (2.0 * Math.abs(cycle - Math.floor(cycle + 0.5)));
                baseColor = ColorUtil.interpolate(factor, new Color(this.customColor1.getValue()), new Color(this.customColor2.getValue()));
                break;
            }
            case 5: {
                double cycle = this.getColorCycle(timeMillis, offset);
                float factor = (float) (2.0 * Math.abs(cycle - Math.floor(cycle + 0.5)));
                if (factor <= 0.5f) {
                    baseColor = ColorUtil.interpolate(factor * 2.0f, new Color(this.customColor1.getValue()), new Color(this.customColor2.getValue()));
                } else {
                    baseColor = ColorUtil.interpolate((factor - 0.5f) * 2.0f, new Color(this.customColor2.getValue()), new Color(this.customColor3.getValue()));
                }
                break;
            }
        }
        float[] hsb = Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), null);
        return Color.getHSBColor(
                hsb[0],
                hsb[1] * ((float) this.colorSaturation.getValue() / 100.0f),
                hsb[2] * ((float) this.colorBrightness.getValue() / 100.0f)
        );
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (this.chatOutline.getValue() && mc.currentScreen instanceof GuiChat) {
            String inputText = ((IAccessorGuiChat) mc.currentScreen).getInputField().getText().trim();
            if (Myau.commandManager != null && Myau.commandManager.isCommandMessage(inputText)) {
                RenderUtil.enableRenderState();
                RenderUtil.drawBorderedRect(
                        2.0f,
                        mc.currentScreen.height - 14,
                        mc.currentScreen.width - 2,
                        mc.currentScreen.height - 2,
                        1.5f,
                        0,
                        this.getColor(System.currentTimeMillis()).getRGB()
                );
                RenderUtil.disableRenderState();
            }
        }

        if (!this.isEnabled() || mc.gameSettings.showDebugInfo) {
            return;
        }

        List<Module> activeModules = Myau.moduleManager.modules.values().stream()
                .filter(module -> module.isEnabled() && !module.isHidden())
                .sorted(Comparator.<Module>comparingInt(this::getModuleWidth).reversed())
                .collect(Collectors.toList());

        float scaleValue = this.scale.getValue();
        boolean hasShadow = this.shadow.getValue();
        int posXMode = this.positionX.getValue();
        int posYMode = this.positionY.getValue();

        float lineHeight = (float) mc.fontRendererObj.FONT_HEIGHT - 1.0f;
        float x = (float) this.offsetX.getValue() + (1.0f + (this.bar.getValue() ? (hasShadow ? 2.0f : 1.0f) : 0.0f)) * scaleValue;
        float y = (float) this.offsetY.getValue() + 1.0f * scaleValue;
        if (posXMode == 1) {
            x = (float) new ScaledResolution(mc).getScaledWidth() - x;
        }
        if (posYMode == 1) {
            y = (float) new ScaledResolution(mc).getScaledHeight() - y - lineHeight * scaleValue;
        }

        GlStateManager.pushMatrix();
        GlStateManager.scale(scaleValue, scaleValue, 0.0f);
        long now = System.currentTimeMillis();
        long index = 0L;
        for (Module module : activeModules) {
            String moduleName = this.getModuleName(module);
            String[] moduleSuffixes = this.getModuleSuffixes(module);
            float totalWidth = this.calculateStringWidth(moduleName, moduleSuffixes) - (hasShadow ? 0 : 1);
            int moduleColor = this.getColor(now, index).getRGB();

            RenderUtil.enableRenderState();
            if (this.background.getValue() > 0) {
                RenderUtil.drawRect(
                        x / scaleValue - 1.0f - (posXMode == 0 ? 0.0f : totalWidth),
                        y / scaleValue - (posYMode == 0 ? (index == 0L ? 1.0f : 0.0f) : (hasShadow ? 1.0f : 0.0f)),
                        x / scaleValue + 1.0f + (posXMode == 0 ? totalWidth : 0.0f),
                        y / scaleValue + lineHeight + (posYMode == 0 ? (hasShadow ? 1.0f : 0.0f) : (index == 0L ? 1.0f : 0.0f)),
                        new Color(0.0f, 0.0f, 0.0f, (float) this.background.getValue() / 100.0f).getRGB()
                );
            }
            if (this.bar.getValue()) {
                if (hasShadow) {
                    RenderUtil.drawRect(
                            x / scaleValue + (posXMode == 0 ? -3.0f : 1.0f),
                            y / scaleValue - (posYMode == 0 ? (index == 0L ? 1.0f : 0.0f) : 1.0f),
                            x / scaleValue + (posXMode == 0 ? -2.0f : 2.0f),
                            y / scaleValue + lineHeight + (posYMode == 0 ? 1.0f : (index == 0L ? 1.0f : 0.0f)),
                            moduleColor
                    );
                    RenderUtil.drawRect(
                            x / scaleValue + (posXMode == 0 ? -2.0f : 2.0f),
                            y / scaleValue - (posYMode == 0 ? (index == 0L ? 1.0f : 0.0f) : 1.0f),
                            x / scaleValue + (posXMode == 0 ? -1.0f : 3.0f),
                            y / scaleValue + lineHeight + (posYMode == 0 ? 1.0f : (index == 0L ? 1.0f : 0.0f)),
                            (moduleColor & 0xFCFCFC) >> 2 | moduleColor & 0xFF000000
                    );
                } else {
                    RenderUtil.drawRect(
                            x / scaleValue + (posXMode == 0 ? -2.0f : 1.0f),
                            y / scaleValue - (posYMode == 0 ? (index == 0L ? 1.0f : 0.0f) : 0.0f),
                            x / scaleValue + (posXMode == 0 ? -1.0f : 2.0f),
                            y / scaleValue + lineHeight + (posYMode == 0 ? 0.0f : (index == 0L ? 1.0f : 0.0f)),
                            moduleColor
                    );
                }
            }
            RenderUtil.disableRenderState();

            GlStateManager.disableDepth();
            if (hasShadow) {
                mc.fontRendererObj.drawStringWithShadow(
                        moduleName,
                        x / scaleValue - (posXMode == 1 ? totalWidth : 0.0f),
                        y / scaleValue,
                        moduleColor
                );
            } else {
                mc.fontRendererObj.drawString(
                        moduleName,
                        x / scaleValue - (posXMode == 1 ? totalWidth : 0.0f),
                        y / scaleValue + (posYMode == 1 ? 1.0f : 0.0f),
                        moduleColor,
                        false
                );
            }

            if (this.suffixes.getValue() && moduleSuffixes.length > 0) {
                float suffixOffset = (float) mc.fontRendererObj.getStringWidth(moduleName) + 3.0f;
                for (String suffix : moduleSuffixes) {
                    if (hasShadow) {
                        mc.fontRendererObj.drawStringWithShadow(
                                suffix,
                                x / scaleValue - (posXMode == 1 ? totalWidth : 0.0f) + suffixOffset,
                                y / scaleValue,
                                ChatColors.GRAY.toRgb()
                        );
                    } else {
                        mc.fontRendererObj.drawString(
                                suffix,
                                x / scaleValue - (posXMode == 1 ? totalWidth : 0.0f) + suffixOffset,
                                y / scaleValue + (posYMode == 1 ? 1.0f : 0.0f),
                                ChatColors.GRAY.toRgb(),
                                false
                        );
                    }
                    suffixOffset += (float) mc.fontRendererObj.getStringWidth(suffix) + (hasShadow ? 3.0f : 2.0f);
                }
            }

            y += (lineHeight + (hasShadow ? 1.0f : 0.0f)) * scaleValue * (posYMode == 0 ? 1.0f : -1.0f);
            ++index;
        }

        if (this.blinkTimer.getValue()) {
            BlinkModules blinkingModule = Myau.blinkManager.getBlinkingModule();
            if (blinkingModule != BlinkModules.NONE && blinkingModule != BlinkModules.AUTO_BLOCK) {
                long packetCount = Myau.blinkManager.countMovementPackets();
                if (packetCount > 0L) {
                    GlStateManager.enableBlend();
                    GlStateManager.blendFunc(770, 771);
                    ScaledResolution scaledResolution = new ScaledResolution(mc);
                    String countText = String.valueOf(packetCount);
                    mc.fontRendererObj.drawString(
                            countText,
                            (float) scaledResolution.getScaledWidth() / 2.0f / scaleValue - (float) mc.fontRendererObj.getStringWidth(countText) / 2.0f,
                            (float) scaledResolution.getScaledHeight() / 5.0f * 3.0f / scaleValue,
                            this.getColor(now, index).getRGB() & 0xFFFFFF | 0xBF000000,
                            hasShadow
                    );
                    GlStateManager.disableBlend();
                }
            }
        }

        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }
}
