package at.searles.fractal.gson.adapters;

import at.searles.fractal.Fractal;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.TreeMap;

import at.searles.fractal.data.FractalData;
import at.searles.fractal.data.ParameterType;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;

public class FractalDataAdapter implements JsonSerializer<FractalData>, JsonDeserializer<FractalData> {

    // ============ These segments are here for historic reasons =============

    private static final String OLD_SCALE_LABEL = "scale";
    private static final String OLD_SOURCE_LABEL = "source";

    private static final String OLD_INTS_LABEL = "ints";
    private static final String OLD_REALS_LABEL = "reals";
    private static final String OLD_CPLXS_LABEL = "cplxs";
    private static final String OLD_BOOLS_LABEL = "bools";
    private static final String OLD_EXPRS_LABEL = "exprs";
    private static final String OLD_COLORS_LABEL = "colors";
    private static final String OLD_PALETTES_LABEL = "palettes";
    private static final String OLD_SCALES_LABEL = "scales";

    private static final String OLD_DATA_LABEL = "arguments";

    // ============ The next segments are the new ones ==============

    private static final String CODE_LABEL = "code";
    private static final String DATA_LABEL = "data";

    // For parameters
    private static final String TYPE_LABEL = "type";
    private static final String VALUE_LABEL = "value";

    @Override
    public JsonElement serialize(FractalData fractal, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject ret = new JsonObject();

        // format:
        // "code": <code as String>
        ret.addProperty(CODE_LABEL, fractal.source);
        ret.add(DATA_LABEL, serializeParameters(fractal.parameters, context));

        return ret;
    }

    public FractalData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            String code = getSourceCode((JsonObject) json);
            Map<String, FractalData.Parameter> data = getParameters((JsonObject) json, context);

            return new FractalData(code, data);
        } catch(Throwable th) {
            throw new JsonParseException(th);
        }
    }

    private String getSourceCode(JsonObject obj) {
        JsonElement codeElement = obj.get(CODE_LABEL);

        if(codeElement == null || !codeElement.isJsonPrimitive() || !((JsonPrimitive) codeElement).isString()) {
            return oldGetSourceCode(obj);
        }

        return codeElement.getAsString();
    }

    private Map<String, FractalData.Parameter> getParameters(JsonObject obj, JsonDeserializationContext context) {

        JsonElement dataElement = obj.get(DATA_LABEL);

        if (dataElement == null || !dataElement.isJsonObject()) {
            return oldGetParameters(obj, context);
        }

        return deserializeParameters(dataElement, context);
    }

    public static Map<String, FractalData.Parameter> deserializeParameters(JsonElement json, JsonDeserializationContext context) throws JsonParseException {
        // {
        // "id": { "type": <type>, "value": ... }
        // }
        try {
            JsonObject obj = (JsonObject) json;

            Map<String, FractalData.Parameter> data = new TreeMap<String, FractalData.Parameter>();

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

                data.put(id, new FractalData.Parameter(type, value));
            }

            return data;
        } catch(Throwable th) {
            throw new JsonParseException(th);
        }
    }

    public static JsonElement serializeParameters(Map<String, FractalData.Parameter> src, JsonSerializationContext context) {
        // {
        // "id": { "type": <type>, "value": ... }
        // }
        JsonObject obj = new JsonObject();

        for(Map.Entry<String, FractalData.Parameter> entry : src.entrySet()) {
            JsonObject jo = new JsonObject();

            jo.addProperty(TYPE_LABEL, entry.getValue().type.identifier);
            jo.add(VALUE_LABEL, context.serialize(entry.getValue().value));

            obj.add(entry.getKey(), jo);
        }

        return obj;
    }

    private String oldGetSourceCode(JsonObject obj) {
        StringBuilder sourceCode = new StringBuilder();

        JsonArray sourceArray = obj.getAsJsonArray(OLD_SOURCE_LABEL);

        for (JsonElement line : sourceArray) {
            sourceCode.append(line.getAsString()).append('\n');
        }

        return sourceCode.toString();
    }

    private Map<String, FractalData.Parameter> oldGetParameters(JsonObject obj, JsonDeserializationContext context) {
        // Fetch data.
        Map<String, FractalData.Parameter> dataMap = new TreeMap<String, FractalData.Parameter>();

        JsonObject data = obj.getAsJsonObject(OLD_DATA_LABEL);

        if (data != null) {
            // all of them are optional.
            JsonObject ints = data.getAsJsonObject(OLD_INTS_LABEL);
            JsonObject reals = data.getAsJsonObject(OLD_REALS_LABEL);
            JsonObject cplxs = data.getAsJsonObject(OLD_CPLXS_LABEL);
            JsonObject bools = data.getAsJsonObject(OLD_BOOLS_LABEL);
            JsonObject exprs = data.getAsJsonObject(OLD_EXPRS_LABEL);
            JsonObject colors = data.getAsJsonObject(OLD_COLORS_LABEL);
            JsonObject palettes = data.getAsJsonObject(OLD_PALETTES_LABEL);
            JsonObject scales = data.getAsJsonObject(OLD_SCALES_LABEL);

            if (ints != null) for (Map.Entry<String, JsonElement> entry : ints.entrySet()) {
                dataMap.put(entry.getKey(), new FractalData.Parameter(ParameterType.Int, entry.getValue().getAsInt()));
            }

            if (reals != null)
                for (Map.Entry<String, JsonElement> entry : reals.entrySet()) {
                    dataMap.put(entry.getKey(), new FractalData.Parameter(ParameterType.Real, entry.getValue().getAsDouble()));
                }

            if (cplxs != null)
                for (Map.Entry<String, JsonElement> entry : cplxs.entrySet()) {
                    dataMap.put(entry.getKey(), new FractalData.Parameter(ParameterType.Cplx, context.deserialize(entry.getValue(), Cplx.class)));
                }

            if (bools != null)
                for (Map.Entry<String, JsonElement> entry : bools.entrySet()) {
                    dataMap.put(entry.getKey(), new FractalData.Parameter(ParameterType.Bool, entry.getValue().getAsBoolean()));
                }

            if (exprs != null)
                for (Map.Entry<String, JsonElement> entry : exprs.entrySet()) {
                    dataMap.put(entry.getKey(), new FractalData.Parameter(ParameterType.Expr, entry.getValue().getAsString()));
                }

            if (colors != null)
                for (Map.Entry<String, JsonElement> entry : colors.entrySet()) {
                    dataMap.put(entry.getKey(), new FractalData.Parameter(ParameterType.Color, entry.getValue().getAsInt()));
                }

            if (palettes != null)
                for (Map.Entry<String, JsonElement> entry : palettes.entrySet()) {
                    dataMap.put(entry.getKey(), new FractalData.Parameter(ParameterType.Palette, context.deserialize(entry.getValue(), Palette.class)));
                }

            if (scales != null)
                for (Map.Entry<String, JsonElement> entry : scales.entrySet()) {
                    dataMap.put(entry.getKey(), new FractalData.Parameter(ParameterType.Scale, context.deserialize(entry.getValue(), Scale.class)));
                }
        }


        // In old versions, scale was on top. There will be
        // most likely forever Jsons with a dedicated scale
        // field. For these, read scale from here.
        JsonElement element = obj.get(OLD_SCALE_LABEL);

        if (element != null) {
            Scale scale = context.deserialize(element, Scale.class);
            dataMap.put(Fractal.SCALE_LABEL, new FractalData.Parameter(ParameterType.Scale, scale));
        }

        return dataMap;
    }
}
