package at.searles.fractal;

import at.searles.fractal.data.FractalData;

import java.util.*;

/**
 * There is one source file for a fractal provider. It is compiled and that way,
 * the instance of ExternData in the fractalprovider is filled.
 */
public class FractalProvider {

    private final ArrayList<Fractal> fractals;
    private final ArrayList<Listener> listeners;

    private final ParameterTable parameterTable;

    /**
     * Parameters that must be set for each fractal. Should be
     * empty if this provider contains only one fractal.
     */
    private final TreeSet<String> exclusiveParameterIds;

    private int keyIndex = -1;

    public FractalProvider() {
        this.fractals = new ArrayList<>(2);
        this.exclusiveParameterIds = new TreeSet<>();
        listeners = new ArrayList<>(2);
        this.parameterTable = new ParameterTable(this);
    }

    private void fireParametersUpdated() {
        parameterTable.invalidate();
        for(Listener l : listeners) {
            l.parameterMapUpdated(this);
        }
    }

    public ParameterEntry getParameterEntryByIndex(int position) {
        return parameterTable.get(position);
    }

    /**
     * Returns the parameter with the key and owner. If it is a
     * shared parameter, the shared visible value is returned. {@code owner}
     * is ignored in this case. returns {@code null} if
     * it does not exist.
     */
    public ParameterEntry getParameterEntry(String id, int owner) {
        return parameterTable.get(id, owner);
    }

    /**
     * Convenience; returns null if getParameterEntry returns null.
     */
    public Object getParameterValue(String id, int owner) {
        ParameterEntry parameterEntry = getParameterEntry(id, owner);

        return parameterEntry != null ? parameterEntry.parameter.value : null;
    }

    /**
     * Sets the parameter. If parameter is shared, parameter is set in
     * all fractals. The value is set directly in all fractals.
     */
    public void setParameterValue(String id, int owner, Object value) {
        boolean somethingChanged = false;

        if(exclusiveParameterIds.contains(id)) {
            Fractal fractal = fractals.get(owner);
            somethingChanged = fractal.setValue(id, value);
        } else {
            for (Fractal fractal : fractals) {
                somethingChanged |= fractal.setValue(id, value);
            }
        }

        if(somethingChanged) {
            fireParametersUpdated();
        }
    }

    /**
     * Adds a new fractal to the end of the list. If there
     * was no fractal before, then the key-index is set to 0.
     * @param fractalData The data of the new fractal
     * @return The index.
     */
    public int addFractal(FractalData fractalData) {
        Fractal fractal = Fractal.fromData(fractalData);
        fractal.compile();

        fractals.add(fractal);

        if(keyIndex < 0) {
            keyIndex = 0;
        }

        fireParametersUpdated();

        return fractals.size() - 1;
    }

    /**
     * Removes the key fractal. The key index is set to the
     * fractal ahead if the last fractal was removed.
     * If this was the last fractal, then the key index is invalid (-1)
     * @return the index of the removed fractal.
     */
    public int removeFractal() {
        int removedIndex = keyIndex;

        fractals.remove(keyIndex);

        if(keyIndex > 0 || fractals.size() == 0) {
            keyIndex--;
        }

        fireParametersUpdated();
        return removedIndex;
    }

    public int fractalCount() {
        return fractals.size();
    }

    public int parameterCount() {
        return parameterTable.count();
    }

    public Fractal getFractal(int index) {
        return fractals.get(index);
    }

    public void addListener(Listener l) {
        listeners.add(l);
    }

    public boolean removeListener(Listener l) {
        return listeners.remove(l);
    }

    /**
     * Sets the data of the current key fractal.
     * @param data
     */
    public void setKeyFractal(FractalData data) {
        fractals.get(keyIndex).setData(data);
        fireParametersUpdated();
    }

    public void setKeyIndex(int newKeyIndex) {
        if(newKeyIndex < 0 || fractalCount() <= newKeyIndex) {
            throw new IllegalArgumentException("out of range");
        }

        this.keyIndex = newKeyIndex;
        fireParametersUpdated();
    }

    public int keyIndex() {
        return keyIndex;
    }

    // Handle exclusive parameters
    public Iterator<String> exclusiveParameters() {
        return exclusiveParameterIds.iterator();
    }

    public boolean isSharedParameter(String id) {
        return !exclusiveParameterIds.contains(id);
    }

    public void removeExclusiveParameter(String id) {
        if(this.exclusiveParameterIds.remove(id)) {
            fireParametersUpdated();
        }
    }

    public void addExclusiveParameter(String id) {
        if(this.exclusiveParameterIds.add(id)) {
            fireParametersUpdated();
        }
    }

    public static class ParameterEntry {
        public final String id;
        public final int owner;
        public final Fractal.Parameter parameter;

        ParameterEntry(String id, int owner, Fractal.Parameter parameter) {
            this.id = id;
            this.owner = owner;
            this.parameter = parameter;
        }
    }

    public interface Listener {
        /**
         * Called if parameters were modified.
         * This includes that there are new parameter values
         * or parameters were added. Fractals that are
         * owned by this provider are informed individually
         * via the FractalListener if the modified parameter
         * affects them.
         */
        void parameterMapUpdated(FractalProvider src);
    }
}
