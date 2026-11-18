package myau.command.commands;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import myau.Myau;
import myau.command.Command;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;

public class VclipCommand extends Command {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static final DecimalFormat DISTANCE_FORMAT = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));

    public VclipCommand() {
        super(new ArrayList<>(Collections.singletonList("vclip")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() < 2) {
            ChatUtil.sendFormatted(String.format("%sUsage: .%s <&odistance&r>&r", Myau.clientName, args.get(0).toLowerCase(Locale.ROOT)));
            return;
        }
        double distance = 0.0;
        try {
            distance = Double.parseDouble(args.get(1));
        } catch (NumberFormatException ignored) {
        } finally {
            mc.thePlayer.setPositionAndUpdate(mc.thePlayer.posX, mc.thePlayer.posY + distance, mc.thePlayer.posZ);
            ChatUtil.sendFormatted(String.format("%sClipped (%s blocks)", Myau.clientName, DISTANCE_FORMAT.format(distance)));
        }
    }
}
