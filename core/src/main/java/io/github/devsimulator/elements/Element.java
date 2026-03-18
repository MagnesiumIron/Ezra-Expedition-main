package io.github.devsimulator.elements;

import com.badlogic.gdx.graphics.Color;
import io.github.devsimulator.helper.PhysicSim;
import java.util.Random;

public abstract class Element {
    public int x, y;
    public boolean hasUpdated = false;

    // --- PHYSICS FLAGS ---
    public boolean isSolid = false;
    public boolean isStatic = false;     // True = Walls (Never moves)
    public boolean isFreeFalling = true; // True = Awake, False = Asleep (Inertia)
    public int density = 0;              // Heavier things sink

    public Color color;
    public int temperature = 20;         // Room temp default
    public int flammability = 0;         // 0 = Fireproof, 100 = Gasoline
    public int corrosionResistance = 0;  // 0 = Weak, 100 = Acid Proof

    protected static final Random random = new Random();

    public Element(int x, int y) {
        this.x = x;
        this.y = y;
        this.color = new Color(1, 1, 1, 1);

        float noise = 0.9f + (float)Math.random() * 0.2f; // 0.9 to 1.1
        this.color.mul(noise, noise, noise, 1.0f);
    }

    public abstract void step(PhysicSim sim);
    // Called when this element touches another
    public void interact(PhysicSim sim, Element neighbor) {
        if (neighbor == null) return;

        //Heat Transfer
        if (this.temperature > neighbor.temperature) {
            neighbor.receiveHeat(sim, (this.temperature - neighbor.temperature) / 4);
        }
    }

    public void receiveHeat(PhysicSim sim, int amount) {
        this.temperature += amount;
        if (flammability > 0 && temperature > 100) {
            if (random.nextInt(100) < flammability) {
                //need fire class
                sim.setElement(x, y, null);
            }
        }
    }

    public boolean corrode(PhysicSim sim) {
        if (corrosionResistance >= 100) return false;
        if (random.nextInt(100) > corrosionResistance) {
            sim.setElement(x, y, null); // Destroy self
            return true;
        }
        return false;
    }

    public void swapPositions(PhysicSim sim, Element other) {
        if (other == null) return;
        int ox = other.x;
        int oy = other.y;
        sim.setElement(this.x, this.y, other);
        sim.setElement(ox, oy, this);
        this.isFreeFalling = true;
        other.isFreeFalling = true;
        this.wakeNeighbors(sim);
    }

    public void wakeNeighbors(PhysicSim sim) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i==0 && j==0) continue;
                Element e = sim.getElement(x + i, y + j);
                if (e != null) e.isFreeFalling = true;
            }
        }
    }

    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
}
