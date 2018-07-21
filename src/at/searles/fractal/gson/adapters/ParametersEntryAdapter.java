package at.searles.fractal.gson.adapters;

import at.searles.fractal.data.Parameters;
import at.searles.fractal.entries.ParametersEntry;
import com.google.gson.*;

import java.lang.reflect.Type;

public class ParametersEntryAdapter implements JsonDeserializer<ParametersEntry>, JsonSerializer<ParametersEntry>  {

    private static final String ICON_FILENAME_LABEL = "icon";
    private static final String DESCRIPTION_LABEL = "description";
    private static final String DATA_LABEL = "data";

    @Override
    public ParametersEntry deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        try {
            JsonObject obj = (JsonObject) jsonElement;

            String iconFilename = obj.get(ICON_FILENAME_LABEL).getAsString();
            String description = obj.get(DESCRIPTION_LABEL).getAsString();
            Parameters data = context.deserialize(obj.get(DATA_LABEL), Parameters.class);

            return new ParametersEntry(iconFilename, description, data);
        } catch(Throwable th) {
            throw new JsonParseException(th);
        }
    }

    @Override
    public JsonElement serialize(ParametersEntry entry, Type type, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();

        obj.addProperty(ICON_FILENAME_LABEL, entry.iconFilename);
        obj.addProperty(DESCRIPTION_LABEL, entry.description);
        obj.add(DATA_LABEL, context.serialize(entry.data, Parameters.class));

        return obj;
    }
}
