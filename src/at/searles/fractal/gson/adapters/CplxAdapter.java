package at.searles.fractal.gson.adapters;

import at.searles.math.Cplx;
import com.google.gson.*;

import java.lang.reflect.Type;

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
