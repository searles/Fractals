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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ParameterKey that = (ParameterKey) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        return type == that.type;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ParameterKey{" +
                "id='" + id + '\'' +
                ", type=" + type +
                '}';
    }
}
