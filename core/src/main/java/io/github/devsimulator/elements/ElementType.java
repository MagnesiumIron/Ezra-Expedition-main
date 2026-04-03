package io.github.devsimulator.elements;
import io.github.devsimulator.controllers.SandManager;

public enum ElementType {
    SAND {
        @Override public Element create(int x, int y) {
            Sand s = SandManager.sandPool.obtain();
            s.init(x, y);
            return s;
        }
    },
    WATER {
        @Override public Element create(int x, int y) {
            Water w = SandManager.waterPool.obtain();
            w.init(x, y);
            return w;
        }
    };

    public abstract Element create(int x, int y);
}

