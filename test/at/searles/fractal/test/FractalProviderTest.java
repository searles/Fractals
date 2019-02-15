package at.searles.fractal.test;

import at.searles.fractal.Fractal;
import at.searles.fractal.FractalProvider;
import at.searles.fractal.data.FractalData;
import at.searles.math.Scale;
import at.searles.meelan.MeelanException;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;


public class FractalProviderTest {

    private FractalData.Builder builders[];
    private FractalProvider provider;
    private ArrayList<Integer> ids;

    private void withSources(String...sources) {
        builders = new FractalData.Builder[sources.length];

        for(int i = 0; i < sources.length; ++i) {
            builders[i] = new FractalData.Builder().setSource(sources[i]);
        }
    }

    private void withParameter(int n, String id, Object value) {
        builders[n].addParameter(id, value);
    }

    private void withProvider(String...exclusiveParameters) {
        ids = new ArrayList<>();
        this.provider = new FractalProvider();

        for(String p : exclusiveParameters) {
            this.provider.addExclusiveParameter(p);
        }

        for(FractalData.Builder b : builders) {
            ids.add(this.provider.addFractal(b.commit()));
        }
    }

    @Test
    public void testSetExclusiveSources() {
        withSources("var a = 1;", "var a = 1;");

        withProvider(Fractal.SOURCE_LABEL);

        provider.setParameterValue(Fractal.SOURCE_LABEL, ids.get(0), "var b = 2;");

        Assert.assertEquals("var b = 2;", provider.getParameterValue(Fractal.SOURCE_LABEL, ids.get(0)));
        Assert.assertEquals("var a = 1;", provider.getParameterValue(Fractal.SOURCE_LABEL, ids.get(1)));
    }

    @Test
    public void testSetNonExclusiveSources() {
        withSources("var a = 1;", "var a = 1;");

        withProvider();

        provider.setParameterValue(Fractal.SOURCE_LABEL, ids.get(0), "var b = 2;");

        Assert.assertEquals("var b = 2;", provider.getParameterValue(Fractal.SOURCE_LABEL, ids.get(0)));
        Assert.assertEquals("var b = 2;", provider.getParameterValue(Fractal.SOURCE_LABEL, ids.get(1)));
    }

    @Test
    public void testOrderExterns() {
        withSources("extern a expr = \"0\"; extern c expr = \"0\"; var x = a + b + c",
                "extern a expr = \"0\"; extern b expr = \"0\"; extern c expr = \"0\"; extern d expr = \"0\"; var x = a + b + c + d");

        withProvider();

        String s = "";
        for(int i = 0; i < provider.parameterCount(); ++i) {
            s += provider.getParameterEntryByIndex(i).key;
        }

        Assert.assertEquals("SourceScaleabcd", s);
    }

    @Test
    public void testOrderWithKeySwitch() {
        withSources("extern a expr = \"0\"; extern b expr = \"0\"; var d = a + b",
                "extern b expr = \"0\"; extern a expr = \"0\"; var d = a + b");

        withProvider();

        provider.setKeyId(2);

        Assert.assertEquals("a", provider.getParameterEntryByIndex(3).key);
        Assert.assertEquals("b", provider.getParameterEntryByIndex(2).key);

        provider.setKeyId(1);

        Assert.assertEquals("a", provider.getParameterEntryByIndex(2).key);
        Assert.assertEquals("b", provider.getParameterEntryByIndex(3).key);
    }

    @Test
    public void testTypeFailOnImplicitExterns() {
        withSources("extern a expr = \"b\"; var d = a",
                "extern a expr = \"0\"; extern b int = 1; var d = a + b");

        withProvider();

        provider.setParameterValue("b", -1, "10");

        // owner is ignored because it is a non-shared parameter
        provider.setKeyId(ids.get(0));
        Assert.assertEquals("10", provider.getParameterValue("b", -1));

        provider.setKeyId(ids.get(1));
        Assert.assertEquals(1, provider.getParameterValue("b", -1));

        provider.setKeyId(ids.get(0));
        Assert.assertEquals("10", provider.getParameterValue("b", -1));
    }

    @Test
    public void testTypeToleranceInExprOnImplicitExterns() {
        withSources("extern a expr = \"b\"; var d = a",
                "extern a expr = \"0\"; extern b int = 1; var d = a + b");

        withProvider();

        provider.setParameterValue("b", -1, 2);

        Assert.assertEquals("0", provider.getParameterValue("b", -1));
    }

    @Test
    public void testIndividualsWithSingularExtern() {
        withSources("extern a int = 0; extern b int = 1; var d = a + b",
                "extern a int = 0; extern b int = 1; extern c int = 2; var d = a + b + c");

        withProvider("b");

        Assert.assertEquals(4 + 2, provider.parameterCount()); // + 2 is scale/source

        Assert.assertEquals("Source", provider.getParameterEntryByIndex(0).key);

        Assert.assertEquals("b", provider.getParameterEntryByIndex(3).key);
        Assert.assertNotEquals(-1, provider.getParameterEntryByIndex(3).owner);

        Assert.assertEquals("b", provider.getParameterEntryByIndex(4).key);
        Assert.assertNotEquals(-1, provider.getParameterEntryByIndex(4).owner);

        Assert.assertEquals("c", provider.getParameterEntryByIndex(5).key);

        Assert.assertEquals("a", provider.getParameterEntryByIndex(2).key);
    }

