package io.github.devsimulator.elements;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import io.github.devsimulator.helper.PhysicSim;
import java.util.BitSet;

public abstract class Element {
    // --- Physics Properties ---
    protected int x, y;
    public Vector3 velocity; // New: Supports velocity-based movement
    public float frictionFactor = 0.5f;
    public boolean isFreeFalling = true;
    public boolean isSolid = false;
    public int density = 0;

    // --- Advanced Simulation Properties (from your snippet) ---
    public Color color;
    public int health = 100;
    public boolean isIgnited = false;
    public int flammabilityResistance = 100;
    public int resetFlammabilityResistance = 50;
    public int heatFactor = 10;
    public int fireDamage = 3;
    public int temperature = 0;
    public boolean isDead = false;
    public boolean hasUpdated = false; // Replaces 'stepped' BitSet

    public Element(int x, int y) {
        this.x = x;
        this.y = y;
        this.velocity = new Vector3(0, 0, 0);
        this.color = new Color(1, 1, 1, 1); // Default white
    }

    // Abstract method every element must implement
    public abstract void step(PhysicSim sim);

    // --- Core Logic from your Snippet ---

    public void swapPositions(PhysicSim sim, Element other) {
        if (other == null) return;
        int otherX = other.x;
        int otherY = other.y;

        sim.setElement(this.x, this.y, other);
        sim.setElement(otherX, otherY, this);
    }

    public void die(PhysicSim sim) {
        this.isDead = true;
        sim.setElement(this.x, this.y, null); // Replaces with "EmptyCell" (null in your current sim)
    }

    public void dieAndReplace(PhysicSim sim, Element newElement) {
        this.isDead = true;
        sim.setElement(this.x, this.y, newElement);
    }

    // --- Heat & Reaction System ---

    public boolean receiveHeat(int heat) {
        if (isIgnited) return false;

        // Random chance to resist heat based on resistance
        this.flammabilityResistance -= (int) (Math.random() * heat);

        if (this.flammabilityResistance <= 0) {
            this.isIgnited = true;
            this.color = Color.ORANGE; // Visual feedback for fire
            return true;
        }
        return false;
    }

    public void takeFireDamage(PhysicSim sim) {
        if (isIgnited) {
            this.health -= fireDamage;
            if (this.health <= 0) {
                die(sim); // Burnt away
            }
        }
    }

    public boolean corrode(PhysicSim sim) {
        this.health -= 170; // Massive damage from acid
        if (this.health <= 0) die(sim);
        return true;
    }

    // --- Color / Visuals ---

    public boolean stain(float r, float g, float b, float a) {
        if (Math.random() > 0.2 || isIgnited) return false;

        this.color.add(r, g, b, a);
        this.color.clamp(); // Ensure RGBA stays 0-1
        return true;
    }

    // --- Getters & Setters ---
    public int getX() { return x; }
    public int getY() { return y; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
}
