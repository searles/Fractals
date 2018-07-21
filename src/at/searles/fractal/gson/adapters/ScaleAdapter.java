package at.searles.fractal.gson.adapters;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

import at.searles.math.Scale;

public class ScaleAdapter implements JsonDeserializer<Scale>, JsonSerializer<Scale> {
    @Override
    public Scale deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        try {
            JsonArray array = (JsonArray) json;

            double data[] = new double[6];

            for (int i = 0; i < 6; ++i)
                data[i] = array.get(i).getAsDouble();

            return new Scale(data[0], data[1], data[2], data[3], data[4], data[5]);
        } catch (Throwable th) {
            throw new JsonParseException(th);
        }

    }

    @Override
    public JsonElement serialize(Scale scale, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray array = new JsonArray();

        array.add(scale.xx);
        array.add(scale.xy);
        array.add(scale.yx);
        array.add(scale.yy);
        array.add(scale.cx);
        array.add(scale.cy);

        return array;
    }
}
