package at.searles.fractal.test;

import at.searles.fractal.FractalProvider;
import at.searles.fractal.data.FractalData;
import at.searles.fractal.entries.FavoriteEntry;
import at.searles.fractal.gson.Serializers;
import org.junit.Assert;
import org.junit.Test;

public class SerializerTest {

    @Test
    public void serializeProviderTest() {
        FractalProvider provider = new FractalProvider();

        FractalData.Builder builder1 = new FractalData.Builder().setSource("var x = 1;");
        builder1.addParameter("x", 2);

        FractalData.Builder builder2 = new FractalData.Builder().setSource("extern A int = 2; var x = A;");
        builder2.addParameter("A", 3);

        provider.addFractal(builder1.commit());
        provider.addFractal(builder2.commit());

        provider.addExclusiveParameter("A");

        String json = Serializers.serializer().toJson(provider);

        FractalProvider provider1 = Serializers.serializer().fromJson(json, FractalProvider.class);

        Assert.assertFalse(provider1.isSharedParameter("A"));
    }

    @Test
    public void deserializeV4Test() {
        Serializers.serializer().fromJson(DATA_V4, FavoriteEntry.class);
    }

    @Test
    public void deserializeV3Test() {
        Serializers.serializer().fromJson(DATA_V3, FavoriteEntry.class);
    }

