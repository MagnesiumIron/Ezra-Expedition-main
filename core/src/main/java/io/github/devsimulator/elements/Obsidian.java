package io.github.devsimulator.elements;
import com.badlogic.gdx.graphics.Color;

public class Obsidian extends MovableSolid {
    public Obsidian(int x, int y) {
        super(x, y, 50); // very heavy and sinks almost in everything
        this.color = new Color(0.15f, 0.05f, 0.2f, 1f); //colour is deep dark purple
    }
}
