package myau.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import myau.Myau;
import myau.mixin.IAccessorMinecraft;
import myau.module.Module;
import myau.property.Property;
import net.minecraft.client.Minecraft;

public class Config {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private final String name;
    private final File file;
    public static String lastConfig = null;

    public Config(String name) {
        if (name != null && !name.isEmpty() && !name.equals("!")) {
            this.name = name;
        } else {
            this.name = "default";
        }
        this.file = new File("./config/Myau/", String.format("%s.json", this.name));
        try {
            Files.createDirectories(this.file.toPath().getParent());
        } catch (Exception exception) {
            ((IAccessorMinecraft) mc).getLogger().error(exception.getMessage());
        }
    }

    public String getName() {
        return this.file.getName();
    }

    public boolean load() {
        try {
            JsonObject rootObject = new JsonParser()
                .parse(new InputStreamReader(Files.newInputStream(this.file.toPath()), StandardCharsets.UTF_8))
                .getAsJsonObject();
            for (Module module : Myau.moduleManager.modules.values()) {
                ArrayList<Property<?>> moduleProperties = Myau.propertyManager.properties.get(module.getClass());
                JsonElement moduleElement = rootObject.get(module.getName());
                if (moduleElement == null) {
                    if (moduleProperties != null) {
                        for (Property<?> property : moduleProperties) {
                            property.reset();
                        }
                    }
                    module.resetHidden();
                    module.resetKey();
                    module.resetEnabled();
                    continue;
                }
                JsonObject moduleObject = moduleElement.getAsJsonObject();
                if (moduleProperties != null) {
                    for (Property<?> property : moduleProperties) {
                        try {
                            if (!property.readJson(moduleObject)) {
                                throw new Exception();
                            }
                        } catch (Exception ignored) {
                            property.reset();
                        }
                    }
                }
                try {
                    module.setHidden(moduleObject.get("hidden").getAsBoolean());
                } catch (Exception ignored) {
                    module.resetHidden();
                }
                try {
                    module.setKey(moduleObject.get("key").getAsInt());
                } catch (Exception ignored) {
                    module.resetKey();
                }
                try {
                    module.setEnabled(moduleObject.get("toggled").getAsBoolean());
                } catch (Exception ignored) {
                    module.resetEnabled();
                }
            }
            Config.lastConfig = this.name;
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    public boolean save() {
        try {
            JsonObject rootObject = new JsonObject();
            for (Module module : Myau.moduleManager.modules.values()) {
                JsonObject moduleObject = new JsonObject();
                moduleObject.addProperty("toggled", module.isEnabled());
                moduleObject.addProperty("key", module.getKey());
                moduleObject.addProperty("hidden", module.isHidden());
                ArrayList<Property<?>> moduleProperties = Myau.propertyManager.properties.get(module.getClass());
                if (moduleProperties != null) {
                    for (Property<?> property : moduleProperties) {
                        property.writeJson(moduleObject);
                    }
                }
                rootObject.add(module.getName(), moduleObject);
            }
            try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(this.file.toPath()), StandardCharsets.UTF_8)) {
                GSON.toJson((JsonElement) rootObject, writer);
            }
            Config.lastConfig = this.name;
            return true;
        } catch (Exception exception) {
            return false;
        }
    }
}
