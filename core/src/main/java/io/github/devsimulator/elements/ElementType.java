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
    },
    LAVA {
        @Override
        public Element create(int x, int y) {
            Lava l = io.github.devsimulator.controllers.SandManager.lavaPool.obtain();
            l.init(x, y);
            return l;
        }
    },
    OBSIDIAN {
        @Override public Element create(int x, int y) { return new Obsidian(x, y); }
    },
    MUD {
        @Override public Element create(int x, int y) { return new Mud(x, y); }
    },
    GLASS {
        @Override public Element create(int x, int y) { return new Glass(x, y); }
    };

    public abstract Element create(int x, int y);
}

