package at.searles.fractal.test;

import at.searles.fractal.entries.FavoriteEntry;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class JsonTest {

    private String json;
    private FavoriteEntry.Collection collection;

    @Test
    public void testV3() throws IOException {
        // This is a collection of version 3 of fractview.
        withJsonFile("collection_2017_08.txt");

        parseCollection();

        assertCount(131);
        assertIconsNotNull();
    }

    private void assertCount(int count) {
        Assert.assertEquals(count, collection.size());
    }

    private void assertIconsNotNull() {
        for(FavoriteEntry entry : collection.values()) {
            Assert.assertNotNull(entry.icon);
        }
    }

    private void parseCollection() {
        this.collection = Utils.parse(json, FavoriteEntry.Collection.class);
    }

    private void withJsonFile(String filename) throws IOException {
        this.json = Utils.readResourceFile(filename);
    }
}
