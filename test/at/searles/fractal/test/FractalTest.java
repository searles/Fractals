package at.searles.fractal.test;

import at.searles.fractal.Fractal;
import at.searles.fractal.data.FractalData;
import at.searles.fractal.data.ParameterKey;
import at.searles.fractal.data.ParameterType;
import at.searles.fractal.data.Parameters;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the central fractal class
 */
public class FractalTest {
    @Test
    public void testFractalDefaultParameter() {
        Fractal fractal = Fractal.fromData("extern a int = 1; var x = a;", new Parameters());
        fractal.compile();
        Assert.assertTrue(fractal.data().isDefaultValue("a"));
    }

    @Test
    public void testFractalNonDefaultParameter() {
        Fractal fractal = Fractal.fromData("extern a int = 1; var x = a;", new Parameters().add(new ParameterKey("a", ParameterType.Int), 2));
        fractal.compile();
        Assert.assertFalse(fractal.data().isDefaultValue("a"));
    }

    @Test
    public void testFractalIntCodeWithExtern() {
        Parameters parameters = new Parameters().add(new ParameterKey("a", ParameterType.Int), 2);
        Fractal fractal = Fractal.fromData("extern a int = 1; var x = a;", parameters);
        fractal.compile();
        Assert.assertEquals(2, fractal.code()[1]);
    }

    @Test
    public void testResetParameter() {
        Parameters parameters = new Parameters().add(new ParameterKey("a", ParameterType.Int), 2);
        Fractal fractal = Fractal.fromData("extern a int = 1; var x = a;", parameters);
        fractal.compile();

        fractal.data().setValue("a", null);
        fractal.compile();

        Assert.assertEquals(1, fractal.code()[1]);
    }

    @Test
    public void testAddExternParameter() {
        Fractal fractal = Fractal.fromData("extern a expr = \"1\"; var x = a;", new Parameters());
        fractal.compile();

        fractal.data().setValue("a", "b"); // b is now a new extern parameter.
        fractal.compile();

        Assert.assertNotNull(fractal.data().entry("b"));
    }

    @Test
    public void testKeepExternParameter() {
        Fractal fractal = Fractal.fromData("extern a expr = \"1\"; var x = a;", new Parameters());
        fractal.compile();

        fractal.data().setValue("a", "b"); // b is now a new extern parameter.
        fractal.compile();

        fractal.data().setValue("b", "13");
        fractal.compile();

        fractal.data().setValue("a", "0");
        fractal.compile();

        Assert.assertNull(fractal.data().entry("b"));

        fractal.data().setValue("a", "b");
        fractal.compile();

        Assert.assertNotNull(fractal.data().entry("b"));
        Assert.assertEquals("13", fractal.data().value("b"));
    }
}
