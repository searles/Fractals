package at.searles.fractal.gson.adapters;

import at.searles.fractal.data.FractalData;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Base64;

import at.searles.fractal.entries.FavoriteEntry;
import at.searles.fractal.Fractal;

public class FavoriteEntryAdapter implements JsonDeserializer<FavoriteEntry>, JsonSerializer<FavoriteEntry> {

    private static final String FRACTAL_LABEL = "fractal";
    private static final String ICON_LABEL = "icon"; // this is optional
    private static final String DESCRIPTION_LABEL = "description"; // this is optional

    @Override
    public FavoriteEntry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            // In older versions, key and descriptor did not exist in this object.
            JsonObject obj = (JsonObject) json;
            FractalData fractal = context.deserialize(obj.get(FRACTAL_LABEL), FractalData.class);

            byte[] icon = null;

            JsonElement iconJson = obj.get(ICON_LABEL);

            if (iconJson != null) {
                String iconBase64 = iconJson.getAsString();
                try {
                    iconBase64 = iconBase64.replace("\n", "");
                    icon = Base64.getDecoder().decode(iconBase64);
                } catch (Throwable e) {
                    e.printStackTrace();
                    // otherwise ignore since bitmap is optional
                }
            }

            JsonElement descriptionJson = obj.get(DESCRIPTION_LABEL);

            String description = descriptionJson == null ? null : descriptionJson.getAsString();

            return new FavoriteEntry(icon, fractal, description);
        } catch (Throwable th) {
            throw new JsonParseException(th);
        }
    }

    @Override
    public JsonElement serialize(FavoriteEntry entry, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();

        // encode icon byte stream as Base64
        if (entry.icon != null) {
            obj.addProperty(ICON_LABEL, Base64.getEncoder().encodeToString(entry.icon));
        }

        obj.add(FRACTAL_LABEL, context.serialize(entry.fractal, FractalData.class));

        if(entry.description != null) {
            obj.addProperty(DESCRIPTION_LABEL, entry.description);
        }

        return obj;
    }
}
