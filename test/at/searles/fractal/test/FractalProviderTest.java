package at.searles.fractal.test;

import at.searles.fractal.Fractal;
import at.searles.fractal.FractalProvider;
import at.searles.fractal.data.FractalData;
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

        p.addExclusiveParameterId("b");

        p.addFractal(fd1);
        p.addFractal(fd2);

        Assert.assertEquals(3 + 2, p.parameterCount()); // + 2 is Scale/Source

        // Individuals first

        Assert.assertEquals("b", p.getParameterEntryByIndex(3).key);
        Assert.assertEquals(0, p.getParameterEntryByIndex(3).owner);

        Assert.assertEquals("b", p.getParameterEntryByIndex(4).key);
        Assert.assertEquals(1, p.getParameterEntryByIndex(4).owner);

        Assert.assertEquals("Source", p.getParameterEntryByIndex(0).key);
        Assert.assertEquals(-1, p.getParameterEntryByIndex(0).owner);

        Assert.assertEquals("Scale", p.getParameterEntryByIndex(1).key);
        Assert.assertEquals(-1, p.getParameterEntryByIndex(1).owner);

        Assert.assertEquals("a", p.getParameterEntryByIndex(2).key);
        Assert.assertEquals(-1, p.getParameterEntryByIndex(2).owner);
    }

    @Test
    public void testIndividualsWithSingularExtern() {
        FractalData fd1 = new FractalData("extern a int = 0; extern b int = 1; var d = a + b", Collections.emptyMap());
        FractalData fd2 = new FractalData("extern a int = 0; extern b int = 1; extern c int = 2; var d = a + b + c", Collections.emptyMap());

        FractalProvider p = new FractalProvider();

        p.addExclusiveParameterId("b");

        p.addFractal(fd1);
        p.addFractal(fd2);

        Assert.assertEquals(4 + 2, p.parameterCount()); // + 2 is scale/source

        Assert.assertEquals("Source", p.getParameterEntryByIndex(0).key);
        Assert.assertEquals(-1, p.getParameterEntryByIndex(0).owner);

        Assert.assertEquals("b", p.getParameterEntryByIndex(3).key);
        Assert.assertNotEquals(-1, p.getParameterEntryByIndex(3).owner);

        Assert.assertEquals("b", p.getParameterEntryByIndex(4).key);
        Assert.assertNotEquals(-1, p.getParameterEntryByIndex(4).owner);

        Assert.assertEquals("c", p.getParameterEntryByIndex(5).key);
        Assert.assertEquals(-1, p.getParameterEntryByIndex(5).owner);

        Assert.assertEquals("a", p.getParameterEntryByIndex(2).key);
        Assert.assertEquals(-1, p.getParameterEntryByIndex(2).owner);
    }

    @Test
    public void testSetSource() {
        FractalData fd = new FractalData("extern a expr = \"0\"; var d = a", Collections.emptyMap());
        FractalProvider p = new FractalProvider();
        p.addFractal(fd);
        p.setParameterValue("Source", -1, "extern b expr = \"1\"; var c = b");
        Assert.assertEquals("b", p.getParameterEntryByIndex(2).key);
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
        fractal.compile();
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

        p.addExclusiveParameterId("b");

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
