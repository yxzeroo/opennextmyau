package myau.property.properties;

import com.google.gson.JsonObject;
import java.util.function.BooleanSupplier;
import myau.property.Property;

public class TextProperty extends Property<String> {
    public TextProperty(String name, String value) {
        this(name, value, null);
    }

    public TextProperty(String name, String value, BooleanSupplier visibleCheck) {
        super(name, value, visibleCheck);
    }

    @Override
    public String getValuePrompt() {
        return "text";
    }

    @Override
    public String formatValue() {
        return String.format("&f%s", this.getValue());
    }

    @Override
    public boolean parseString(String input) {
        return this.setValue(input);
    }

    @Override
    public boolean readJson(JsonObject jsonObject) {
        return this.parseString(jsonObject.get(this.getName()).getAsString());
    }

    @Override
    public void writeJson(JsonObject jsonObject) {
        jsonObject.addProperty(this.getName(), this.getValue());
    }
}
