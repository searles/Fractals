package at.searles.fractal.gson.adapters;

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

import at.searles.fractal.Fractal;
import at.searles.fractal.data.FractalData;
import at.searles.fractal.data.ParameterKey;
import at.searles.fractal.data.ParameterType;
import at.searles.fractal.data.Parameters;
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

    @Override
    public JsonElement serialize(FractalData fractal, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject ret = new JsonObject();

        // format:
        // "code": <code as String>
        ret.addProperty(CODE_LABEL, fractal.source);
        ret.add(DATA_LABEL, context.serialize(fractal.data));

        return ret;
    }

    public FractalData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            String code = getSourceCode((JsonObject) json);
            Parameters data = getParameters((JsonObject) json, context);

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

    private Parameters getParameters(JsonObject obj, JsonDeserializationContext context) {

        JsonElement dataElement = obj.get(DATA_LABEL);

        if (dataElement == null || !dataElement.isJsonObject()) {
            return oldGetParameters(obj, context);
        }

        return context.deserialize(dataElement, Parameters.class);
    }

    private String oldGetSourceCode(JsonObject obj) {
        StringBuilder sourceCode = new StringBuilder();

        JsonArray sourceArray = obj.getAsJsonArray(OLD_SOURCE_LABEL);

        for (JsonElement line : sourceArray) {
            sourceCode.append(line.getAsString()).append('\n');
        }

        return sourceCode.toString();
    }

    private Parameters oldGetParameters(JsonObject obj, JsonDeserializationContext context) {
        // Fetch data.
        Parameters dataMap = new Parameters();

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
                dataMap.add(new ParameterKey(entry.getKey(), ParameterType.Int), entry.getValue().getAsInt());
            }

            if (reals != null)
                for (Map.Entry<String, JsonElement> entry : reals.entrySet()) {
                    dataMap.add(new ParameterKey(entry.getKey(), ParameterType.Real), entry.getValue().getAsDouble());
                }

            if (cplxs != null)
                for (Map.Entry<String, JsonElement> entry : cplxs.entrySet()) {
                    dataMap.add(new ParameterKey(entry.getKey(), ParameterType.Cplx), context.deserialize(entry.getValue(), Cplx.class));
                }

            if (bools != null)
                for (Map.Entry<String, JsonElement> entry : bools.entrySet()) {
                    dataMap.add(new ParameterKey(entry.getKey(), ParameterType.Bool), entry.getValue().getAsBoolean());
                }

            if (exprs != null)
                for (Map.Entry<String, JsonElement> entry : exprs.entrySet()) {
                    dataMap.add(new ParameterKey(entry.getKey(), ParameterType.Expr), entry.getValue().getAsString());
                }

            if (colors != null)
                for (Map.Entry<String, JsonElement> entry : colors.entrySet()) {
                    dataMap.add(new ParameterKey(entry.getKey(), ParameterType.Color), entry.getValue().getAsInt());
                }

            if (palettes != null)
                for (Map.Entry<String, JsonElement> entry : palettes.entrySet()) {
                    dataMap.add(new ParameterKey(entry.getKey(), ParameterType.Palette), context.deserialize(entry.getValue(), Palette.class));
                }

            if (scales != null)
                for (Map.Entry<String, JsonElement> entry : scales.entrySet()) {
                    dataMap.add(new ParameterKey(entry.getKey(), ParameterType.Scale), context.deserialize(entry.getValue(), Scale.class));
                }
        }


        // In old versions, scale was on top. There will be
        // most likely forever Jsons with a dedicated scale
        // field. For these, read scale from here.
        JsonElement element = obj.get(OLD_SCALE_LABEL);

        if (element != null) {
            Scale scale = context.deserialize(element, Scale.class);
            dataMap.add(new ParameterKey(Fractal.SCALE_LABEL, ParameterType.Scale), scale);
        }

        return dataMap;
    }
}
