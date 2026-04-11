package io.github.devsimulator.elements;

import com.badlogic.gdx.graphics.Color;
import io.github.devsimulator.helper.PhysicSim;

public class Obsidian extends Element {

    public Obsidian(int x, int y) {
        super(x, y);
        this.isSolid = true;
        this.isStatic = true;
        this.isFreeFalling = false;
        this.density = 50;
        this.color = new Color(0.15f, 0.05f, 0.2f, 1f); // Deep dark purple
    }

    @Override
    public void step(PhysicSim sim) {
    }
}
