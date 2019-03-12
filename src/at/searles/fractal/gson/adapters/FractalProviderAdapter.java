package at.searles.fractal.gson.adapters;

import at.searles.fractal.Fractal;
import at.searles.fractal.FractalProvider;
import at.searles.fractal.data.FractalData;
import com.google.gson.*;

import java.lang.reflect.Type;

public class FractalProviderAdapter implements JsonSerializer<FractalProvider>, JsonDeserializer<FractalProvider> {

    private static final String FRACTAL_COLLECTION = "fractals";
    private static final String EXCLUSIVE_PARAMETERS = "exclusiveParameters";

    @Override
    public FractalProvider deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {

        JsonObject obj = jsonElement.getAsJsonObject();

        JsonArray fractals = obj.getAsJsonArray(FRACTAL_COLLECTION);
        JsonArray exclusives = obj.getAsJsonArray(EXCLUSIVE_PARAMETERS);

        FractalProvider provider = new FractalProvider();

        for(JsonElement item : fractals) {
            provider.addFractal(context.deserialize(item, FractalData.class));
        }

        for(JsonElement item : exclusives) {
            provider.addExclusiveParameter(item.getAsString());
        }

        return provider;
    }

    @Override
    public JsonElement serialize(FractalProvider provider, Type type, JsonSerializationContext context) {
        JsonArray fractals = new JsonArray();

        for(int id : provider.fractalIds()) {
            fractals.add(context.serialize(provider.getFractal(id).data(), FractalData.class));
        }

        JsonArray exclusives = new JsonArray();

        for(String parameter: provider.exclusiveParameters()) {
            exclusives.add(parameter);
        }

        JsonObject jsonObject = new JsonObject();

        jsonObject.add(FRACTAL_COLLECTION, fractals);
        jsonObject.add(EXCLUSIVE_PARAMETERS, exclusives);

        return jsonObject;
    }
}
