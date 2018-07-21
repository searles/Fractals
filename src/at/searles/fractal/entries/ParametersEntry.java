package at.searles.fractal.entries;

import java.util.LinkedHashMap;
import at.searles.fractal.data.Parameters;

/**
 * And now for the demos/presets.
 */
public class ParametersEntry {

    public final String iconFilename;
    public final String description;
    public final Parameters data;

    public ParametersEntry(String iconFilename, String description, Parameters data) {
        this.iconFilename = iconFilename;
        this.description = description;
        this.data = data;
    }

    public static class Collection extends LinkedHashMap<String, ParametersEntry> {}
}
