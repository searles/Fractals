package at.searles.fractal;

import java.util.*;

import at.searles.fractal.data.ParameterKey;
import at.searles.fractal.data.ParameterType;
import at.searles.fractal.data.Parameters;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.meelan.MeelanException;
import at.searles.meelan.optree.Tree;
import at.searles.meelan.optree.inlined.ExternDeclaration;
import at.searles.meelan.optree.inlined.Id;
import at.searles.meelan.optree.inlined.Lambda;
import at.searles.meelan.symbols.ExternData;
import at.searles.meelan.symbols.SymTable;
import at.searles.meelan.symbols.VarCounter;
import at.searles.meelan.values.*;

public class FractalExternData implements ExternData {

    /**
     * data contains a label Scale that contains the scale of the fractal.
     */
    public static final String SCALE_LABEL = "Scale";
    public static final ParameterKey SCALE_KEY = new ParameterKey(SCALE_LABEL, ParameterType.Scale);
    public static final Scale DEFAULT_SCALE = new Scale(2, 0, 0, 2, 0, 0);
    private static final String SCALE_DESCRIPTION = "Scale";

    public static final String SOURCE_LABEL = "Source";
    public static final ParameterKey SOURCE_KEY = new ParameterKey(SOURCE_LABEL, ParameterType.Source);
    private static final String SOURCE_DESCRIPTION = "Source Code";

    private static final String TEMP_VAR = "_";

    private Set<String> activeEntries;
    private LinkedHashMap<String, Entry> entries;

    private Parameters customValues;

    public static FractalExternData fromParameters(Parameters parameters) {
        return new FractalExternData(new Parameters(parameters));
    }

    public static FractalExternData empty() {
        return new FractalExternData(new Parameters());
    }

    private FractalExternData(Parameters parameters) {
        entries = new LinkedHashMap<>();
        activeEntries = new LinkedHashSet<>();
        customValues = parameters;
    }

    @Override
    public void addDefinition(String id, String externType, String description, Tree value) {
        ParameterType type = ParameterType.fromString(externType);

        if(type == null) {
            throw new MeelanException("Unknown type", value);
        }

        Object defaultValue = convertAstToValue(externType, value);

        Entry entry = new Entry(new ParameterKey(id, type), description, defaultValue);

        Entry oldEntry = entries.put(id, entry);

        if(oldEntry != null && !type.equals(oldEntry.key.type)) {
            throw new MeelanException("extern with same name but different type", value);
        }
    }

    @Override
    public void initialize() {
        entries.clear();
        activeEntries.clear();

        // always contains default scale and current source code.
        addGlobalDefaults();
    }

    private void addGlobalDefaults() {
        // source must be injected into editor.
        entries.put(SOURCE_LABEL, new Entry(SOURCE_KEY, SOURCE_DESCRIPTION, ""));
        entries.put(SCALE_LABEL, new Entry(SCALE_KEY, SCALE_DESCRIPTION, DEFAULT_SCALE));

        activeEntries.add(SOURCE_LABEL);
        activeEntries.add(SCALE_LABEL);
    }

    @Override
    public Iterable<String> ids() {
        return activeEntries;
    }

    @Override
    public boolean setValue(String id, Object value) {
        Entry entry = entries.get(id);

        if(entry == null) {
            return false;
        }

        if(value == null) {
            customValues.remove(entry.key);
        } else {
            customValues.add(entry.key, value);
        }

        return activeEntries.contains(id);
    }

    @Override
    public boolean isDefaultValue(String id) {
        Entry entry = entries.get(id);
        return !customValues.contains(entry.key);
    }

    @Override
    public Object value(String id) {
        Entry entry = entries.get(id);

        if(entry == null) {
            return null;
        }

        Object value = customValues.get(entry.key);

        return value != null ? value : entry.defaultValue;
    }

    @Override
    public Object convertAstToValue(String typeString, Tree tree) throws MeelanException {
        // preprocess tree to allow simple calculations
        ParameterType type = ParameterType.fromString(typeString);

        if(type == null) {
            return null;
        }

        return type.toValue(tree);
    }

    @Override
    public Tree findAst(String id) {
        Entry entry = entryOrAddDefault(id);
        return createTreeFrom(entry, value(id));
    }

    private Tree createTreeFrom(Entry entry, Object value) {
        switch (entry.key.type) {
            case Int:
                return new Int(((Number) value).intValue());
            case Real:
                return new Real(((Number) value).doubleValue());
            case Cplx:
                return new CplxVal((Cplx) value);
            case Bool:
                return new Bool((Boolean) value);
            case Color:
                return new Int((Integer) value);
            case Expr:
                // This may throw a MeelanException!
                return ParserInstance.get().parseExpr((String) value);
            case Palette:
                return paletteLambda(entry.key.id);
            case Scale:
                // first occurrence of this palette.
                return registerScale(entry.key.id);
        }

        throw new IllegalArgumentException("missing case: " + entry.key.type);
    }

    private int indexOfType(String id, ParameterType type) {
        int count = 0;

        for(Entry entry : entries.values()) {
            if(entry.key.type.equals(type)) {
                if (entry.key.id.equals(id)) {
                    return count;
                }

                count++;
            }
        }

        throw new IllegalArgumentException("no such " + type + " found");
    }

    private Tree paletteLambda(String id) {
        // 1. get index of this palette
        // 2. return 'palette(index)
        // 3. let currying do the rest.

        int paletteIndex = indexOfType(id, ParameterType.Palette);

        Tree body = LdPalette.get().apply(Arrays.asList(new Int(paletteIndex), new Id(TEMP_VAR)));

        return new Lambda(Collections.singletonList(TEMP_VAR), body);
    }

    private Tree registerScale(String id) {
        throw new MeelanException("scale is not yet supported", null);
    }

    public <T> List<T> filterByType(ParameterType type) {
        LinkedList<T> list = new LinkedList<>();

        for(Entry entry : entries.values()) {
            if(entry.key.type.equals(type)) {
                // TODO: can this cause crashes?
                list.add((T) value(entry.key.id));
            }
        }

        return list;
    }

    /**
     * Creates a new entry if the one that is queried does not exist.
     * Furthermore, marks entry as active.
     */
    private Entry entryOrAddDefault(String id) {
        Entry entry = entries.get(id);

        if(entry == null) {
            // otherwise add default value.
            entry = new Entry(new ParameterKey(id, ParameterType.Expr),
                    String.format("  (%s)", id), "0");
            entries.put(id, entry);
        }

        activeEntries.add(id);

        return entry;
    }

    /**
     * Returns null if it does not exist.
     */
    public Entry entry(String id) {
        return entries.get(id);
    }

    public ParameterType type(String id) {
        return entries.get(id).key.type;
    }

    public String description(String id) {
        return entries.get(id).description;
    }

    public static class Entry {
        public final ParameterKey key;
        public final String description;
        public final Object defaultValue;

        private Entry(ParameterKey key, String description, Object value) {
            this.key = key;
            this.description = description;
            this.defaultValue = value;
        }
    }
}
