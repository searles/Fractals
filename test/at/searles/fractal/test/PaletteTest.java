package at.searles.fractal.test;

import at.searles.fractal.Fractal;
import at.searles.fractal.FractviewInstructionSet;
import at.searles.fractal.data.FractalData;
import at.searles.fractal.entries.FavoriteEntry;
import at.searles.meelan.compiler.Ast;
import at.searles.meelan.compiler.IntCode;
import at.searles.meelan.parser.MeelanEnv;
import at.searles.parsing.parser.ParserStream;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class PaletteTest {
    @Test
    public void paletteInlineTest() {
        String source = "extern lakepalette palette = [" +
                "[#000, #000, #000, #000]," +
                "[#f00, #ff0, #0f8, #00f]," +
                "[#f88, #ff8, #afc, #88f]];" +
                "var x = lakepalette (1:1)";

        Fractal f = Fractal.fromData(new FractalData(source, Collections.emptyMap()));

        int[] code = f.code();

        Assert.assertNotNull(code);
    }
}
