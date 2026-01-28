package io.github.devsimulator.elements;

import io.github.devsimulator.helper.PhysicSim;

public abstract class Liquid extends Element {

    private final int dispersionRate;

    public Liquid(int x, int y, int density, int dispersionRate) {
        super(x, y);
        this.isSolid = false; // Important: Allows solids to sink through it
        this.density = density;
        this.dispersionRate = dispersionRate;
        this.frictionFactor = 0.1f;
    }

    @Override
    public void step(PhysicSim sim) {
        if (hasUpdated) return;
        hasUpdated = true;

        // 1. Gravity Check (Move Down)
        if (tryMove(sim, x, y - 1)) return;

        // 2. Dispersion (Flow Sideways)
        // Liquids act differently: they slide horizontally if they can't go down
        int direction = Math.random() < 0.5 ? 1 : -1;

        // Try to flow sideways up to 'dispersionRate' distance
        for (int i = 1; i <= dispersionRate; i++) {
            int targetX = x + (i * direction);

            // If we hit a wall/solid, stop flowing this direction
            if (sim.isWall(targetX, y)) break;

            // If we find an empty spot, move there
            if (sim.isEmpty(targetX, y)) {
                sim.moveElement(x, y, targetX, y);
                return;
            }
        }
    }

    private boolean tryMove(PhysicSim sim, int tx, int ty) {
        if (sim.isEmpty(tx, ty)) {
            sim.moveElement(x, y, tx, ty);
            return true;
        }
        return false;
    }
}
