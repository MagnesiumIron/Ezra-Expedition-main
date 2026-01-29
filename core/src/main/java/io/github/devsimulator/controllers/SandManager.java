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
import io.github.devsimulator.elements.ElementType;
import io.github.devsimulator.helper.PhysicSim;

public class SandManager {
    public PhysicSim sim;
    private Texture whitePixel;

    // Config
    private final float MAP_WIDTH = 960f;
    private final float MAP_HEIGHT = 640f;

    // UPDATE: Changed to 3:2 Ratio (240x160)
    // 960 / 240 = 4 pixels per cell (Perfect Square)
    // 640 / 160 = 4 pixels per cell (Perfect Square)
    private final float SIM_W = 240f;
    private final float SIM_H = 160f;

    public SandManager() {
        sim = new PhysicSim((int)SIM_W, (int)SIM_H);
        createTexture();
    }

    private void createTexture() {
        // Create a single white pixel to be tinted later
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1f, 1f, 1f, 1f);
        pixmap.fill();
        whitePixel = new Texture(pixmap);
        pixmap.dispose();
    }

    public void initLevel(TiledMap map) {
        float cellW = MAP_WIDTH / SIM_W; // Should be 4.0
        float cellH = MAP_HEIGHT / SIM_H; // Should be 4.0

        // 1. WALLS ("collisions" layer)
        // We use Math.ceil here to prevent "Gaps" at the edges of walls
        MapLayer collisionLayer = map.getLayers().get("collisions");
        if (collisionLayer != null) {
            for (MapObject object : collisionLayer.getObjects()) {
                if (object instanceof RectangleMapObject) {
                    Rectangle rect = ((RectangleMapObject) object).getRectangle();
                    float flippedY = MAP_HEIGHT - rect.y - rect.height;

                    // Calculate Start and End indices
                    // Math.ceil ensures we cover the partial pixels at the end
                    int startX = (int) (rect.x / cellW);
                    int endX = (int) Math.ceil((rect.x + rect.width) / cellW);

                    int startY = (int) (flippedY / cellH);
                    int endY = (int) Math.ceil((flippedY + rect.height) / cellH);

                    // Mark these spots as Walls
                    for (int x = startX; x < endX; x++) {
                        for (int y = startY; y < endY; y++) {
                            sim.setWall(x, y, true);
                        }
                    }
                }
            }
        }

        // 2. Spawn Sand
        spawnLayer(map, "sand_zones", ElementType.SAND);

        // 3. Spawn Water
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
