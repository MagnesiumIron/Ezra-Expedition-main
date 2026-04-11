package io.github.devsimulator.elements;
import io.github.devsimulator.controllers.SandManager;
import io.github.devsimulator.helper.PhysicSim;

public class Lava extends Liquid {
    private float glowTimer;

    public Lava(int x, int y) {
        // thick and slow movement realistically depicting lava
        super(x, y, 2, 2);
        init(x, y);
    }

    public void init(int x, int y) {
        this.x = x;
        this.y = y;
        this.glowTimer = (float)Math.random() * 10f; // offset glowing
        updateColor();
    }

    private void updateColor() {
        float intensity = (float) (Math.sin(glowTimer) * 0.5f + 0.5f);
        float r = 0.85f + (intensity * 0.15f);
        float g = 0.2f + (intensity * 0.3f);
        float b = 0.05f;
        this.color.set(r, g, b, 1f);

        float variance = (float)Math.random() * 0.1f;
        this.color.add(variance, variance * 0.5f, 0, 0);
    }

    @Override
    public void step(PhysicSim sim) {
        int oldX = this.x;
        int oldY = this.y;
        super.step(sim);

        glowTimer += 0.05f;
        if (random.nextInt(10) < 2) {
            updateColor();
        }

        boolean isResting = (this.x == oldX && this.y == oldY);
        Element above = sim.getElement(this.x, this.y + 1);
        // smoke emitter
        if (isResting && (above == null || above instanceof EmptyCell)) {
            this.isFreeFalling = true;
            if (random.nextInt(100) < 1) {
                Smoke s = SandManager.smokePool.obtain();
                s.init(this.x, this.y + 1);
                sim.setElement(this.x, this.y + 1, s);
            }
        }
    }

    @Override
    public void freeToPool() {
        SandManager.lavaPool.free(this);
    }
}
