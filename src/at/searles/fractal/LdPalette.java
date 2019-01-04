package at.searles.fractal;

import at.searles.meelan.ops.SystemInstruction;
import at.searles.meelan.ops.SystemType;
import at.searles.meelan.optree.Tree;
import at.searles.meelan.types.BaseType;
import at.searles.meelan.types.FunctionType;
import at.searles.meelan.values.Const;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LdPalette extends SystemInstruction {

    private static LdPalette singleton;

    public static LdPalette get() {
        if(singleton == null) {
            singleton = new LdPalette();
        }

        return singleton;
    }

    private LdPalette() {
        super(
                new FunctionType[]{
                        new FunctionType(Arrays.asList(BaseType.integer, BaseType.cplx), BaseType.quat),
                        new FunctionType(Arrays.asList(BaseType.integer, BaseType.cplx), BaseType.integer)
                },
                SystemType.signatures(
                        SystemType.signature(SystemType.integer, SystemType.cplx, SystemType.quatReg),
                        SystemType.signature(SystemType.integer, SystemType.cplxReg, SystemType.quatReg),
                        SystemType.signature(SystemType.integer, SystemType.cplx, SystemType.integerReg),
                        SystemType.signature(SystemType.integer, SystemType.cplxReg, SystemType.integerReg)
                ),
                Kind.Expr
        );
    }

    @Override
    protected Const evaluate(FunctionType functionType, List<Tree> list) {
        return null;
    }

    @Override
    protected String getFunctionName(ArrayList<SystemType> signature) {
        if(SystemType.integerReg.equals(signature.get(signature.size() - 1))) {
            return "palette_int";
        }

        return "palette_lab";
    }
}
