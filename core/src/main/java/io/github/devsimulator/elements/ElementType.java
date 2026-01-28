package io.github.devsimulator.elements;

public enum ElementType {
    DIRT {
        @Override public Element create(int x, int y) { return new Dirt(x, y); }
    },
    SAND {
        @Override public Element create(int x, int y) { return new Sand(x, y); }
    },
    WATER {
        @Override public Element create(int x, int y) { return new Water(x, y); }
    };

    public abstract Element create(int x, int y);
}
