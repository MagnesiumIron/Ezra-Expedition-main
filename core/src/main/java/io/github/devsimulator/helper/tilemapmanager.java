package io.github.devsimulator.helper;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.*;
import io.github.devsimulator.Main;

public class tilemapmanager {
<<<<<<< Updated upstream
=======

    // --- DATA CLASSES ---
    public static class TransitionData {
        public String targetMap;
        public float spawnX, spawnY;
        public TransitionData(String targetMap, float spawnX, float spawnY) {
            this.targetMap = targetMap;
            this.spawnX = spawnX;
            this.spawnY = spawnY;
        }
    }

    public static class RuneData {
        public String elementType;
        public boolean isSpawner, isContainer, isConsumed;
        public int runeID;
        public float worldX, worldY, width, height;
        public RuneData(String type, boolean spawner, boolean container, int id, float x, float y, float w, float h) {
            this.elementType = type; this.isSpawner = spawner; this.isContainer = container; this.isConsumed = false;
            this.runeID = id; this.worldX = x; this.worldY = y; this.width = w; this.height = h;
        }
    }

    public static class InteractableData {
        public String header, description;
        public boolean isHero;
        public float worldX, worldY, width, height;
        public InteractableData(String header, String desc, boolean isHero, float x, float y, float w, float h) {
            this.header = header; this.description = desc; this.isHero = isHero;
            this.worldX = x; this.worldY = y; this.width = w; this.height = h;
        }
    }

    // --- LAYER PARSING ---
>>>>>>> Stashed changes
    public static void createBoundaries(TiledMap map, World world) {

        // 1. STATIC WALLS ("collisions")
        createLayerBodies(map, world, "collisions", false, "WALL");

        // 2. DOORS ("door") - Solid, but will be destroyed later
        createLayerBodies(map, world, "door", false, "DOOR");

        // 3. KEYS ("key") - Sensor (Walk through)
        createLayerBodies(map, world, "key", true, "KEY");

        // 4. GOAL ("goal") - Sensor
        createLayerBodies(map, world, "goal", true, "GOAL");
    }

    private static void createLayerBodies(TiledMap map, World world, String layerName, boolean isSensor, String userData) {
        MapLayer layer = map.getLayers().get(layerName);
        if (layer == null) return;

        for (MapObject object : layer.getObjects()) {
            if (object instanceof RectangleMapObject) {
                Rectangle rect = ((RectangleMapObject) object).getRectangle();

                BodyDef bdef = new BodyDef();
                bdef.type = BodyDef.BodyType.StaticBody;
                bdef.position.set((rect.getX() + rect.getWidth() / 2) / Main.PPM,
                    (rect.getY() + rect.getHeight() / 2) / Main.PPM);

                Body body = world.createBody(bdef);
                PolygonShape shape = new PolygonShape();
                shape.setAsBox(rect.getWidth() / 2 / Main.PPM, rect.getHeight() / 2 / Main.PPM);

                FixtureDef fdef = new FixtureDef();
                fdef.shape = shape;
                fdef.isSensor = isSensor;

                body.createFixture(fdef).setUserData(userData);
                shape.dispose();
            }
        }
    }
}
