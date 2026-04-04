package io.github.devsimulator.elements;

import io.github.devsimulator.controllers.SandManager;
import io.github.devsimulator.helper.PhysicSim;

public class Smoke extends Element {
    private int lifeSpan;
    private int initialLifeSpan;

    public Smoke(int x, int y) {
        super(x, y);
        init(x, y);
    }

    public void init(int x, int y) {
        this.x = x;
        this.y = y;
        this.isFreeFalling = true;
        this.density = -1; // move up

        //lifespan of smoke is to 2-3 seconds for a taller plume
        this.initialLifeSpan = 120 + random.nextInt(60);
        this.lifeSpan = this.initialLifeSpan;

        // Random shades of dark semi-transparent gray colour
        float shade = 0.8f + random.nextFloat() * 0.2f;
        this.color.set(shade, shade, shade, 1.0f);
    }

    @Override
    public void step(PhysicSim sim) {
        lifeSpan--;
        if (lifeSpan <= 0) {
            sim.setElement(x, y, null); // Dissipate into the air
            this.freeToPool();
            return;
        }

        this.color.a = (float) lifeSpan / initialLifeSpan; //billowing effect

        if (random.nextInt(100) < 15) {
            int dir = 0;
            if (random.nextInt(100) < 40) {
                dir = random.nextInt(3) - 1; // Drift slightly left or right
            }

            // Try to rise UP
            if (tryMove(sim, x + dir, y + 1)) return;
            if (tryMove(sim, x, y + 1)) return;
            if (tryMove(sim, x - dir, y + 1)) return;

            // Try to move horizontally if blocked from above
            if (tryMove(sim, x + dir, y)) return;
        }
    }

    private boolean tryMove(PhysicSim sim, int nx, int ny) {
        Element target = sim.getElement(nx, ny);
        if (target == null|| target instanceof EmptyCell) {
            sim.setElement(x, y, null);
            sim.setElement(nx, ny, this);
            this.x = nx;
            this.y = ny;
            return true;
        }
        return false;
    }

    @Override
    public void freeToPool() {
        SandManager.smokePool.free(this);
    }
}
