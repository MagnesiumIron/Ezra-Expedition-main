package io.github.devsimulator.elements;

import com.badlogic.gdx.graphics.Color;
import io.github.devsimulator.helper.PhysicSim;

public class Mud extends Element {
    public Mud(int x, int y) {
        super(x, y);
        this.isSolid = true;        // Ezra can stand on it
        this.isStatic = true;       // It will NOT drop or fall
        this.isFreeFalling = false; // It starts "asleep"
        this.density = 12;          // Heavier than dirt
        this.color = new Color(0.4f, 0.25f, 0.15f, 1f); // Dark and wet brown
    }

    @Override
    public void step(PhysicSim sim) {
        // Mud is now a solid, static platform.
        // It stays where the Water + Sand reaction happened.
    }
}
