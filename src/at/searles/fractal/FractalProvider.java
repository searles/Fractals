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
    private Map<String, List<Listener>> listeners;

    /**
     * Key fractals. These are initialized in constructor and remain fixed.
     */
    private final LinkedHashMap<String, Fractal> fractals;

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

    private final List<String> labels;

    /**
     * Creates a provider with only one fractal
     * @param fractalData The only fractal.
     * @return the fractal provider
     */
    public static FractalProvider singleFractal(FractalData fractalData) {
        FractalProvider fractalProvider = new FractalProvider(Collections.emptyList());

        Fractal fractal = Fractal.fromData(fractalData);
        fractal.compile();

        fractalProvider.fractals.put("Fractal", fractal);
        fractalProvider.updateEntries();

        return fractalProvider;
    }

    /**
     * This one creates a provider for two fractals. One parameter is
     * individual, most likely a bool (eg isJuliaSet).
     * @param fractalData1 first one
     * @param fractalData2 second one
     * @param individuals parameters that are not shared, eg scale and a boolean
     * @return the fractal provider.
     */
    public static FractalProvider dualFractal(FractalData fractalData1, FractalData fractalData2, String...individuals) {
        FractalProvider fractalProvider = new FractalProvider(Arrays.asList(individuals));

        Fractal fractal1 = Fractal.fromData(fractalData1);
        fractal1.compile();

        Fractal fractal2 = Fractal.fromData(fractalData2);
        fractal2.compile();

        fractalProvider.fractals.put("Fractal 1", fractal1);
        fractalProvider.fractals.put("Fractal 2", fractal2);

        fractalProvider.updateEntries();

        return fractalProvider;
    }

    private FractalProvider(Collection<String> individualParameters) {
        this.individualParameters = new HashSet<>(individualParameters);
        this.fractals = new LinkedHashMap<>();
        this.listeners = new HashMap<>();
        this.keys = new ArrayList<>();
        this.entries = new LinkedHashMap<>();

        this.labels = new ArrayList<>();
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
        labels.clear();

        for(Map.Entry<String, Fractal> entry : fractals.entrySet()) {
            labels.add(entry.getKey());

            Fractal fractal = entry.getValue();

            for(String id : fractal.data().ids()) {
                FractalExternData.Entry dataEntry = fractal.data().entry(id);

                if(individualParameters.contains(id)) {
                    // individual parameters are always new.
                    String label = annotatedId(dataEntry.key.id, entry.getKey());
                    ParameterKey key = new ParameterKey(label, dataEntry.key.type);

                    keys.add(individualIndex++, key);

                    // entries will always contain the individual key, but the
                    // annotated description.
                    // Thereby, there is an easy-to-resolve representation owner + id.

                    String description = String.format("%s (%s)", dataEntry.description, entry.getKey());
                    boolean isDefault = fractal.data().isDefaultValue(id);
                    Object value = fractal.data().value(id);

                    entries.put(key, new ParameterEntry(dataEntry.key, entry.getKey(), description + "", isDefault, value));
                } else {
                    if(!entries.containsKey(dataEntry.key)) {
                        keys.add(dataEntry.key);
                        String description = dataEntry.description;
                        boolean isDefault = fractal.data().isDefaultValue(id);
                        Object value = fractal.data().value(id);

                        entries.put(dataEntry.key, new ParameterEntry(dataEntry.key, null, description, isDefault, value));
                    }
                }
            }
        }
    }

    public int fractalsCount() {
        return labels.size();
    }

    public String label(int index) {
        return labels.get(index);
    }

    /**
     * This one is called from the editor parameter list. Therefore,
     * id might be annotated with '::'.
     * @param value null if it should be reset to default.
     */
    public void set(ParameterKey key, Object value) {
        ParameterEntry entry = entries.get(key);

        if(entry == null) {
            throw new IllegalArgumentException("this should not happen - there is no such argument");
        }

        if(entry.owner == null) {
            // try to set in all fractals
            for(Map.Entry<String, Fractal> fractalEntry : fractals.entrySet()) {
                FractalExternData.Entry oldEntry = fractalEntry.getValue().data().entry(key.id);

                if(oldEntry != null && !oldEntry.key.type.equals(key.type)) {
                    throw new IllegalArgumentException("incompatible entries: new=" + key + ", old=" + oldEntry.key);
                }

                boolean somethingChanged = fractalEntry.getValue().data().setValue(key.id, value);

                if(somethingChanged) {
                    handleFractalChanged(fractalEntry.getKey());
                }
            }

            updateEntries();
        } else {
            Fractal fractal = fractals.get(entry.owner);

            boolean somethingChanged = fractal.data().setValue(entry.key.id, value);

            if(somethingChanged) {
                handleFractalChanged(entry.owner);
            }
        }
    }

    /**
     * This one is called from on-screen editors. id here
     * is not annotated. It forwards the call to the set method.
     * @param fractalLabel is ignored if it is a non-individual parameter.
     */
    public void set(ParameterKey key, String fractalLabel, Object value) {
        // check whether it should be set in all fractals
        if(!individualParameters.contains(key.id)) {
            set(key, value);
        } else {
            // only set in label
            String annotatedId = annotatedId(key.id, fractalLabel);
            ParameterKey annotatedKey = new ParameterKey(annotatedId, key.type);

            set(annotatedKey, value);
        }
    }

    /**
     * Get fractal. Label might be further specified, eg in a video with a frame
     * parameter.
     * @param label
     * @return
     */
    public Fractal get(String label) {
        return fractals.get(label);
    }

    private String annotatedId(String id, String label) {
        return id + SEPARATOR + label;
    }

    private void handleFractalChanged(String label) {
        List<Listener> listenerList = this.listeners.get(label);

        if(listenerList == null) {
            return;
        }

        Fractal fractal = fractals.get(label);
        fractal.compile();

        for(Listener listener : listenerList) {
            listener.fractalModified(fractal);
        }
    }

    // ## Methods for listener

    public void addListener(String label, Listener listener) {
        List<Listener> listenerList = this.listeners.get(label);

        if(listenerList == null) {
            listenerList = new LinkedList<>();
            this.listeners.put(label, listenerList);
        }

        listenerList.add(listener);
    }

    public void removeListener(String label, Listener listener) {
        List<Listener> listenerList = listeners.get(label);

        if(listenerList != null) {
            listenerList.remove(listener);
        }
    }
    
    public static interface Listener {
        void fractalModified(Fractal fractal);
    }

    public static class ParameterEntry {
        public final ParameterKey key;
        public final String owner; // for indiviuals only, otherwise null.
        public final String description;
        public final boolean isDefault;
        public final Object value;

        private ParameterEntry(ParameterKey key, String owner, String description, boolean isDefault, Object value) {
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
