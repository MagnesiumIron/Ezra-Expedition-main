package io.github.devsimulator.controllers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import io.github.devsimulator.elements.*;
import io.github.devsimulator.entities.Player;
import io.github.devsimulator.helper.PhysicSim;
import io.github.devsimulator.helper.tilemapmanager.RuneData;
import io.github.devsimulator.Main;

public class SandManager {
    public PhysicSim sim;
    private Texture whitePixel;
    private int simW;
    private int simH;
    private final float CELL_SIZE = 4f;

    public Array<RuneData> allRunes = new Array<>();
    public Array<RuneData> activeContainers = new Array<>();
    public Array<Integer> completedGeysers = new Array<>();

    public Player player;

    public static final Pool<Sand> sandPool = new Pool<Sand>(2000, 15000) {
        @Override protected Sand newObject() { return new Sand(-1, -1); }
    };

    public static final Pool<Water> waterPool = new Pool<Water>(2000, 15000) {
        @Override protected Water newObject() { return new Water(-1, -1); }
    };

    public static final Pool<Lava> lavaPool = new Pool<Lava>(2000, 15000) {
        @Override protected Lava newObject() { return new Lava(-1, -1); }
    };

    public static final Pool<Smoke> smokePool = new Pool<Smoke>(2000, 15000) {
        @Override protected Smoke newObject() { return new Smoke(-1, -1); }
    };

    public SandManager() {
        createTexture();
    }

    public void initLevel(TiledMap map) {
        //Recycle the old map first before creating a new one
        if (sim != null) {
            sim.clearToPool();
        }

        int tilesX = map.getProperties().get("width", Integer.class);
        int tilesY = map.getProperties().get("height", Integer.class);
        int tileW = map.getProperties().get("tilewidth", Integer.class);
        int tileH = map.getProperties().get("tileheight", Integer.class);

        simW = (int) ((tilesX * tileW) / CELL_SIZE);
        simH = (int) ((tilesY * tileH) / CELL_SIZE);
        sim = new PhysicSim(simW, simH);

        // Initialize Collisions
        // Initialize Collisions
        MapLayer collisionLayer = map.getLayers().get("collisions");
        if (collisionLayer != null) {

            // 1. RECTANGLES (You already have this)
            for (RectangleMapObject object : collisionLayer.getObjects().getByType(RectangleMapObject.class)) {
                Rectangle rect = object.getRectangle();
                int startX = Math.max(0, (int) (rect.x / CELL_SIZE));
                int endX = Math.min(simW, (int) Math.ceil((rect.x + rect.width) / CELL_SIZE));
                int startY = Math.max(0, (int) (rect.y / CELL_SIZE));
                int endY = Math.min(simH, (int) Math.ceil((rect.y + rect.height) / CELL_SIZE));

                for (int x = startX; x < endX; x++) {
                    for (int y = startY; y < endY; y++) {
                        sim.setWall(x, y, true);
                    }
                }
            }

            // 2. POLYGONS (ADD THIS NEW BLOCK!)
            for (PolygonMapObject object : collisionLayer.getObjects().getByType(PolygonMapObject.class)) {
                Polygon polygon = object.getPolygon();
                Rectangle bounds = polygon.getBoundingRectangle();

                int startX = Math.max(0, (int) (bounds.x / CELL_SIZE));
                int endX = Math.min(simW, (int) Math.ceil((bounds.x + bounds.width) / CELL_SIZE));
                int startY = Math.max(0, (int) (bounds.y / CELL_SIZE));
                int endY = Math.min(simH, (int) Math.ceil((bounds.y + bounds.height) / CELL_SIZE));

                for (int x = startX; x < endX; x++) {
                    for (int y = startY; y < endY; y++) {
                        float pixelX = (x * CELL_SIZE) + (CELL_SIZE / 2f);
                        float pixelY = (y * CELL_SIZE) + (CELL_SIZE / 2f);
                        if (polygon.contains(pixelX, pixelY)) {
                            sim.setWall(x, y, true);
                        }
                    }
                }
            }
        }

        allRunes.clear();
        activeContainers.clear();
        completedGeysers.clear();

        // Initialize Runes
        MapLayer runeLayer = map.getLayers().get("runes");
        if (runeLayer != null) {
            for (RectangleMapObject object : runeLayer.getObjects().getByType(RectangleMapObject.class)) {
                String rawType = object.getProperties().get("elementType", "SAND", String.class).toUpperCase().trim();
                boolean isSpawner = object.getProperties().get("isSpawner", false, Boolean.class);
                boolean isContainer = object.getProperties().get("isContainer", false, Boolean.class);
                int id = object.getProperties().get("runeID", 0, Integer.class);

                Rectangle r = object.getRectangle();
                allRunes.add(new RuneData(rawType, isSpawner, isContainer, id, r.x, r.y, r.width, r.height));
            }

            for (PolygonMapObject object : runeLayer.getObjects().getByType(PolygonMapObject.class)) {
                String rawType = object.getProperties().get("elementType", "SAND", String.class).toUpperCase().trim();
                boolean isSpawner = object.getProperties().get("isSpawner", false, Boolean.class);
                boolean isContainer = object.getProperties().get("isContainer", false, Boolean.class);
                int id = object.getProperties().get("runeID", 0, Integer.class);

                // Convert the polygon into a bounding rectangle so the spawner logic can read it
                Rectangle r = object.getPolygon().getBoundingRectangle();
                allRunes.add(new RuneData(rawType, isSpawner, isContainer, id, r.x, r.y, r.width, r.height));
            }

        }

        // We MUST call these at the end of initLevel to populate the starting zones!
        spawnLayer(map, "sand_zones", ElementType.SAND);
        spawnLayer(map, "water_zones", ElementType.WATER);
        spawnLayer(map, "lava_zones", ElementType.LAVA);

        Gdx.app.log("SandManager", "Level Initialized: Zones spawned.");
    }

