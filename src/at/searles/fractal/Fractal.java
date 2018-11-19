package at.searles.fractal;


import java.net.InterfaceAddress;
import java.util.*;

import at.searles.fractal.data.FractalData;
import at.searles.fractal.data.ParameterType;
import at.searles.lexer.utils.IntervalSet;
import at.searles.math.Scale;
import at.searles.math.color.Palette;
import at.searles.meelan.MeelanException;
import at.searles.meelan.compiler.Ast;
import at.searles.meelan.compiler.IntCode;
import at.searles.meelan.ops.Instruction;
import at.searles.meelan.optree.Tree;
import at.searles.meelan.optree.Vec;
import at.searles.meelan.optree.inlined.ExternDeclaration;
import at.searles.meelan.optree.inlined.Id;
import at.searles.meelan.optree.inlined.Lambda;
import at.searles.meelan.symbols.IdResolver;
import at.searles.meelan.values.Int;
import at.searles.meelan.values.Real;

/*
 * When parsing, an instance of ExternData is created.
 * Additionally, there is a Map of custom parameters.
 *
 * So, ExternData gets a list of parameters, with
 * a type and a default  The order must
 * be preserved.
 *
 * Parameter also contains data type. Order of parameters
 * is of no importance.
 *
 * LinkedHashMap<String, ExternElement>
 */

/**
 * Fractal = FractalData + Ast + IntCode + Listeners.
 */

public class Fractal {

    public static final String SCALE_LABEL = "Scale";
    public static final Scale DEFAULT_SCALE = new Scale(2, 0, 0, 2, 0, 0);
    private static final String SCALE_DESCRIPTION = "Scale";

    public static final String SOURCE_LABEL = "Source";
    private static final String SOURCE_DESCRIPTION = "Source Code";

    private FractalData data; // source code and parameters

    // the next value is generated during parsing. They are
    // kept until the source code is modified.
    private Ast ast;
    private List<ExternDeclaration> externs;

    private TreeMap<String, Integer> order; // order; does not contain inlined ones.
    private TreeMap<String, ExternDeclaration> decls; // declarations by id.

    private List<Palette> palettes; // updated during compilation
    private TreeMap<String, Integer> paletteIndices;

    private List<Scale> scales; // updated during compilation
    private TreeMap<String, Integer> scaleIndices; // fixme not used currently

    // Data created during compilation
    private FractalResolver resolver;

    private TreeMap<String, Parameter> entries;
    private List<String> parameterOrder;

    // Order of parameters should be as follows:

    // final step
    private int[] code;

    private List<Listener> listeners;

    public static Fractal fromData(FractalData data) {
        // Step 1: create ast.
        Ast ast = ParserInstance.get().parseSource(data.source());
        List<ExternDeclaration> externs = ast.traverseExternData();
        return new Fractal(data, ast, externs);
    }

    private Fractal(FractalData data, Ast ast, List<ExternDeclaration> externs) {
        this.data = data;
        this.ast = ast;
        this.externs = externs;

        this.resolver = new FractalResolver();

        this.listeners = new LinkedList<>();

        // Find scales and palettes from externs.
        // We might store more than necessary, but it
        // is way simpler this way, and we can assume
        // that programmers are at least a bit reasonable.
        initExternData();
    }

    public void addListener(Listener l) {
        listeners.add(l);
    }

    public boolean removeListener(Listener l) {
        return listeners.remove(l);
    }

    private void notifyListeners() {
        for(Listener l : listeners) {
            l.fractalModified(this);
        }
    }

    /**
     * Precondition: "externs" contains a correct value.
     * Sets order, palettes and scales.
     */
    private void initExternData() {
        // Initializations that only depend on Ast and ExternDeclarations.
        this.order = new TreeMap<>();
        this.decls = new TreeMap<>();
        this.palettes = new LinkedList<>();
        this.paletteIndices = new TreeMap<>();
        this.scales = new LinkedList<>();
        this.scaleIndices = new TreeMap<>();

        int counter = 0;
        int paletteCounter = 0;
        int scaleCounter = 0;

        // also add default palettes and scales

        for(ExternDeclaration extern : externs) {
            this.order.put(extern.id, counter++);
            this.decls.put(extern.id, extern);

            if(ParameterType.fromString(extern.externTypeString) == ParameterType.Palette) {
                this.paletteIndices.put(extern.id, paletteCounter++);
                this.palettes.add((Palette) ParameterType.Palette.toValue(extern.value));
            }

            if(ParameterType.fromString(extern.externTypeString) == ParameterType.Scale) {
                this.scaleIndices.put(extern.id, scaleCounter++);
                this.scales.add((Scale) ParameterType.Scale.toValue(extern.value));
            }
        }
    }

