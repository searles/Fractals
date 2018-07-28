package at.searles.fractal.test;

import at.searles.fractal.entries.FavoriteEntry;
import at.searles.fractal.entries.ParametersEntry;
import at.searles.fractal.entries.SourceEntry;
import at.searles.fractal.gson.Serializers;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class EntriesTest {

    @Test
    public void testReadParametersAssets() throws IOException {
        String json = Utils.readResourceFile("assets/parameters.json");
        ParametersEntry.Collection collection = Utils.parse(json, ParametersEntry.Collection.class);

        Assert.assertEquals(1, collection.size());
    }


    @Test
    public void testReadSourceAssets() throws IOException {
        String json = Utils.readResourceFile("assets/sources.json");
        SourceEntry.Collection collection = Utils.parse(json, SourceEntry       .Collection.class);

        Assert.assertEquals(1, collection.size());
    }
}
