package myau.command.commands;

import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Locale;
import myau.Myau;
import myau.command.Command;
import myau.enums.ChatColors;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

public class DenickCommand extends Command {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public DenickCommand() {
        super(new ArrayList<>(Collections.singletonList("denick")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() < 2) {
            ChatUtil.sendFormatted(String.format("%sUsage: .%s <&oname&r>&r", Myau.clientName, args.get(0).toLowerCase(Locale.ROOT)));
            return;
        }
        NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(ChatColors.formatColor(args.get(1)));
        if (playerInfo == null) {
            ChatUtil.sendRaw(String.format(ChatColors.formatColor("%sNo entity with name &o%s&r"), ChatColors.formatColor(Myau.clientName), args.get(1)));
            return;
        }
        GameProfile profile = playerInfo.getGameProfile();
        Property textures = Iterables.getFirst(profile.getProperties().get("textures"), null);
        if (textures == null) {
            ChatUtil.sendRaw(String.format(ChatColors.formatColor("%sNo textures for entity with name &o%s&r"), ChatColors.formatColor(Myau.clientName), args.get(1)));
            return;
        }
        String decodedJson = new String(Base64.getDecoder().decode(textures.getValue().getBytes(StandardCharsets.UTF_8)));
        String realName = decodedJson.contains("profileName\" : \"") ? decodedJson.split("profileName\" : \"")[1].split("\"")[0] : "?";
        String realUuid = decodedJson.contains("profileId\" : \"") ? decodedJson.split("profileId\" : \"")[1].split("\"")[0] : "?";
        ChatUtil.sendRaw(String.format(
            ChatColors.formatColor("%s%s&r -> %s (&o%s&r)&r"),
            ChatColors.formatColor(Myau.clientName),
            profile.getName().replace("\u00a7", "&"),
            realName,
            realUuid
        ));
        if (!realUuid.isEmpty() && !realUuid.equals("?")) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(realUuid), null);
        }
    }

    @Override
    public ArrayList<String> completeTab(ArrayList<String> args) {
        return args.size() == 2 ? null : new ArrayList<>();
    }
}
