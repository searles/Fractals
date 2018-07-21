package at.searles.fractal.test;

import at.searles.fractal.Fractal;
import at.searles.fractal.data.Parameters;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;

/**
 * Created by searles on 20.07.18.
 */
public class CompileTest {
    @Test
    public void testCompileDefault() throws IOException {
        withSourceFile("Default.fv");
        withParameters();

        actCompileFractal();

        Assert.assertNotNull(fractal.code());
    }

    private Fractal fractal;
    private String source;
    private Parameters parameters;

    private void actCompileFractal() {
        fractal = Fractal.fromData(source, parameters);
        fractal.compile();
    }

    private void withSourceFile(String filename) throws IOException {
        File file = new File("test/resources/Default.fv");
        try(BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            source = sb.toString();
        }
    }

    private void withParameters() {
        parameters = new Parameters();
    }
}