    public void toggleSpawner(RuneData trigger) {
        if (completedGeysers.contains(trigger.runeID, false)) return;

        for (RuneData r : allRunes) {
            if (r.isContainer && r.runeID == trigger.runeID) {
                if (activeContainers.contains(r, true)) {
                    activeContainers.removeValue(r, true);
                } else {
                    activeContainers.add(r);
                }
            }
        }
    }

    public void update() {
        if (sim == null) return;
        sim.update();

        int pX = -999, pY = -999;
        if (player != null && player.b2body != null) {
            pX = (int) (player.b2body.getPosition().x * Main.PPM / CELL_SIZE);
            pY = (int) (player.b2body.getPosition().y * Main.PPM / CELL_SIZE);
        }

        for (int c = activeContainers.size - 1; c >= 0; c--) {
            RuneData container = activeContainers.get(c);

            int startX = (int) (container.worldX / CELL_SIZE);
            int endX = (int) ((container.worldX + container.width) / CELL_SIZE);
            int botY = (int) (container.worldY / CELL_SIZE) + 1;
            int topY = (int) ((container.worldY + container.height) / CELL_SIZE);

            ElementType type;
            if (container.elementType.equalsIgnoreCase("WATER")) {
                type = ElementType.WATER;
            } else if (container.elementType.equalsIgnoreCase("LAVA")) {
                type = ElementType.LAVA;
            } else {
                type = ElementType.SAND;
            }

            int count = 2;
            boolean spawnedAtLeastOne = false;

            for (int i = 0; i < count; i++) {
                int rx = MathUtils.random(startX, endX - 1);
                for (int y = botY; y < topY; y++) {
                    if (rx >= 0 && rx < simW && y >= 0 && y < simH) {
                        // Anti-Buried Shield
                        if (Math.abs(rx - pX) < 2 && Math.abs(y - pY) < 3) continue;

                        Element current = sim.getElement(rx, y);
                        if (current == null || current instanceof EmptyCell) {
                            sim.setElement(rx, y, createElement(rx, y, type));
                            spawnedAtLeastOne = true;
                            break;
                        }
                    }
                }
            }

            if (!spawnedAtLeastOne) {
                boolean isCompletelyFull = true;
                int checkY = topY - 1;
                for (int x = startX; x < endX; x++) {
                    if (x >= 0 && x < simW && checkY >= 0 && checkY < simH) {
                        Element check = sim.getElement(x, checkY);
                        if (check == null || check instanceof EmptyCell) {
                            isCompletelyFull = false;
                            break;
                        }
                    }
                }
                if (isCompletelyFull) {
                    completedGeysers.add(container.runeID);
                    activeContainers.removeIndex(c);
                }
            }
        }
    }

    private Element createElement(int x, int y, ElementType type) {
        if (type == null) return null;
        return type.create(x, y);
    }

    public void spawnLayer(TiledMap map, String layerName, ElementType type) {
        MapLayer layer = map.getLayers().get(layerName);
        if (layer != null) {
            for (RectangleMapObject object : layer.getObjects().getByType(RectangleMapObject.class)) {
                Rectangle rect = object.getRectangle();
                sim.fillArea((int)(rect.x/CELL_SIZE), (int)(rect.y/CELL_SIZE),
                    (int)(rect.width/CELL_SIZE), (int)(rect.height/CELL_SIZE), type);
            }
        }
    }

    private void createTexture() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1f, 1f, 1f, 1f);
        pixmap.fill();
        whitePixel = new Texture(pixmap);
        pixmap.dispose();
    }

    public void render(SpriteBatch batch) {
        if (sim == null || whitePixel == null) return;
        for (int y = 0; y < simH; y++) {
            for (int x = 0; x < simW; x++) {
                Element e = sim.getElement(x, y);
                if (e != null && !(e instanceof EmptyCell)) {
                    batch.setColor(e.color != null ? e.color : com.badlogic.gdx.graphics.Color.WHITE);
                    batch.draw(whitePixel, x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }
        }
        batch.setColor(1, 1, 1, 1);
    }

    public void dispose() {
        if (whitePixel != null) whitePixel.dispose();
    }
}
