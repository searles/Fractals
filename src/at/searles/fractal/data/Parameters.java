package at.searles.fractal.data;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class Parameters implements Iterable<ParameterKey> {
    private Map<ParameterKey, Object> data;

    public Parameters() {
        this.data = new TreeMap<>();
    }

    public Parameters(Parameters other) {
        this();
        this.data.putAll(other.data);
    }

    public Parameters add(ParameterKey key, Object value) {
        this.data.put(key, value);
        return this;
    }

    public void merge(Parameters other) {
        this.data.putAll(other.data);
    }

    public Object get(ParameterKey key) {
        return data.get(key);
    }

    public boolean contains(ParameterKey key) {
        return data.containsKey(key);
    }

    public boolean remove(ParameterKey key) {
        return data.remove(key) != null;
    }

    @Override
    public Iterator<ParameterKey> iterator() {
        return data.keySet().iterator();
    }
}
