package at.searles.fractal;


import java.util.*;

import at.searles.fractal.data.FractalData;
import at.searles.fractal.data.ParameterType;
import at.searles.math.Scale;
import at.searles.math.color.Palette;
import at.searles.meelan.MeelanException;
import at.searles.meelan.compiler.Ast;
import at.searles.meelan.compiler.IntCode;
import at.searles.meelan.ops.Instruction;
import at.searles.meelan.optree.Tree;
import at.searles.meelan.optree.inlined.ExternDeclaration;
import at.searles.meelan.optree.inlined.Id;
import at.searles.meelan.optree.inlined.Lambda;
import at.searles.meelan.symbols.IdResolver;
import at.searles.meelan.values.Int;

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

    // fixme setters package private. only access from provider

    public static final String SCALE_LABEL = "Scale";
    public static final Scale DEFAULT_SCALE = new Scale(2, 0, 0, 2, 0, 0);
    private static final String SCALE_DESCRIPTION = "Scale";

    public static final String SOURCE_LABEL = "Source";
    private static final String SOURCE_DESCRIPTION = "Source Code";

    /**
     * Pure data container
     */
    private FractalData data;

    private int historyIndex;
    private ArrayList<FractalData> history;

    /**
     * Ast of the source code
     */
    private Ast ast;

    /**
     * Extern declarations in the Ast.
     */
    private LinkedHashMap<String, ExternDeclaration> externDeclarations; // declarations by id.

    /**
     * Palettes in order
     */
    private List<Palette> palettes; // updated during compilation

    /**
     * For each id that is used for a palette, store its index which
     * correspons to the position in the list before. These data are needed
     * later. For other non-default RS-datastructures (eg scale) the same
     * method is required.
     */
    private List<String> paletteIds;

    // FIXME can't scale be a matrix?
    private List<Scale> scales; // updated during compilation
    private TreeMap<String, Integer> scaleIndices; // fixme not used currently

    /**
     * The resolver for ids during compilation. Used for
     * externs
     */
    private FractalResolver resolver;

    /**
     * Used parameters. Includes implcitly defined parameters which
     * are not in the externDeclaration.
     */
    private LinkedHashMap<String, Parameter> entries;

    // Order of parameters should be as follows:

    // final step
    private int[] code;

    private List<Listener> listeners;

    private static LinkedHashMap<String, ExternDeclaration> externsMap(Ast ast) {
        List<ExternDeclaration> externs = ast.traverseExternData();
        LinkedHashMap<String, ExternDeclaration> map = new LinkedHashMap<>();
        for(ExternDeclaration extern: externs) {
            map.put(extern.id, extern);
        }

        return map;
    }

    public static Fractal fromData(FractalData data) {
        // Step 1: create ast.
        Ast ast = ParserInstance.get().parseSource(data.source());

        Fractal fractal = new Fractal(data, ast, externsMap(ast));
        fractal.compile();
        return fractal;
    }

    private Fractal(FractalData data, Ast ast, LinkedHashMap<String, ExternDeclaration> externDeclarations) {
        this.data = data;
        this.ast = ast;
        this.externDeclarations = externDeclarations;

        this.resolver = new FractalResolver();

        this.listeners = new LinkedList<>();

        this.history = new ArrayList<>();
        this.historyIndex = 0;

        // Find scales and palettes from externs.
        // We might store more than necessary, but it
        // is way simpler this way, and we can assume
        // that programmers are at least a bit reasonable.
        initStructureTypes();
    }

    // === Handle Listeners ===

    public void addListener(Listener l) {
        listeners.add(l);
    }

    public boolean removeListener(Listener l) {
        return listeners.remove(l);
    }

    private void notifyFractalModified() {
        for(Listener l : listeners) {
            l.fractalModified(this);
        }
    }

    // === Compilation and Structure Types ===

    /**
     * Precondition: "externs" contains a correct value.
     * Sets order, palettes and scales.
     */
    private void initStructureTypes() {
        // Initializations that only depend on Ast and ExternDeclarations.
        this.palettes = new ArrayList<>();
        this.paletteIds = new ArrayList<>();

        this.scales = new LinkedList<>(); // FIXME replace by matrix?
        this.scaleIndices = new TreeMap<>();

        int scaleCounter = 0;

        for(ExternDeclaration extern : externDeclarations.values()) {
            // The content in scales and palettes is not yet correct; it only
            // reflects the information from all extern-statements in the code.
            // The actual values are inserted in the compile method.

            // Add scales
            if(ParameterType.fromString(extern.externTypeString) == ParameterType.Palette) {
                this.paletteIds.add(extern.id);
            }

            // Add declarations
            if(ParameterType.fromString(extern.externTypeString) == ParameterType.Scale) {
                // FIXME none of both is needed.
                this.scaleIndices.put(extern.id, scaleCounter++);
                this.scales.add((Scale) ParameterType.Scale.toValue(extern.value));
            }
        }
    }

    private void compile() {
        // update data structures
        entries = new LinkedHashMap<>();

        entries.put(SOURCE_LABEL, new Parameter(
                SOURCE_LABEL,
                SOURCE_DESCRIPTION,
                data.source,
                null,
                ParameterType.Source,
                true
        ));

        // place holder to preserve order. It will be added afterwards
        entries.put(SCALE_LABEL, null);

        // next instruction will update 'entries' and 'parameterOrder'
        IntCode asmCode = ast.compile(FractviewInstructionSet.get(), resolver);
        this.code = asmCode.createIntCode();

        // update palette list.
        palettes.clear();

        for(String paletteId : paletteIds) {
            Palette p = (Palette) getParameter(paletteId).value;
            palettes.add(p);
        }

        // and update scale

        // FIXME scales should work like palettes.

        Scale customScale = (Scale) data.getValue(SCALE_LABEL);

        // XXX Ideally, here would be an approach similar to palette.

        if(customScale != null) {
            entries.put(SCALE_LABEL, new Parameter(
                    SCALE_LABEL,
                    "Current Zoom", // FIXME name.
                    customScale,
                    null, // not needed because it is not implemented
                    ParameterType.Scale,
                    true
            ));
        } else {
            // either default or declared.
            Scale scale;

            ExternDeclaration declaredScale = externDeclarations.get(SCALE_LABEL);
            if(declaredScale != null) {
                scale = (Scale) ParameterType.Scale.toValue(declaredScale.value);
            } else {
                scale = DEFAULT_SCALE;
            }

            entries.put(SCALE_LABEL, new Parameter(
                    SCALE_LABEL,
                    "Current Zoom",
                    scale,
                    null, // not needed because it is not implemented
                    ParameterType.Scale,
                    false
            ));
        }
    }

    public int[] code() {
        return code;
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

    // === Parameters and Values ===

    public Iterable<Fractal.Parameter> requiredParameters() {
        return entries.values();
    }

    public Iterable<ExternDeclaration> externDeclarations() {
        return externDeclarations.values();
    }

    public Parameter getParameter(String id) {
        return entries.get(id);
    }

    public boolean setValue(String id, Object value) {
        if(!entries.containsKey(id)) {
            return false;
        }

        if(id.equals(SOURCE_LABEL)) {
            setData(this.data.copySetSource((String) value), false, true, false);
        } else {
            FractalData newData = value != null ? data.copySetParameter(id, getParameter(id).type, value) : data.copyResetParameter(id);
            setData(newData, true, true, false);
        }

        return true; // something changed.
    }

    public Scale scale() {
        return (Scale) getParameter(SCALE_LABEL).value;
    }

    public FractalData data() {
        return data;
    }

    public String source() {
        return data.source;
    }

    void setData(FractalData data, boolean keepAst, boolean storeInHistory, boolean isRollback) {
        FractalData oldData = this.data;
        this.data = data;

        try {
            if(!keepAst) {
                this.ast = ParserInstance.get().parseSource(data.source());
                this.externDeclarations = externsMap(ast);
                initStructureTypes();
            }

            compile();
        } catch (MeelanException ex) {
            // roll back (it was already successful).
            setData(oldData, keepAst, false, true);
            throw ex; // rethrow.
        }

        // success on compiling. If requested, store in history.
        if(storeInHistory) {
            history.add(historyIndex++, oldData);
        }

        if(!isRollback) {
            // should not be called if there was a compiler error.
            notifyFractalModified();
        }
    }

    // === Handle History ===

    public boolean historyForward() {
        if(historyIndex >= history.size()) {
            return false;
        }

        FractalData newData = history.get(historyIndex++);
        setData(newData, false, false, false);

        return true;
    }

    public boolean historyBack() {
        if(historyIndex <= 0) {
            return false;
        }

        FractalData newData = history.get(--historyIndex);
        setData(newData, false, false, false);

        return true;
    }

    // === Internal data structures ===

    public interface Listener {
        void fractalModified(Fractal fractal);
    }

    public static class Parameter {
        public final String id;
        public final String description;
        public final Object value;
        final Tree ast;
        public final ParameterType type;
        public final boolean isDefault;

        private Parameter(String id, String description, Object value, Tree ast, ParameterType type, boolean isDefault) {
            this.id = id;
            this.description = description;
            this.value = value;
            this.ast = ast;
            this.type = type;
            this.isDefault = isDefault;
        }
    }

    /**
     * For Meelan; returns identifiers that are associated with this fractal
     */
    private class FractalResolver implements IdResolver {

        private static final String TEMP_VAR = "_";

        private Tree paletteLambda(String id) {
            // 1. get index of this palette
            // 2. return 'palette(index)
            // 3. let currying do the rest.

            int paletteIndex = paletteIds.indexOf(id); // there are not that many.
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

            return new Parameter(decl.id, decl.description, value, ast, type, isDefault);
        }

        Tree declaredEntry(String id) {
            Parameter entry = entries.get(id);

            if(entry == null) {
                // does it exist in declarations?
                ExternDeclaration decl = externDeclarations.get(id);

                if(decl == null) {
                    return null;
                }

                entry = fromDecl(decl);

                entries.put(id, entry); // cache.

                lastLabel = entry.description;
            }

            return entry.ast;
        }

        /**
         * Last traversed parameter. Used for description of inlined
         * parameters.
         */
        private String lastLabel;

        private Tree inlinedEntry(String id) {
            ParameterType storedType = data.getParameterType(id); // null if not existent

            Parameter entry;

            String label = "id (" + lastLabel + ")";

            if(storedType == ParameterType.Expr) {
                // Use stored entry
                Object value = data.getValue(id);

                entry = new Parameter(
                        id,
                        label,
                        value,
                        ParameterType.Expr.toTree(value),
                        ParameterType.Expr,
                        false
                );
            } else {
                // Use default entry
                entry = new Parameter(
                        id,
                        label,
                        "0",
                        new Int(0),
                        ParameterType.Expr,
                        true
                );
            }

            entries.put(id, entry);
            lastLabel = id;
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
