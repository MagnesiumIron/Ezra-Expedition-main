package io.github.devsimulator.elements;

import io.github.devsimulator.helper.PhysicSim;

public abstract class Liquid extends Element {
    private final int dispersionRate;

    public Liquid(int x, int y, int density, int dispersionRate) {
        super(x, y);
        this.isSolid = false;
        this.density = density;
        this.dispersionRate = dispersionRate;
        this.isStatic = false;
    }

    @Override
    public void step(PhysicSim sim) {
        if (hasUpdated) return;
        hasUpdated = true;

        // 1. Gravity (Down)
        if (tryMoveOrSwap(sim, x, y - 1)) return;

        // 2. Flow (Sideways)
        int dir = Math.random() < 0.5 ? 1 : -1;

        /*if (!attemptFlow(sim, dir, dispersionRate)) {
            attemptFlow(sim, -dir, dispersionRate);
        }*/

        // Scan for the furthest valid move
        for (int i = 1; i <= dispersionRate; i++) {
            int targetX = x + (dir * i);

            // STOP if we hit a wall or bounds
            if (!sim.isWithinBounds(targetX, y) || sim.isWall(targetX, y)) break;

            Element neighbor = sim.getElement(targetX, y);

            if (neighbor == null) {
                // Empty spot found - Move there!
                sim.moveElement(x, y, targetX, y);
                return;
            } else if (sim.processAlchemy(this, neighbor)) {
                // --- ALCHEMY CHECK (SIDEWAYS) ---
                return;
            } else if (!neighbor.isSolid && this.density > neighbor.density) {
                // Lighter liquid/gas found - Swap!
                swapPositions(sim, neighbor);
                return;
            } else {
                // Blocked by solid or heavier liquid - STOP looking in this direction
                break;
            }
        }
    }

    private boolean tryMoveOrSwap(PhysicSim sim, int tx, int ty) {
        if (!sim.isWithinBounds(tx, ty) || sim.isWall(tx, ty)) return false;

        Element neighbor = sim.getElement(tx, ty);
        if (neighbor == null) {
            sim.moveElement(x, y, tx, ty);
            return true;
        }
        //chemical reaction checker
        if (sim.processAlchemy(this, neighbor)) {
            return true;
        }

        // Swap if we are heavier water sinks in oil or gas
        if (!neighbor.isSolid && this.density > neighbor.density) {
            swapPositions(sim, neighbor);
            return true;
        }
        return false;
    }
}
