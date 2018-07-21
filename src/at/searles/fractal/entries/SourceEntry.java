package at.searles.fractal.entries;

import java.util.LinkedHashMap;

/**
 * Main menus
 */
public class SourceEntry {

    public final String iconFilename;
    public final String description;
    public final String sourceFilename;

    public SourceEntry(String iconFilename, String description, String sourceFilename) {
        this.iconFilename = iconFilename;
        this.description = description;
        this.sourceFilename = sourceFilename;
    }

    public static class Collection extends LinkedHashMap<String, SourceEntry> {}
}
