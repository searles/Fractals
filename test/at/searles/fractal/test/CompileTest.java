package at.searles.fractal.test;

import at.searles.fractal.Fractal;
import at.searles.fractal.data.FractalData;
import at.searles.meelan.MeelanException;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class CompileTest {
    @Test
    public void testAdditionOfThree() throws IOException {
        withSourceFile("assets/sources/v3/ThreeBug.fv");
        withParameters();

        actCompileFractal();

        Assert.assertNotNull(fractal.code());
    }

    @Test
    public void testSimpleBug() throws IOException {
        withSourceFile("assets/sources/v3/Simple.fv");
        withParameters();

        actCompileFractal();

        Assert.assertNotNull(fractal.code());
    }

    @Test
    public void testCompileDefault() throws IOException {
        withSourceFile("assets/sources/v3/Default.fv");
        withParameters();

        actCompileFractal();

        Assert.assertNotNull(fractal.code());
    }

    @Test
    public void testV3() throws IOException {
        withParameters();

        File dir = new File("test/resources/assets/sources/v3");

        File[] files = dir.listFiles();

        for(File file : files) {
            this.source = Utils.readFile(file);

            try {
                actCompileFractal();
            } catch(MeelanException e) {
                e.printStackTrace();
                Assert.fail(file.toString());
            }
        }
    }

    private Fractal fractal;
    private String source;
    private Map<String, FractalData.Parameter> parameters;

    private void actCompileFractal() {
        fractal = Fractal.fromData(new FractalData(source, parameters));
    }

    private void withSourceFile(String filename) throws IOException {
        this.source = Utils.readResourceFile(filename);
    }

    private void withParameters() {
        parameters = new HashMap<>();
    }
}
