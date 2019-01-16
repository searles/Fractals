package at.searles.fractal.test;

import at.searles.fractal.data.FractalData;
import at.searles.fractal.entries.FavoriteEntry;
import at.searles.fractal.gson.Serializers;
import at.searles.math.color.Palette;
import at.searles.meelan.ops.numeric.Fract;
import org.junit.Assert;
import org.junit.Test;
import sun.awt.AWTAccessor;

import java.io.IOException;

public class JsonTest {

    private String json;
    private FavoriteEntry.Collection collection;

    @Test
    public void testV3() throws IOException {
        // This is a collection of version 3 of fractview.
        withJsonFile("collection_2017_08.txt");

        parseCollection();

        assertCount(131);
        assertIconsNotNull();
    }

    @Test
    public void testV3_2() throws IOException {
        // This is a collection of version 3 of fractview.
        withJsonFile("backup-2017-08-25.txt");

        parseCollection();

        assertCount(160);
    }


    @Test
    public void testV3_2_check() throws IOException {
        // This is a collection of version 3 of fractview.
        withJsonFile("backup-2017-08-25.txt");

        parseCollection();

        this.collection.forEach((id, entry) -> {
            String json = Serializers.serializer().toJson(entry.fractal, FractalData.class);
            FractalData data = Serializers.serializer().fromJson(json, FractalData.class);
            String json2 = Serializers.serializer().toJson(data, FractalData.class);

            Assert.assertEquals(json, json2);
        });
    }

    private void assertCount(int count) {
        Assert.assertEquals(count, collection.size());
    }

    private void assertIconsNotNull() {
        for(FavoriteEntry entry : collection.values()) {
            Assert.assertNotNull(entry.icon);
        }
    }

