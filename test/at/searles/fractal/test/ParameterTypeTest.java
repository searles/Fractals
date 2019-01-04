package at.searles.fractal.test;

import at.searles.fractal.data.ParameterType;
import at.searles.math.color.Palette;
import at.searles.meelan.optree.Tree;
import at.searles.meelan.optree.Vec;
import at.searles.meelan.values.Int;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class ParameterTypeTest {
    @Test
    public void testSingleColorPalette() {
        // pal = [[0xffff0000]]
        Tree tree = new Vec(Collections.singletonList(new Vec(Collections.singletonList(new Int(0xffff0000)))));
        Object value = ParameterType.Palette.toValue(tree);

        Assert.assertTrue(value instanceof Palette);

        Palette palette = (Palette) value;

        Assert.assertEquals(1, palette.width());
        Assert.assertEquals(1, palette.height());
        Assert.assertEquals(0xffff0000, palette.argb(0, 0));
    }

    @Test
    public void testSingleColorPalette1Dim() {
        // pal = [0xffff0000]
        Tree tree = new Vec(Collections.singletonList(new Int(0xffff0000)));
        Object value = ParameterType.Palette.toValue(tree);

        Assert.assertTrue(value instanceof Palette);

        Palette palette = (Palette) value;

        Assert.assertEquals(1, palette.width());
        Assert.assertEquals(1, palette.height());
        Assert.assertEquals(0xffff0000, palette.argb(0, 0));
    }

    @Test
    public void testSingleColorPaletteSingleColor() {
        // pal = 0xffff0000
        Tree tree = new Int(0xffff0000);
        Object value = ParameterType.Palette.toValue(tree);

        Assert.assertTrue(value instanceof Palette);

        Palette palette = (Palette) value;

        Assert.assertEquals(1, palette.width());
        Assert.assertEquals(1, palette.height());
        Assert.assertEquals(0xffff0000, palette.argb(0, 0));
    }
}
