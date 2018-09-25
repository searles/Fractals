package at.searles.fractal;

import at.searles.fractal.data.FractalData;
import at.searles.fractal.data.ParameterType;

import java.util.*;

/**
 * There is one source file for a fractal provider. It is compiled and that way,
 * the instance of ExternData in the fractalprovider is filled.
 */
public class FractalProvider {

    private final ArrayList<FractalEntry> fractalEntries;

    private final ArrayList<ParameterEntry> parameterOrder;
    private final TreeMap<AnnotatedKey, ParameterEntry> parameters;

    public FractalProvider() {
        this.fractalEntries = new ArrayList<>(2);
        this.parameters = new TreeMap<>();
        parameterOrder = new ArrayList<>(); // must call updateModel once.
    }

    /**
     * Returns the parameter with the key and owner
     * @param owner -1 if there is no exclusive owner
     */
    public ParameterEntry getParameter(String key, int owner) {
        if(owner != -1) {
            FractalEntry fractalEntry = fractalEntries.get(owner);

            if (fractalEntry.exclusiveParameters.contains(key)) {
                // exclusive parameter
                return parameters.get(new AnnotatedKey(key, owner));
            }
        }

        return parameters.get(new AnnotatedKey(key, -1));
    }

    public ParameterEntry getParameterByIndex(int index) {
        return parameterOrder.get(index);
    }

    /**
     * Sets the parameter. If owner is -1 or an owner for which 'key' is non-exclusive
     * then it is changed for all fractals for which it is non-exclusive.
     */
    public void setParameter(String key, int owner, Object value) {
        if(owner != -1) {
            FractalEntry fractalEntry = fractalEntries.get(owner);

            if(fractalEntry.exclusiveParameters.contains(key)) {
                // exclusive parameter
                if(fractalEntry.fractal.data().setValue(key, value)) {
                    handleFractalChanged(owner);
                    updateParameterMap();
                }

                return;
            }
        }

        // set in all parameters.
        boolean somethingChanged = false;

        for(int index = 0; index < fractalEntries.size(); ++index) {
            FractalEntry fractalEntry = fractalEntries.get(index);
            if (!fractalEntry.exclusiveParameters.contains(key)) {
                Fractal fractal = fractalEntry.fractal;

                if(fractal.data().setValue(key, value)) {
                    // something changed.
                    handleFractalChanged(index);
                    somethingChanged = true;
                }
            }
        }

        if(somethingChanged) {
            updateParameterMap();
        }
    }

    public int addFractal(FractalData fractalData, String...exclusiveParameters) {
        Fractal fractal = Fractal.fromData(fractalData);
        fractal.compile();

        FractalEntry entry = new FractalEntry(fractal, exclusiveParameters);

        fractalEntries.add(entry);

        updateParameterMap();

        return fractalEntries.size() - 1;
    }

    public int fractalCount() {
        return fractalEntries.size();
    }

    public int parameterCount() {
        return parameterOrder.size();
    }

    public Fractal getFractal(int index) {
        return fractalEntries.get(index).fractal;
    }

    public void removeFractal(int index) {
        fractalEntries.remove(index);
        updateParameterMap();
    }

    public void addListener(int index, Listener l) {
        fractalEntries.get(index).listeners.add(l);
    }

    public boolean removeListener(int index, Listener l) {
        return fractalEntries.get(index).listeners.remove(l);
    }