    @Test
    public void testParametersChangeOnFractalRemoval() {
        withSources("extern a int = 0; extern b int = 1; var d = a + b",
                "extern c int = 2; var d = c");

        FractalProvider p = new FractalProvider();

        int id0 = p.addFractal(builders[0].commit());

        Assert.assertEquals(4, p.parameterCount());

        int id1 = p.addFractal(builders[1].commit());

        Assert.assertEquals(5, p.parameterCount());

        p.setKeyId(id0);
        p.removeFractal(id0);

        Assert.assertEquals(1, p.fractalCount());
        Assert.assertEquals(id1, p.keyId());
        Assert.assertEquals(3, p.parameterCount());

        int newId = p.addFractal(builders[0].commit());

        p.setKeyId(newId);

        Assert.assertEquals(5, p.parameterCount());

        p.removeFractal(newId);

        Assert.assertEquals(1, p.fractalCount());
        Assert.assertEquals(id1, p.keyId());
        Assert.assertEquals(3, p.parameterCount());
    }

    @Test
    public void testParametersChangeOnFractalRemovalWithUnsharedParameters() {
        withSources("extern a int = 0; extern b int = 1; var d = a + b",
                "extern a int = 0; extern c int = 2; var d = c");

        withParameter(0, "a", 0);
        withParameter(1, "a", 0);

        FractalProvider p = new FractalProvider();
        p.addExclusiveParameter("a");
        p.addExclusiveParameter("b");
        p.addExclusiveParameter("c");

        int id0 = p.addFractal(builders[0].commit());

        Assert.assertEquals(4, p.parameterCount());

        int id1 = p.addFractal(builders[1].commit());

        Assert.assertEquals(5, p.parameterCount());

        p.setKeyId(id0);

        p.removeFractal(id0);

        Assert.assertEquals(1, p.fractalCount());
        Assert.assertEquals(id1, p.keyId());
        Assert.assertEquals(3, p.parameterCount());

        int newId = p.addFractal(builders[0].commit());

        p.setKeyId(newId);

        Assert.assertEquals(5, p.parameterCount());

        p.removeFractal(newId);

        Assert.assertEquals(1, p.fractalCount());
        Assert.assertEquals(id1, p.keyId());
        Assert.assertEquals(3, p.parameterCount());
    }

    @Test
    public void testSetSource() {
        withSources("extern a expr = \"0\"; var d = a");
        withProvider();

        provider.setParameterValue("Source", -1, "extern b expr = \"1\"; var c = b");
        Assert.assertEquals("b", provider.getParameterEntryByIndex(2).key);
    }

    @Test
    public void testExternExpr() {
        withSources("extern a expr = \"0\"; var d = a");
        withProvider();

        Assert.assertEquals("0", provider.getParameterEntryByIndex(2).parameter.value);
    }

    @Test
    public void testExternExprNonDefault() {
        withSources("extern a expr = \"0\"; var d = a");

        withProvider();

        provider.setParameterValue("a", -1, "1");

        // 0 is scale
        Assert.assertEquals("1", provider.getParameterEntryByIndex(2).parameter.value);
    }

    @Test
    public void testSetScale() {
        withSources("var a = 0");

        withProvider();

        Fractal fractal = provider.getFractal(ids.get(0));

        provider.setParameterValue("Scale", -1, Scale.createScaled(1));

        Scale sc = fractal.scale();
        Assert.assertEquals(sc.xx, 1., 1e-24);
    }

    @Test
    public void testExternExprParsingError() {
        withSources("extern a expr = \"0\"; var d = a");
        withProvider();

        try {
            provider.setParameterValue("a", -1, "+1");
            Assert.fail();
        } catch(MeelanException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testListeners() {
        // Set-up:
        withSources("extern a int = 0; extern b int = 1; var d = a + b",
                "extern a int = 0; extern b int = 1; extern c int = 2; var d = a + b + c");

        withProvider("b");

        int listenerCalled[] = new int[]{0, 0};

        Fractal.Listener listener0 = fractal -> listenerCalled[0]++;

        Fractal.Listener listener1 = fractal -> listenerCalled[1]++;

        provider.getFractal(1).addListener(listener0);
        provider.getFractal(2).addListener(listener1);

        // Act:
        provider.setParameterValue("a", -1, 5);

        // Should have been modified in both
        Assert.assertEquals(1, listenerCalled[0]);
        Assert.assertEquals(1, listenerCalled[1]);

        provider.setParameterValue("b", ids.get(0), 9);

        Assert.assertEquals(2, listenerCalled[0]); // individual in first.
        Assert.assertEquals(1, listenerCalled[1]);

        // c is only defined in fractal (1).
        provider.setParameterValue("c", ids.get(0), 13);

        // d is not defined
        provider.setParameterValue("d", ids.get(0), 404);

        Assert.assertEquals(2, listenerCalled[0]); // individual in first.
        Assert.assertEquals(2, listenerCalled[1]);

        Assert.assertEquals(9, provider.getParameterEntryByIndex(3).parameter.value); // b in [0]
        Assert.assertEquals(1, provider.getParameterEntryByIndex(4).parameter.value); // b in [1]

        Assert.assertEquals(13, provider.getParameterEntryByIndex(5).parameter.value); // c in [1]

        Assert.assertEquals(5, provider.getParameterEntryByIndex(2).parameter.value); // a in both
    }
}
