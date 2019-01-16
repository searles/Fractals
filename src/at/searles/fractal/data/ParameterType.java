package at.searles.fractal.data;

import at.searles.fractal.ParserInstance;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;
import at.searles.meelan.MeelanException;
import at.searles.meelan.optree.Tree;
import at.searles.meelan.optree.Vec;
import at.searles.meelan.values.*;

import java.util.List;

public enum ParameterType {
    // XXX in the future, all types should be expr.
    Int("int") {
        @Override
        public Object toValue(Tree tree) {
            if(tree instanceof Int) {
                return ((Int) tree).value();
            }

            throw new MeelanException("not an int", tree);
        }

        @Override
        public Tree toTree(Object value) {
            if(!(value instanceof Number)) {
                throw new TypeCastException(this, value);
            }

            return new Int(((Number) value).intValue());
        }

        @Override
        public boolean isInstance(Object value) {
            return value instanceof Number;
        }
    },
    Real("real") {
        @Override
        public Double toValue(Tree tree) {
            if(tree instanceof Int) {
                return (double) ((Int) tree).value();
            }

            if(tree instanceof Real) {
                return ((Real) tree).value();
            }

            throw new MeelanException("not a real", tree);
        }

        @Override
        public Tree toTree(Object value) {
            if(!(value instanceof Number)) {
                throw new TypeCastException(this, value);
            }

            return new Real(((Number) value).doubleValue());
        }

        @Override
        public boolean isInstance(Object value) {
            return value instanceof Number;
        }
    },
    Cplx("cplx") {
        @Override
        public Object toValue(Tree tree) {
            if(tree instanceof CplxVal) {
                return ((CplxVal) tree).value();
            }

            if(tree instanceof Real) {
                return new Cplx(((Real) tree).value(), 0);
            }

            if(tree instanceof Int) {
                return new Cplx(((Int) tree).value(), 0);
            }

            throw new MeelanException("not a cplx", tree);
        }

        @Override
        public Tree toTree(Object value) {
            if(value instanceof Number) {
                return new CplxVal(new Cplx(((Number) value).doubleValue()));
            }

            if(!(value instanceof Cplx)) {
                throw new TypeCastException(this, value);
            }

            return new CplxVal((Cplx) value);
        }

        @Override
        public boolean isInstance(Object value) {
            return value instanceof Cplx || value instanceof Number;
        }
    },
    Bool("bool") {
        @Override
        public Object toValue(Tree tree) {
            if(tree instanceof Bool) {
                return ((Bool) tree).value();
            }

            throw new MeelanException("not a bool", tree);
        }

        @Override
        public Tree toTree(Object value) {
            if(!isInstance(value)) {
                throw new TypeCastException(this, value);
            }

            return new Bool((Boolean) value);
        }

        @Override
        public boolean isInstance(Object value) {
            return value instanceof Boolean;
        }
    },
    Expr("expr") {
        @Override
        public Object toValue(Tree tree) {
            // returns a string
            if(tree instanceof StringVal) {
                return ((StringVal) tree).value();
            }

            throw new MeelanException("not a string", tree);
        }

        @Override
        public Tree toTree(Object value) {
            // kinda joker
            return ParserInstance.get().parseExpr(value.toString());
        }

        @Override
        public boolean isInstance(Object value) {
            return value instanceof String;
        }
    },
    Color("color") {
        @Override
        public Object toValue(Tree tree) {
            return Int.toValue(tree);
        }

        @Override
        public Tree toTree(Object value) {
            if(!isInstance(value)) {
                throw new TypeCastException(this, value);
            }

            return new Int(((Number) value).intValue());
        }

        @Override
        public boolean isInstance(Object value) {
            return value instanceof Number;
        }
    },
    Palette("palette") {
        @Override
        public Palette toValue(Tree tree) {
            if(!(tree instanceof Vec)) {
                throw new MeelanException("not a palette", tree);
            }

            List<Tree> rows = ((Vec) tree).values();

            if((rows.isEmpty() || rows.stream().filter(row -> !(row instanceof Vec)).findAny().isPresent())) {
                throw new MeelanException("not a palette", tree);
            }

            int height = rows.size();
            int width = rows.stream()
                    .mapToInt(row -> ((Vec) row).values().size())
                    .max()
                    .orElseGet(() -> 1);

            // height must not be 0, width may be (it is filled up with black)

            int[] colors = new int[width * height];

            int index = 0;

            for(Tree row : rows) {
                int[] rowColors = ((Vec) row).values().stream().mapToInt(item -> {
                            if (!(item instanceof Int)) {
                                throw new MeelanException("not an integer", item);
                            }

                            return ((Int) item).value();
                        }
                ).toArray();

                for (int i = 0; i < width; ++i) {
                    colors[index++] = rowColors[i % rowColors.length];
                }
            }

            return new Palette(width, height, colors);
        }

        @Override
        public boolean isInstance(Object value) {
            return value instanceof Palette;
        }

        @Override
        public Tree toTree(Object value) {
            throw new IllegalArgumentException("prevent this to correctly handle palettes");
        }
    },
    Scale("scale") {
        @Override
        public Object toValue(Tree tree) {
            if(!(tree instanceof Vec)) {
                throw new MeelanException("not a scale", tree);
            }

            Vec vec = (Vec) tree;

            if(vec.size() != 6) {
                throw new MeelanException("not a scale", tree);
            }

            double values[] = new double[6];

            int index = 0;
            for(Tree arg : vec.values()) {
                values[index++] = (Double) Real.toValue(arg);
            }

            return new Scale(values[0], values[1], values[2], values[3], values[4], values[5]);
        }

        @Override
        public Tree toTree(Object value) {
            throw new IllegalArgumentException("prevent this to correctly handle scales");
        }

        @Override
        public boolean isInstance(Object value) {
            return value instanceof Scale;
        }
    }, Source("source") {
        @Override
        public Object toValue(Tree tree) {
            throw new MeelanException("Cannot use source inside of program. Use expr instead.", tree);
        }

        @Override
        public Tree toTree(Object value) {
            throw new IllegalArgumentException("this is unexpected and clearly a bug. please report");
        }

        @Override
        public boolean isInstance(Object value) {
            return value instanceof String;
        }
    };

    public final String identifier;

    ParameterType(String identifier) {
        this.identifier = identifier;
    }

    public static ParameterType fromString(String s) {
        for (ParameterType t : ParameterType.values()) {
            if (t.identifier.equals(s)) {
                return t;
            }
        }

        return null;
    }

    public abstract Object toValue(Tree tree);

    /**
     * This is not equivalent to the parsed tree!
     */
    public abstract Tree toTree(Object value);

    public abstract boolean isInstance(Object value);
}
