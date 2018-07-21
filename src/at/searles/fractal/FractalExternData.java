package at.searles.fractal;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import at.searles.fractal.data.ParameterKey;
import at.searles.fractal.data.ParameterType;
import at.searles.fractal.data.Parameters;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;
import at.searles.meelan.MeelanException;
import at.searles.meelan.optree.Tree;
import at.searles.meelan.optree.inlined.ExternDeclaration;
import at.searles.meelan.optree.inlined.FuncDeclaration;
import at.searles.meelan.optree.inlined.Id;
import at.searles.meelan.symbols.ExternData;
import at.searles.meelan.values.*;

public class FractalExternData implements ExternData {

    private static final String TEMP_VAR = "__xy";

    private LinkedHashMap<String, Entry> entries;
    private Parameters customValues;

    private int paletteCount;

    public static FractalExternData fromParameters(Parameters parameters) {
        return new FractalExternData(new Parameters(parameters));
    }

    public static FractalExternData empty() {
        return new FractalExternData(new Parameters());
    }

    private FractalExternData(Parameters parameters) {
        entries = new LinkedHashMap<>();
        customValues = parameters;
    }

    @Override
    public void addDefinition(String id, String externType, String description, Tree value) {
        ParameterType type = ParameterType.fromString(externType);

        if(type == null) {
            throw new MeelanException("Unknown type", value);
        }

        Object defaultValue = convertToValue(externType, value);

        Entry entry = new Entry(new ParameterKey(id, type), description, defaultValue);

        Entry oldEntry = entries.put(id, entry);

        if(oldEntry != null && !type.equals(oldEntry.key.type)) {
            throw new MeelanException("extern with same name but different type", value);
        }
    }

    @Override
    public void clearIds() {
        entries.clear();
        paletteCount = 0;
    }

    @Override
    public Iterable<String> ids() {
        return entries.keySet();
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

        return entries.containsKey(id);
    }

    @Override
    public boolean isDefaultValue(String id) {
        Entry entry = entries.get(id);
        return !customValues.contains(entry.key);
    }

    @Override
    public Object value(String id) {
        Entry entry = entries.get(id);
        Object value = customValues.get(entry.key);

        return value != null ? value : entry.defaultValue;
    }

    @Override
    public Object convertToValue(String typeString, Tree tree) throws MeelanException {
        ParameterType type = ParameterType.fromString(typeString);

        if(type == null) {
            return null;
        }

        return type.toValue(tree);
    }

    @Override
    public Tree internalValue(String id) {
        Entry entry = entries.get(id);

        if(entry == null) {
            return null;
        }

        Object value = value(id);

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
                // first occurrence of this palette.
                return registerPalette(entry.key.id);
            case Scale:
                // first occurrence of this palette.
                return registerScale(entry.key.id);
        }

        throw new IllegalArgumentException("missing case: " + entry.key.type);
    }

    private Tree registerPalette(String id) {
        return new FuncDeclaration(id, Collections.singletonList(TEMP_VAR),
                LdPalette.get().apply(Arrays.asList(new Int(paletteCount), new Id(TEMP_VAR))));
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

    public Entry entry(String id) {
        return entries.get(id);
    }

    public static class Entry {
        public final ParameterKey key;
        public final String description;
        public final Object defaultValue;

        private Entry(ParameterKey key, String description, Object defaultValue) {
            this.key = key;
            this.description = description;
            this.defaultValue = defaultValue;
        }
    }
}
