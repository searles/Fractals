package at.searles.fractal.data;

public class ParameterKey implements Comparable<ParameterKey> {

    public final String id;
    public final ParameterType type;

    public ParameterKey(String id, ParameterType type) {
        this.id = id;
        this.type = type;
    }

    @Override
    public int compareTo(ParameterKey other) {
        int cmp = id.compareTo(other.id);
        return cmp != 0 ? cmp : type.compareTo(other.type);
    }
}
