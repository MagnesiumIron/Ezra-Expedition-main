package io.github.devsimulator.elements;

import com.badlogic.gdx.graphics.Color;

public class Sand extends MovableSolid {
    public Sand(int x, int y) {
        super(x, y, 10); // Density 10 (Heavier than water)
        this.color = new Color(0.94f, 0.76f, 0.5f, 1f); // Classic yellow sand color
    }
}
