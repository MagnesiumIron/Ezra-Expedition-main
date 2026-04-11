package io.github.devsimulator.elements;

import com.badlogic.gdx.graphics.Color;
import io.github.devsimulator.helper.PhysicSim;

public class Mud extends Element {

    public Mud(int x, int y) {
        super(x, y);
        this.isSolid = true;
        this.isStatic = false;
        this.isFreeFalling = false;
        this.density = 12;
        this.color = new Color(0.4f, 0.25f, 0.15f, 1f); // Dark and wet brown
    }

    @Override
    public void step(PhysicSim sim) {
    }
}
