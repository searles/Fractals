package at.searles.fractal.gson.adapters;

import at.searles.fractal.Fractal;
import at.searles.fractal.data.FractalData;
import at.searles.fractal.data.ParameterType;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;
import com.google.gson.*;
import com.google.gson.internal.LazilyParsedNumber;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.logging.Logger;

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
    public JsonElement serialize(FractalData data, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject ret = new JsonObject();

        // format:
        // "code": <code as String>
        ret.addProperty(CODE_LABEL, data.source());
        ret.add(DATA_LABEL, serializeParameters(data, context));

        return ret;
    }

    public FractalData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        FractalData.Builder builder = new FractalData.Builder();

        try {
            String source = getSourceCode((JsonObject) json);
            builder.setSource(source);
            return queryParameters((JsonObject) json, context, builder);
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

    private FractalData queryParameters(JsonObject obj, JsonDeserializationContext context, FractalData.Builder builder) {

        JsonElement dataElement = obj.get(DATA_LABEL);

        if (dataElement == null || !dataElement.isJsonObject()) {
            return oldDeserializeParameters(obj, context, builder);
        }

        return deserializeParameters(dataElement.getAsJsonObject(), context, builder);
    }

    private static FractalData deserializeParameters(JsonObject json, JsonDeserializationContext context, FractalData.Builder builder) throws JsonParseException {
        // {
        // "id": "value"
        // }
        try {
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                String id = entry.getKey();
                JsonElement jsonValue = entry.getValue();

                Object value = null;

                if(jsonValue.isJsonPrimitive()) {
                    JsonPrimitive jsonPrimitive = jsonValue.getAsJsonPrimitive();

                    if(jsonPrimitive.isBoolean()) {
                        value = jsonPrimitive.getAsBoolean();
                    } else if(jsonPrimitive.isNumber()) {
                        value = jsonPrimitive.getAsBigDecimal();
                    } else if(jsonPrimitive.isString()) {
                        value = jsonPrimitive.getAsString();
                    } else {
                        // This should not happen since the list above is exhaustive.
                        throw new UnsupportedOperationException("this is actually a bug");
                    }
                } else if(jsonValue.isJsonArray()) {
                    // scale or palette. get a hint.
                    if(jsonValue.getAsJsonArray().size() == 2) {
                        value = context.deserialize(jsonValue, Cplx.class);
                    } else {
                        value = context.deserialize(jsonValue, Scale.class);
                    }
                } else if(jsonValue.isJsonObject()) {
                    value = context.deserialize(jsonValue, Palette.class);
                }

//                switch (type) { // nullptr is intended.
//                    case Int: // fall through
//                    case Color:
//                        value = jsonValue.getAsNumber().intValue();
//                        break;
//                    case Real:
//                        value = jsonValue.getAsNumber().doubleValue();
//                        break;
//                    case Expr:
//                        value = jsonValue.getAsString();
//                        break;
//                    case Bool:
//                        value = jsonValue.getAsBoolean();
//                        break;
//                    case Cplx:
//                        value = context.deserialize(jsonValue, Cplx.class);
//                        break;
//                    case Palette:
//                        value = context.deserialize(jsonValue, Palette.class);
//                        break;
//                    case Scale:
//                        value = context.deserialize(jsonValue, Scale.class);
//                        break;
//                    default:
//                        throw new IllegalArgumentException("bad type: " + id + ", " + type);
//                }

                if(value != null) {
                    if (!builder.addParameter(id, value)) {
                        // fixme Logger.getLogger("FractalDataAdapter", "ignoring undefined parameter " + id + " that was " + value);
                    }
                } else {
                    // fixme Logger.getLogger("FractalDataAdapter", "value with type " + builder.queryType(id) + " is ignored. It is formatted as " + jsonValue);
                }
            }

            return builder.commit();
        } catch(Throwable th) {
            throw new JsonParseException(th);
        }
    }

    private String oldGetSourceCode(JsonObject obj) {
        StringBuilder sourceCode = new StringBuilder();

        JsonArray sourceArray = obj.getAsJsonArray(OLD_SOURCE_LABEL);

        for (JsonElement line : sourceArray) {
            sourceCode.append(line.getAsString()).append('\n');
        }

        return sourceCode.toString();
    }

    private FractalData oldDeserializeParameters(JsonObject obj, JsonDeserializationContext context, FractalData.Builder builder) {
        // Fetch data.
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
                builder.addParameter(entry.getKey(), entry.getValue().getAsInt());
            }

            if (reals != null)
                for (Map.Entry<String, JsonElement> entry : reals.entrySet()) {
                    builder.addParameter(entry.getKey(), entry.getValue().getAsDouble());
                }

            if (cplxs != null)
                for (Map.Entry<String, JsonElement> entry : cplxs.entrySet()) {
                    builder.addParameter(entry.getKey(), context.deserialize(entry.getValue(), Cplx.class));
                }

            if (bools != null)
                for (Map.Entry<String, JsonElement> entry : bools.entrySet()) {
                    builder.addParameter(entry.getKey(), entry.getValue().getAsBoolean());
                }

            if (exprs != null)
                for (Map.Entry<String, JsonElement> entry : exprs.entrySet()) {
                    builder.addParameter(entry.getKey(), entry.getValue().getAsString());
                }

            if (colors != null)
                for (Map.Entry<String, JsonElement> entry : colors.entrySet()) {
                    builder.addParameter(entry.getKey(), entry.getValue().getAsInt());
                }

            if (palettes != null)
                for (Map.Entry<String, JsonElement> entry : palettes.entrySet()) {
                    builder.addParameter(entry.getKey(), context.deserialize(entry.getValue(), Palette.class));
                }

            if (scales != null)
                for (Map.Entry<String, JsonElement> entry : scales.entrySet()) {
                    builder.addParameter(entry.getKey(), context.deserialize(entry.getValue(), Scale.class));
                }
        }


        // In old versions, scale was on top. There will be
        // most likely forever Jsons with a dedicated scale
        // field. For these, read scale from here.
        JsonElement element = obj.get(OLD_SCALE_LABEL);

        if (element != null) {
            Scale scale = context.deserialize(element, Scale.class);
            builder.addParameter(Fractal.SCALE_LABEL, scale);
        }

        return builder.commit();
    }


    private static JsonElement serializeParameters(FractalData data, JsonSerializationContext context) {
        // {
        // "id": ... }
        // }
        JsonObject obj = new JsonObject();

        data.forEachParameter((id, value) -> {
            obj.add(id, context.serialize(value));
        });

        return obj;
    }
}