    @Test
    public void testAddBadArguments() {
        String source = "// Lyapunov\n" +
                "var x int, y int, color int;\n" +
                "\n" +
                "func get_color(c, value) {\n" +
                "    extern Break_Bound real = 1e9;\n" +
                "    extern Maximum_Depth int = 250;\n" +
                "\n" +
                "    def a = c.x;\n" +
                "    def b = c.y;\n" +
                "\n" +
                "    extern Lyapunov_Sequence expr = \"[a,a,a,a,b,b,b,b]\";\n" +
                "\n" +
                "    extern Init expr = \"0.5\";\n" +
                "\n" +
                "    var z0 real = Init;\n" +
                "    \n" +
                "    var z real = z0;\n" +
                "    \n" +
                "    var r real;\n" +
                "    \n" +
                "    extern Function expr = \"r * z * (1 - z)\";\n" +
                "\n" +
                "    extern Warm_Up int = 81;\n" +
                "    \n" +
                "    // TODO use for loop\n" +
                "    var i int = 0;\n" +
                "    \n" +
                "    while next(i, Warm_Up) do {\n" +
                "        r = select(i, Lyapunov_Sequence);\n" +
                "        z = Function;\n" +
                "    };\n" +
                "    \n" +
                "    i = 0;\n" +
                "    var lyaexp real = 0;\n" +
                "    \n" +
                "    while {\n" +
                "        r = select(i, Lyapunov_Sequence);\n" +
                "        z = Function;\n" +
                "       lyaexp = lyaexp + log abs derive(Function, z);\n" +
                "       abs lyaexp < Break_Bound and next(i, Maximum_Depth)\n" +
                "    };\n" +
                "    \n" +
                "    lyaexp = lyaexp / i;\n" +
                "    \n" +
                "    extern Positive_Palette palette = [[#000, #800, #fa0, #fd4, #ff8, #fff]];\n" +
                "    extern Negative_Palette palette = [[#000, #8af, #acf, #28f]];\n" +
                "\n" +
                "    // and get values\n" +
                "    extern Positive_Value expr = \"sqrt(atan(-lyaexp) (2 / PI))\";\n" +
                "    extern Positive_Transfer expr = \"value\";\n" +
                "\n" +
                "    extern Negative_Value expr = \"0\";\n" +
                "    extern Negative_Transfer expr = \"lyaexp\";\n" +
                "\n" +
                "    { value = Positive_Value; Positive_Palette Positive_Transfer } if lyaexp < 0 else\n" +
                "    { value = Negative_Value; Negative_Palette Negative_Transfer }\n" +
                "}\n" +
                "\n" +
                "// ******************************************\n" +
                "// * Next are just drawing procedures. They *\n" +
                "// * should be the same for all drawings.   *                 \n" +
                "// ******************************************\n" +
                "\n" +
                "extern supersampling bool = false;\n" +
                "extern light bool = false;\n" +
                "\n" +
                "// drawpixel for 2D\n" +
                "func drawpixel_2d(x, y) { \n" +
                "    var c cplx = map(x, y);\n" +
                "    var value real;\n" +
                "    get_color(c, value) // value is not used\n" +
                "}\n" +
                "\n" +
                "// drawpixel for 3D\n" +
                "func drawpixel_3d(x, y) {\n" +
                "    var c00 cplx = map(x, y),\n" +
                "        c10 cplx = map(x + 1, y + 0.5),\n" +
                "        c01 cplx = map(x + 0.5, y + 1);\n" +
                "    \n" +
                "    var h00 real, h10 real, h01 real; // heights\n" +
                "    \n" +
                "    // color is already kinda super-sampled\n" +
                "    var color = (get_color(c00, h00) + get_color(c10, h10) + get_color(c01, h01)) / 3;\n" +
                "\n" +
                "    // get height out of value\n" +
                "    func height(value) {\n" +
                "        extern valuetransfer expr = \"value\";\n" +
                "        valuetransfer\n" +
                "    }\n" +
                "    \n" +
                "    h00 = height h00; h01 = height h01; h10 = height h10;\n" +
                "\n" +
                "    // get the normal vector (cross product)\n" +
                "    var xp = c10 - c00, xz = h10 - h00;\n" +
                "    var yp = c01 - c00, yz = h01 - h00;\n" +
                "    \n" +
                "    var np cplx = (xp.y yz - xz yp.y) : (xz yp.x - xp.x yz);\n" +
                "    var nz real = xp.x yp.y - xp.y yp.x;\n" +
                "        \n" +
                "    // normalize np and nz\n" +
                "    var nlen = sqrt(rad2 np + sqr nz);\n" +
                "    np = np / nlen; nz = nz / nlen;\n" +
                "        \n" +
                "    // get light direction\n" +
                "    extern lightvector cplx = -0.667 : -0.667; // direction from which the light is coming\n" +
                "    def lz = sqrt(1 - sqr re lightvector - sqr im lightvector); // this is inlined\n" +
                "\n" +
                "    // Lambert's law.\n" +
                "    var cos_a real = dot(lightvector, np) + lz nz;\n" +
                "\n" +
                "    // diffuse reflexion with ambient factor\n" +
                "    extern lightintensity real = 1;\n" +
                "    extern ambientlight real = 0.5;\n" +
                "\n" +
                "    // if lumen is negative it is behind, \n" +
                "    // but I tweak it a bit for the sake of the looks:\n" +
                "    // cos_a = -1 (which is super-behind) ==> 0\n" +
                "    // cos_a = 0 ==> ambientlight\n" +
                "    // cos_a = 1 ==> lightintensity\n" +
                "\n" +
                "    // for a mathematically correct look use the following:\n" +
                "    // if cos_a < 0 then cos_a = 0;\n" +
                "    // color.a = color.a * (ambientlight + lightintensity lumen);\n" +
                "    \n" +
                "    def d = lightintensity / 2; // will be inlined later\n" +
                "\n" +
                "    // Change L in Lab-Color\n" +
                "    color.a = color.a (((d - ambientlight) cos_a + d) cos_a + ambientlight);\n" +
                "\n" +
                "    // Next, specular reflection. Viewer is always assumed to be in direction (0,0,1)\n" +
                "    extern specularintensity real = 1;\n" +
                "\n" +
                "    extern shininess real = 8;\n" +
                "\n" +
                "    // r = 2 n l - l; v = 0:0:1\n" +
                "    var spec_refl = 2 cos_a nz - lz;\n" +
                "    \n" +
                "    // 100 because L in the Lab-Model is between 0 and 100\n" +
                "    if spec_refl > 0 then\n" +
                "        color.a = color.a + 100 * specularintensity * spec_refl ^ shininess;\n" +
                "\n" +
                "    color\n" +
                "}\n" +
                "\n" +
                "func do_pixel(x, y) {\n" +
                "    // two or three dimensions?\n" +
                "    def drawpixel = drawpixel_3d if light else drawpixel_2d;\n" +
                "    \n" +
                "    func drawaapixel(x, y) {\n" +
                "        0.25 (\n" +
                "            drawpixel(x - 0.375, y - 0.125) + \n" +
                "            drawpixel(x + 0.125, y - 0.375) + \n" +
                "            drawpixel(x + 0.375, y + 0.125) +\n" +
                "            drawpixel(x - 0.125, y + 0.375)         \n" +
                "        );\n" +
                "    }\n" +
                "\n" +
                "    // which function to apply?\n" +
                "    def fn = drawpixel if not supersampling else drawaapixel;\n" +
                "\n" +
                "    color = lab2int fn(x, y)\n" +
                "}\n" +
                "\n" +
                "// and finally call the draing procedure\n" +
                "do_pixel(x, y)\n";

        FractalData.Builder builder = new FractalData.Builder();
        builder.setSource(source);
        Assert.assertEquals(true, builder.addParameter("warmup", "1"));
        Assert.assertEquals(false, builder.addParameter("warmup", 1));
        Assert.assertEquals(true, builder.addParameter("Positive_Palette", new Palette(1, 1, new int[]{0})));

        // true because it is an expr.
    }

    private void parseCollection() {
        this.collection = Utils.parse(json, FavoriteEntry.Collection.class);
    }

    private void withJsonFile(String filename) throws IOException {
        this.json = Utils.readResourceFile(filename);
    }
}
