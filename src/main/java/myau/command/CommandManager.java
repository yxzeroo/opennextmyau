package myau.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import myau.Myau;
import myau.event.EventTarget;
import myau.event.events.PostTickEvent;
import myau.event.events.SendPacketEvent;
import myau.mixin.IAccessorC14PacketTabComplete;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C14PacketTabComplete;
import net.minecraftforge.client.ClientCommandHandler;

public class CommandManager {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private String[] pendingCompletions;
    public final ArrayList<Command> commands = new ArrayList<>();

    private void executeCommand(String rawMessage) {
        ArrayList<String> args = new ArrayList<>(Arrays.asList(rawMessage.substring(1).trim().split("\\s+")));
        String commandName = args.get(0).toLowerCase(Locale.ROOT);
        if (commandName.isEmpty()) {
            ChatUtil.sendFormatted(String.format("%sUnknown command&r", Myau.clientName));
            return;
        }
        for (Command command : this.commands) {
            for (String alias : command.names) {
                if (alias.equals(commandName)) {
                    command.runCommand(args);
                    return;
                }
            }
        }
        ChatUtil.sendFormatted(String.format("%sUnknown command (&o%s&r)&r", Myau.clientName, commandName));
    }

    private String[] computeCompletions(String rawInput) {
        ArrayList<String> args = new ArrayList<>(Arrays.asList(rawInput.substring(1).split("\\s+", -1)));
        String commandName = args.get(0).toLowerCase(Locale.ROOT);
        if (args.size() == 1) {
            ArrayList<String> matches = new ArrayList<>();
            for (Command command : this.commands) {
                for (String alias : command.names) {
                    if (alias.startsWith(commandName)) {
                        matches.add(String.format(".%s", alias));
                    }
                }
            }
            return matches.toArray(new String[0]);
        }
        for (Command command : this.commands) {
            for (String alias : command.names) {
                if (alias.equals(commandName)) {
                    ArrayList<String> candidates = command.completeTab(args);
                    if (candidates == null) {
                        return null;
                    }
                    String lastPrefix = args.get(args.size() - 1).toLowerCase(Locale.ROOT);
                    return candidates.stream()
                        .filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(lastPrefix))
                        .toArray(String[]::new);
                }
            }
        }
        return new String[0];
    }

    public boolean isCommandMessage(String message) {
        if (message == null || message.length() < 2) {
            return false;
        }
        return message.charAt(0) == '.' && Character.isLetterOrDigit(message.charAt(1));
    }

    @EventTarget(priority = 150)
    public void onSendPacket(SendPacketEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (event.getPacket() instanceof C01PacketChatMessage) {
            C01PacketChatMessage packet = (C01PacketChatMessage) event.getPacket();
            String message = packet.getMessage();
            if (this.isCommandMessage(message)) {
                event.setCancelled(true);
                this.executeCommand(message);
            }
            return;
        }
        if (event.getPacket() instanceof C14PacketTabComplete) {
            C14PacketTabComplete packet = (C14PacketTabComplete) event.getPacket();
            String message = packet.getMessage();
            if (this.isCommandMessage(message)) {
                this.pendingCompletions = this.computeCompletions(message);
                if (this.pendingCompletions != null) {
                    event.setCancelled(true);
                    return;
                }
                String[] parts = message.split("\\s+", -1);
                String lastToken = parts[parts.length - 1];
                ((IAccessorC14PacketTabComplete) packet).setMessage(lastToken.isEmpty() ? " " : lastToken);
            }
        }
    }

    @EventTarget(priority = -150)
    public void onPostTick(PostTickEvent event) {
        if (this.pendingCompletions != null) {
            try {
                if (mc.currentScreen instanceof GuiChat) {
                    ClientCommandHandler.instance.latestAutoComplete = this.pendingCompletions;
                    ((GuiChat) mc.currentScreen).onAutocompleteResponse(new String[0]);
                }
            } catch (Exception ignored) {
            }
            this.pendingCompletions = null;
        }
    }
}
