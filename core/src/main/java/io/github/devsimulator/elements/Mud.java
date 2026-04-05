package io.github.devsimulator.elements;
import com.badlogic.gdx.graphics.Color;

public class Mud extends MovableSolid {
    public Mud(int x, int y) {
        super(x, y, 12); // a little bit heavier than dirt
        this.color = new Color(0.4f, 0.25f, 0.15f, 1f); // dark and wet brown
    }
}
