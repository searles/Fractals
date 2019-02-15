package at.searles.fractal;

import java.util.*;

public class FractalCollection {

    private ArrayList<Integer> order;
    private TreeMap<Integer, Fractal> fractals;

    private int nextId; // counter

    public FractalCollection() {
        nextId = 1; // start with 1.
        this.order = new ArrayList<>(16);
        this.fractals = new TreeMap<>();
    }

    /**
     * @return true if the order was reorganized
     */
    public boolean setKeyId(int id) {
        if(!order.remove((Integer) id)) {
            return false;
        }

        order.add(0, id);

        return true;
    }

    public int keyId() {
        return order.get(0);
    }

    public Fractal get(int ownerId) {
        return fractals.get(ownerId);
    }

    public Iterable<Integer> ids() {
        return order;
    }

    public Iterable<Fractal> fractals() {
        return () -> new Iterator<Fractal>() {
            Iterator<Integer> it = ids().iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Fractal next() {
                return get(it.next());
            }
        };
    }

    public int add(Fractal fractal) {
        int id = nextId++;

        order.add(id);
        fractals.put(id, fractal);

        return id;
    }

    public boolean remove(int id) {
        order.remove((Integer) id);
        return fractals.remove(id) != null;
    }

    public int size() {
        return fractals.size();
    }
}
