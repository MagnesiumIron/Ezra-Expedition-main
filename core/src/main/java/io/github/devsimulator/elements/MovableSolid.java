package io.github.devsimulator.elements;

import io.github.devsimulator.helper.PhysicSim;

public abstract class MovableSolid extends Element {

    public MovableSolid(int x, int y, int density) {
        super(x, y);
        this.isSolid = true;
        this.isStatic = false;
        this.density = density;
        this.isFreeFalling = true; // Start awake
    }

    @Override
    public void step(PhysicSim sim) {
        if (hasUpdated) return;
        hasUpdated = true;

        if (!isFreeFalling) {
            Element below = sim.getElement(x, y - 1);
            Element belowLeft = sim.getElement(x - 1, y - 1);
            Element belowRight = sim.getElement(x + 1, y - 1);

            boolean canFall = (sim.isWithinBounds(x, y - 1) && (below == null || !below.isSolid)) ||
                (sim.isWithinBounds(x - 1, y - 1) && (belowLeft == null || !belowLeft.isSolid)) ||
                (sim.isWithinBounds(x + 1, y - 1) && (belowRight == null || !belowRight.isSolid));

            if (canFall) {
                isFreeFalling = true;
            } else { return; }
        }

        /*if (!isFreeFalling) {
            Element below = sim.getElement(x, y - 1);
            // If empty below OR liquid below -> WAKE UP
            if (sim.isWithinBounds(x, y - 1) && (below == null || !below.isSolid)) {
                isFreeFalling = true;
            } else {
                return;
            }
        }*/

        if (tryMoveOrSwap(sim, x, y - 1)) {
            wakeNeighbors(sim); // Movement wakes neighbors
            return;
        }
        if (isFreeFalling) {
            int dir = random.nextBoolean() ? 1 : -1;
            if (tryMoveOrSwap(sim, x + dir, y - 1)) {
                wakeNeighbors(sim);
                return;
            }
            if (tryMoveOrSwap(sim, x - dir, y - 1)) {
                wakeNeighbors(sim);
                return;
            }
        }
        this.isFreeFalling = false;
    }

    private boolean tryMoveOrSwap(PhysicSim sim, int tx, int ty) {
        if (!sim.isWithinBounds(tx, ty) || sim.isWall(tx, ty)) return false;

        Element neighbor = sim.getElement(tx, ty);

        // Move to Empty
        if (neighbor == null) {
            sim.moveElement(x, y, tx, ty);
            return true;
        }

        if (!neighbor.isSolid && this.density > neighbor.density) {
            swapPositions(sim, neighbor);
            return true;
        }
        this.interact(sim, neighbor);

        return false;
    }
}
