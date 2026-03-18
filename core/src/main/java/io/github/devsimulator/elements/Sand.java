package io.github.devsimulator.elements;

import com.badlogic.gdx.graphics.Color;

public class Sand extends MovableSolid {
    public Sand(int x, int y) {
        super(x, y, 10);
        // Base Sand Color
        this.color.set(0.94f, 0.76f, 0.5f, 1f);

        float variation = (float)Math.random() * 0.1f - 0.05f; // +/- 5%
        this.color.add(variation, variation, variation, 0);
    }
}
