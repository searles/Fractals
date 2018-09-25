package at.searles.fractal.test;

import at.searles.fractal.Fractal;
import at.searles.fractal.FractalProvider;
import at.searles.fractal.data.FractalData;
import at.searles.fractal.data.ParameterKey;
import at.searles.fractal.data.ParameterType;
import at.searles.fractal.data.Parameters;
import at.searles.math.Scale;
import at.searles.meelan.MeelanException;
import org.junit.Assert;
import org.junit.Test;

public class FractalProviderTest {
    @Test
    public void testIndividualParameters() {
        FractalData fd1 = new FractalData("extern a int = 0; extern b int = 1; var c = a + b", new Parameters());
        FractalData fd2 = new FractalData("extern a int = 0; extern b int = 1; var c = a + b", new Parameters());

        FractalProvider p = new FractalProvider();

        p.addFractal(fd1, "b");
        p.addFractal(fd2, "b");

        Assert.assertEquals(3 + 1, p.parameterCount()); // + 1 is Scale

        // Individuals first

        Assert.assertEquals("b", p.getParameterByIndex(0).key);
        Assert.assertNotEquals(-1, p.getParameterByIndex(0).owner);

        Assert.assertEquals("b", p.getParameterByIndex(1).key);
        Assert.assertNotEquals(-1, p.getParameterByIndex(1).owner);

        Assert.assertEquals("Scale", p.getParameterByIndex(2).key);
        Assert.assertEquals(-1, p.getParameterByIndex(2).owner);

        Assert.assertEquals("a", p.getParameterByIndex(3).key);
        Assert.assertEquals(-1, p.getParameterByIndex(3).owner);
    }

    @Test
    public void testIndividualsWithSingularExtern() {
        FractalData fd1 = new FractalData("extern a int = 0; extern b int = 1; var d = a + b", new Parameters());
        FractalData fd2 = new FractalData("extern a int = 0; extern b int = 1; extern c int = 2; var d = a + b + c", new Parameters());

        FractalProvider p = new FractalProvider();

        p.addFractal(fd1, "b");
        p.addFractal(fd2, "b");

        Assert.assertEquals(4 + 1, p.parameterCount()); // + 1 is scale

        // Individuals first

        Assert.assertEquals("b", p.getParameterByIndex(0).key);
        Assert.assertNotEquals(-1, p.getParameterByIndex(0).owner);

        Assert.assertEquals("b", p.getParameterByIndex(1).key);
        Assert.assertNotEquals(-1, p.getParameterByIndex(1).owner);

        Assert.assertEquals("c", p.getParameterByIndex(2).key);
        Assert.assertEquals(-1, p.getParameterByIndex(2).owner);

        Assert.assertEquals("Scale", p.getParameterByIndex(3).key);
        Assert.assertEquals(-1, p.getParameterByIndex(3).owner);

        Assert.assertEquals("a", p.getParameterByIndex(4).key);
        Assert.assertEquals(-1, p.getParameterByIndex(4).owner);
    }

    @Test
    public void testExternExpr() {
        FractalData fd1 = new FractalData("extern a expr = \"0\"; var d = a", new Parameters());

        FractalProvider p = new FractalProvider();

        p.addFractal(fd1);

        Assert.assertEquals("0", p.getParameterByIndex(1).value);
    }

    @Test
    public void testExternExprNonDefault() {
        FractalData fd1 = new FractalData("extern a expr = \"0\"; var d = a", new Parameters());

        FractalProvider p = new FractalProvider();

        p.addFractal(fd1);

        p.setParameter("a", -1, "1");

        // 0 is scale
        Assert.assertEquals("1", p.getParameterByIndex(1).value);
    }

    @Test
    public void testSetScale() {
        FractalData fd1 = new FractalData("var a = 0", new Parameters());

        FractalProvider p = new FractalProvider();

        p.addFractal(fd1);

        Fractal fractal = p.getFractal(0);

        p.setParameter("Scale", -1, Scale.createScaled(2));
        fractal.compile();
    }

    @Test
    public void testExternExprParsingError() {
        FractalData fd1 = new FractalData("extern a expr = \"0\"; var d = a", new Parameters());

        FractalProvider p = new FractalProvider();

        p.addFractal(fd1);

        try {
            p.setParameter("a", -1, "+1");
            Assert.fail();
        } catch(MeelanException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testListeners() {
        // Set-up:
        FractalData fd1 = new FractalData("extern a int = 0; extern b int = 1; var d = a + b", new Parameters());
        FractalData fd2 = new FractalData("extern a int = 0; extern b int = 1; extern c int = 2; var d = a + b + c", new Parameters());

        FractalProvider p = new FractalProvider();

        p.addFractal(fd1, "b");
        p.addFractal(fd2, "b");

        int listenerCalled[] = new int[]{0, 0};

        FractalProvider.Listener listener0 = fractal -> listenerCalled[0]++;

        FractalProvider.Listener listener1 = fractal -> listenerCalled[1]++;

        p.addListener(0, listener0);
        p.addListener(1, listener1);

        // Act:
        p.setParameter("a", -1, 5);

        // Should have been modified in both
        Assert.assertEquals(1, listenerCalled[0]);
        Assert.assertEquals(1, listenerCalled[1]);

        p.setParameter("b", 0, 9);

        Assert.assertEquals(2, listenerCalled[0]); // individual in first.
        Assert.assertEquals(1, listenerCalled[1]);

        // c is only defined in fractal (1).
        p.setParameter("c", 0, 13);

        // d is not defined
        p.setParameter("d", 0, 404);

        Assert.assertEquals(2, listenerCalled[0]); // individual in first.
        Assert.assertEquals(2, listenerCalled[1]);

        Assert.assertEquals(9, p.getParameterByIndex(0).value); // b in [0]
        Assert.assertEquals(1, p.getParameterByIndex(1).value); // b in [1]

        Assert.assertEquals(13, p.getParameterByIndex(2).value); // c in [1]
        // 2 is scale
        Assert.assertEquals(5, p.getParameterByIndex(4).value); // a in both
    }
}
