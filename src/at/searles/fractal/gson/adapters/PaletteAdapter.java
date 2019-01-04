package at.searles.fractal.gson.adapters;

import at.searles.math.color.Palette;
import com.google.gson.*;

import java.lang.reflect.Type;

public class PaletteAdapter implements JsonDeserializer<Palette>, JsonSerializer<Palette> {
    private static final String WIDTH_LABEL = "width";
    private static final String HEIGHT_LABEL = "height";
    private static final String COLORS_LABEL = "colors";

    @Override
    public Palette deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        try {
            JsonObject object = (JsonObject) json;

            int width = object.get(WIDTH_LABEL).getAsInt();
            int height = object.get(HEIGHT_LABEL).getAsInt();

            JsonArray array = object.getAsJsonArray(COLORS_LABEL);

            int colors[] = new int[height * width];

            for (int i = 0; i < colors.length; ++i) {
                colors[i] = array.get(i).getAsInt();
            }

            return new Palette(width, height, colors);
        } catch (Throwable th) {
            throw new JsonParseException(th);
        }
    }

    @Override
    public JsonElement serialize(Palette palette, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject object = new JsonObject();

        object.addProperty(WIDTH_LABEL, palette.width());
        object.addProperty(HEIGHT_LABEL, palette.height());

        JsonArray array = new JsonArray();

        for (int y = 0; y < palette.height(); ++y) {
            for (int x = 0; x < palette.width(); ++x) {
                array.add(palette.argb(x, y));
            }
        }

        object.add(COLORS_LABEL, array);

        return object;
    }
}