    public void compile() {
        // initialize data structures
        entries = new TreeMap<>();
        parameterOrder = new LinkedList<>();

        parameterOrder.add(SOURCE_LABEL);
        parameterOrder.add(SCALE_LABEL);

        // next instruction will update 'entries' and 'parameterOrder'
        IntCode asmCode = ast.compile(FractviewInstructionSet.get(), resolver);
        this.code = asmCode.createIntCode();

        // add source and scale
        entries.put(SOURCE_LABEL, new Parameter(
                SOURCE_DESCRIPTION,
                data.source,
                null,
                ParameterType.Source,
                true
        ));

        // and initialize scale

        Scale customScale = (Scale) data.getValue(SCALE_LABEL);

        // XXX Ideally, here would be an approach similar to palette.

        if(customScale != null) {
            entries.put(SCALE_LABEL, new Parameter(
                    "Current Zoom",
                    customScale,
                    null, // not needed because it is not implemented
                    ParameterType.Scale,
                    true
            ));
        } else {
            // either default or declared.
            ExternDeclaration declaredScale = decls.get(SCALE_LABEL);

            Scale scale = DEFAULT_SCALE;

            if(declaredScale != null) {
                scale = (Scale) ParameterType.Scale.toValue(declaredScale.value);
            }

            entries.put(SCALE_LABEL, new Parameter(
                    "Current Zoom",
                    scale,
                    null, // not needed because it is not implemented
                    ParameterType.Scale,
                    false
            ));
        }
    }

    /**
     * This can be used to sort parameters along multiple fractals.
     */
    public LinkedHashMap<String, Integer> createParameterDegrees() {
        // Finally sort traversal order.
        // If an element in parameterOrder has no
        // corresponding entry in order, ignore it.
        LinkedHashMap<String, Integer> degrees = new LinkedHashMap<>();

        int lastDegree = -1;

        for(String id : parameterOrder) {
            Integer degree = order.get(id);

            if(degree == null) {
                degree = lastDegree;
            } else {
                lastDegree = degree;
            }

            degrees.put(id, degree);
        }

        return degrees;
    }

    /**
     * Since palettes must be transferred directly to the script, convenience method
     * to collect all palettes
     */
    public List<Palette> palettes() {
        return palettes;
    }

    public List<Scale> scales() {
        return scales;
    }

    public Parameter getParameter(String id) {
        return entries.get(id);
    }

    public boolean setValue(String id, Object value) {
        if(!entries.containsKey(id)) {
            return false;
        }

        FractalData oldData = data;

        if(id.equals(SOURCE_LABEL)) {
            // for source we need a new ast.
            data = data.newWithSource((String) value);
            this.ast = ParserInstance.get().parseSource(data.source());
            this.externs = ast.traverseExternData();
        } else if(value == null) {
            data = data.newRemoveParameter(id);
        } else {
            data = data.newSetParameter(id, getParameter(id).type, value);
        }

        try {
            compile();
            notifyListeners();
        } catch (MeelanException ex) {
            // roll back
            this.data = oldData;
            throw ex; // rethrow.
        }

        return true; // something changed.
    }

