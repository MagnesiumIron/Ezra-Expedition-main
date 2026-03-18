package io.github.devsimulator.controllers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
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
    private final float SIM_W = 240f;
    private final float SIM_H = 160f;

    public SandManager() {
        sim = new PhysicSim((int)SIM_W, (int)SIM_H);
        createTexture();
    }

    public void initLevel(TiledMap map) {
        float cellW = MAP_WIDTH / SIM_W;
        float cellH = MAP_HEIGHT / SIM_H;

        // 1. WALLS
        MapLayer collisionLayer = map.getLayers().get("collisions");
        if (collisionLayer != null) {
            for (MapObject object : collisionLayer.getObjects()) {
                if (object instanceof RectangleMapObject) {
                    Rectangle rect = ((RectangleMapObject) object).getRectangle();
                    int startX = (int) (rect.x / cellW);
                    int endX = (int) Math.ceil((rect.x + rect.width) / cellW);
                    int startY = (int) (rect.y / cellH);
                    int endY = (int) Math.ceil((rect.y + rect.height) / cellH);

                    for (int x = startX; x < endX; x++) {
                        for (int y = startY; y < endY; y++) {
                            sim.setWall(x, y, true);
                        }
                    }
                }
            }
        }
        spawnLayer(map, "sand_zones", ElementType.SAND);
        spawnLayer(map, "water_zones", ElementType.WATER);
    }

    private void spawnLayer(TiledMap map, String layerName, ElementType type) {
        MapLayer layer = map.getLayers().get(layerName);
        if (layer == null) {
            layer = map.getLayers().get(layerName.substring(0, layerName.length() - 1));
        }

        if (layer != null) {
            Gdx.app.log("SAND_MGR", "Found Layer: " + layer.getName() + " with " + layer.getObjects().getCount() + " objects.");

            for (MapObject object : layer.getObjects()) {
                if (object instanceof RectangleMapObject) {
                    Rectangle rect = ((RectangleMapObject) object).getRectangle();

                    int simX = (int) (rect.x / (MAP_WIDTH / SIM_W));
                    int simY = (int) (rect.y / (MAP_HEIGHT / SIM_H));
                    int simW = (int) (rect.width / (MAP_WIDTH / SIM_W));
                    int simH = (int) (rect.height / (MAP_HEIGHT / SIM_H));

                    Gdx.app.log("SAND_MGR", "Spawning " + type + " at (" + simX + "," + simY + ")");
                    sim.fillArea(simX, simY, simW, simH, type);
                }
            }
        } else {
            Gdx.app.error("SAND_MGR", "COULD NOT FIND LAYER: " + layerName);
        }
    }

    private void createTexture() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1f, 1f, 1f, 1f);
        pixmap.fill();
        whitePixel = new Texture(pixmap);
        pixmap.dispose();
    }

    public void update() { sim.update(); }

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
        batch.setColor(1, 1, 1, 1);
    }

    public void dispose() { if (whitePixel != null) whitePixel.dispose(); }
}
