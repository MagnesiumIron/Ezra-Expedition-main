package io.github.devsimulator.elements;

import io.github.devsimulator.helper.PhysicSim;

public abstract class Gas extends Element {
    private int dispersionRate;

    public Gas(int x, int y, int density, int dispersionRate) {
        super(x, y);
        this.isSolid = false;
        this.density = density;
        this.dispersionRate = dispersionRate;
    }

    @Override
    public void step(PhysicSim sim) {
        if (hasUpdated) return;
        hasUpdated = true;

        // 1. Rise (Up)
        if (tryMoveOrSwap(sim, x, y + 1)) return;

        // 2. Rise Diagonally (to get around ceilings)
        int dir = Math.random() < 0.5 ? 1 : -1;
        if (tryMoveOrSwap(sim, x + dir, y + 1)) return;
        if (tryMoveOrSwap(sim, x - dir, y + 1)) return;

        // 3. Disperse (Sideways)
        for (int i = 1; i <= dispersionRate; i++) {
            int targetX = x + (dir * i);
            if (!sim.isWithinBounds(targetX, y) || sim.isWall(targetX, y)) break;

            if (tryMoveOrSwap(sim, targetX, y)) return;
        }
    }

    private boolean tryMoveOrSwap(PhysicSim sim, int tx, int ty) {
        if (!sim.isWithinBounds(tx, ty) || sim.isWall(tx, ty)) return false;

        Element neighbor = sim.getElement(tx, ty);

        if (neighbor == null) {
            sim.moveElement(x, y, tx, ty);
            return true;
        }
        // Swap if we are LIGHTER (Gas rises through Water)
        if (!neighbor.isStatic && this.density < neighbor.density) {
            swapPositions(sim, neighbor);
            return true;
        }
        return false;
    }
}
