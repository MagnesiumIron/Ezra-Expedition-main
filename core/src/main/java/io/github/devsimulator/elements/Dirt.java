package io.github.devsimulator.elements;

import com.badlogic.gdx.graphics.Color;

public class Dirt extends MovableSolid {
    public Dirt(int x, int y) {
        // x, y, density (Sand/Dirt is usually around 5-10)
        super(x, y, 10);
        this.color = new Color(0.76f, 0.7f, 0.5f, 1f); // Set Color here!
    }
}