    private void updateParameterMap() {
        parameters.clear();

        LinkedHashSet<ParameterEntry> exclusiveEntries = new LinkedHashSet<>();
        LinkedHashSet<ParameterEntry> globalEntries = new LinkedHashSet<>();

        // the next one contains parameters that are used in all
        // fractals.
        LinkedHashSet<ParameterEntry> globalUniversalEntries = new LinkedHashSet<>();

        for(int index = 0; index < fractalEntries.size(); ++index) {
            FractalEntry fractalEntry = fractalEntries.get(index);

            // to get a proper sorting.
            LinkedHashSet<ParameterEntry> globalEntriesOfCurrent = new LinkedHashSet<>();

            for(String id : fractalEntry.fractal.data().ids()) {

                FractalExternData data = fractalEntry.fractal.data();

                if(fractalEntry.exclusiveParameters.contains(id)) {
                    // exclusive parameter
                    ParameterEntry entry = new ParameterEntry(
                            id, index, data.isDefaultValue(id),
                            data.type(id), data.description(id),
                            data.value(id)
                    );

                    parameters.put(new AnnotatedKey(id, index), entry);
                    exclusiveEntries.add(entry);
                } else {
                    // non-exclusive
                    AnnotatedKey key = new AnnotatedKey(id, -1);

                    ParameterEntry entry = parameters.get(key);

                    if(entry == null) {
                        // It is the responsibility of the caller
                        // to ensure consistency of data.
                        entry = new ParameterEntry(
                                id, -1, data.isDefaultValue(id),
                                data.type(id), data.description(id),
                                data.value(id)
                        );

                        parameters.put(key, entry);
                        globalEntries.add(entry);
                    }

                    globalEntriesOfCurrent.add(entry);
                } // end non-exclusive
            } // end fractal

            if(index == 0) {
                globalUniversalEntries = globalEntriesOfCurrent;
            } else {
                globalUniversalEntries.retainAll(globalEntriesOfCurrent);
            }
        }

        globalEntries.removeAll(globalUniversalEntries);

        // arrange order
        parameterOrder.clear();
        parameterOrder.ensureCapacity(exclusiveEntries.size() + globalUniversalEntries.size() + globalEntries.size());
        // first exclusives

        parameterOrder.addAll(exclusiveEntries);

        // next those that are global but do not occur in all.
        parameterOrder.addAll(globalEntries);

        // finally all global ones that occur in all fractals.
        parameterOrder.addAll(globalUniversalEntries);
    }

    private void handleFractalChanged(int index) {
        FractalEntry fractalEntry = fractalEntries.get(index);

        // check fractal to catch parser errors.
        fractalEntry.fractal.compile();

        for(Listener listener : fractalEntry.listeners) {
            listener.fractalModified(fractalEntry.fractal);
        }
    }

    private class AnnotatedKey implements Comparable<AnnotatedKey> {
        final String key;
        final int owner; // -1 if global

        private AnnotatedKey(String key, int owner) {
            this.key = key;
            this.owner = owner;
        }

        @Override
        public int compareTo(AnnotatedKey o) {
            int cmp = key.compareTo(o.key);
            return cmp != 0 ? cmp : Integer.compare(owner, o.owner);
        }
    }

    public static class ParameterEntry {
        public final String key;
        public final int owner;

        // The next ones are NOT part of any equals-comparison!
        public final boolean isDefault;
        public final ParameterType type;
        public final String description;
        public final Object value;
        // End.

        private ParameterEntry(String key, int owner, boolean isDefault, ParameterType type, String description, Object value) {
            this.key = key;
            this.owner = owner;
            this.isDefault = isDefault;
            this.type = type;
            this.description = description;
            this.value = value;
        }

        public int hashCode() {
            return key.hashCode() * 31 + owner;
        }

        public boolean equals(Object o) {
            if(o == null || o.getClass() != ParameterEntry.class) {
                return false;
            }

            ParameterEntry that = ((ParameterEntry) o);

            return key.equals(that.key) && owner == that.owner;
        }
    }

    private class FractalEntry {
        final Fractal fractal;
        final LinkedList<Listener> listeners;
        final TreeSet<String> exclusiveParameters;

        private FractalEntry(Fractal fractal, String...exclusiveParameters) {
            this.fractal = fractal;
            this.listeners = new LinkedList<>();
            this.exclusiveParameters = new TreeSet<>(Arrays.asList(exclusiveParameters));
        }
    }


