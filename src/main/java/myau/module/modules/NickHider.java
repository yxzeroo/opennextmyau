package myau.module.modules;

import java.util.regex.Matcher;
import myau.enums.ChatColors;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.TextProperty;
import net.minecraft.client.Minecraft;

public class NickHider extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final TextProperty name = new TextProperty("name", "You");
    public final BooleanProperty scoreboard = new BooleanProperty("scoreboard", true);
    public final BooleanProperty level = new BooleanProperty("level", true);

    public NickHider() {
        super("NickHider", false, 0, true);
    }

    public String replaceNick(String text) {
        if (text == null || mc.thePlayer == null) {
            return text;
        }
        if (this.scoreboard.getValue() && text.matches("\u00a77\\d{2}/\\d{2}/\\d{2}(?:\\d{2})?  ?\u00a78.*")) {
            text = text.replaceAll("\u00a78", "\u00a78\u00a7k").replaceAll("[^\\x00-\\x7F\u00a7]", "?");
        }
        return text.replaceAll(mc.thePlayer.getName(), Matcher.quoteReplacement(ChatColors.formatColor(this.name.getValue())));
    }
}
