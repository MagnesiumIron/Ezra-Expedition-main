package io.github.devsimulator.elements;

import io.github.devsimulator.helper.PhysicSim;

public abstract class MovableSolid extends Element {

    // Take density in constructor (Sand is heavier than Water)
    public MovableSolid(int x, int y, int density) {
        super(x, y);
        this.isSolid = true;      // It's a solid
        this.density = density;   // Set its weight
        this.isFreeFalling = true;
    }

    @Override
    public void step(PhysicSim sim) {
        // Prevent updating the same particle twice in one frame
        if (hasUpdated) return;
        hasUpdated = true;

        // 1. Try moving directly down (Gravity)
        if (tryMoveOrSwap(sim, x, y - 1)) {
            return;
        }

        // 2. Randomize direction (-1 or 1)
        int direction = Math.random() < 0.5 ? -1 : 1;

        // 3. Try moving diagonally
        if (tryMoveOrSwap(sim, x + direction, y - 1)) {
            return;
        }

        // 4. Try the other diagonal
        if (tryMoveOrSwap(sim, x - direction, y - 1)) {
            return;
        }

        // If we reached here, we stopped moving
        this.isFreeFalling = false;
    }

    // Helper: Returns true if we successfully moved or swapped
    private boolean tryMoveOrSwap(PhysicSim sim, int targetX, int targetY) {
        // Bounds check handled by sim methods usually, but good to be safe
        if (targetX < 0 || targetX >= 200 || targetY < 0 || targetY >= 200) return false;

        Element neighbor = sim.getElement(targetX, targetY);

        // Case A: The spot is empty -> Move there
        if (neighbor == null) {
            // Check for walls (from your Main.java logic)
            if (sim.isWall(targetX, targetY)) return false;

            sim.moveElement(x, y, targetX, targetY);
            return true;
        }

        // Case B: The spot has a Liquid/Gas -> Swap if we are heavier
        // We check: Neighbor is NOT solid (Liquid/Gas) AND we are denser
        if (!neighbor.isSolid && this.density > neighbor.density) {
            swapPositions(sim, neighbor);
            return true;
        }

        return false;
    }
}
