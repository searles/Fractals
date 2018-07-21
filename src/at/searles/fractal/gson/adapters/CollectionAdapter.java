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

public class CollectionAdapter<A, B extends Map<String, A>> implements JsonSerializer<B>, JsonDeserializer<Map<String, A>> {

    private final Class<A> cl;
    private final Creator<B> creator;

    public CollectionAdapter(Class<A> cl, Creator<B> creator) {
        this.cl = cl;
        this.creator = creator;
    }

    @Override
    public B deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            JsonObject obj = (JsonObject) json;

            B collection = creator.create();

            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                A value = context.deserialize(entry.getValue(), cl);
                collection.put(entry.getKey(), value);
            }

            return collection;
        } catch (Throwable th) {
            throw new JsonParseException(th);
        }
    }

    @Override
    public JsonElement serialize(B src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject object = new JsonObject();

        for (Map.Entry<String, A> entry : src.entrySet()) {
            JsonElement jsonEntry = context.serialize(entry.getValue(), cl);
            object.add(entry.getKey(), jsonEntry);
        }

        return object;
    }

    public static interface Creator<B> {
        B create();
    }
}
