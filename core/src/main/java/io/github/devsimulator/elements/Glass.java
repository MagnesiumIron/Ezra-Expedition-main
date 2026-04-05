package io.github.devsimulator.elements;
import com.badlogic.gdx.graphics.Color;

public class Glass extends MovableSolid {
    public Glass(int x, int y) {
        super(x, y, 15);
        this.color = new Color(0.8f, 0.9f, 1.0f, 0.6f); // we use an icy blue colour
    }
}
