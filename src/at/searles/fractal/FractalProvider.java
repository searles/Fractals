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
    private final ArrayList<ParameterMapListener> parameterMapListeners;

    private final ArrayList<ParameterEntry> parameterOrder;
    private final TreeMap<AnnotatedKey, ParameterEntry> parameters;

    public FractalProvider() {
        this.fractalEntries = new ArrayList<>(2);
        this.parameters = new TreeMap<>();
        parameterOrder = new ArrayList<>(); // must call updateModel once.
        parameterMapListeners = new ArrayList<>(2);
    }

    /**
     * Returns the parameter with the key and owner
     * @param owner -1 if there is no exclusive owner
     */
    public ParameterEntry getParameterEntry(String key, int owner) {
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
     * Returns null if it does not exist.
     */
    public Object getParameter(String key, int owner) {
        ParameterEntry parameterEntry = getParameterEntry(key, owner);

        return parameterEntry != null ? parameterEntry.value : null;
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

                // is it the source code?
                if(key.equals(FractalExternData.SOURCE_KEY.id)) {
                    fractalEntry.fractal.setSource((String) value);
                    handleFractalChanged(owner);
                    updateParameterMap();
                } else if(fractalEntry.fractal.data().setValue(key, value)) {
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

                if(key.equals(FractalExternData.SOURCE_KEY.id)) {
                    fractalEntry.fractal.setSource((String) value);
                    handleFractalChanged(index);
                    somethingChanged = true;
                } else if(fractal.data().setValue(key, value)) {
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

    public void addFractalListener(int index, FractalListener l) {
        fractalEntries.get(index).listeners.add(l);
    }

    public boolean removeFractalListener(int index, FractalListener l) {
        return fractalEntries.get(index).listeners.remove(l);
    }

    public void addParameterMapListener(ParameterMapListener l) {
        parameterMapListeners.add(l);
    }

    public boolean removeParameterMapListener(ParameterMapListener l) {
        return parameterMapListeners.remove(l);
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
                    // exclusive parameter, add index to description.
                    ParameterEntry entry = new ParameterEntry(
                            id, index, data.isDefaultValue(id),
                            data.type(id), String.format("%s (%d)", data.description(id), index),
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

        for(ParameterMapListener l : parameterMapListeners) {
            l.parameterMapModified(this);
        }
    }

    private void handleFractalChanged(int index) {
        FractalEntry fractalEntry = fractalEntries.get(index);

        // check fractal to catch parser errors.
        fractalEntry.fractal.compile();

        for(FractalListener listener : fractalEntry.listeners) {
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
        public final String description; // TODO: Add index.
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
        final LinkedList<FractalListener> listeners;
        final TreeSet<String> exclusiveParameters;

        private FractalEntry(Fractal fractal, String...exclusiveParameters) {
            this.fractal = fractal;
            this.listeners = new LinkedList<>();
            this.exclusiveParameters = new TreeSet<>(Arrays.asList(exclusiveParameters));
        }
    }


    public interface FractalListener {
        void fractalModified(Fractal fractal);
    }

    public interface ParameterMapListener {
        void parameterMapModified(FractalProvider src);
    }
}
