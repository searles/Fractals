package at.searles.fractal.gson.adapters;

import at.searles.fractal.entries.SourceEntry;
import com.google.gson.*;

import java.lang.reflect.Type;

public class SourceEntryAdapter implements JsonDeserializer<SourceEntry>, JsonSerializer<SourceEntry> {

    private static final String ICON_FILENAME_LABEL = "icon";
    private static final String DESCRIPTION_LABEL = "description";
    private static final String SOURCE_FILENAME_LABEL = "source";

    @Override
    public SourceEntry deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        try {
            JsonObject obj = (JsonObject) jsonElement;

            String iconFilename = obj.get(ICON_FILENAME_LABEL).getAsString();
            String description = obj.get(DESCRIPTION_LABEL).getAsString();
            String code = obj.get(SOURCE_FILENAME_LABEL).getAsString();

            return new SourceEntry(iconFilename, description, code);
        } catch(Throwable th) {
            throw new JsonParseException(th);
        }
    }

    @Override
    public JsonElement serialize(SourceEntry entry, Type type, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();

        obj.addProperty(ICON_FILENAME_LABEL, entry.iconFilename);
        obj.addProperty(DESCRIPTION_LABEL, entry.description);
        obj.addProperty(SOURCE_FILENAME_LABEL, entry.sourceFilename);

        return obj;
    }
}
