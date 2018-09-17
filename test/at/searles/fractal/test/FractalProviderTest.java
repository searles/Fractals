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

        FractalProvider p = FractalProvider.dualFractal(fd1, fd2, "b");

        Assert.assertEquals(3 + 1, p.parameterCount()); // + 1 is Scale

        // Individuals first

        Assert.assertEquals("b", p.getParameter(0).key.id);
        Assert.assertNotEquals(-1, p.getParameter(0).owner);

        Assert.assertEquals("b", p.getParameter(1).key.id);
        Assert.assertNotEquals(-1, p.getParameter(1).owner);

        Assert.assertEquals("Scale", p.getParameter(2).key.id);
        Assert.assertEquals(-1, p.getParameter(2).owner);

        Assert.assertEquals("a", p.getParameter(3).key.id);
        Assert.assertEquals(-1, p.getParameter(3).owner);
    }

    @Test
    public void testIndividualsWithSingularExtern() {
        FractalData fd1 = new FractalData("extern a int = 0; extern b int = 1; var d = a + b", new Parameters());
        FractalData fd2 = new FractalData("extern a int = 0; extern b int = 1; extern c int = 2; var d = a + b + c", new Parameters());

        FractalProvider p = FractalProvider.dualFractal(fd1, fd2, "b");

        Assert.assertEquals(4 + 1, p.parameterCount()); // + 1 is scale

        // Individuals first

        Assert.assertEquals("b", p.getParameter(0).key.id);
        Assert.assertNotEquals(-1, p.getParameter(0).owner);

        Assert.assertEquals("b", p.getParameter(1).key.id);
        Assert.assertNotEquals(-1, p.getParameter(1).owner);

        Assert.assertEquals("Scale", p.getParameter(2).key.id);
        Assert.assertEquals(-1, p.getParameter(2).owner);

        Assert.assertEquals("a", p.getParameter(3).key.id);
        Assert.assertEquals(-1, p.getParameter(3).owner);

        Assert.assertEquals("c", p.getParameter(4).key.id);
        Assert.assertEquals(-1, p.getParameter(4).owner);
    }

    @Test
    public void testExternExpr() {
        FractalData fd1 = new FractalData("extern a expr = \"0\"; var d = a", new Parameters());

        FractalProvider p = FractalProvider.singleFractal(fd1);

        Assert.assertEquals("0", p.getParameter(1).value);
    }

    @Test
    public void testExternExprNonDefault() {
        FractalData fd1 = new FractalData("extern a expr = \"0\"; var d = a", new Parameters());

        FractalProvider p = FractalProvider.singleFractal(fd1);

        p.set(new ParameterKey("a", ParameterType.Expr), "1");

        // 0 is scale
        Assert.assertEquals("1", p.getParameter(1).value);
    }

    @Test
    public void testSetScale() {
        FractalData fd1 = new FractalData("var a = 0", new Parameters());

        FractalProvider p = FractalProvider.singleFractal(fd1);
        Fractal fractal = p.get(0);

        p.set(new ParameterKey("Scale", ParameterType.Scale), Scale.createScaled(2));
        fractal.compile();
    }

    @Test
    public void testExternExprParsingError() {
        FractalData fd1 = new FractalData("extern a expr = \"0\"; var d = a", new Parameters());

        FractalProvider p = FractalProvider.singleFractal(fd1);

        try {
            p.set(new ParameterKey("a", ParameterType.Expr), "+1");
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

        FractalProvider p = FractalProvider.dualFractal(fd1, fd2, "b");

        int listenerCalled[] = new int[]{0, 0};

        FractalProvider.Listener listener0 = fractal -> listenerCalled[0]++;

        FractalProvider.Listener listener1 = fractal -> listenerCalled[1]++;

        p.addListener(0, listener0);
        p.addListener(1, listener1);

        // Act:
        p.set(new ParameterKey("a", ParameterType.Int), 5);

        // Should have been modified in both
        Assert.assertEquals(1, listenerCalled[0]);
        Assert.assertEquals(1, listenerCalled[1]);

        p.set(new ParameterKey("b", ParameterType.Int), 0, 9);

        Assert.assertEquals(2, listenerCalled[0]); // individual in first.
        Assert.assertEquals(1, listenerCalled[1]);

        // c is only defined in fractal (1).
        p.set(new ParameterKey("c", ParameterType.Int), 0, 13);

        // d is not defined
        try {
            p.set(new ParameterKey("d", ParameterType.Int), 0, 404);
            Assert.fail();
        } catch (IllegalArgumentException ignore) {
        }

        // a is not defined with the specified type
        try {
            p.set(new ParameterKey("a", ParameterType.Palette), 0, 404);
            Assert.fail();
        } catch (IllegalArgumentException ignore) {
        }

        Assert.assertEquals(2, listenerCalled[0]); // individual in first.
        Assert.assertEquals(2, listenerCalled[1]);

        Assert.assertEquals(9, p.getParameter(0).value); // b in [0]
        Assert.assertEquals(1, p.getParameter(1).value); // b in [1]
        // 2 is scale
        Assert.assertEquals(5, p.getParameter(3).value); // a in both
        Assert.assertEquals(13, p.getParameter(4).value); // c in [1]
    }
}
