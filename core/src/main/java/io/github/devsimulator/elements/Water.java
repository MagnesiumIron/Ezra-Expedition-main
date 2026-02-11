package io.github.devsimulator.elements;

import com.badlogic.gdx.graphics.Color;

public class Water extends Liquid {
    public Water(int x, int y) {
        super(x, y, 5, 5); // Density 5 (Lighter than sand), Dispersion 5 (Flows fast)
        this.color = new Color(0.2f, 0.4f, 1.0f, 0.8f); // Transparent blue
    }
}
