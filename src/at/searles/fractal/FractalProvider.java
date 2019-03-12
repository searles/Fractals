package at.searles.fractal;

import at.searles.fractal.data.FractalData;

import java.util.ArrayList;
import java.util.TreeSet;

/**
 * There is one source file for a fractal provider. It is compiled and that way,
 * the instance of ExternData in the fractalprovider is filled.
 */
public class FractalProvider {

    private final FractalCollection collection;
    private final TreeSet<String> exclusiveParameters;

    private transient final ArrayList<Listener> listeners;

    private transient ParameterTable table;
    private transient boolean isTableValid;

    public FractalProvider() {
        this.collection = new FractalCollection();
        this.exclusiveParameters = new TreeSet<>();
        listeners = new ArrayList<>(2);
        table = new ParameterTable();
        isTableValid = false;
    }

    private ParameterTable table() {
        if(!isTableValid) {
            table.init(collection, exclusiveParameters);
            isTableValid = true;
        }

        return table;
    }

    private void invalidateTable() {
        isTableValid = false;
    }


    public boolean updateParameterValue(String key, int id, Object value) {
        if(!collection.get(id).updateValue(key, value)) {
            return false;
        }

        invalidateTable();
        return true;
    }

    public boolean updateAllParameterValue(String key, Object value) {
        boolean status = false;
        for(Fractal fractal : collection.fractals()) {
            status |= fractal.updateValue(key, value);
        }

        if(status) {
            invalidateTable();
        }

        return status;
    }


    // === Handle parameters ===

    private void fireParametersUpdated() {
        for(Listener l : listeners) {
            l.parameterMapUpdated(this);
        }
    }

    /**
     * Uses the position of the parameter.
     */
    public ParameterTable.Entry getParameterEntryByIndex(int position) {
        return table().get(position);
    }

    /**
     * Returns the parameter with the key and owner. If it is a
     * shared parameter, the shared visible value is returned. {@code owner}
     * is ignored in this case.
     * @return returns {@code null} if
     * it does not exist.
     */
    public ParameterTable.Entry getParameterEntry(String id, int owner) {
        return table().get(id, owner);
    }

    /**
     * Convenience; returns null if getParameterEntry returns null.
     */
    public Object getParameterValue(String id, int owner) {
        ParameterTable.Entry parameterEntry = getParameterEntry(id, owner);

        return parameterEntry != null ? parameterEntry.parameter.value : null;
    }

    private boolean setIndividualParameterValue(String key, int id, Object value) {
        boolean updated = collection.get(id).updateValue(key, value);

        if(updated) {
            invalidateTable();
        }

        return updated;
    }

    /**
     * Sets the parameter. If parameter is shared, parameter is set in
     * all fractals. The value is set directly in all fractals.
     */
    public void setParameterValue(String key, int id, Object value) {
        boolean updated = false;

        if(exclusiveParameters.contains(key)) {
            updated = setIndividualParameterValue(key, id, value);
        } else {
            // for all ids
            for(Integer i : collection.ids()) {
                updated |= setIndividualParameterValue(key, i, value);
            }
        }

        if(updated) {
            fireParametersUpdated();
        }
    }

    // === Add/Remove Fractals ===

    /**
     * Adds a new fractal to the end of the list. If there
     * was no fractal before, then the key-index is set to 0.
     * @param fractalData The data of the new fractal
     * @return The id.
     */
    public int addFractal(FractalData fractalData) {
        Fractal fractal = Fractal.fromData(fractalData);
        int id = collection.add(fractal);

        invalidateTable();
        fireParametersUpdated();

        return id;
    }

    /**
     * Removes the key fractal. The key index is set to the
     * fractal ahead if the last fractal was removed.
     * If this was the last fractal, then the key index is invalid (-1)
     * @return the index of the removed fractal.
     */
    public boolean removeFractal(int id) {
        boolean status = collection.remove(id);
        invalidateTable();
        fireParametersUpdated();
        return status;
    }

    // === Getters ===

    public int fractalCount() {
        return collection.size();
    }

    public int parameterCount() {
        return table().count();
    }

    public Fractal getFractal(int id) {
        return collection.get(id);
    }

    // === Handle listeners ===

    public void addListener(Listener l) {
        listeners.add(l);
    }

    public boolean removeListener(Listener l) {
        return listeners.remove(l);
    }

    // === History ===

    // FIXME history should be moved.

    public boolean historyForward(int id) {
        if(getFractal(id).historyForward()) {
            fireParametersUpdated();
            return true;
        }

        return false;
    }

    public boolean historyBack(int id) {
        if(getFractal(id).historyBack()) {
            fireParametersUpdated();
            return true;
        }

        return false;
    }

    // === Key Fractals ===

    /**
     * Sets the data of the current key fractal.
     */
    public void setFractal(int ownerId, FractalData data) {
        collection.get(ownerId).setData(data, true, false);
        fireParametersUpdated();
    }

    // === Handle exclusive parameters ===

    public boolean isSharedParameter(String id) {
        return !exclusiveParameters.contains(id);
    }

    public void removeExclusiveParameter(String id) {
        if(exclusiveParameters.remove(id)) {
            invalidateTable();
            fireParametersUpdated();
        }
    }

    public void addExclusiveParameter(String id) {
        if(exclusiveParameters.add(id)) {
            invalidateTable();
            fireParametersUpdated();
        }
    }

    public void setKeyId(int keyId) {
        collection.setKeyId(keyId);
        invalidateTable();
    }

    public int keyId() {
        return collection.keyId();
    }

    public Iterable<Integer> fractalIds() {
        return collection.ids();
    }

    public Iterable<String> exclusiveParameters() {
        return exclusiveParameters;
    }

    // === Internal data structures ===

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