    public static interface Listener {
        void fractalModified(Fractal fractal);
    }

//
//    /**
//     * After every change, the following is updated and used to
//     * obtain values for editors, list adapters etc...
//     * It is not written directly.
//     */
//    private final List<ParameterKey> keys;
//    private final HashMap<ParameterKey, ParameterEntry> entries;
//
//    /**
//     * Creates a provider with only one fractal
//     * @param fractalData The only fractal.
//     * @return the fractal provider
//     */
//    public static FractalProvider singleFractal(FractalData fractalData) {
//        Fractal fractal = Fractal.fromData(fractalData);
//        fractal.compile();
//
//        FractalProvider provider = new FractalProvider();
//        provider.addFractal(fractal);
//
//        return provider;
//    }
//
//    /**
//     * This one creates a provider for two fractals. It first creates a
//     * provider with a single fractal and then splits it using the
//     * keys of the individual parameteres.
//     * @param fractalData first one
//     * @param individuals parameters that are not shared. Most likely "Scale" and "juliaset".
//     * @return the fractal provider.
//     */
//    public static FractalProvider dualFractal(FractalData fractalData, FractalData fractalData2, String...individuals) {
//        FractalProvider fractalProvider = singleFractal(fractalData);
//        fractalProvider.split(fractalData2, individuals);
//
//        return fractalProvider;
//    }
//
//    private FractalProvider() {
//        // individual for each fractal
//        this.fractals = new ArrayList<>();
//        this.listeners = new ArrayList<>();
//
//        // general parameters
//        this.individualParameters = new HashSet<>();
//        this.keys = new ArrayList<>();
//        this.entries = new LinkedHashMap<>();
//    }
//
//    private void addFractal(Fractal fractal) {
//        fractals.add(fractal);
//        this.listeners.add(new ArrayList<>());
//
//        updateEntries();
//    }
//
//    /**
//     * The provider can be split along of individuals. Each of them can
//     * use a unique value (that is not shared). Base of such a split
//     * must be a single provider without individuals.
//     * @return the label of the split fractal.
//     */
//    public void split(FractalData fractalData, String... individuals) {
//        if(fractalsCount() != 1 || !individualParameters.isEmpty()) {
//            throw new IllegalArgumentException("cannot split this fractal provider");
//        }
//
//        individualParameters.addAll(Arrays.asList(individuals));
//
//        // split fractal
//        Fractal splitFractal = Fractal.fromData(fractalData);
//        splitFractal.compile();
//
//        addFractal(splitFractal);
//    }
//
//    /**
//     * Return to single-fractal mode: Remove all individuals and
//     * keep values of
//     * @param index
//     */
//    public void unsplit(int index) {
//        if(index < 0 || fractalsCount() <= index) {
//            throw new IllegalArgumentException("index out of range");
//        }
//
//        // remove others
//        individualParameters.clear();
//
//        for(int i = fractalsCount(); i >= 0; --i) {
//            if(i != index) {
//                fractals.remove(i);
//                listeners.remove(i);
//            }
//        }
//
//        updateEntries();
//    }
//
//    public Iterable<ParameterKey> parameterKeys() {
//        return keys;
//    }
//
//    public int parameterCount() {
//        return keys.size();
//    }
//
//    public ParameterEntry getParameter(int index) {
//        if(index >= keys.size()) {
//            throw new IllegalArgumentException("index out of range");
//        }
//
//        ParameterKey key = keys.get(index);
//
//        ParameterEntry entry = entries.get(key);
//
//        if(entry == null) {
//            throw new NullPointerException(String.format("bug, no entry for key %s %s", key.id, key.type));
//        }
//
//        return entry;
//    }
//
//    // ==== update entries from key fractals ====
//
//    private void updateEntries() {
//        int individualIndex = 0;
//
//        keys.clear();
//        entries.clear();
//
//        // order of elements:
//        // 1. individual and unique entries of 0
//        // 2. individual and unique entries of 1
//        // ...
//        // all other parameters.
//        // map<list<integer>, parameter>
//
//        for(int index = 0; index < fractalsCount(); ++index) {
//            Fractal fractal = fractals.get(index);
//            for(String id : fractal.data().ids()) {
//                FractalExternData.Entry dataEntry = fractal.data().entry(id);
//
//                if(individualParameters.contains(id)) {
//                    // individual parameters are always new.
//                    AnnotatedId label = new AnnotatedId(dataEntry.key.id, index);
//                    ParameterKey key = new ParameterKey(label, dataEntry.key.type);
//
//                    keys.add(individualIndex++, key);
//
//                    // entries will always contain the individual key, but the
//                    // annotated description.
//                    // Thereby, there is an easy-to-resolve representation owner + id.
//
//                    String description = String.format("%s (%s)", dataEntry.description, index);
//                    boolean isDefault = fractal.data().isDefaultValue(id);
//                    Object value = fractal.data().value(id);
//
//                    entries.put(key, new ParameterEntry(dataEntry.key, index, description + "", isDefault, value));
//                } else {
//                    if(!entries.containsKey(dataEntry.key)) {
//                        keys.add(dataEntry.key);
//                        String description = dataEntry.description;
//                        boolean isDefault = fractal.data().isDefaultValue(id);
//                        Object value = fractal.data().value(id);
//
//                        entries.put(dataEntry.key, new ParameterEntry(dataEntry.key, -1, description, isDefault, value));
//                    }
//                }
//            }
//        }
//    }
//
//    public int fractalsCount() {
//        return fractals.size();
//    }
//
//    /**
//     * This one is called from the editor parameter list. Therefore,
//     * parameter key might be annotated with '::'.
//     * @param value null if it should be reset to default.
//     */
//    private void setParameter(ParameterKey key, Object value) {
//        ParameterEntry entry = entries.get(key);
//
//        if(entry == null) {
//            // Did not exist
//            throw new IllegalArgumentException("this should not happen - there is no such argument");
//        }
//
//        if(entry.owner == -1) {
//            // try to set in all fractals
//            for(int index = 0; index < fractalsCount(); ++index) {
//                Fractal fractal = fractals.get(index);
//
//                FractalExternData.Entry oldEntry = fractal.data().entry(key.id);
//
//                if(oldEntry != null && !oldEntry.key.type.equals(key.type)) {
//                    throw new IllegalArgumentException("incompatible entries: new=" + key + ", old=" + oldEntry.key);
//                }
//
//                boolean somethingChanged = fractal.data().setValue(key.id, value);
//
//                if(somethingChanged) {
//                    handleFractalChanged(index);
//                }
//            }
//        } else {
//            Fractal fractal = fractals.get(entry.owner);
//
//            boolean somethingChanged = fractal.data().setValue(entry.key.id, value);
//
//            if(somethingChanged) {
//                handleFractalChanged(entry.owner);
//            }
//        }
//
//        updateEntries();
//    }
//
//    /**
//     * This one is called from on-screen editors. id here
//     * is not annotated. It forwards the call to the set method.
//     * @param fractalIndex is ignored if it is a non-individual parameter.
//     */
//    public void setParameter(ParameterKey key, int fractalIndex, Object value) {
//        // check whether it should be set in all fractals
//        if(!individualParameters.contains(key.id)) {
//            setParameter(key, value);
//        } else {
//            // only set in label
//            String annotatedId = annotatedId(key.id, fractalIndex);
//            ParameterKey annotatedKey = new ParameterKey(annotatedId, key.type);
//
//            setParameter(annotatedKey, value);
//        }
//    }
//
//    /**
//     * This method returns the entry for the given parameter key. Careful,
//     * if spilts are used, it is the responsibility of the caller
//     * of split that divergent parameters in data are consistent with
//     * individuals (otherwise there would be multiple non-individual keys
//     * with the same value; in this case, the first occurence is returned).
//     */
//    private Object getParameter(ParameterKey key) {
//        ParameterEntry entry = entries.get(key);
//
//        if(entry == null) {
//            // Did not exist
//            throw new IllegalArgumentException("this should not happen - there is no such argument");
//        }
//
//        if(entry.owner == -1) {
//            // find it in all fractals; return first one.
//            for(int index = 0; index < fractalsCount(); ++index) {
//                Fractal fractal = fractals.get(index);
//
//                FractalExternData.Entry oldEntry = fractal.data().entry(key.id);
//
//                if(oldEntry != null && !oldEntry.key.type.equals(key.type)) {
//                    throw new IllegalArgumentException("incompatible entries: new=" + key + ", old=" + oldEntry.key);
//                }
//
//                Object value = fractal.data().value(key.id);
//
//                if(value != null) {
//                    return value;
//                }
//            }
//
//            throw new IllegalArgumentException("not found");
//        }
//
//        Fractal fractal = fractals.get(entry.owner);
//        return fractal.data().value(entry.key.id);
//    }
//
//    /**
//     * Counterpart of set.
//     * @return
//     */
//    public Object getParameter(ParameterKey key, int fractalIndex) {
//        if(!individualParameters.contains(key.id)) {
//            return getParameter(key);
//        } else {
//            // only set in label
//            String annotatedId = annotatedId(key.id, fractalIndex);
//            ParameterKey annotatedKey = new ParameterKey(annotatedId, key.type);
//
//            return getParameter(annotatedKey);
//        }
//    }
//
//    /**
//     * Get fractal with index.
//     */
//    public Fractal getFractal(int index) {
//        return fractals.get(index);
//    }
//
//    private void handleFractalChanged(int index) {
//        List<Listener> listenerList = this.listeners.get(index);
//
//        // check fractal to catch parser errors.
//        Fractal fractal = fractals.get(index);
//        fractal.compile();
//
//        if(listenerList == null) {
//            return;
//        }
//
//        for(Listener listener : listenerList) {
//            listener.fractalModified(fractal);
//        }
//    }
//
//    // ## Methods for listener
//
//    public void addListener(int index, Listener listener) {
//        this.listeners.get(index).add(listener);
//    }
//
//    public boolean removeListener(int index, Listener listener) {
//        return listeners.get(index).remove(listener);
//    }
//
//
//    private static class AnnotatedId {
//        final String id;
//        final int owner;
//
//
//        AnnotatedId(String id) {
//            this(id, -1);
//        }
//
//        AnnotatedId(String id, int owner) {
//            this.id = id;
//            this.owner = owner;
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            if (this == o) return true;
//            if (o == null || getClass() != o.getClass()) return false;
//
//            AnnotatedId that = (AnnotatedId) o;
//
//            return owner == that.owner && id.equals(that.id);
//        }
//
//        @Override
//        public int hashCode() {
//            return id.hashCode() * 31 + owner;
//        }
//
//        @Override
//        public String toString() {
//            return owner == -1 ? id : (id + "::" + owner);
//        }
//    }
//
//    public static class ParameterEntry {
//        public final ParameterKey key;
//        public final int owner; // for indiviuals only, otherwise null.
//        public final String description;
//        public final boolean isDefault;
//        public final Object value;
//
//        private ParameterEntry(ParameterKey key, int owner, String description, boolean isDefault, Object value) {
//            this.key = key;
//            this.owner = owner;
//            this.description = description;
//            this.isDefault = isDefault;
//            this.value = value;
//        }
//
//        @Override
//        public String toString() {
//            return "ParameterEntry{" +
//                    "key=" + key +
//                    ", owner='" + owner + '\'' +
//                    ", description='" + description + '\'' +
//                    ", isDefault=" + isDefault +
//                    ", value=" + value +
//                    '}';
//        }
//    }
}
