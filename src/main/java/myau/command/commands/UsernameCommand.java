package myau.command.commands;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Arrays;
import myau.Myau;
import myau.command.Command;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;
import net.minecraft.util.StringUtils;

public class UsernameCommand extends Command {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public UsernameCommand() {
        super(new ArrayList<>(Arrays.asList("username", "name", "ign")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        Session session = mc.getSession();
        if (session == null) {
            return;
        }
        String username = session.getUsername();
        if (StringUtils.isNullOrEmpty(username)) {
            return;
        }
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(username), null);
            ChatUtil.sendFormatted(String.format("%sYour username has been copied to the clipboard (&o%s&r)&r", Myau.clientName, username));
        } catch (Exception exception) {
            ChatUtil.sendFormatted(String.format("%sFailed to copy&r", Myau.clientName));
        }
    }
}
