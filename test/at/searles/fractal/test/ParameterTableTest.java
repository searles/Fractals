package at.searles.fractal.test;

import at.searles.fractal.Fractal;
import at.searles.fractal.FractalCollection;
import at.searles.fractal.ParameterTable;
import at.searles.fractal.data.FractalData;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class ParameterTableTest {
    private FractalCollection collection;
    private ParameterTable table;
    private Map<Integer, Integer> ids;

    private void withFractals(FractalData...dataFields) {
        collection = new FractalCollection();
        ids = new HashMap<>();

        int i = 0;

        for(FractalData data : dataFields) {
            int id = collection.add(Fractal.fromData(data));
            ids.put(i++, id);
        }
    }

    private void withExclusiveParameters(String...keys) {
        Set<String> set = new TreeSet<>();
        set.addAll(Arrays.asList(keys));

        table = new ParameterTable().init(collection, set);
    }

    @Test
    public void testIndividualParameters() {
        withFractals(
            new FractalData.Builder()
                    .setSource("extern a int = 0; extern b int = 1; var c = a + b").commit(),
            new FractalData.Builder()
                    .setSource("extern a int = 0; extern b int = 1; var c = a + b").commit()
        );

        withExclusiveParameters("b");

        Assert.assertEquals(3 + 2, table.count()); // + 2 is Scale/Source

        // Individuals first

        Assert.assertEquals("b", table.get(3).key);
        Assert.assertEquals(ids.get(0), (Integer) table.get(3).owner);

        Assert.assertEquals("b", table.get(4).key);
        Assert.assertEquals(ids.get(1), (Integer) table.get(4).owner);

        Assert.assertEquals("Source", table.get(0).key);

        Assert.assertEquals("Scale", table.get(1).key);

        Assert.assertEquals("a", table.get(2).key);
    }

    @Test
    public void testOrderExterns() {
        withFractals(
                new FractalData.Builder()
                        .setSource("extern a expr = \"0\"; extern c expr = \"0\"; var x = a + b + c").commit(),
                new FractalData.Builder()
                        .setSource("extern a expr = \"0\"; extern b expr = \"0\"; extern c expr = \"0\"; extern d expr = \"0\"; var x = a + b + c + d").commit()
        );

        withExclusiveParameters("b");

        String s = "";
        for(int i = 0; i < table.count(); ++i) {
            s += table.get(i).key;
        }

        Assert.assertEquals("SourceScaleabbcd", s);
    }
}
