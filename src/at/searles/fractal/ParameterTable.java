package at.searles.fractal;

import at.searles.meelan.optree.inlined.ExternDeclaration;

import java.util.*;

public class ParameterTable {

    private final FractalProvider provider;
    private final ArrayList<FractalProvider.ParameterEntry> parametersInOrder;

    private final Map<String, FractalProvider.ParameterEntry> sharedParameters;
    private final Map<String, FractalProvider.ParameterEntry[]> unsharedParameters;
    private boolean invalid;

    public ParameterTable(FractalProvider provider) {
        this.provider = provider;
        this.parametersInOrder = new ArrayList<>(128);

        sharedParameters = new HashMap<>();
        unsharedParameters = new HashMap<>();

        this.invalid = true;
    }

    // ============ API ============

    public FractalProvider.ParameterEntry get(String id, int index) {
        update();

        if(provider.isSharedParameter(id)) {
            return sharedParameters.get(id);
        }

        FractalProvider.ParameterEntry[] entries = unsharedParameters.get(id);
        return entries != null ? entries[index] : null;
    }

    public FractalProvider.ParameterEntry get(int position) {
        update();

        return parametersInOrder.get(position);
    }

    public int count() {
        update();

        return parametersInOrder.size();
    }

    public void invalidate() {
        this.invalid = true;
    }

    // ===================== private =================

    private void update() {
        if(invalid) {
            addRequiredParameterEntries();
            orderByExternDefinitions();
            groupParameters();

            invalid = false;
        }
    }

    /**
     * Returns all fractals in order except for the
     * key fractal which comes first.
     * Example: keyIndex == 2, there are 6 fractals, order
     * is 2,0,1,3,4,5
     */
    private int fractalIndex(int i) {
        if(i == 0) {
            return provider.keyIndex();
        } else if(i <= provider.keyIndex()) {
            return i - 1;
        } else {
            return i;
        }
    }

    /**
     * Returns all required (=value is actually used and not just in
     * the source code) parameters that should be shown in order
     * of fractals (keyfractal is first) and traversal.
     */
    private void addRequiredParameterEntries() {
        parametersInOrder.clear();

        for(int i = 0; i < provider.fractalCount(); ++i) {
            int fractalIndex = fractalIndex(i);

            for(Fractal.Parameter p : provider.getFractal(fractalIndex).requiredParameters()) { // those that are actually in use; including source/scale
                FractalProvider.ParameterEntry entry = new FractalProvider.ParameterEntry(p.id, fractalIndex, p);
                parametersInOrder.add(entry);
            }
        }
    }

    private Collection<String> createExternsOrder() {
        ArrayList<String> list = new ArrayList<>();
        ArrayList<String> tmp = new ArrayList<>();

        list.add(Fractal.SOURCE_LABEL);
        list.add(Fractal.SCALE_LABEL);

        for(int i = 0; i < provider.fractalCount(); ++i) {
            int fractalIndex = fractalIndex(i);

            for(ExternDeclaration extern : provider.getFractal(fractalIndex).externDeclarations()) {
                int pos = list.indexOf(extern.id);

                if(pos != -1) {
                    // add before extern.
                    list.addAll(pos, tmp);
                    tmp.clear();
                } else {
                    // does not exist yet.
                    tmp.add(extern.id);
                }
            }

            list.addAll(tmp);
            tmp.clear();
        }

        LinkedHashSet<String> externsOrder = new LinkedHashSet<>();
        externsOrder.addAll(list);

        return externsOrder;
    }

    private void orderByExternDefinitions() {
        // Step 1: Build up externs-order
        Collection<String> externsOrder = createExternsOrder();

        int pos = 0;

        for(String id : externsOrder) {
            for(int i = 0; i < provider.fractalCount(); ++i) {
                int fractalIndex = fractalIndex(i);

                int startRange = pos; // things ahead were already sorted.

                // find parameter
                while(startRange < parametersInOrder.size()) {
                    FractalProvider.ParameterEntry p = parametersInOrder.get(startRange);
                    if(p.id.equals(id) && p.owner == fractalIndex) {
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
                        !externsOrder.contains(parametersInOrder.get(endRange).id)) {
                    endRange++;
                }

                moveRangeTo(startRange, endRange, pos);
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
    private void moveRangeTo(int startRange, int endRange, int pos) {
        if(startRange == pos) {
            // nothing to do.
            return;
        }

        // pos is always ahead of startRange
        reverse(pos, endRange);
        reverse(pos, pos + endRange - startRange);
        reverse(pos + endRange - startRange, endRange);
        // this is just beautiful...
    }

    /**
     * Reverses range start (incl) to end (excl) in parametersInOrder
     */
    private void reverse(int start, int end) {
        for(int l = start, r = end - 1; l < r; ++l, r--) {
            FractalProvider.ParameterEntry tmp = parametersInOrder.get(l);
            parametersInOrder.set(l, parametersInOrder.get(r));
            parametersInOrder.set(r, tmp);
        }
    }

    /**
     * Adds the first entry for shared parameters.
     */
    private void groupParameters() {
        sharedParameters.clear();
        unsharedParameters.clear();

        // use only one entry for shared parameters
        Set<String> markSharedParameters = new HashSet<>();

        for (int i = 0; i < parametersInOrder.size(); ++i) {
            String id = parametersInOrder.get(i).id;
            if (markSharedParameters.contains(id)) {
                // fixme here I should check the parameter type?
                parametersInOrder.remove(i);
                i--;
            } else {
                FractalProvider.ParameterEntry entry = parametersInOrder.get(i);
                if (provider.isSharedParameter(id)) {
                    markSharedParameters.add(id);
                    sharedParameters.put(id, entry);
                } else {
                    FractalProvider.ParameterEntry[] entries = unsharedParameters.get(id);

                    if (entries == null) {
                        entries = new FractalProvider.ParameterEntry[provider.fractalCount()];
                        unsharedParameters.put(id, entries);
                    }

                    entries[entry.owner] = entry;
                }
            }
        }
    }
}