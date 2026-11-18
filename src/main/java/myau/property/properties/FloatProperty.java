package myau.property.properties;

import com.google.gson.JsonObject;
import java.util.function.BooleanSupplier;
import myau.property.Property;

public class FloatProperty extends Property<Float> {
    private final Float minimum;
    private final Float maximum;

    public FloatProperty(String name, Float value, Float minimum, Float maximum) {
        this(name, value, minimum, maximum, null);
    }

    public FloatProperty(String name, Float value, Float minimum, Float maximum, BooleanSupplier visibleCheck) {
        super(name, value, v -> v >= minimum && v <= maximum, visibleCheck);
        this.minimum = minimum;
        this.maximum = maximum;
    }

    @Override
    public String getValuePrompt() {
        return String.format("%s-%s", this.minimum, this.maximum);
    }

    @Override
    public String formatValue() {
        return String.format("&6%s", this.getValue());
    }

    @Override
    public boolean parseString(String input) {
        return this.setValue(Float.parseFloat(input));
    }

    @Override
    public boolean readJson(JsonObject jsonObject) {
        return this.setValue(jsonObject.get(this.getName()).getAsNumber().floatValue());
    }

    @Override
    public void writeJson(JsonObject jsonObject) {
        jsonObject.addProperty(this.getName(), this.getValue());
    }
}
