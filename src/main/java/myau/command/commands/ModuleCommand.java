package myau.command.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import myau.Myau;
import myau.command.Command;
import myau.module.Module;
import myau.property.Property;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ModeProperty;
import myau.util.ChatUtil;

public class ModuleCommand extends Command {
    public ModuleCommand() {
        super(new ArrayList<>(Myau.moduleManager.modules.values().stream().map(Module::getName).collect(Collectors.toList())));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        Module module = Myau.moduleManager.getModule(args.get(0));
        if (args.size() < 2) {
            List<Property<?>> allProperties = Myau.propertyManager.properties.get(module.getClass());
            if (allProperties != null) {
                List<Property<?>> visibleProperties = allProperties.stream()
                    .filter(Property::isVisible)
                    .collect(Collectors.toList());
                if (!visibleProperties.isEmpty()) {
                    ChatUtil.sendFormatted(String.format("%s%s:&r", Myau.clientName, module.formatModule()));
                    for (Property<?> property : visibleProperties) {
                        ChatUtil.sendFormatted(String.format("&7\u00bb&r %s: %s&r", property.getName(), property.formatValue()));
                    }
                    return;
                }
            }
            ChatUtil.sendFormatted(String.format("%s%s has no properties&r", Myau.clientName, module.formatModule()));
            return;
        }
        Property<?> property = Myau.propertyManager.getProperty(module, args.get(1));
        if (property == null) {
            ChatUtil.sendFormatted(String.format("%s%s has no property &o%s&r", Myau.clientName, module.getName(), args.get(1)));
            return;
        }
        if (!property.isVisible()) {
            ChatUtil.sendFormatted(String.format("%s%s: &o%s&r is not available&r", Myau.clientName, module.getName(), property.getName()));
            return;
        }
        if (args.size() < 3 && !(property instanceof BooleanProperty)) {
            ChatUtil.sendFormatted(String.format("%s%s: &o%s&r is set to %s&r (%s)&r", Myau.clientName, module.getName(), property.getName(), property.formatValue(), property.getValuePrompt()));
            return;
        }
        String rawValue = args.size() < 3 ? null : String.join(" ", args.subList(2, args.size()));
        try {
            if (property.parseString(rawValue)) {
                ChatUtil.sendFormatted(String.format("%s%s: &o%s&r has been set to %s&r", Myau.clientName, module.getName(), property.getName(), property.formatValue()));
                return;
            }
        } catch (Exception ignored) {
        }
        ChatUtil.sendFormatted(String.format("%sInvalid value for property &o%s&r (%s)&r", Myau.clientName, property.getName(), property.getValuePrompt()));
    }

    @Override
    public ArrayList<String> completeTab(ArrayList<String> args) {
        Module module = Myau.moduleManager.getModule(args.get(0));
        if (module == null) {
            return new ArrayList<>();
        }
        if (args.size() == 2) {
            ArrayList<Property<?>> moduleProperties = Myau.propertyManager.properties.get(module.getClass());
            if (moduleProperties != null) {
                return moduleProperties.stream()
                    .filter(Property::isVisible)
                    .map(property -> property.getName().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toCollection(ArrayList::new));
            }
        }
        if (args.size() == 3) {
            Property<?> property = Myau.propertyManager.getProperty(module, args.get(1));
            if (property instanceof ModeProperty && property.isVisible()) {
                return Arrays.stream(((ModeProperty) property).getModes())
                    .map(mode -> mode.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toCollection(ArrayList::new));
            }
        }
        return new ArrayList<>();
    }
}
