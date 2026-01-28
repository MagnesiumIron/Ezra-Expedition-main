package io.github.devsimulator.elements;

import io.github.devsimulator.helper.PhysicSim;

public class EmptyCell extends Element {
    private static EmptyCell instance;

    private EmptyCell() {
        super(-1, -1); // Coordinates don't matter for the singleton
        this.isSolid = false;
        this.density = -1000; // Lighter than everything
    }

    public static EmptyCell getInstance() {
        if (instance == null) {
            instance = new EmptyCell();
        }
        return instance;
    }

    @Override
    public void step(PhysicSim sim) {
        // Empty cells do nothing
    }
}