    private static String DATA_V4 = "{\"icon\":\"ignored==\"," +
            "\"fractal\":{" +
            "\"code\":\"// Default Preset\\n" +
            "// This is a good start for all kinds of fractals\\n" +
            "// including newton sets, nova fractals and others.\\n" +
            "var x int, y int, color int;\\n\\n\\nextern maxdepth int = 120;" +
            "\\n\\n\\n// c: coordinates, breakcondition: a function whether we " +
            "should stop, \\n// value: a real variable to return some kind of " +
            "value\\n//        used in 3d-types for the height.\\n// returns a " +
            "quat representing the color\\nfunc escapetime(c, breakcondition) " +
            "{\\n    // some further arguments\\n    extern juliaset bool = false;" +
            "\\n\\n    var i int = 0,\\n        zlast cplx = 0,\\n        z cplx," +
            "\\n        znext cplx = 0,\\n        p cplx;\\n\\n    if juliaset then " +
            "{\\n        z = c;\\n        extern juliapoint cplx = -0.8:0.16;\\n  " +
            "      p = juliapoint;\\n    } else {\\n        extern mandelinit expr " +
            "= \\\"0\\\";\\n        z = mandelinit;\\n        p = c\\n    }\\n\\n    " +
            "extern function expr = \\\"mandelbrot(z, p)\\\";\\n\\n    var color " +
            "quat;\\n\\n    while {\\n        znext = function;\\n        not " +
            "breakcondition(i, znext, z, zlast, c, p, color)\\n    } do {\\n        " +
            "// advance to next values\\n        zlast = z;\\n        z = znext;\\n    }\\n\\n    // return color\\n    color\\n}\\n\\n// everything that is drawn must have a get_color-function.\\n\\n// " +
            "c = coordinates (scaled)\\n// value is a real variable for " +
            "z-information in 3D\\n// but also otherwise convenient to separate drawing\\n// algorithm from transfer" +
            "\\n// returns color.\\nfunc get_color(c, value) " +
            "{\\n\\n    // if the fractal accumulates some values\\n    // like in traps or addends, here is a got place to do it.\\n" +
            "\\n    func breakcondition(i, znext, z, zlast, c, p, color) {\\n        func bailoutcolor() {\\n            extern bailout real = 128;\\n            extern max_power real = 2;\\n            var smooth_i = smoothen(znext, bailout, max_power) ;\\n\\n            // the next ones are only used in 3d-fractals\\n            extern bailoutvalue expr = \\\"log(20 + i + smooth_i)\\\";\\n            value = bailoutvalue ;\\n        \\n            extern bailouttransfer expr = \\\"value\\\";\\n\\n            extern bailoutpalette palette = [\\n                    [#006, #26c, #fff, #fa0, #303]];\\n    \\n            color = bailoutpalette bailouttransfer\\n        }\\n\\n        func lakecolor() {\\n            " +
            "extern epsilon real = 1e-9;\\n        \\n            // the next ones are only used in 3d-fractals" +
            "\\n            extern lakevalue expr = \\\"log(1 + rad znext)\\\";\\n            value = lakevalue;\\n" +
            "\\n            extern laketransfer expr =\\n                \\\"arcnorm znext : value\\\";\\n\\n            extern lakepalette palette = [\\n                [#000, #000, #000, #000]," +
            "\\n                [#f00, #ff0, #0f8, #00f],\\n                [#f88, #ff8, #afc, #88f]];\\n\\n            color = lakepalette laketransfer\\n        }\\n\\n        { lakecolor() ; true } if not next(i, maxdepth) else\\n        radrange(znext, z, bailout, epsilon, bailoutcolor(), lakecolor())\\n    }\\n    \\n    escapetime(c, breakcondition)\\n}\\n\\nfunc get_color_test(c, value) {\\n    // this one is just here for testing light effects\\n    // circle + donut + green bg\\n    var rc = rad c;\\n    \\n    { value = (circlefn rc + 5); int2lab #0000ff} if rc < 1 else\\n    " +
            "{ value = circlefn abs (rc - 3); int2lab #ff0000 } if rc =< 4 and rc >= 2 else\\n    { value = -10; int2lab #00ff00 }\\n}\\n\\n// ******************************************\\n// * Next are just drawing procedures. They *\\n// * should be the same for all drawings.   *                 \\n// ******************************************\\n\\nextern supersampling bool = false;\\nextern light bool = false;\\n\\n// drawpixel for 2D\\nfunc drawpixel_2d(x, y) { \\n    var c cplx = map(x, y);\\n    var value real;\\n    var color = get_color(c, value); // value is not used\\n    0.5 (color + color)\\n}\\n\\n// drawpixel for 3D\\nfunc drawpixel_3d(x, y) {\\n    var c00 cplx = map(x, y),\\n        c10 cplx = map(x + 1, y + 0.5),\\n        c01 cplx = map(x + 0.5, y + 1);\\n    \\n    var h00 real, h10 real, h01 real; // heights\\n    \\n    // color is already kinda super-sampled\\n    var color = (get_color(c00, h00) + get_color(c10, h10) + get_color(c01, h01)) / 3;\\n" +
            "\\n    // get height out of value\\n    func height(value) {\\n        extern valuetransfer expr = \\\"value\\\";\\n        valuetransfer\\n    }\\n    \\n    h00 = height h00; h01 = height h01; h10 = height h10;\\n\\n    // get the normal vector (cross product)\\n    var xp = c10 - c00, xz = h10 - h00;\\n    var yp = c01 - c00, yz = h01 - h00;\\n    \\n    var np cplx = (xp.y yz - xz yp.y) : (xz yp.x - xp.x yz);\\n    var nz real = xp.x yp.y - xp.y yp.x;\\n        \\n    // normalize np and nz\\n    var nlen = sqrt(rad2 np + sqr nz);\\n    np = np / nlen; nz = nz / nlen;\\n        \\n    // get light direction\\n    extern lightvector cplx = -0.667 : -0.667; // direction from which the light is coming\\n    def lz = sqrt(1 - sqr re lightvector - sqr im lightvector); // this is inlined\\n\\n    // Lambert's law.\\n    var cos_a real = dot(lightvector, np) + lz nz;" +
            "\\n\\n    // diffuse reflexion with ambient factor\\n    extern lightintensity real = 1;\\n    extern ambientlight real = 0.5;\\n\\n    // if lumen is negative it is behind, \\n    // but I tweak it a bit for the sake of the looks:\\n    // cos_a = -1 (which is super-behind) ==> 0\\n    // cos_a = 0 ==> ambientlight\\n    // cos_a = 1 ==> lightintensity\\n\\n    // for a mathematically correct look use the following:\\n    // if cos_a < 0 then cos_a = 0;\\n    // color.a = color.a * (ambientlight + lightintensity lumen);\\n    " +
            "\\n    def d = lightintensity / 2; // will be inlined later\\n\\n    // Change L in Lab-Color\\n    color.a = color.a (((d - ambientlight) cos_a + d) cos_a + ambientlight);\\n\\n    // Next, specular reflection. Viewer is always assumed to be in direction (0,0,1)\\n    extern specularintensity real = 1;\\n\\n    extern shininess real = 8;\\n\\n    // r = 2 n l - l; v = 0:0:1\\n    " +
            "var spec_refl = 2 cos_a nz - lz;\\n    \\n    // 100 because L in the Lab-Model is between 0 and 100\\n    if spec_refl > 0 then\\n        color.a = color.a + 100 * specularintensity * spec_refl ^ shininess;\\n\\n    color\\n}\\n\\nfunc do_pixel(x, y) {\\n    // two or three dimensions?\\n    def drawpixel = drawpixel_3d if light else drawpixel_2d;\\n    \\n    func drawaapixel(x, y) {\\n        0.25 (\\n            drawpixel(x - 0.375, y - 0.125) + \\n            drawpixel(x + 0.125, y - 0.375) + \\n            drawpixel(x + 0.375, y + 0.125) +\\n            drawpixel(x - 0.125, y + 0.375)         \\n        );\\n    }\\n\\n    // which function to apply?\\n    def fn = drawpixel if not supersampling else drawaapixel;\\n\\n    color = lab2int fn(x, y)\\n}\\n\\n// and finally call the draing procedure\\ndo_pixel(x, y)\"," +
            "\"data\":{" +
            "\"Scale\":[0.925727128982544,0.5388092994689941,-0.5388092994689941,0.925727128982544,-0.3866233825683594,-0.04304816573858261]," +
            "\"function\":\"mandelbrot(rabs z, p)\"}}," +
            "\"description\":\"2019-01-13, 09:20\"}";

