package at.searles.fractal;

import at.searles.meelan.optree.inlined.ExternDeclaration;

import java.util.ArrayList;
import java.util.Map;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ParameterTable {

    FractalProvider provider;
    ArrayList<FractalProvider.ParameterEntry> parametersInOrder;

    Map<String, FractalProvider.ParameterEntry> sharedParameters;
    Map<String, FractalProvider.ParameterEntry[]> unsharedParameters;


    public void initialize() {
        parametersInOrder.clear();

        addUsedParameters();
        orderByExternDefinitions();
        groupParameters();


    }

    private int fractalIndex(int i) {
        if(i == 0) {
            return provider.keyIndex();
        } else if(i <= provider.keyIndex()) {
            return i - 1;
        } else {
            return i;
        }
    }

    private void addUsedParameters() {
        for(int i = 0; i < provider.fractalCount(); ++i) {
            int fractalIndex = fractalIndex(i);

            for(Fractal.Parameter p : provider.getFractal(fractalIndex).parameters()) { // those that are actually in use; including source/scale
                FractalProvider.ParameterEntry entry = new FractalProvider.ParameterEntry(...);
                parametersInOrder.add(entry);
                // not yet needed put(p.id, fractalIndex, entry); // fixme create entire parameter entry
            }
        }
    }

    private void orderByExternDefinitions() {
        LinkedHashSet<String> parameterIds = new LinkedHashSet<>();
        for(int i = 0; i < provider.fractalCount(); ++i) {
            int fractalIndex = fractalIndex(i);

            for(ExternDeclaration extern : provider.getFractal(fractalIndex).externs()) {
                parameterIds.add(extern.id());
            }
        }

        int pos = 0;

        for(String id : parameterIds) {
            for(int i = 0; i < provider.fractalCount(); ++i) {
                int fractalIndex = fractalIndex(i);

                int startRange = pos; // things ahead were already sorted.

                while(startRange < parametersInOrder.size()) {
                    ParameterEntry p = parametersInOrder.get(startRange);
                    if(p.id.equals(id) && p.index == fractalIndex) {
                        break;
                    }

                    startRange++;
                }

                if (startRange >= parametersInOrder.size()) {
                    // not found.
                    continue;
                }

                int endRange = startRange + 1;

                while(endRange < parametersInOrder.size() && !parameterIds.contains(parametersInOrder.get(endRange).id)) {
                    endRange++;
                }

                moveRangeTo(startRange, endRange, pos);
                pos += endRange - startRange;
            }
        }
    }

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
            ParameterEntry tmp = parametersInOrder.get(l);
            parametersInOrder.set(l, parametersInOrder.get(r));
            parametersInOrder.set(r, tmp);
        }
    }

    private void groupSharedParameters() {
        // use only one entry for shared parameters
        Set<String> usedSharedParameters = new HashSet<String>();

        for(int i = 0; i < parametersInOrder.size(); ++i) {
            String id = get(i).id;
            if(usedSharedParameters.contains(id)) {
                parametersInOrder.remove(i);
                i--;
            } else {
                ParameterEntry entry = parametersInOrder.get(i);
                if(provider.isSharedParameter(id)) {
                    usedSharedParameters.add(id);
                    sharedParameters.put(id, entry);
                } else {
                    ParameterEntry[] entries = unsharedParameters.get(id);

                    if(entries == null) {
                        entries = new ParameterEntry[provider.fractalCount()];
                        unsharedParameters.put(id, entries);
                    }

                    entries[entry.index] = entry;
                }
            }
        }
    }

    public ParameterEntry get(String id, int index) {
        if(provider.isSharedParameter(id)) {
            return sharedParameters.get(id);
        }

        ParameterEntry[] entries = unsharedParameters.get(id);
        return entries != null ? entries[index] : null;
    }

    public ParameterEntry get(int position) {
        return parametersInOrder.get(position);
    }

    public int count() {
        return parametersInOrder.size();
    }

}