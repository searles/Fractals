package at.searles.fractal.data;

import at.searles.fractal.ParserInstance;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;
import at.searles.meelan.MeelanException;
import at.searles.meelan.optree.Tree;
import at.searles.meelan.optree.Vec;
import at.searles.meelan.values.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public enum ParameterType {
    // XXX in the future, all types should be expr.
    Int("int") {
        @Override
        public Object toValue(Tree tree) {
            if(tree instanceof at.searles.meelan.values.Int) {
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
    },
    Real("real") {
        @Override
        public Double toValue(Tree tree) {
            if(tree instanceof at.searles.meelan.values.Int) {
                return (double) ((at.searles.meelan.values.Int) tree).value();
            }

            if(tree instanceof at.searles.meelan.values.Real) {
                return ((at.searles.meelan.values.Real) tree).value();
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
    },
    Cplx("cplx") {
        @Override
        public Object toValue(Tree tree) {
            if(tree instanceof at.searles.meelan.values.Int) {
                return new at.searles.math.Cplx(((at.searles.meelan.values.Int) tree).value());
            }

            if(tree instanceof at.searles.meelan.values.Real) {
                return new at.searles.math.Cplx(((at.searles.meelan.values.Real) tree).value());
            }

            if(tree instanceof at.searles.meelan.values.CplxVal) {
                return ((at.searles.meelan.values.CplxVal) tree).value();
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
    },
    Bool("bool") {
        @Override
        public Object toValue(Tree tree) {
            if(tree instanceof at.searles.meelan.values.Bool) {
                return ((at.searles.meelan.values.Bool) tree).value();
            }

            throw new MeelanException("not a bool", tree);
        }

        @Override
        public Tree toTree(Object value) {
            if(!(value instanceof Boolean)) {
                throw new TypeCastException(this, value);
            }

            return new Bool((Boolean) value);
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
    },
    Color("color") {
        @Override
        public Object toValue(Tree tree) {
            return Int.toValue(tree);
        }

        @Override
        public Tree toTree(Object value) {
            if(!(value instanceof Integer)) {
                throw new TypeCastException(this, value);
            }

            return new Int((Integer) value);
        }
    },
    Palette("palette") {
        List<List<Integer>> toTable(Tree tree) {
            if(!(tree instanceof Vec)) {
                throw new MeelanException("not a table", tree);
            }

            List<Tree> values = ((Vec) tree).values();

            if(values.isEmpty()) {
                throw new MeelanException("palette must not be empty", tree);
            }

            List<List<Integer>> palette = new ArrayList<>(values.size());

            if(!(values.get(0) instanceof Vec)) {
                // [ c, c, c]
                List<Integer> row = toRow(tree);

                for(Integer color : row) {
                    palette.add(Collections.singletonList(color));
                }

                return palette;
            }

            palette.addAll(values.stream().map(this::toRow).collect(Collectors.toList()));

            return palette;
        }

        private List<Integer> toRow(Tree tree) {
            if(tree instanceof at.searles.meelan.values.Int) {
                return Collections.singletonList(toColor(tree));
            } else if(tree instanceof Vec) {
                ArrayList<Integer> row = ((Vec) tree).values().stream().map(this::toColor).collect(Collectors.toCollection(ArrayList::new));

                return row;
            }

            throw new MeelanException("not a palette row", tree);
        }

        private Integer toColor(Tree tree) {
            if(tree instanceof at.searles.meelan.values.Int) {
                return ((at.searles.meelan.values.Int) tree).value();
            }

            throw new MeelanException("", tree);
        }

        @Override
        public Palette toValue(Tree tree) {
            List<List<Integer>> table;

            if(tree instanceof at.searles.meelan.values.Int) {
                table = Collections.singletonList(Collections.singletonList(((at.searles.meelan.values.Int) tree).value()));
            } else {
                table = toTable(tree);
            }

            int height = table.size();

            if(height == 0) {
                return new Palette(1, 1, new int[]{0xff000000});
            }

            int width = 1;

            for(List<Integer> row : table) {
                width = Math.max(row.size(), width);
            }

            int colors[] = new int[height * width];

            int y = 0;

            for(List<Integer> row : table) {
                // if row is empty, use black.
                for(int x = 0; x < width; ++x) {
                    int color = row.size() > 0 ? row.get(x % row.size()) : 0xff000000;

                    colors[y * width + x] = color;
                }

                y++;
            }

            return new Palette(width, height, colors);
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
    }, Source("source") {
        @Override
        public Object toValue(Tree tree) {
            throw new MeelanException("Cannot use source inside of program. Use expr instead.", tree);
        }

        @Override
        public Tree toTree(Object value) {
            throw new IllegalArgumentException("this is unexpected and clearly a bug. please report");
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
}

/*
    private Tree createTreeFrom(Entry entry, Object value) {
        switch (entry.key.type) {
            case Int:
                return new Int(((Number) value).intValue());
            case Real:
                return new Real(((Number) value).doubleValue());
            case Cplx:
                return new CplxVal((Cplx) value);
            case Bool:
                return new Bool((Boolean) value);
            case Color:
                return new Int((Integer) value);
            case Expr:
                // This may throw a MeelanException!
                return ParserInstance.get().parseExpr((String) value);
            case Palette:
                return paletteLambda(entry.key.id);
            case Scale:
                // first occurrence of this palette.
                return registerScale(entry.key.id);
        }

        throw new IllegalArgumentException("missing case: " + entry.key.type);
    }

 */
