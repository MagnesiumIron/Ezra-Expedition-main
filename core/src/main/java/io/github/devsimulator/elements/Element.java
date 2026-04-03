package io.github.devsimulator.elements;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Pool.Poolable;
import io.github.devsimulator.helper.PhysicSim;
import java.util.Random;

/*
*   We added the Poolable here to reset the elements objects when they die. This solves the Memory leak
* particularly during transitions because everytime the player died or transition to other maps, the manager will
* create a new PhysicsSim() object. So, what will happen is since the sandMgr created a new object, the old one
* will just be abandoned. And that's a massive waste and could lead to massive memory leak. So, implementing Poolable
* is viable option to recycle those abandoned particles instead of constantly allocating new memory since you are
* creating a new object each time the player died or transitioned.
*
*/

public abstract class Element implements Poolable {
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

    @Override
    public void reset() {
        this.hasUpdated = false;
        this.isFreeFalling = true;
        this.temperature = 20;
    }

    public void freeToPool() {} //leave this empty as this will be overridden by particular Elements

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
                this.freeToPool();
            }
        }
    }

    public boolean corrode(PhysicSim sim) {
        if (corrosionResistance >= 100) return false;
        if (random.nextInt(100) > corrosionResistance) {
            sim.setElement(x, y, null); // Destroy self
            this.freeToPool();
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
