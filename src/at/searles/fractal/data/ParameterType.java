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

public enum ParameterType {
    Int("int") {
        @Override
        public Object toValue(Tree tree) {
            if(tree instanceof at.searles.meelan.values.Int) {
                return ((Int) tree).value();
            }

            throw new MeelanException("not an int", tree);
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
    },
    Bool("bool") {
        @Override
        public Object toValue(Tree tree) {
            if(tree instanceof at.searles.meelan.values.Bool) {
                return ((at.searles.meelan.values.Bool) tree).value();
            }

            throw new MeelanException("not a bool", tree);
        }
    },
    Expr("expr") {
        @Override
        public Object toValue(Tree tree) {
            if(tree instanceof StringVal) {
                return ((StringVal) tree).value();
            }

            throw new MeelanException("not a string", tree);
        }
    },
    Color("color") {
        @Override
        public Object toValue(Tree tree) {
            return Int.toValue(tree);
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

            for(Tree row : values) {
                palette.add(toRow(row));
            }

            return palette;
        }

        private List<Integer> toRow(Tree tree) {
            if(tree instanceof at.searles.meelan.values.Int) {
                return Collections.singletonList(toColor(tree));
            } else if(tree instanceof Vec) {
                ArrayList<Integer> row = new ArrayList<>();

                for(Tree cell : ((Vec) tree).values()) {
                    row.add(toColor(cell));
                }

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
        public Object toValue(Tree tree) {
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

            // TODO: create other constructor
            return new Scale(values[0], values[1], values[2], values[3], values[4], values[5]);
        }
    }, Source("source") {
        @Override
        public Object toValue(Tree tree) {
            throw new MeelanException("Cannot use source inside of program. Use expr instead.", tree);
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

    // TODO: refactor: move to FractalExternData.
    public abstract Object toValue(Tree tree);

}

