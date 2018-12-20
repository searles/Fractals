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

    private final ArrayList<ParameterEntry> parametersInOrder;

    private final ArrayList<ParameterEntry> sortedParameters;
    private final Comparator<ParameterEntry> cmp;

    /**
     * Parameters that must be set for each fractal. Should be
     * empty if this provider contains only one fractal.
     */
    private final TreeSet<String> exclusiveParameterIds;

    private boolean invalid = true;

    private int keyIndex = -1;

    public FractalProvider() {
        this.fractals = new ArrayList<>(2);

        this.exclusiveParameterIds = new TreeSet<>();

        parametersInOrder = new ArrayList<>(); // must call updateModel once.
        sortedParameters = new ArrayList<>();

        cmp = ((Comparator<ParameterEntry>) (e1, e2) -> e1.key.compareTo(e2.key))
                .thenComparing((e1, e2) -> Integer.compare(e1.owner, e2.owner));

        listeners = new ArrayList<>(2);
    }

    private void validate() {
        if(invalid) {
            invalid = false;

            resetParameterMap();
        }
    }

    private void invalidate() {
        invalid = true;

        for(Listener l : listeners) {
            l.parameterMapUpdated(this);
        }
    }

    private void resetParameterMap() {
        // FIXME overly complicated wrt sources
        // FIXME use key fractal if owner is -1

        parametersInOrder.clear();

        // for all parameters store which fractal uses it.

        if(fractals.isEmpty()) {
            return;
        }

        Map<String, Integer> parameterDegrees = fractals.get(0).createParameterDegrees();

        // This map stores, which owners actually require an exclusive value.
        Map<String, List<Integer>> ownersOfExclusives = new HashMap<>();

        // initialize ownersOfExclusives and add index 0.
        for(String id : exclusiveParameterIds) {
            List<Integer> indices = new LinkedList<>();

            if(parameterDegrees.containsKey(id)) {
                indices.add(0); // is this one an individual parameter?
            }

            ownersOfExclusives.put(id, indices);
        }

        for(int i = 1; i < fractals.size(); ++i) {
            LinkedHashMap<String, Integer> localDegrees = fractals.get(i).createParameterDegrees();
            parameterDegrees.putAll(localDegrees);

            for(Map.Entry<String, List<Integer>> entry : ownersOfExclusives.entrySet()) {
                if(localDegrees.containsKey(entry.getKey())) {
                    entry.getValue().add(i);
                }
            }
        }

        if(parameterDegrees == null) {
            // empty.
            return;
        }

        ArrayList<String> ids = new ArrayList<>(parameterDegrees.keySet());

        // sort ids by degree. For same degree keep order (which is order of owners).
        // each id occurs at most once!
        ids.sort((id1, id2) -> Integer.compare(parameterDegrees.get(id1), parameterDegrees.get(id2)));

        for(String id : ids) {
            List<Integer> owners = ownersOfExclusives.get(id);

            if(owners != null) {
                // there are owners.
                for(Integer owner : owners) {
                    Fractal.Parameter p = fractals.get(owner).getParameter(id);

                    assert p != null;

                    ParameterEntry entry = new ParameterEntry(id, owner, p);
                    parametersInOrder.add(entry);
                }
            } else {
                // no owners. Find first entry.
                Fractal.Parameter p = null;
                for(Fractal fractal : fractals) {
                    p = fractal.getParameter(id);

                    if(p != null) {
                        break;
                    }
                }

                assert p != null;

                ParameterEntry entry = new ParameterEntry(id, -1, p);
                parametersInOrder.add(entry);
            }
        }

        // sort map.
        sortedParameters.clear();
        sortedParameters.addAll(parametersInOrder);

        // sort by key/owner.
        sortedParameters.sort(cmp);
    }

    public ParameterEntry getParameterEntryByIndex(int position) {
        validate();
        return parametersInOrder.get(position);
    }

    /**
     * Returns the parameter with the key and owner. Must be an exact match; there
     * is no owner-magic like in setParameterValue.
     * @param owner -1 if there is no exclusive owner
     */
    private ParameterEntry getParameterEntry(String id, int owner) {
        validate();

        if(!exclusiveParameterIds.contains(id)) {
            owner = -1;
        }

        int position = Collections.binarySearch(sortedParameters, new ParameterEntry(id, owner, null), cmp);
        return sortedParameters.get(position);
    }

    public Object getParameterValue(String id, int owner) {
        ParameterEntry parameterEntry = getParameterEntry(id, owner);

        return parameterEntry != null ? parameterEntry.parameter.value : null;
    }

    /**
     * Sets the parameter. If owner is -1 or an owner for which 'key' is non-exclusive
     * then it is changed for all fractals for which it is non-exclusive.
     */
    public void setParameterValue(String id, int owner, Object value) {
        // FIXME check what happens when source changes
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
            invalidate();
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

        invalidate();

        if(keyIndex < 0) {
            keyIndex = 0;
        }

        return fractals.size() - 1;
    }

    public int fractalCount() {
        return fractals.size();
    }

    public int parameterCount() {
        validate();
        return parametersInOrder.size();
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
     * Removes the key fractal. The key index is set to the
     * fractal ahead if the last fractal was removed.
     * If this was the last fractal, then the key index is invalid (-1)
     * @return the index of the removed fractal.
     */
    public int removeFractal() {
        int removedIndex = keyIndex;

        fractals.remove(keyIndex);

        if(keyIndex == fractalCount()) {
            keyIndex--;
        }

        invalidate(); // something would definitely have changed.

        return removedIndex;
    }

    /**
     * Sets the data of the current key fractal.
     * @param data
     */
    public void setKeyFractal(FractalData data) {
        fractals.get(keyIndex).setData(data);
        invalidate();
    }

    public void setKeyIndex(int newKeyIndex) {
        if(newKeyIndex < 0 || fractalCount() <= newKeyIndex) {
            throw new IllegalArgumentException("out of range");
        }

        this.keyIndex = newKeyIndex;

        invalidate();
    }

    public int keyIndex() {
        return keyIndex;
    }

    // Handle exclusive parameters
    public Iterator<String> exclusiveParameters() {
        return exclusiveParameterIds.iterator();
    }

    public boolean isExclusiveParameter(String id) {
        return exclusiveParameterIds.contains(id);
    }

    public void removeExclusiveParameter(String id) {
        if(this.exclusiveParameterIds.remove(id)) {
            invalidate();
        }
    }

    public void addExclusiveParameter(String id) {
        if(this.exclusiveParameterIds.add(id)) {
            invalidate();
        }
    }

    public static class ParameterEntry {
        public final String key;
        public final int owner;
        public final Fractal.Parameter parameter;

        ParameterEntry(String key, int owner, Fractal.Parameter parameter) {
            this.key = key;
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
