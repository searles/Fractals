package at.searles.fractal.gson.adapters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Map;

import at.searles.fractal.data.ParameterKey;
import at.searles.fractal.data.ParameterType;
import at.searles.fractal.data.Parameters;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;

public class ParametersAdapter implements JsonDeserializer<Parameters>, JsonSerializer<Parameters> {
    private static final String TYPE_LABEL = "type";
    private static final String VALUE_LABEL = "value";

    @Override
    public Parameters deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        // {
        // "id": { "type": <type>, "value": ... }
        // }
        try {
            JsonObject obj = (JsonObject) json;

            Parameters data = new Parameters();

            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String id = entry.getKey();

                JsonObject item = entry.getValue().getAsJsonObject();

                ParameterType type = ParameterType.fromString(item.get(TYPE_LABEL).getAsString());

                if(type == null) {
                    throw new NullPointerException("no such type");
                }

                JsonElement valueElement = item.get(VALUE_LABEL);

                Object value;

                switch (type) { // nullptr is intended.
                    case Int: // fall through
                    case Color:
                        value = valueElement.getAsNumber().intValue();
                        break;
                    case Real:
                        value = valueElement.getAsNumber().doubleValue();
                        break;
                    case Expr:
                        value = valueElement.getAsString();
                        break;
                    case Bool:
                        value = valueElement.getAsBoolean();
                        break;
                    case Cplx:
                        value = context.deserialize(valueElement, Cplx.class);
                        break;
                    case Palette:
                        value = context.deserialize(valueElement, Palette.class);
                        break;
                    case Scale:
                        value = context.deserialize(valueElement, Scale.class);
                        break;
                    default:
                        throw new IllegalArgumentException("bad type: " + id + ", " + type);
                }

                data.add(new ParameterKey(id, type), value);
            }

            return data;
        } catch(Throwable th) {
            throw new JsonParseException(th);
        }
    }

    @Override
    public JsonElement serialize(Parameters src, Type typeOfSrc, JsonSerializationContext context) {
        // {
        // "id": { "type": <type>, "value": ... }
        // }
        JsonObject obj = new JsonObject();

        for(ParameterKey key : src) {
            JsonElement value = context.serialize(src.get(key));

            JsonObject entry = new JsonObject();

            entry.addProperty(TYPE_LABEL, key.type.identifier);
            entry.add(VALUE_LABEL, value);

            obj.add(key.id, entry);
        }

        return obj;
    }
}
