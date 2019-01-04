package at.searles.fractal.test;

import at.searles.fractal.Fractal;
import at.searles.fractal.FractalProvider;
import at.searles.fractal.data.FractalData;
import at.searles.fractal.data.ParameterType;
import at.searles.fractal.data.TypeCastException;
import at.searles.math.Scale;
import at.searles.meelan.MeelanException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class FractalProviderTest {
    @Test
    public void testIndividualParameters() {
        FractalData fd1 = new FractalData("extern a int = 0; extern b int = 1; var c = a + b", Collections.emptyMap());
        FractalData fd2 = new FractalData("extern a int = 0; extern b int = 1; var c = a + b", Collections.emptyMap());

        FractalProvider p = new FractalProvider();

        p.addExclusiveParameter("b");

        p.addFractal(fd1);
        p.addFractal(fd2);

        Assert.assertEquals(3 + 2, p.parameterCount()); // + 2 is Scale/Source

        // Individuals first

        Assert.assertEquals("b", p.getParameterEntryByIndex(3).id);
        Assert.assertEquals(0, p.getParameterEntryByIndex(3).owner);

        Assert.assertEquals("b", p.getParameterEntryByIndex(4).id);
        Assert.assertEquals(1, p.getParameterEntryByIndex(4).owner);

        Assert.assertEquals("Source", p.getParameterEntryByIndex(0).id);

        Assert.assertEquals("Scale", p.getParameterEntryByIndex(1).id);

        Assert.assertEquals("a", p.getParameterEntryByIndex(2).id);
    }

    @Test
    public void testOrder() {
        FractalData fd1 = new FractalData("extern a expr = \"0\"; extern b expr = \"d\"; extern c expr = \"0\"; var x = a + b + c", Collections.emptyMap());
        FractalData fd2 = new FractalData("extern a expr = \"0\"; extern b expr = \"d\"; extern c expr = \"0\"; var x = a + b + c",
                Collections.emptyMap());
        FractalProvider p = new FractalProvider();
        p.addFractal(fd1);
        p.addFractal(fd2);

        p.addExclusiveParameter("a");
        p.addExclusiveParameter("b");
        p.addExclusiveParameter("c");
        p.addExclusiveParameter("d");
        p.addExclusiveParameter("e");

        p.setParameterValue("a", 0, "e");
        p.setParameterValue("a", 1, "e+f");
        p.setParameterValue("b", 1, "0");
        p.setParameterValue("c", 1, "g+h");

        String s = "";
        for(int i = 0; i < p.parameterCount(); ++i) {
            s += p.getParameterEntryByIndex(i).id;
        }

        Assert.assertEquals("SourceScaleaeaefbdbccgh", s);
    }

    @Test
    public void testOrderExterns() {
        FractalData fd1 = new FractalData("extern a expr = \"0\"; extern c expr = \"0\"; var x = a + b + c", Collections.emptyMap());
        FractalData fd2 = new FractalData("extern a expr = \"0\"; extern b expr = \"0\"; extern c expr = \"0\"; extern d expr = \"0\"; var x = a + b + c + d", Collections.emptyMap());
        FractalProvider p = new FractalProvider();
        p.addFractal(fd1);
        p.addFractal(fd2);

        String s = "";
        for(int i = 0; i < p.parameterCount(); ++i) {
            s += p.getParameterEntryByIndex(i).id;
        }

        Assert.assertEquals("SourceScaleabcd", s);
    }

    @Test
    public void testOrderWithKeySwitch() {
        FractalData fd1 = new FractalData("extern a expr = \"0\"; extern b expr = \"0\"; var d = a + b", Collections.emptyMap());
        FractalData fd2 = new FractalData("extern b expr = \"0\"; extern a expr = \"0\"; var d = a + b", Collections.emptyMap());

        FractalProvider p = new FractalProvider();
        p.addFractal(fd1);
        p.addFractal(fd2);

        p.setKeyIndex(1);

        Assert.assertEquals("a", p.getParameterEntryByIndex(3).id);
        Assert.assertEquals("b", p.getParameterEntryByIndex(2).id);

        p.setKeyIndex(0);

        Assert.assertEquals("a", p.getParameterEntryByIndex(2).id);
        Assert.assertEquals("b", p.getParameterEntryByIndex(3).id);
    }

    @Test
    public void testTypeFailOnImplicitExterns() {
        FractalData fd1 = new FractalData("extern a expr = \"b\"; var d = a", Collections.emptyMap());
        FractalData fd2 = new FractalData("extern a expr = \"0\"; extern b int = 1; var d = a + b", Collections.emptyMap());
        FractalProvider p = new FractalProvider();
        p.addFractal(fd1);
        p.addFractal(fd2);

        try {
            p.setParameterValue("b", -1, "0");
            Assert.fail();
        } catch (TypeCastException ex) {}
    }

    @Test
    public void testTypeToleranceInExprOnImplicitExterns() {
        FractalData fd1 = new FractalData("extern a expr = \"b\"; var d = a", Collections.emptyMap());
        FractalData fd2 = new FractalData("extern a expr = \"0\"; extern b int = 1; var d = a + b", Collections.emptyMap());
        FractalProvider p = new FractalProvider();
        p.addFractal(fd1);
        p.addFractal(fd2);

        p.setParameterValue("b", -1, 2); // should set value in both

        Assert.assertEquals(2, p.getParameterValue("b", -1));
    }

    @Test
    public void testIndividualsWithSingularExtern() {
        FractalData fd1 = new FractalData("extern a int = 0; extern b int = 1; var d = a + b", Collections.emptyMap());
        FractalData fd2 = new FractalData("extern a int = 0; extern b int = 1; extern c int = 2; var d = a + b + c", Collections.emptyMap());

        FractalProvider p = new FractalProvider();

        p.addExclusiveParameter("b");

        p.addFractal(fd1);
        p.addFractal(fd2);

        Assert.assertEquals(4 + 2, p.parameterCount()); // + 2 is scale/source

        Assert.assertEquals("Source", p.getParameterEntryByIndex(0).id);

        Assert.assertEquals("b", p.getParameterEntryByIndex(3).id);
        Assert.assertNotEquals(-1, p.getParameterEntryByIndex(3).owner);

        Assert.assertEquals("b", p.getParameterEntryByIndex(4).id);
        Assert.assertNotEquals(-1, p.getParameterEntryByIndex(4).owner);

        Assert.assertEquals("c", p.getParameterEntryByIndex(5).id);

        Assert.assertEquals("a", p.getParameterEntryByIndex(2).id);
    }

    @Test
    public void testParametersChangeOnFractalRemoval() {
        FractalData fd1 = new FractalData("extern a int = 0; extern b int = 1; var d = a + b", Collections.emptyMap());
        FractalData fd2 = new FractalData("extern c int = 2; var d = c", Collections.emptyMap());

        FractalProvider p = new FractalProvider();

        p.addFractal(fd1);

        Assert.assertEquals(4, p.parameterCount());

        p.addFractal(fd2);

        Assert.assertEquals(5, p.parameterCount());

        p.setKeyIndex(0);

        p.removeFractal();

        Assert.assertEquals(1, p.fractalCount());
        Assert.assertEquals(0, p.keyIndex());
        Assert.assertEquals(3, p.parameterCount());

        p.addFractal(fd1);

        p.setKeyIndex(1);

        Assert.assertEquals(5, p.parameterCount());

        p.removeFractal();

        Assert.assertEquals(1, p.fractalCount());
        Assert.assertEquals(0, p.keyIndex());
        Assert.assertEquals(3, p.parameterCount());
    }

    @Test
    public void testParametersChangeOnFractalRemovalWithUnsharedParameters() {
        FractalData fd1 = new FractalData("extern a int = 0; extern b int = 1; var d = a + b", Collections.singletonMap("a", new FractalData.Parameter(ParameterType.Int, 0)));
        FractalData fd2 = new FractalData("extern a int = 0; extern c int = 2; var d = c", Collections.singletonMap("a", new FractalData.Parameter(ParameterType.Int, 0)));

        FractalProvider p = new FractalProvider();
        p.addExclusiveParameter("a");
        p.addExclusiveParameter("b");
        p.addExclusiveParameter("c");

        p.addFractal(fd1);

        Assert.assertEquals(4, p.parameterCount());

        p.addFractal(fd2);

        Assert.assertEquals(5, p.parameterCount());

        p.setKeyIndex(0);

        p.removeFractal();

        Assert.assertEquals(1, p.fractalCount());
        Assert.assertEquals(0, p.keyIndex());
        Assert.assertEquals(3, p.parameterCount());

        p.addFractal(fd1);

        p.setKeyIndex(1);

        Assert.assertEquals(5, p.parameterCount());

        p.removeFractal();

        Assert.assertEquals(1, p.fractalCount());
        Assert.assertEquals(0, p.keyIndex());
        Assert.assertEquals(3, p.parameterCount());
    }

    @Test
    public void testSetSource() {
        FractalData fd = new FractalData("extern a expr = \"0\"; var d = a", Collections.emptyMap());
        FractalProvider p = new FractalProvider();
        p.addFractal(fd);
        p.setParameterValue("Source", -1, "extern b expr = \"1\"; var c = b");
        Assert.assertEquals("b", p.getParameterEntryByIndex(2).id);
    }

    @Test
    public void testExternExpr() {
        FractalData fd1 = new FractalData("extern a expr = \"0\"; var d = a", Collections.emptyMap());

        FractalProvider p = new FractalProvider();

        p.addFractal(fd1);

        Assert.assertEquals("0", p.getParameterEntryByIndex(2).parameter.value);
    }

    @Test
    public void testExternExprNonDefault() {
        FractalData fd1 = new FractalData("extern a expr = \"0\"; var d = a", Collections.emptyMap());

        FractalProvider p = new FractalProvider();

        p.addFractal(fd1);

        p.setParameterValue("a", -1, "1");

        // 0 is scale
        Assert.assertEquals("1", p.getParameterEntryByIndex(2).parameter.value);
    }

    @Test
    public void testSetScale() {
        FractalData fd1 = new FractalData("var a = 0", Collections.emptyMap());

        FractalProvider p = new FractalProvider();

        p.addFractal(fd1);

        Fractal fractal = p.getFractal(0);

        p.setParameterValue("Scale", -1, Scale.createScaled(2));
    }

    @Test
    public void testExternExprParsingError() {
        FractalData fd1 = new FractalData("extern a expr = \"0\"; var d = a", Collections.emptyMap());

        FractalProvider p = new FractalProvider();

        p.addFractal(fd1);

        try {
            p.setParameterValue("a", -1, "+1");
            Assert.fail();
        } catch(MeelanException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testListeners() {
        // Set-up:
        FractalData fd1 = new FractalData("extern a int = 0; extern b int = 1; var d = a + b", Collections.emptyMap());
        FractalData fd2 = new FractalData("extern a int = 0; extern b int = 1; extern c int = 2; var d = a + b + c", Collections.emptyMap());

        FractalProvider p = new FractalProvider();

        p.addExclusiveParameter("b");

        p.addFractal(fd1);
        p.addFractal(fd2);

        int listenerCalled[] = new int[]{0, 0};

        Fractal.Listener listener0 = fractal -> listenerCalled[0]++;

        Fractal.Listener listener1 = fractal -> listenerCalled[1]++;

        p.getFractal(0).addListener(listener0);
        p.getFractal(1).addListener(listener1);

        // Act:
        p.setParameterValue("a", -1, 5);

        // Should have been modified in both
        Assert.assertEquals(1, listenerCalled[0]);
        Assert.assertEquals(1, listenerCalled[1]);

        p.setParameterValue("b", 0, 9);

        Assert.assertEquals(2, listenerCalled[0]); // individual in first.
        Assert.assertEquals(1, listenerCalled[1]);

        // c is only defined in fractal (1).
        p.setParameterValue("c", 0, 13);

        // d is not defined
        p.setParameterValue("d", 0, 404);

        Assert.assertEquals(2, listenerCalled[0]); // individual in first.
        Assert.assertEquals(2, listenerCalled[1]);

        Assert.assertEquals(9, p.getParameterEntryByIndex(3).parameter.value); // b in [0]
        Assert.assertEquals(1, p.getParameterEntryByIndex(4).parameter.value); // b in [1]

        Assert.assertEquals(13, p.getParameterEntryByIndex(5).parameter.value); // c in [1]

        Assert.assertEquals(5, p.getParameterEntryByIndex(2).parameter.value); // a in both
    }
}
