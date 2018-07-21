package at.searles.fractal.gson.adapters;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

import at.searles.math.Cplx;

public class CplxAdapter implements JsonDeserializer<Cplx>, JsonSerializer<Cplx> {

    @Override
    public Cplx deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        try {
            JsonArray array = json.getAsJsonArray();

            double re = array.get(0).getAsDouble();
            double im = array.get(1).getAsDouble();

            return new Cplx(re, im);
        } catch (Throwable th) {
            throw new JsonParseException(th);
        }
    }

    @Override
    public JsonElement serialize(Cplx cplx, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray array = new JsonArray();

        array.add(cplx.re());
        array.add(cplx.im());

        return array;
    }
}