    private static String DATA_V3 = "" +
            "  {\n" +
            "    \"icon\": \"iC\\n\",\n" +
            "    \"fractal\": {\n" +
            "      \"source\": [\n" +
            "        \"// Slight variation of Default, simply added branch_avg in break condition.\",\n" +
            "        \"var x int, y int, color int;\",\n" +
            "        \"\",\n" +
            "        \"extern maxdepth int \\u003d 120;\",\n" +
            "        \"\",\n" +
            "        \"// some further arguments\",\n" +
            "        \"extern juliaset bool \\u003d false;\",\n" +
            "        \"extern juliapoint cplx \\u003d -0.8:0.16;\",\n" +
            "        \"\",\n" +
            "        \"func escapetime(c, breakcondition) {\",\n" +
            "        \"\\tvar i int \\u003d 0,\",\n" +
            "        \"\\t\\tp cplx \\u003d juliapoint if juliaset else c,\",\n" +
            "        \"\\t\\tzlast cplx \\u003d 0,\",\n" +
            "        \"\\t\\tz cplx,\",\n" +
            "        \"\\t\\tznext cplx \\u003d 0;\",\n" +
            "        \"\",\n" +
            "        \"\\textern mandelinit expr \\u003d \\\"0\\\";\",\n" +
            "        \"\",\n" +
            "        \"    z \\u003d c if juliaset else mandelinit;\",\n" +
            "        \"\",\n" +
            "        \"\\textern function expr \\u003d \\\"mandelbrot(z, p)\\\";\",\n" +
            "        \"\",\n" +
            "        \"\\tvar color quat;\",\n" +
            "        \"\",\n" +
            "        \"\\twhile {\",\n" +
            "        \"\\t\\tznext \\u003d function;\",\n" +
            "        \"\\t\\tnot breakcondition(i, znext, z, zlast, c, p, color)\",\n" +
            "        \"\\t} do {\",\n" +
            "        \"\\t\\t// advance to next values\",\n" +
            "        \"\\t\\tzlast \\u003d z;\",\n" +
            "        \"\\t\\tz \\u003d znext;\",\n" +
            "        \"\\t}\",\n" +
            "        \"\",\n" +
            "        \"\\t// return color\",\n" +
            "        \"\\tcolor\",\n" +
            "        \"}\",\n" +
            "        \"\",\n" +
            "        \"func get_color(c, value) { \",\n" +
            "        \"\\tvar branch_avg real \\u003d 0; // sum of addends\",\n" +
            "        \"\",\n" +
            "        \"\\tfunc breakcondition(i, znext, z, zlast, c, p, color) {\",\n" +
            "        \"\\t\\textern addend_start int \\u003d 2;\",\n" +
            "        \"\\t\\textern addend expr \\u003d \\\"0.5 + 0.5 sin(6 arc znext)\\\";\",\n" +
            "        \"\",\n" +
            "        \"\\t\\tfunc bailoutcolor() {\",\n" +
            "        \"\\t\\t\\textern bailout real \\u003d 512;\",\n" +
            "        \"\\t\\t\\textern max_power real \\u003d 2;\",\n" +
            "        \"\\t\\t\\tvar smooth_i \\u003d smoothen(znext, bailout, max_power) ;\",\n" +
            "        \"\\t\\t\\t\",\n" +
            "        \"\\t\\t\\t// smooth_i is interpolated here\",\n" +
            "        \"\\t\\t\\textern interpolate_smooth_i bool \\u003d false;\",\n" +
            "        \"\\t\\t\\tif interpolate_smooth_i then smooth_i \\u003d 0.5 - 0.5 cos PI smooth_i;\",\n" +
            "        \"\",\n" +
            "        \"\\t\\t\\t// and also add addend according to smooth_i value\",\n" +
            "        \"\\t\\t\\tbranch_avg \\u003d (branch_avg + smooth_i addend) / \",\n" +
            "        \"\\t\\t\\t\\t(i + smooth_i - addend_start);\",\n" +
            "        \"\\t\\t\\t\\t\",\n" +
            "        \"            // the next ones are only used in 3d-fractals\",\n" +
            "        \"\\t\\t\\textern bailoutvalue expr \\u003d \\\"branch_avg\\\";\",\n" +
            "        \"\\t\\t\\tvalue \\u003d bailoutvalue ;\",\n" +
            "        \"\",\n" +
            "        \"\\t\\t\\textern bailouttransfer expr \\u003d\",\n" +
            "        \"\\t\\t\\t\\t\\\"branch_avg : log(i + smooth_i)\\\";\",\n" +
            "        \"\\t\\t\\textern bailoutpalette palette \\u003d [\",\n" +
            "        \"\\t\\t\\t\\t[#f80, #f22, #40f, #008, #080, #ff8],\",\n" +
            "        \"\\t\\t\\t\\t[#fff, #000, #fff, #000, #fff, #000]];\",\n" +
            "        \"\",\n" +
            "        \"\",\n" +
            "        \"\\t\\t\\tcolor \\u003d bailoutpalette bailouttransfer\",\n" +
            "        \"\\t\\t}\",\n" +
            "        \"\\t\",\n" +
            "        \"\\t\\tfunc lakecolor() {\",\n" +
            "        \"\\t\\t\\textern epsilon real \\u003d 1e-9;\",\n" +
            "        \"\\t\\t\\t\",\n" +
            "        \"\\t\\t\\textern lakevalue expr \\u003d \\\"0\\\";\",\n" +
            "        \"\\t\\t\\tvalue \\u003d lakevalue;\",\n" +
            "        \"\\t\\t\\t\",\n" +
            "        \"\\t\\t\\textern laketransfer expr \\u003d \\\"value\\\";\",\n" +
            "        \"\\t\\t\\textern lakepalette palette \\u003d [[#000]];\",\n" +
            "        \"\\t\\t\\tcolor \\u003d lakepalette laketransfer\",\n" +
            "        \"\\t\\t}\",\n" +
            "        \"\",\n" +
            "        \"\\t\\t{ lakecolor() ; true } if not next(i, maxdepth) else // i \\u003c maxdepth? then lake.\",\n" +
            "        \"\\t\\ttrue if radrange(znext, z, bailout, epsilon, bailoutcolor(), lakecolor()) else // bailout or epsilon? \",\n" +
            "        \"\\t\\t{ branch_avg \\u003d branch_avg + addend ; false } if i \\u003e\\u003d addend_start else // do some work here\",\n" +
            "        \"\\t\\tfalse\",\n" +
            "        \"\\t}\",\n" +
            "        \"\",\n" +
            "        \"\\t// draw single pixel\",\n" +
            "        \"\\tescapetime(c, breakcondition)\",\n" +
            "        \"}\",\n" +
            "        \"\",\n" +
            "        \"\",\n" +
            "        \"// ******************************************\",\n" +
            "        \"// * Next are just drawing procedures. They *\",\n" +
            "        \"// * should be the same for all drawings.   *                 \",\n" +
            "        \"// ******************************************\",\n" +
            "        \"\",\n" +
            "        \"extern supersampling bool \\u003d false;\",\n" +
            "        \"extern light bool \\u003d false;\",\n" +
            "        \"\",\n" +
            "        \"// drawpixel for 2D\",\n" +
            "        \"func drawpixel_2d(x, y) { \",\n" +
            "        \"\\tvar c cplx \\u003d map(x, y);\",\n" +
            "        \"\\tvar value real;\",\n" +
            "        \"\\tget_color(c, value) // value is not used\",\n" +
            "        \"}\",\n" +
            "        \"\",\n" +
            "        \"// drawpixel for 3D\",\n" +
            "        \"func drawpixel_3d(x, y) {\",\n" +
            "        \"\\tvar c00 cplx \\u003d map(x, y),\",\n" +
            "        \"\\t\\tc10 cplx \\u003d map(x + 1, y + 0.5),\",\n" +
            "        \"\\t\\tc01 cplx \\u003d map(x + 0.5, y + 1);\",\n" +
            "        \"\\t\",\n" +
            "        \"\\tvar h00 real, h10 real, h01 real; // heights\",\n" +
            "        \"\\t\",\n" +
            "        \"\\t// color is already kinda super-sampled\",\n" +
            "        \"\\tvar color \\u003d (get_color(c00, h00) + get_color(c10, h10) + get_color(c01, h01)) / 3;\",\n" +
            "        \"\",\n" +
            "        \"\\t// get height out of value\",\n" +
            "        \"\\tfunc height(value) {\",\n" +
            "        \"\\t\\textern valuetransfer expr \\u003d \\\"value\\\";\",\n" +
            "        \"\\t\\tvaluetransfer\",\n" +
            "        \"\\t}\",\n" +
            "        \"\\t\",\n" +
            "        \"\\th00 \\u003d height h00; h01 \\u003d height h01; h10 \\u003d height h10;\",\n" +
            "        \"\",\n" +
            "        \"\\t// get the normal vector (cross product)\",\n" +
            "        \"\\tvar xp \\u003d c10 - c00, xz \\u003d h10 - h00;\",\n" +
            "        \"\\tvar yp \\u003d c01 - c00, yz \\u003d h01 - h00;\",\n" +
            "        \"\\t\",\n" +
            "        \"\\tvar np cplx \\u003d (xp.y yz - xz yp.y) : (xz yp.x - xp.x yz);\",\n" +
            "        \"\\tvar nz real \\u003d xp.x yp.y - xp.y yp.x;\",\n" +
            "        \"\\t\\t\",\n" +
            "        \"\\t// normalize np and nz\",\n" +
            "        \"\\tvar nlen \\u003d sqrt(rad2 np + sqr nz);\",\n" +
            "        \"\\tnp \\u003d np / nlen; nz \\u003d nz / nlen;\",\n" +
            "        \"\\t\\t\",\n" +
            "        \"\\t// get light direction\",\n" +
            "        \"\\textern lightvector cplx \\u003d -0.667 : -0.667; // direction from which the light is coming\",\n" +
            "        \"\\tdef lz \\u003d sqrt(1 - sqr re lightvector - sqr im lightvector); // this is inlined\",\n" +
            "        \"\",\n" +
            "        \"\\t// Lambert\\u0027s law.\",\n" +
            "        \"\\tvar cos_a real \\u003d dot(lightvector, np) + lz nz;\",\n" +
            "        \"\",\n" +
            "        \"\\t// diffuse reflexion with ambient factor\",\n" +
            "        \"\\textern lightintensity real \\u003d 1;\",\n" +
            "        \"\\textern ambientlight real \\u003d 0.5;\",\n" +
            "        \"\",\n" +
            "        \"\\t// if lumen is negative it is behind, \",\n" +
            "        \"\\t// but I tweak it a bit for the sake of the looks:\",\n" +
            "        \"\\t// cos_a \\u003d -1 (which is super-behind) \\u003d\\u003d\\u003e 0\",\n" +
            "        \"\\t// cos_a \\u003d 0 \\u003d\\u003d\\u003e ambientlight\",\n" +
            "        \"\\t// cos_a \\u003d 1 \\u003d\\u003d\\u003e lightintensity\",\n" +
            "        \"\",\n" +
            "        \"\\t// for a mathematically correct look use the following:\",\n" +
            "        \"\\t// if cos_a \\u003c 0 then cos_a \\u003d 0;\",\n" +
            "        \"\\t// color.a \\u003d color.a * (ambientlight + lightintensity lumen);\",\n" +
            "        \"\\t\",\n" +
            "        \"\\tdef d \\u003d lightintensity / 2; // will be inlined later\",\n" +
            "        \"\",\n" +
            "        \"\\t// Change L in Lab-Color\",\n" +
            "        \"\\tcolor.a \\u003d color.a (((d - ambientlight) cos_a + d) cos_a + ambientlight);\",\n" +
            "        \"\",\n" +
            "        \"\\t// Next, specular reflection. Viewer is always assumed to be in direction (0,0,1)\",\n" +
            "        \"\\textern specularintensity real \\u003d 1;\",\n" +
            "        \"\",\n" +
            "        \"\\textern shininess real \\u003d 8;\",\n" +
            "        \"\",\n" +
            "        \"\\t// r \\u003d 2 n l - l; v \\u003d 0:0:1\",\n" +
            "        \"\\tvar spec_refl \\u003d 2 cos_a nz - lz;\",\n" +
            "        \"\\t\",\n" +
            "        \"\\t// 100 because L in the Lab-Model is between 0 and 100\",\n" +
            "        \"\\tif spec_refl \\u003e 0 then\",\n" +
            "        \"\\t\\tcolor.a \\u003d color.a + 100 * specularintensity * spec_refl ^ shininess;\",\n" +
            "        \"\",\n" +
            "        \"\\tcolor\",\n" +
            "        \"}\",\n" +
            "        \"\",\n" +
            "        \"func do_pixel(x, y) {\",\n" +
            "        \"\\t// two or three dimensions?\",\n" +
            "        \"\\tdef drawpixel \\u003d drawpixel_3d if light else drawpixel_2d;\",\n" +
            "        \"\\t\",\n" +
            "        \"\\tfunc drawaapixel(x, y) {\",\n" +
            "        \"\\t\\t0.25 (\",\n" +
            "        \"\\t\\t\\tdrawpixel(x - 0.375, y - 0.125) + \",\n" +
            "        \"\\t\\t\\tdrawpixel(x + 0.125, y - 0.375) + \",\n" +
            "        \"\\t\\t\\tdrawpixel(x + 0.375, y + 0.125) +\",\n" +
            "        \"\\t\\t\\tdrawpixel(x - 0.125, y + 0.375)\\t\\t\\t\",\n" +
            "        \"\\t\\t);\",\n" +
            "        \"\\t}\",\n" +
            "        \"\",\n" +
            "        \"\\t// which function to apply?\",\n" +
            "        \"\\tdef fn \\u003d drawpixel if not supersampling else drawaapixel;\",\n" +
            "        \"\",\n" +
            "        \"\\tcolor \\u003d lab2int fn(x, y)\",\n" +
            "        \"}\",\n" +
            "        \"\",\n" +
            "        \"// and finally call the drawing procedure\",\n" +
            "        \"do_pixel(x, y)\"\n" +
            "      ],\n" +
            "      \"arguments\": {\n" +
            "        \"ints\": {\n" +
            "          \"maxdepth\": 1200\n" +
            "        },\n" +
            "        \"reals\": {\n" +
            "          \"bailout\": 51200.0\n" +
            "        },\n" +
            "        \"bools\": {\n" +
            "          \"light\": true,\n" +
            "          \"interpolate_smooth_i\": false\n" +
            "        },\n" +
            "        \"exprs\": {\n" +
            "          \"valuetransfer\": \"value/19999999\",\n" +
            "          \"bailoutvalue\": \"i+smooth_i\",\n" +
            "          \"addend\": \"0.5 + 0.5 sin(2 arc znext)\",\n" +
            "          \"bailouttransfer\": \"branch_avg*6\"\n" +
            "        },\n" +
            "        \"palettes\": {\n" +
            "          \"lakepalette\": {\n" +
            "            \"width\": 3,\n" +
            "            \"height\": 5,\n" +
            "            \"colors\": [\n" +
            "              -16777216,\n" +
            "              -16775353,\n" +
            "              -4203956,\n" +
            "              -4203956,\n" +
            "              -52179,\n" +
            "              -9085757,\n" +
            "              -6161034,\n" +
            "              -4014018,\n" +
            "              -16777216,\n" +
            "              -1120529,\n" +
            "              -16777216,\n" +
            "              -65730,\n" +
            "              -2621480,\n" +
            "              -1120529,\n" +
            "              -35944\n" +
            "            ]\n" +
            "          },\n" +
            "          \"bailoutpalette\": {\n" +
            "            \"width\": 8,\n" +
            "            \"height\": 1,\n" +
            "            \"colors\": [\n" +
            "              -16711621,\n" +
            "              -15,\n" +
            "              -1,\n" +
            "              -7783841,\n" +
            "              -4426,\n" +
            "              -12713896,\n" +
            "              -7143390,\n" +
            "              -1074\n" +
            "            ]\n" +
            "          }\n" +
            "        },\n" +
            "        \"scales\": {\n" +
            "          \"Scale\": [\n" +
            "            -1.275140806317907E-7,\n" +
            "            -3.084483757456835E-7,\n" +
            "            3.084483757456835E-7,\n" +
            "            -1.275140806317907E-7,\n" +
            "            -1.7465254311194451,\n" +
            "            3.5628061356917815E-6\n" +
            "          ]\n" +
            "        }\n" +
            "      }\n" +
            "    },\n" +
            "    \"description\": \"2017-08-13, 10:07\"\n" +
            "  }\n";
}
