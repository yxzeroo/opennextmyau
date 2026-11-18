package myau.property;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import myau.module.Module;

public class PropertyManager {
    public final LinkedHashMap<Class<?>, ArrayList<Property<?>>> properties = new LinkedHashMap<>();

    public Property<?> getProperty(Module module, String name) {
        ArrayList<Property<?>> moduleProperties = this.properties.get(module.getClass());
        if (moduleProperties == null) {
            return null;
        }
        String normalized = name.replace("-", "");
        return moduleProperties.stream()
            .filter(property -> normalized.equalsIgnoreCase(property.getName().replace("-", "")))
            .findFirst()
            .orElse(null);
    }
}
