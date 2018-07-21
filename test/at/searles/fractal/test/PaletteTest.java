package at.searles.fractal.test;

import at.searles.fractal.FractalExternData;
import at.searles.fractal.FractviewInstructionSet;
import at.searles.meelan.compiler.Ast;
import at.searles.meelan.compiler.IntCode;
import at.searles.meelan.parser.MeelanEnv;
import at.searles.parsing.parser.ParserStream;
import org.junit.Assert;
import org.junit.Test;

public class PaletteTest {
    @Test
    public void paletteInlineTest() {
        String source = "extern lakepalette palette = [" +
                "[#000, #000, #000, #000]," +
                "[#f00, #ff0, #0f8, #00f]," +
                "[#f88, #ff8, #afc, #88f]];" +
                "var x = lakepalette (1:1)";

        Ast ast = Ast.parse(new MeelanEnv(), ParserStream.fromString(source));

        IntCode code = ast.compile(FractviewInstructionSet.get(), FractalExternData.empty());

        Assert.assertNotNull(code);
    }
}
