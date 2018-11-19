package at.searles.fractal.test;

import at.searles.fractal.Fractal;
import at.searles.fractal.data.FractalData;
import at.searles.fractal.data.ParameterType;
import at.searles.math.Scale;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

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
        fractal.compile();
        Assert.assertTrue(fractal.getParameter("a").isDefault);
    }

    @Test
    public void testFractalNonDefaultParameter() {
        Fractal fractal = fromSource("extern a int = 1; var x = a;", "a", ParameterType.Int, 2);
        fractal.compile();
        Assert.assertFalse(fractal.getParameter("a").isDefault);
    }

    @Test
    public void testFractalIntCodeWithExtern() {
        Fractal fractal = fromSource("extern a int = 1; var x = a;", "a", ParameterType.Int, 2);
        fractal.compile();
        Assert.assertEquals(2, fractal.code()[1]);
    }

    @Test
    public void testResetParameter() {
        Fractal fractal = fromSource("extern a int = 1; var x = a;", "a", ParameterType.Int, 2);
        fractal.compile();

        fractal.setValue("a", null);
        fractal.compile();

        Assert.assertEquals(1, fractal.code()[1]);
    }

    @Test
    public void testAddExternParameter() {
        Fractal fractal = fromSource("extern a expr = \"1\"; var x = a;");
        fractal.compile();

        fractal.setValue("a", "b"); // b is now a new extern parameter.
        fractal.compile();

        Assert.assertNotNull(fractal.getParameter("b"));
    }

    @Test
    public void testKeepExternParameter() {
        Fractal fractal = fromSource("extern a expr = \"1\"; var x = a;");
        fractal.compile();

        fractal.setValue("a", "b"); // b is now a new extern parameter.
        fractal.compile();

        fractal.setValue("b", "13");
        fractal.compile();

        fractal.setValue("a", "0");
        fractal.compile();

        Assert.assertNull(fractal.getParameter("b"));

        fractal.setValue("a", "b");
        fractal.compile();

        Assert.assertNotNull(fractal.getParameter("b"));
        Assert.assertEquals("13", fractal.value("b"));
    }

    @Test
    public void testFractalHasScale() {
        Fractal fractal = fromSource("var x = 0;");
        fractal.compile();

        Assert.assertNotNull(fractal.getParameter(Fractal.SCALE_LABEL));
    }

    @Test
    public void testOverrideDefaultScale() {
        Fractal fractal = fromSource("extern Scale scale = [5, 0, 0, 5, 0, 0]; var x = 0;");
        fractal.compile();

        Scale scale = fractal.scale();

        Assert.assertTrue(5. == scale.xx);
    }
}
