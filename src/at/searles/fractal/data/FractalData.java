package at.searles.fractal.data;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class FractalData implements Iterable<String> {

    // FractalData contains the source code.

    public final String source;
    public final Map<String, Parameter> parameters;

    public FractalData(String source, Map<String, Parameter> parameters) {
        this.source = source;
        this.parameters = parameters;
    }

    @Override
    public Iterator<String> iterator() {
        return parameters.keySet().iterator();
    }

    public FractalData copyResetParameter(String id) {
        TreeMap<String, Parameter> newData = new TreeMap<>(parameters);
        newData.remove(id);
        return new FractalData(source, newData);
    }

    public FractalData copySetParameter(String id, ParameterType type, Object value) {
        TreeMap<String, Parameter> newData = new TreeMap<>(parameters);
        newData.put(id, new Parameter(type, value));
        return new FractalData(source, newData);
    }

    public FractalData copySetSource(String newSource) {
        return new FractalData(newSource, parameters);
    }

    public String source() {
        return source;
    }

    public Parameter getParameter(String id) {
        return parameters.get(id);
    }

    public ParameterType getParameterType(String id) {
        Parameter parameter = parameters.get(id);

        return parameter != null ? parameter.type : null;
    }

    public Object getValue(String id) {
        Parameter parameter = parameters.get(id);

        return parameter != null ? parameter.value : null;
    }

    public static class Parameter {
        public final ParameterType type;
        public final Object value;

        public Parameter(ParameterType type, Object value) {
            this.type = type;
            this.value = value;
        }
    }
}
