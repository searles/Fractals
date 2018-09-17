package at.searles.fractal;

import at.searles.fractal.data.FractalData;
import at.searles.fractal.data.ParameterKey;

import java.util.*;

/**
 * There is one source file for a fractal provider. It is compiled and that way,
 * the instance of ExternData in the fractalprovider is filled.
 */
public class FractalProvider {

    private static final String SEPARATOR = "::";

    /**
     * Listeners that are informed if a fractal
     * is modified due to a changed parameter.
     */
    private ArrayList<List<Listener>> listeners;

    /**
     * Key fractals. These are initialized in constructor and remain fixed.
     */
    private final ArrayList<Fractal> fractals;

    /**
     * Which parameters should not be shared?
     */
    private final HashSet<String> individualParameters; // without '::'.

    /**
     * After every change, the following is updated and used to
     * obtain values for editors, list adapters etc...
     * It is not written directly.
     */
    private final List<ParameterKey> keys;
    private final HashMap<ParameterKey, ParameterEntry> entries;

    /**
     * Creates a provider with only one fractal
     * @param fractalData The only fractal.
     * @return the fractal provider
     */
    public static FractalProvider singleFractal(FractalData fractalData) {
        Fractal fractal = Fractal.fromData(fractalData);
        fractal.compile();

        FractalProvider provider = new FractalProvider();
        provider.add(fractal);

        return provider;
    }

    /**
     * This one creates a provider for two fractals. It first creates a
     * provider with a single fractal and then splits it using the
     * keys of the individual parameteres.
     * @param fractalData first one
     * @param individuals parameters that are not shared. Most likely "Scale" and "juliaset".
     * @return the fractal provider.
     */
    public static FractalProvider dualFractal(FractalData fractalData, FractalData fractalData2, String...individuals) {
        FractalProvider fractalProvider = singleFractal(fractalData);
        fractalProvider.split(fractalData2, individuals);

        return fractalProvider;
    }

    private FractalProvider() {
        // individual for each fractal
        this.fractals = new ArrayList<>();
        this.listeners = new ArrayList<>();

        // general parameters
        this.individualParameters = new HashSet<>();
        this.keys = new ArrayList<>();
        this.entries = new LinkedHashMap<>();
    }

    private void add(Fractal fractal) {
        fractals.add(fractal);
        this.listeners.add(new ArrayList<>());

        updateEntries();
    }

    /**
     * The provider can be split along of individuals. Each of them can
     * use a unique value (that is not shared). Base of such a split
     * must be a single provider without individuals.
     * @return the label of the split fractal.
     */
    public void split(FractalData fractalData, String... individuals) {
        if(fractalsCount() != 1 || !individualParameters.isEmpty()) {
            throw new IllegalArgumentException("cannot split this fractal provider");
        }

        individualParameters.addAll(Arrays.asList(individuals));

        // split fractal
        Fractal splitFractal = Fractal.fromData(fractalData);
        splitFractal.compile();

        add(splitFractal);
    }

    /**
     * Return to single-fractal mode: Remove all individuals and
     * keep values of
     * @param index
     */
    public void unsplit(int index) {
        if(index < 0 || fractalsCount() <= index) {
            throw new IllegalArgumentException("index out of range");
        }

        // remove others
        individualParameters.clear();

        for(int i = fractalsCount(); i >= 0; --i) {
            if(i != index) {
                fractals.remove(i);
                listeners.remove(i);
            }
        }

        updateEntries();
    }

    public Iterable<ParameterKey> keys() {
        return keys;
    }

    public int parameterCount() {
        return keys.size();
    }

    public ParameterEntry getParameter(int index) {
        if(index >= keys.size()) {
            throw new IllegalArgumentException("index out of range");
        }

        ParameterKey key = keys.get(index);

        ParameterEntry entry = entries.get(key);

        if(entry == null) {
            throw new NullPointerException(String.format("bug, no entry for key %s %s", key.id, key.type));
        }

        return entry;
    }

    // ==== update entries from key fractals ====

