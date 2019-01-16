package at.searles.fractal.data;

import at.searles.fractal.Fractal;
import at.searles.fractal.ParserInstance;
import at.searles.meelan.MeelanException;
import at.searles.meelan.compiler.Ast;
import at.searles.meelan.optree.inlined.ExternDeclaration;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;

public class FractalData implements Iterable<String> {

    // FractalData contains the source code.

    private final String source;
    private final Map<String, Object> parameters;
    private final Map<String, ExternDeclaration> externDecls;
    private final Ast ast;

    private FractalData(String source, Ast ast, Map<String, ExternDeclaration> externDecls, Map<String, Object> parameters) {
        this.source = source;
        this.ast = ast;
        this.externDecls = externDecls;
        this.parameters = parameters;
    }

    @Override
    public Iterator<String> iterator() {
        return parameters.keySet().iterator();
    }

    public FractalData copyResetParameter(String id) {
        // no need for a builder.
        if(!parameters.containsKey(id)) {
            return this;
        }

        Map<String, Object> newParameters = new TreeMap<>();
        newParameters.putAll(parameters);
        newParameters.remove(id);

        return new FractalData(source, ast, externDecls, newParameters);
    }

    public FractalData copySetParameter(String id, Object value) {
        ParameterType type = queryType(id);

        if(!type.isInstance(value)) {
            return this;
        }

        Map<String, Object> newParameters = new TreeMap<>();
        newParameters.putAll(parameters);
        newParameters.put(id, value);

        return new FractalData(source, ast, externDecls, newParameters);
    }

    public FractalData copySetSource(String newSource) {
        Builder builder = new Builder();

        builder.setSource(newSource);

        parameters.forEach(builder::addParameter);
        return builder.commit();
    }

    public String source() {
        return source;
    }

    public ParameterType queryType(String id) {
        return queryType(id, externDecls);
    }

    public Object getValue(String id) {
        return parameters.get(id);
    }

    private static ParameterType queryType(String id, Map<String, ExternDeclaration> externDecls) {
        if(id.equals(Fractal.SCALE_LABEL)) {
            return ParameterType.Scale;
        }

        if(id.equals(Fractal.SOURCE_LABEL)) {
            throw new IllegalArgumentException("source must be handled in a different way");
        }

        ExternDeclaration decl = externDecls.get(id);

        if(decl == null) {
            return ParameterType.Expr;
        }

        ParameterType type = ParameterType.fromString(decl.externTypeString);

        if(type == null) {
            throw new MeelanException("No such type", decl);
        }

        return type;
    }

    public void forEachParameter(BiConsumer<String, Object> consumer) {
        parameters.forEach(consumer);
    }

    public Map<String, ExternDeclaration> externDecls() {
        return externDecls;
    }

    public Ast ast() {
        return ast;
    }

    public static class Builder {

        private String source;
        private Ast ast;
        private Map<String, ExternDeclaration> externDecls;

        // All parameters in here are non-default.
        private Map<String, Object> parameters;

        public Builder setSource(String source) throws MeelanException {
            if(this.source != null) {
                throw new IllegalArgumentException("source already set");
            }

            this.source = source;
            this.ast = ParserInstance.get().parseSource(source);
            this.externDecls = this.ast.collectExternDecls();
            parameters = new LinkedHashMap<>();

            return this;
        }

        public boolean addParameter(String id, Object value) {
            ParameterType type = queryType(id);

            if(type.isInstance(value)) {
                parameters.put(id, value);
                return true;
            }

            return false;
        }

        public ParameterType queryType(String id) {
            return FractalData.queryType(id, externDecls);
        }

        /**
         * After using this method, do not reuse the builder.
         */
        public FractalData commit() {
            return new FractalData(source, ast, externDecls, parameters);
        }
    }
}