    public Object value(String id) {
        if(id.equals(SOURCE_LABEL)) {
            return data.source;
        } else {
            return data.getParameter(id).value;
        }
    }


//
//        /**
//         * Simple constructor
//         */
//    private Fractal(String sourceCode, Ast ast, FractalExternData parameters) {
//        if(sourceCode == null || parameters == null || ast == null) {
//            throw new NullPointerException();
//        }
//
//        this.sourceCode = sourceCode;
//        this.ast = ast;
//        this.data = parameters;
//    }
//
//    public void setSource(String source) {
//        Ast ast = ParserInstance.get().parseSource(source);
//
//        this.sourceCode = source;
//        this.ast = ast;
//    }
//
//    public void compile() {
//        IntCode code = ast.compile(FractviewInstructionSet.get(), data);
//        this.code = code.createIntCode();
//    }
//
//    public FractalExternData data() {
//        return data;
//    }
//
//    // ======== Some convenience methods to obtain data ========
//
    public Scale scale() {
        return (Scale) getParameter(SCALE_LABEL).value;
    }
//
//
//    public String sourceCode() {
//        return sourceCode;
//    }
//
//    public FractalData toData() {
//        Parameters exportData = new Parameters();
//
//        for(String id : data.ids()) {
//            if(!data.isDefaultValue(id)) {
//                FractalExternData.Entry entry = data.entry(id);
//                exportData.add(entry.key, data.value(id));
//            }
//        }
//
//        return new FractalData(sourceCode, exportData);
//    }

    public int[] code() {
        return code;
    }

    public interface Listener {
        void fractalModified(Fractal fractal);
    }

    public static class Parameter {
        public final String description;
        public final Object value;
        final Tree ast;
        public final ParameterType type;
        public final boolean isDefault;

        private Parameter(String description, Object value, Tree ast, ParameterType type, boolean isDefault) {
            this.description = description;
            this.value = value;
            this.ast = ast;
            this.type = type;
            this.isDefault = isDefault;
        }
    }

    private class FractalResolver implements IdResolver {

        private static final String TEMP_VAR = "_";

        private Tree paletteLambda(String id) {
            // 1. get index of this palette
            // 2. return 'palette(index)
            // 3. let currying do the rest.

            int paletteIndex = paletteIndices.get(id);

            Tree body = LdPalette.get().apply(Arrays.asList(new Int(paletteIndex), new Id(TEMP_VAR)));

            return new Lambda(Collections.singletonList(TEMP_VAR), body);
        }

        private Tree registerScale(String id) {
            throw new MeelanException("scale is not yet supported", null);
        }

        Parameter fromDecl(ExternDeclaration decl) {
            ParameterType type = ParameterType.fromString(decl.externTypeString);

            if(type == null) {
                throw new MeelanException("bad type", decl);
            }

            boolean isDefault;
            Object value;

            if(data.getParameterType(decl.id) == type) {
                value = data.getValue(decl.id);
                isDefault = false;
            } else {
                value = type.toValue(decl.value);
                isDefault = true;
            }

            Tree ast;

            if(type == ParameterType.Palette) {
                ast = paletteLambda(decl.id);
            } else if(type == ParameterType.Scale) {
                ast = registerScale(decl.id);
            } else {
                ast = type.toTree(value);
            }

            return new Parameter(decl.description, value, ast, type, isDefault);
        }

        Tree declaredEntry(String id) {
            Parameter entry = entries.get(id);

            if(entry == null) {
                // does it exist in declarations?
                ExternDeclaration decl = decls.get(id);

                if(decl == null) {
                    return null;
                }

                entry = fromDecl(decl);

                entries.put(id, entry); // cache.
                parameterOrder.add(id);
            }

            return entry.ast;
        }

        private Tree inlinedEntry(String id) {
            ParameterType storedType = data.getParameterType(id); // null if not existent

            Parameter entry;

            if(storedType == ParameterType.Expr) {
                // Use stored entry
                Object value = data.getValue(id);

                entry = new Parameter(
                        "inline parameter",
                        value,
                        ParameterType.Expr.toTree(value),
                        ParameterType.Expr,
                        false
                );
            } else {
                // Use default entry
                entry = new Parameter(
                        "inline parameter",
                        "0",
                        new Int(0),
                        ParameterType.Expr,
                        true
                );
            }

            entries.put(id, entry);
            parameterOrder.add(id);

            return entry.ast;
        }


        @Override
        public Tree valueOf(String id) {
            // This method adds inlined parameters.

            // is there an extern definition?
            Tree externParameter = declaredEntry(id);

            if(externParameter != null) {
                return externParameter;
            }

            Instruction instruction = FractviewInstructionSet.get().get(id);

            if(instruction != null) {
                return instruction;
            }

            // Inlined value - create expr parameter.
            return inlinedEntry(id);
        }
    }
}
