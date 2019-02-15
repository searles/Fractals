package at.searles.fractal;

import at.searles.meelan.optree.inlined.ExternDeclaration;

import java.util.*;

public class ParameterTable {

    private final ArrayList<Entry> parametersInOrder;

    private final Map<String, Entry> sharedParameters;
    private final Map<String, Map<Integer, Entry>> unsharedParameters;

    public ParameterTable() {
        this.parametersInOrder = new ArrayList<>(128);
        this.sharedParameters = new HashMap<>();
        this.unsharedParameters = new HashMap<>();
    }

    public ParameterTable init(FractalCollection collection, Set<String> exclusiveParameterIds) {
        this.parametersInOrder.clear();
        this.sharedParameters.clear();
        this.unsharedParameters.clear();

        orderByExternDefinitions(collection);
        groupParameters(exclusiveParameterIds);

        return this;
    }

    public Entry get(String key, int id) {
        Entry entry = sharedParameters.get(key);

        if(entry != null) {
            // it is shared
            return entry;
        }

        Map<Integer, Entry> entries = unsharedParameters.get(key);
        return entries != null ? entries.get(id) : null;
    }

    public Entry get(int position) {
        return parametersInOrder.get(position);
    }

    public int count() {
        return parametersInOrder.size();
    }

    // From here algorithms for setting up the ds.

    /**
     * Returns all required (=value is actually used and not just declared in
     * the source code) parameters that should be shown in order
     * of fractals (keyfractal is first) and traversal.
     */
    private void requiredParameterEntries(FractalCollection collection) {
        for(Integer id : collection.ids()) {
            for(Fractal.Parameter p : collection.get(id).requiredParameters()) { // those that are actually in use; including source/scale
                Entry entry = new Entry(p.id, id, p);
                parametersInOrder.add(entry);
            }
        }
    }

    private static LinkedHashSet<String> createExternsOrder(FractalCollection collection) {
        ArrayList<String> allExterns = new ArrayList<>();
        ArrayList<String> localExterns = new ArrayList<>();

        allExterns.add(Fractal.SOURCE_LABEL);
        allExterns.add(Fractal.SCALE_LABEL);

        for(Fractal fractal : collection.fractals()) {
            for(ExternDeclaration extern: fractal.externDeclarations()) {
                int pos = allExterns.indexOf(extern.id);

                if(pos != -1) {
                    // add before extern.
                    allExterns.addAll(pos, localExterns);
                    localExterns.clear();
                } else {
                    // does not exist yet.
                    localExterns.add(extern.id);
                }
            }

            allExterns.addAll(localExterns);
            localExterns.clear();
        }

        LinkedHashSet<String> externsOrder = new LinkedHashSet<>();
        externsOrder.addAll(allExterns);

        return externsOrder;
    }

    private void orderByExternDefinitions(FractalCollection collection) {
        // Step 1: Build up externs-order
        requiredParameterEntries(collection);
        Set<String> externsOrder = createExternsOrder(collection);

        int pos = 0;

        for(String key : externsOrder) {
            for(Integer id : collection.ids()) {
                int startRange = pos; // things ahead were already sorted.

                // find parameter
                while(startRange < parametersInOrder.size()) {
                    Entry p = parametersInOrder.get(startRange);
                    if(p.key.equals(key) && id.equals(p.owner)) {
                        break;
                    }

                    startRange++;
                }

                if (startRange >= parametersInOrder.size()) {
                    // not found.
                    continue;
                }

                // yes found. Find all dependants.

                int endRange = startRange + 1;

                while(endRange < parametersInOrder.size() &&
                        !externsOrder.contains(parametersInOrder.get(endRange).key)) {
                    endRange++;
                }

                moveRangeTo(startRange, endRange, pos, parametersInOrder);
                pos += endRange - startRange;
            }
        }
    }

    /**
     * Moves entries in parametersInOrder from startRange/endRange(excl)
     * to pos.
     * eg [a,b,c,d,e].moveRangeTo(2,4,1) results in
     * [a,c,d,b,e]. pos is assumed to be ahead of startRange.
     */
    private static <A> void moveRangeTo(int startRange, int endRange, int pos, List<A> list) {
        if(startRange == pos) {
            // nothing to do.
            return;
        }

        // pos is always ahead of startRange
        reverse(pos, endRange, list);
        reverse(pos, pos + endRange - startRange, list);
        reverse(pos + endRange - startRange, endRange, list);
        // this is just beautiful...
    }

    /**
     * Reverses range start (incl) to end (excl) in parametersInOrder
     */
    private static <A> void reverse(int start, int end, List<A> list) {
        for(int l = start, r = end - 1; l < r; ++l, r--) {
            A tmp = list.get(l);
            list.set(l, list.get(r));
            list.set(r, tmp);
        }
    }

    /**
     * Adds the first entry for shared parameters.
     */
    private void groupParameters(Set<String> exclusiveParameterIds) {
        // use only one entry for shared parameters
        Set<String> markSharedParameters = new HashSet<>();

        for (int i = 0; i < parametersInOrder.size(); ++i) {
            String key = parametersInOrder.get(i).key;
            if (markSharedParameters.contains(key)) {
                // XXX should I check the parameter type?
                parametersInOrder.remove(i);
                i--;
            } else {
                Entry entry = parametersInOrder.get(i);
                if (!exclusiveParameterIds.contains(key)) {
                    markSharedParameters.add(key);
                    sharedParameters.put(key, entry);
                } else {
                    Map<Integer, Entry> entries = unsharedParameters.get(key);

                    if (entries == null) {
                        entries = new TreeMap<>();
                        unsharedParameters.put(key, entries);
                    }

                    entries.put(entry.owner, entry);
                }
            }
        }
    }

    public static class Entry {
        public final String key;
        public final int owner;
        public final Fractal.Parameter parameter;

        Entry(String key, int owner, Fractal.Parameter parameter) {
            this.key = key;
            this.owner = owner;
            this.parameter = parameter;
        }
    }

}