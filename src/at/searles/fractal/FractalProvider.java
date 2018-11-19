package at.searles.fractal;

import at.searles.fractal.data.FractalData;

import java.util.*;

/**
 * There is one source file for a fractal provider. It is compiled and that way,
 * the instance of ExternData in the fractalprovider is filled.
 */
public class FractalProvider {

    private final ArrayList<Fractal> fractals;
    private final ArrayList<ParameterMapListener> parameterMapListeners;

    private final ArrayList<ParameterEntry> parametersInOrder;

    private final ArrayList<ParameterEntry> sortedParameters;
    private final Comparator<ParameterEntry> cmp;
            ;

    /**
     * Parameters that must be set for each fractal. Should be
     * empty if this provider contains only one fractal.
     */
    private final TreeSet<String> exclusiveParameterIds;

    public FractalProvider() {
        this.fractals = new ArrayList<>(2);

        this.exclusiveParameterIds = new TreeSet<>();

        parametersInOrder = new ArrayList<>(); // must call updateModel once.
        sortedParameters = new ArrayList<>();

        cmp = ((Comparator<ParameterEntry>) (e1, e2) -> e1.key.compareTo(e2.key))
                .thenComparing((e1, e2) -> Integer.compare(e1.owner, e2.owner));

        parameterMapListeners = new ArrayList<>(2);
    }

    /**
     * Returns the parameter with the key and owner
     * @param owner -1 if there is no exclusive owner
     */
    public ParameterEntry getParameterEntry(String id, int owner) {
        int position = Collections.binarySearch(sortedParameters, new ParameterEntry(id, owner, null), cmp);
        return sortedParameters.get(position);
    }

    public ParameterEntry getParameterEntryByIndex(int position) {
        return parametersInOrder.get(position);
    }


    /**
     * Sets the parameter. If owner is -1 or an owner for which 'key' is non-exclusive
     * then it is changed for all fractals for which it is non-exclusive.
     */
    public void setParameterValue(String id, int owner, Object value) {
        boolean somethingChanged = false;
        if(exclusiveParameterIds.contains(id) && owner != -1) {
            Fractal fractal = fractals.get(owner);
            somethingChanged = fractal.setValue(id, value);
        } else {
            for (Fractal fractal : fractals) {
                somethingChanged |= fractal.setValue(id, value);
            }
        }

        if(somethingChanged) {
            updateParameterMap();
        }
    }

    public int addFractal(FractalData fractalData) {
        Fractal fractal = Fractal.fromData(fractalData);
        fractal.compile();

        fractals.add(fractal);

        updateParameterMap();

        return fractals.size() - 1;
    }

//    public int setFractal(int index, FractalData fractalData, String...exclusiveParameters) {
//        Fractal fractal = Fractal.fromData(fractalData);
//
//        Fractal entry = new Fractal(fractal, exclusiveParameters);
//        entry.listeners.addAll(fractals.get(0).listeners);
//
//        fractals.set(index, entry);
//
//        handleFractalChanged(index); // FIXME because does it work for multiple ones?
//        updateParameterMap();
//
//        return index;
//    }

    public int fractalCount() {
        return fractals.size();
    }

    public int parameterCount() {
        return parametersInOrder.size();
    }

    public Fractal getFractal(int index) {
        return fractals.get(index);
    }

    public void removeFractal(int index) {
        fractals.remove(index);
        updateParameterMap();
    }

    public void addParameterMapListener(ParameterMapListener l) {
        parameterMapListeners.add(l);
    }

    public boolean removeParameterMapListener(ParameterMapListener l) {
        return parameterMapListeners.remove(l);
    }

    public void addExclusiveParameterId(String id) {
        if(this.exclusiveParameterIds.add(id)) {
            updateParameterMap();
        }
    }

    private void updateParameterMap() {
        parametersInOrder.clear();

        // for all parameters store which fractal uses it.

        if(fractals.isEmpty()) {
            return;
        }

        Map<String, Integer> parameterDegrees = fractals.get(0).createParameterDegrees();
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

        for(ParameterMapListener l : parameterMapListeners) {
            l.parameterMapModified(this);
        }
    }

    public static class ParameterEntry {
        public final String key;
        public final int owner;
        public final Fractal.Parameter parameter;

        public ParameterEntry(String key, int owner, Fractal.Parameter parameter) {
            this.key = key;
            this.owner = owner;
            this.parameter = parameter;
        }
    }

    public interface ParameterMapListener {
        void parameterMapModified(FractalProvider src);
    }
}