    private void updateEntries() {
        int individualIndex = 0;

        keys.clear();
        entries.clear();

        for(int index = 0; index < fractalsCount(); ++index) {
            Fractal fractal = fractals.get(index);
            for(String id : fractal.data().ids()) {
                FractalExternData.Entry dataEntry = fractal.data().entry(id);

                if(individualParameters.contains(id)) {
                    // individual parameters are always new.
                    String label = annotatedId(dataEntry.key.id, index);
                    ParameterKey key = new ParameterKey(label, dataEntry.key.type);

                    keys.add(individualIndex++, key);

                    // entries will always contain the individual key, but the
                    // annotated description.
                    // Thereby, there is an easy-to-resolve representation owner + id.

                    String description = String.format("%s (%s)", dataEntry.description, index);
                    boolean isDefault = fractal.data().isDefaultValue(id);
                    Object value = fractal.data().value(id);

                    entries.put(key, new ParameterEntry(dataEntry.key, index, description + "", isDefault, value));
                } else {
                    if(!entries.containsKey(dataEntry.key)) {
                        keys.add(dataEntry.key);
                        String description = dataEntry.description;
                        boolean isDefault = fractal.data().isDefaultValue(id);
                        Object value = fractal.data().value(id);

                        entries.put(dataEntry.key, new ParameterEntry(dataEntry.key, -1, description, isDefault, value));
                    }
                }
            }
        }
    }

    public int fractalsCount() {
        return fractals.size();
    }

    /**
     * This one is called from the editor parameter list. Therefore,
     * id might be annotated with '::'.
     * @param value null if it should be reset to default.
     */
    public void set(ParameterKey key, Object value) {
        ParameterEntry entry = entries.get(key);

        if(entry == null) {
            // Did not exist
            throw new IllegalArgumentException("this should not happen - there is no such argument");
        }

        if(entry.owner == -1) {
            // try to set in all fractals
            for(int index = 0; index < fractalsCount(); ++index) {
                Fractal fractal = fractals.get(index);

                FractalExternData.Entry oldEntry = fractal.data().entry(key.id);

                if(oldEntry != null && !oldEntry.key.type.equals(key.type)) {
                    throw new IllegalArgumentException("incompatible entries: new=" + key + ", old=" + oldEntry.key);
                }

                boolean somethingChanged = fractal.data().setValue(key.id, value);

                if(somethingChanged) {
                    handleFractalChanged(index);
                }
            }
        } else {
            Fractal fractal = fractals.get(entry.owner);

            boolean somethingChanged = fractal.data().setValue(entry.key.id, value);

            if(somethingChanged) {
                handleFractalChanged(entry.owner);
            }
        }

        updateEntries();
    }

    /**
     * This one is called from on-screen editors. id here
     * is not annotated. It forwards the call to the set method.
     * @param fractalIndex is ignored if it is a non-individual parameter.
     */
    public void set(ParameterKey key, int fractalIndex, Object value) {
        // check whether it should be set in all fractals
        if(!individualParameters.contains(key.id)) {
            set(key, value);
        } else {
            // only set in label
            String annotatedId = annotatedId(key.id, fractalIndex);
            ParameterKey annotatedKey = new ParameterKey(annotatedId, key.type);

            set(annotatedKey, value);
        }
    }

    /**
     * Get fractal with index.
     */
    public Fractal get(int index) {
        return fractals.get(index);
    }

    private String annotatedId(String id, int index) {
        return id + SEPARATOR + index;
    }

    private void handleFractalChanged(int index) {
        List<Listener> listenerList = this.listeners.get(index);

        // check fractal to catch parser errors.
        Fractal fractal = fractals.get(index);
        fractal.compile();

        if(listenerList == null) {
            return;
        }

        for(Listener listener : listenerList) {
            listener.fractalModified(fractal);
        }
    }

    // ## Methods for listener

    public void addListener(int index, Listener listener) {
        this.listeners.get(index).add(listener);
    }

    public boolean removeListener(int index, Listener listener) {
        return listeners.get(index).remove(listener);
    }
    
    public static interface Listener {
        void fractalModified(Fractal fractal);
    }

    public static class ParameterEntry {
        public final ParameterKey key;
        public final int owner; // for indiviuals only, otherwise null.
        public final String description;
        public final boolean isDefault;
        public final Object value;

        private ParameterEntry(ParameterKey key, int owner, String description, boolean isDefault, Object value) {
            this.key = key;
            this.owner = owner;
            this.description = description;
            this.isDefault = isDefault;
            this.value = value;
        }

        @Override
        public String toString() {
            return "ParameterEntry{" +
                    "key=" + key +
                    ", owner='" + owner + '\'' +
                    ", description='" + description + '\'' +
                    ", isDefault=" + isDefault +
                    ", value=" + value +
                    '}';
        }
    }
}
