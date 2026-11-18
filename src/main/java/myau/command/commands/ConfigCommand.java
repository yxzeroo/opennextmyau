package myau.command.commands;

import java.awt.Desktop;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import myau.Myau;
import myau.command.Command;
import myau.config.Config;
import myau.enums.ChatColors;
import myau.module.Module;
import myau.property.Property;
import myau.util.ChatUtil;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.WildcardFileFilter;

public class ConfigCommand extends Command {
    public static final FileFilter JSON_FILTER = new WildcardFileFilter("*.json", IOCase.INSENSITIVE);

    public ConfigCommand() {
        super(new ArrayList<>(Arrays.asList("config", "cfg", "c")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() < 2) {
            String cmd = args.get(0).toLowerCase(Locale.ROOT);
            ChatUtil.sendFormatted(String.format("%sUsage: .%s &oload&r/&osave&r <&oname&r> | .%s &oreset&r | .%s &olist&r | .%s &ofolder&r", Myau.clientName, cmd, cmd, cmd, cmd));
            return;
        }
        String action = args.get(1);
        if (action.equalsIgnoreCase("l")) {
            action = args.size() < 3 ? "list" : "load";
        }
        switch (action.toLowerCase(Locale.ROOT)) {
            case "load":
            case "reload": {
                Config config = new Config(args.size() < 3 ? Config.lastConfig : args.get(2));
                if (config.load()) {
                    ChatUtil.sendFormatted(String.format("%sConfig has been loaded (&a&o%s&r)&r", Myau.clientName, config.getName()));
                } else {
                    ChatUtil.sendFormatted(String.format("%sConfig couldn't be loaded (&c&o%s&r)&r", Myau.clientName, config.getName()));
                }
                return;
            }
            case "save":
            case "s": {
                Config config = new Config(args.size() < 3 ? Config.lastConfig : args.get(2));
                if (config.save()) {
                    ChatUtil.sendFormatted(String.format("%sConfig has been saved (&a&o%s&r)&r", Myau.clientName, config.getName()));
                } else {
                    ChatUtil.sendFormatted(String.format("%sConfig couldn't be saved (&c&o%s&r)&r", Myau.clientName, config.getName()));
                }
                return;
            }
            case "reset":
            case "clear": {
                for (Module module : Myau.moduleManager.modules.values()) {
                    List<Property<?>> moduleProperties = Myau.propertyManager.properties.get(module.getClass());
                    if (moduleProperties != null) {
                        for (Property<?> property : moduleProperties) {
                            property.reset();
                        }
                    }
                    module.resetHidden();
                    module.resetKey();
                    module.resetEnabled();
                }
                return;
            }
            case "list": {
                try {
                    File[] files = new File("./config/Myau/").listFiles(JSON_FILTER);
                    if (files == null) {
                        throw new Exception();
                    }
                    if (files.length == 0) {
                        ChatUtil.sendFormatted(String.format("%sNo configs found (&o%s&r)&r", Myau.clientName, "./config/Myau/"));
                    }
                    Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
                    ChatUtil.sendFormatted(String.format("%sConfigs:&r", Myau.clientName));
                    for (File file : files) {
                        String displayText = ChatColors.formatColor(String.format("&7\u00bb&r &o%s&r", file.getName()));
                        String loadCommand = String.format(".config load %s", FilenameUtils.removeExtension(file.getName()));
                        ChatUtil.addChatComponent(
                            new ChatComponentText(displayText).setChatStyle(
                                new ChatStyle()
                                    .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, loadCommand))
                                    .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(loadCommand)))
                            )
                        );
                    }
                } catch (Exception exception) {
                    ChatUtil.sendFormatted(String.format("%sFailed to read (&o%s&r)&r", Myau.clientName, "./config/Myau/"));
                }
                return;
            }
            case "folder":
            case "f":
            case "directory":
            case "dir": {
                try {
                    Desktop.getDesktop().open(new File("./config/Myau/"));
                } catch (Exception exception) {
                    ChatUtil.sendFormatted(String.format("%sFailed to open (&o%s&r)&r", Myau.clientName, "./config/Myau/"));
                }
                return;
            }
            default: {
                ChatUtil.sendFormatted(String.format("%sInvalid argument (&o%s&r)&r", Myau.clientName, args.get(1)));
            }
        }
    }

    @Override
    public ArrayList<String> completeTab(ArrayList<String> args) {
        if (args.size() == 2) {
            return new ArrayList<>(Arrays.asList(
                "load", "reload", "l", "save", "s", "reset", "clear", "list", "folder", "f", "directory", "dir"
            ));
        }
        if (args.size() == 3) {
            String action = args.get(1).toLowerCase(Locale.ROOT);
            if (action.equals("load") || action.equals("reload") || action.equals("l") || action.equals("save") || action.equals("s")) {
                File[] files = new File("./config/Myau/").listFiles(JSON_FILTER);
                if (files != null) {
                    Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
                    return Arrays.stream(files)
                        .map(File::getName)
                        .map(FilenameUtils::removeExtension)
                        .collect(Collectors.toCollection(ArrayList::new));
                }
            }
        }
        return new ArrayList<>();
    }
}
