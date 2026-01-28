package io.github.devsimulator.controllers;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import io.github.devsimulator.elements.Element;
import io.github.devsimulator.elements.ElementType; // Use the Enum!
import io.github.devsimulator.helper.PhysicSim;

public class SandManager {
    public PhysicSim sim;
    private Texture whitePixel; // Renamed to represent what it actually is

    // Config
    private final float MAP_WIDTH = 960f;
    private final float MAP_HEIGHT = 640f;
    private final float SIM_W = 200f;
    private final float SIM_H = 200f;

    public SandManager() {
        sim = new PhysicSim((int)SIM_W, (int)SIM_H);
        createTexture();
    }

    private void createTexture() {
        // We create a single white pixel. We will tint this pixel
        // to match the color defined inside Sand.java or Water.java
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1f, 1f, 1f, 1f);
        pixmap.fill();
        whitePixel = new Texture(pixmap);
        pixmap.dispose();
    }

    public void initLevel(TiledMap map) {
        // ... (Keep your Wall Loading logic here) ...

        // 2. Spawn Sand
        spawnLayer(map, "sand_zones", ElementType.SAND);

        // 3. Spawn Water (If you have a water layer)
        spawnLayer(map, "water_zones", ElementType.WATER);
    }

    // A generic helper to spawn any layer type
    private void spawnLayer(TiledMap map, String layerName, ElementType type) {
        MapLayer layer = map.getLayers().get(layerName);
        if (layer != null) {
            for (MapObject object : layer.getObjects()) {
                if (object instanceof RectangleMapObject) {
                    Rectangle rect = ((RectangleMapObject) object).getRectangle();
                    float flippedY = MAP_HEIGHT - rect.y - rect.height;

                    int simX = (int) (rect.x / (MAP_WIDTH / SIM_W));
                    int simY = (int) (flippedY / (MAP_HEIGHT / SIM_H));
                    int simW = (int) (rect.width / (MAP_WIDTH / SIM_W));
                    int simH = (int) (rect.height / (MAP_HEIGHT / SIM_H));

                    // Use the specific Type (Sand/Water) here!
                    sim.fillArea(simX, simY, simW, simH, type);
                }
            }
        }
    }

    public void update() {
        sim.update();
    }

    public void render(SpriteBatch batch) {
        float cellWidth = MAP_WIDTH / SIM_W;
        float cellHeight = MAP_HEIGHT / SIM_H;

        for (int y = 0; y < (int)SIM_H; y++) {
            for (int x = 0; x < (int)SIM_W; x++) {
                Element e = sim.getElement(x, y);
                if (e != null) {
                    // HERE IS THE MAGIC:
                    // We grab the color defined in Sand.java or Water.java
                    batch.setColor(e.color);
                    batch.draw(whitePixel, x * cellWidth, y * cellHeight, cellWidth, cellHeight);
                }
            }
        }
        batch.setColor(1, 1, 1, 1); // Reset color
    }

    public void dispose() {
        if (whitePixel != null) whitePixel.dispose();
    }
}
