package io.github.devsimulator.helper;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import io.github.devsimulator.Main;
import io.github.devsimulator.controllers.SandManager;

public class tilemapmanager {

    public static class TransitionData {
        public String targetMap;
        public float spawnX, spawnY;

        public TransitionData(String targetMap, float spawnX, float spawnY) {
            this.targetMap = targetMap;
            this.spawnX = spawnX;
            this.spawnY = spawnY;
        }
    }

    public static void createBoundaries(TiledMap map, World world) {
        createLayerBodies(map, world, "collisions", false, "WALL");
        createLayerBodies(map, world, "door", false, "DOOR");
        createLayerBodies(map, world, "key", true, "KEY");
        createLayerBodies(map, world, "goal", true, "GOAL");

        createTransitions(map, world);
    }

    private static void createTransitions(TiledMap map, World world) {
        MapLayer layer = map.getLayers().get("transitions");
        if (layer == null) return;

        for (MapObject object : layer.getObjects()) {
            if (object instanceof RectangleMapObject) {
                Rectangle rect = ((RectangleMapObject) object).getRectangle();

                System.out.println("--- DOOR DIAGNOSTIC ---");
                System.out.println("I found a door! Here are the exact properties I see on it:");

                // Print every property the game actually sees
                java.util.Iterator<String> keys = object.getProperties().getKeys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object val = object.getProperties().get(key);
                    System.out.println(" -> Name: '" + key + "' | Type: " + val.getClass().getSimpleName() + " | Value: " + val);
                }
                System.out.println("-----------------------");

                String targetMap = object.getProperties().get("targetMap", String.class);
                Float spawnX = object.getProperties().get("spawnX", Float.class);
                Float spawnY = object.getProperties().get("spawnY", Float.class);

                if (targetMap != null && spawnX != null && spawnY != null) {
                    BodyDef bdef = new BodyDef();
                    bdef.type = BodyDef.BodyType.StaticBody;
                    bdef.position.set((rect.getX() + rect.getWidth() / 2) / Main.PPM,
                        (rect.getY() + rect.getHeight() / 2) / Main.PPM);

                    Body body = world.createBody(bdef);
                    PolygonShape shape = new PolygonShape();
                    shape.setAsBox(rect.getWidth() / 2 / Main.PPM, rect.getHeight() / 2 / Main.PPM);

                    FixtureDef fdef = new FixtureDef();
                    fdef.shape = shape;
                    fdef.isSensor = true;

                    body.createFixture(fdef).setUserData(new TransitionData(targetMap, spawnX, spawnY));
                    shape.dispose();
                    System.out.println("SUCCESS: Door activated!");
                } else {
                    System.out.println("FAILED: One or more properties are missing or the wrong type.");
                }
            }
        }
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

    public static void loadLevel(Main game, String mapName, float spawnX, float spawnY) {
        System.out.println("Transitioning to map: " + mapName);

        Array<Body> bodies = new Array<Body>();
        game.world.getBodies(bodies);
        for (Body b : bodies) {
            if (b.getFixtureList().size > 0) {
                Object userData = b.getFixtureList().first().getUserData();
                if (!"PLAYER".equals(userData) && !"FOOT_SENSOR".equals(userData)) {
                    game.world.destroyBody(b);
                }
            }
        }

        if (game.map != null) game.map.dispose();
        if (game.sandManager != null) game.sandManager.dispose();

        game.map = new TmxMapLoader().load(mapName);
        game.mapRenderer.setMap(game.map);

        createBoundaries(game.map, game.world);
        game.sandManager = new SandManager();
        game.sandManager.initLevel(game.map);

        if (game.player != null) {
            game.player.b2body.setTransform(spawnX, spawnY, 0);
            game.player.b2body.setLinearVelocity(0, 0);
        }
    }
}
