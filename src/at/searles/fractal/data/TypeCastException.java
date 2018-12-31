package at.searles.fractal.data;

public class TypeCastException extends RuntimeException {
    public TypeCastException(ParameterType expected, Object value) {
        super("bad type");
        // fixme more life.
    }
}
