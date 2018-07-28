package at.searles.fractal.test;

import at.searles.fractal.Fractal;
import at.searles.fractal.data.Parameters;
import at.searles.meelan.MeelanException;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;

public class CompileTest {
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
    private Parameters parameters;

    private void actCompileFractal() {
        fractal = Fractal.fromData(source, parameters);
        fractal.compile();
    }

    private void withSourceFile(String filename) throws IOException {
        this.source = Utils.readResourceFile(filename);
    }

    private void withParameters() {
        parameters = new Parameters();
    }
}
