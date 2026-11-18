package myau.util;

import myau.enums.ChatColors;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

public class ChatUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void addChatComponent(IChatComponent component) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(component);
        }
    }

    public static void sendFormatted(String message) {
        addChatComponent(new ChatComponentText(ChatColors.formatColor(message)));
    }

    public static void sendRaw(String message) {
        addChatComponent(new ChatComponentText(message));
    }

    public static void sendMessage(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.sendChatMessage(message);
        }
    }
}
