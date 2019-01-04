package at.searles.fractal.gson;

import at.searles.fractal.data.FractalData;
import at.searles.fractal.entries.FavoriteEntry;
import at.searles.fractal.gson.adapters.*;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * This class contains all serializers for custom classes from this and other
 * projects. Json-Serialization should only use this class.
 */

public class Serializers {

    private static Gson gson = null;

    public static Gson serializer() {
        // No need for synchronization since this will usually
        // be only called from the ui thread.
        if (gson == null) {
            GsonBuilder gsonBuilder = new GsonBuilder();

            // register some types
            gsonBuilder.registerTypeAdapter(Cplx.class, new CplxAdapter());
            gsonBuilder.registerTypeAdapter(Scale.class, new ScaleAdapter());
            gsonBuilder.registerTypeAdapter(Palette.class, new PaletteAdapter());

            gsonBuilder.registerTypeAdapter(FractalData.class, new FractalDataAdapter());

            gsonBuilder.registerTypeAdapter(FavoriteEntry.class, new FavoriteEntryAdapter());

            gsonBuilder.registerTypeAdapter(FavoriteEntry.Collection.class,
                    new CollectionAdapter<>(FavoriteEntry.class, FavoriteEntry.Collection::new));

            gsonBuilder.setLenient();

            gson = gsonBuilder.create();
        }

        return gson;
    }
}
