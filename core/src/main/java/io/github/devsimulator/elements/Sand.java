package io.github.devsimulator.elements;
import io.github.devsimulator.controllers.SandManager;
import com.badlogic.gdx.graphics.Color;

public class Sand extends MovableSolid {
    public Sand(int x, int y) {
        super(x, y, 10);
        init(x, y);
    }

    public void init(int x, int y) {// Base Sand Color
        this.x = x;
        this.y = y;
        this.color.set(0.94f, 0.76f, 0.5f, 1f);
        float variation = (float)Math.random() * 0.1f - 0.05f; // +/- 5%
        this.color.add(variation, variation, variation, 0);
    }

    @Override
    public void freeToPool() {
        SandManager.sandPool.free(this); // Send back to the pool
    }
}
