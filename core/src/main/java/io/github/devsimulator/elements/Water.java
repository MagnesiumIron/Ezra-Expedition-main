package io.github.devsimulator.elements;
import io.github.devsimulator.controllers.SandManager;
import com.badlogic.gdx.graphics.Color;

public class Water extends Liquid {
    public Water(int x, int y) {
        super(x, y, 5, 25); // Density 5 (Lighter than sand), Dispersion 5 (Flows fast)
        init(x, y);
    }

    public void init(int x, int y) {
        this.x = x;
        this.y = y;
        this.color.set(0.2f, 0.4f, 1.0f, 0.8f); // Transparent blue
    }

    @Override
    public void freeToPool() {
        SandManager.waterPool.free(this); // Send back to the pool
    }
}
