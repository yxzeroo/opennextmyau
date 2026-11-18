package myau.property.properties;

import com.google.gson.JsonObject;
import java.util.function.BooleanSupplier;
import myau.property.Property;

public class ColorProperty extends Property<Integer> {
    public ColorProperty(String name, Integer color) {
        this(name, color, null);
    }

    public ColorProperty(String name, Integer color, BooleanSupplier visibleCheck) {
        super(name, color, v -> v >= 0 && v <= 0xFFFFFF, visibleCheck);
    }

    @Override
    public String getValuePrompt() {
        return "RGB";
    }

    @Override
    public String formatValue() {
        String hex = String.format("%06X", this.getValue());
        return String.format("&c%s&a%s&9%s", hex.substring(0, 2), hex.substring(2, 4), hex.substring(4, 6));
    }

    @Override
    public boolean parseString(String input) {
        return this.setValue(Integer.parseInt(input.replace("#", ""), 16));
    }

    @Override
    public boolean readJson(JsonObject jsonObject) {
        return this.parseString(jsonObject.get(this.getName()).getAsString());
    }

    @Override
    public void writeJson(JsonObject jsonObject) {
        jsonObject.addProperty(this.getName(), String.format("%06X", this.getValue()));
    }
}
