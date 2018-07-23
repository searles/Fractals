package at.searles.fractal;

import at.searles.meelan.ops.InstructionSet;
import at.searles.meelan.ops.analysis.*;
import at.searles.meelan.ops.arithmetics.*;
import at.searles.meelan.ops.bool.And;
import at.searles.meelan.ops.bool.Not;
import at.searles.meelan.ops.bool.Or;
import at.searles.meelan.ops.color.*;
import at.searles.meelan.ops.comparison.*;
import at.searles.meelan.ops.complex.*;
import at.searles.meelan.ops.cons.Cons;
import at.searles.meelan.ops.cons.IntToReal;
import at.searles.meelan.ops.cons.RealToInt;
import at.searles.meelan.ops.graphics.Box;
import at.searles.meelan.ops.graphics.Circle;
import at.searles.meelan.ops.graphics.Line;
import at.searles.meelan.ops.graphics.Segment;
import at.searles.meelan.ops.numeric.*;
import at.searles.meelan.ops.rewriting.Derive;
import at.searles.meelan.ops.rewriting.Horner;
import at.searles.meelan.ops.rewriting.Newton;
import at.searles.meelan.ops.rewriting.Solve;
import at.searles.meelan.ops.special.*;
import at.searles.meelan.ops.sys.*;

public class FractviewInstructionSet extends InstructionSet {

    private static FractviewInstructionSet singleton;

    public static FractviewInstructionSet get() {
        if(singleton == null) {
            singleton = new FractviewInstructionSet();
        }

        return singleton;
    }

    private FractviewInstructionSet() {
        init();
    }

    private void init() {
        // Constructors
        this.addSystemInstruction("cons", Cons.get());
        this.addSystemInstruction("real", RealToInt.get());
        this.addSystemInstruction(IntToReal.get());

        // Arithmetics
        this.addSystemInstruction("add", Add.get());
        this.addSystemInstruction("sub", Sub.get());
        this.addSystemInstruction("mul", Mul.get());
        this.addSystemInstruction("div", Div.get());
        this.addSystemInstruction("mod", Mod.get());
        this.addSystemInstruction("pow", Pow.get());

        this.addSystemInstruction("recip", Recip.get());
        this.addSystemInstruction("neg", Neg.get());


        // analysis
        this.addSystemInstruction("atan", Atan.get());
        this.addSystemInstruction("atanh", Atanh.get());
        this.addSystemInstruction("cos", Cos.get());
        this.addSystemInstruction("cosh", Cosh.get());
        this.addSystemInstruction("exp", Exp.get());
        this.addSystemInstruction("log", Log.get());
        this.addSystemInstruction("sin", Sin.get());
        this.addSystemInstruction("sinh", Sinh.get());
        this.addSystemInstruction("sqr", Sqr.get());
        this.addSystemInstruction("sqrt", Sqrt.get());
        this.addSystemInstruction("tan", Tan.get());
        this.addSystemInstruction("tanh", Tanh.get());

        // numeric
        this.addSystemInstruction("abs", Abs.get());
        this.addSystemInstruction("floor", Floor.get());
        this.addSystemInstruction("ceil", Ceil.get());
        this.addSystemInstruction("fract", Fract.get());
        this.addSystemInstruction("dot", Dot.get());
        this.addSystemInstruction("circlefn", CircleFn.get());
        this.addSystemInstruction("scalarmul", ScalarMul.get());
        this.addSystemInstruction("max", Max.get());
        this.addSystemInstruction("min", Min.get());

        // complex
        this.addSystemInstruction("conj", Conj.get());
        this.addSystemInstruction("flip", Flip.get());
        this.addSystemInstruction("rabs", RAbs.get());
        this.addSystemInstruction("iabs", IAbs.get());
        this.addSystemInstruction("norm", Norm.get());
        this.addSystemInstruction("polar", Polar.get());
        this.addSystemInstruction("rect", Rect.get());
        this.addSystemInstruction("arc", Arc.get());
        this.addSystemInstruction("arcnorm", Arcnorm.get());
        this.addSystemInstruction("rad", Rad.get());
        this.addSystemInstruction("rad2", Rad2.get());
        this.addSystemInstruction("dist", Dist.get());
        this.addSystemInstruction("dist2", Dist2.get());
        this.addSystemInstruction("re", Re.get());
        this.addSystemInstruction("im", Im.get());

        // bools
        this.addInstruction("and", And.get());
        this.addInstruction("or", Or.get());
        this.addInstruction("not", Not.get());

        // comparisons
        this.addSystemInstruction("eq", Equal.get());
        this.addSystemInstruction("less", Less.get());
        this.addInstruction("neq", NonEqual.get());
        this.addInstruction("geq", GreaterEqual.get());
        this.addInstruction("leq", LessEq.get());
        this.addInstruction("greater", Greater.get());

        // graphics
        this.addSystemInstruction("box", Box.get());
        this.addSystemInstruction("circle", Circle.get());
        this.addSystemInstruction("line", Line.get());
        this.addSystemInstruction("segment", Segment.get());

        // colors
        this.addSystemInstruction("int2lab", Int2Lab.get());
        this.addSystemInstruction("int2rgb", Int2Rgb.get());
        this.addSystemInstruction("lab2int", Lab2Int.get());
        this.addSystemInstruction("lab2rgb", Lab2Rgb.get());
        this.addSystemInstruction("rgb2int", Rgb2Int.get());
        this.addSystemInstruction("rgb2lab", Rgb2Lab.get());

        this.addSystemInstruction(FromPalette.get());
        this.addSystemInstruction("over", Over.get());

        // special
        this.addSystemInstruction("distless", DistLess.get());
        this.addSystemInstruction("radless", RadLess.get());
        this.addSystemInstruction("radrange", RadRange.get());
        this.addSystemInstruction("smooth", Smooth.get());
        this.addSystemInstruction("smoothen", Smooth.get());
        this.addSystemInstruction("mandelbrot", Mandelbrot.get());

        // sys
        this.addSystemInstruction("next", Next.get());
        this.addSystemInstruction("map", MapCoordinates.get());
        this.addSystemInstruction(Mov.get());
        this.addSystemInstruction(Jump.get());
        this.addSystemInstruction(JumpRel.get());

        this.addInstruction("length", Length.get());

        this.addInstruction("error", RaiseError.get());
        this.addInstruction("derive", Derive.get());
        this.addInstruction("newton", Newton.get());
        this.addInstruction("horner", Horner.get());
        this.addInstruction("solve", Solve.get());

        // specials for fractview
        this.addSystemInstruction(LdPalette.get());
    }
}
