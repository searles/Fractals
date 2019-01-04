package at.searles.fractal.test;

import at.searles.fractal.Fractal;
import at.searles.fractal.data.FractalData;
import at.searles.fractal.data.ParameterType;
import at.searles.math.Scale;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

/**
 * Tests for the central fractal class
 */
public class FractalTest {
    static Fractal fromSource(String source) {
        return Fractal.fromData(new FractalData(source, Collections.emptyMap()));
    }

    static Fractal fromSource(String source, String id, ParameterType type, Object value) {
        return Fractal.fromData(new FractalData(source, Collections.singletonMap(id, new FractalData.Parameter(type, value))));
    }

    @Test
    public void testFractalDefaultParameter() {
        Fractal fractal = fromSource("extern a int = 1; var x = a;");
        Assert.assertTrue(fractal.getParameter("a").isDefault);
    }

    @Test
    public void testFractalNonDefaultParameter() {
        Fractal fractal = fromSource("extern a int = 1; var x = a;", "a", ParameterType.Int, 2);
        Assert.assertFalse(fractal.getParameter("a").isDefault);
    }

    @Test
    public void testFractalIntCodeWithExtern() {
        Fractal fractal = fromSource("extern a int = 1; var x = a;", "a", ParameterType.Int, 2);
        Assert.assertEquals(2, fractal.code()[1]);
    }

    @Test
    public void testResetParameter() {
        Fractal fractal = fromSource("extern a int = 1; var x = a;", "a", ParameterType.Int, 2);

        fractal.setValue("a", null);

        Assert.assertEquals(1, fractal.code()[1]);
    }

    @Test
    public void testAddExternParameter() {
        Fractal fractal = fromSource("extern a expr = \"1\"; var x = a;");

        fractal.setValue("a", "b"); // b is now a new extern parameter.

        Assert.assertNotNull(fractal.getParameter("b"));
    }

    @Test
    public void testKeepExternParameter() {
        Fractal fractal = fromSource("extern a expr = \"1\"; var x = a;");

        fractal.setValue("a", "b"); // b is now a new extern parameter.

        fractal.setValue("b", "13");

        fractal.setValue("a", "0");

        Assert.assertNull(fractal.getParameter("b"));

        fractal.setValue("a", "b");

        Assert.assertNotNull(fractal.getParameter("b"));
        Assert.assertEquals("13", fractal.getParameter("b").value);
    }

    @Test
    public void testFractalHasScale() {
        Fractal fractal = fromSource("var x = 0;");

        Assert.assertNotNull(fractal.getParameter(Fractal.SCALE_LABEL));
    }

    @Test
    public void testOverrideDefaultScale() {
        Fractal fractal = fromSource("extern Scale scale = [5, 0, 0, 5, 0, 0]; var x = 0;");

        Scale scale = fractal.scale();

        Assert.assertTrue(5. == scale.xx);
    }
}
