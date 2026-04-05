package io.github.devsimulator.helper;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.*;
import io.github.devsimulator.Main;

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

    public static void createBoundaries(TiledMap map, World world) {

        WorldContactListener.clearHashes();

        MapLayer collisionLayer = map.getLayers().get("collisions");
        if (collisionLayer != null) {
            MapObjects objects = collisionLayer.getObjects();
            for (MapObject object : objects.getByType(RectangleMapObject.class)) {
                Rectangle rect = ((RectangleMapObject) object).getRectangle();
                BodyDef bdef = new BodyDef();
                bdef.type = BodyDef.BodyType.StaticBody;
                float w = rect.getWidth() / Main.PPM;
                float h = rect.getHeight() / Main.PPM;
                bdef.position.set((rect.getX() / Main.PPM) + w / 2, (rect.getY() / Main.PPM) + h / 2);

                Body body = world.createBody(bdef);
                PolygonShape shape = new PolygonShape();
                shape.setAsBox(w / 2, h / 2);
                FixtureDef fdef = new FixtureDef();
                fdef.shape = shape;

                if (object.getProperties().containsKey("isKey")) {
                    fdef.isSensor = true;
                    body.createFixture(fdef).setUserData("KEY");
                } else if (object.getProperties().containsKey("isDoor")) {
                    body.createFixture(fdef).setUserData("DOOR");
                } else {
                    body.createFixture(fdef).setUserData("GROUND");
                }
                shape.dispose();
            }

            for (MapObject object : objects.getByType(PolygonMapObject.class)) {
                Polygon polygon = ((PolygonMapObject) object).getPolygon();

                BodyDef bdef = new BodyDef();
                bdef.type = BodyDef.BodyType.StaticBody;
                bdef.position.set(polygon.getX() / Main.PPM, polygon.getY() / Main.PPM);
                Body body = world.createBody(bdef);

                float[] vertices = polygon.getVertices();
                float[] worldVertices = new float[vertices.length];
                for (int i = 0; i < vertices.length; ++i) {
                    worldVertices[i] = vertices[i] / Main.PPM;
                }

                PolygonShape shape = new PolygonShape();
                shape.set(worldVertices);

                body.createFixture(shape, 0).setUserData("GROUND");
                shape.dispose();
            }
        }

        MapLayer transitionLayer = map.getLayers().get("transitions");
        if (transitionLayer != null) {
            for (MapObject object : transitionLayer.getObjects().getByType(RectangleMapObject.class)) {
                Rectangle rect = ((RectangleMapObject) object).getRectangle();
                BodyDef bdef = new BodyDef();
                bdef.type = BodyDef.BodyType.StaticBody;
                float w = rect.getWidth() / Main.PPM;
                float h = rect.getHeight() / Main.PPM;
                bdef.position.set((rect.getX() / Main.PPM) + w / 2, (rect.getY() / Main.PPM) + h / 2);

                Body body = world.createBody(bdef);
                PolygonShape shape = new PolygonShape();
                shape.setAsBox(w / 2, h / 2);
                FixtureDef fdef = new FixtureDef();
                fdef.shape = shape;
                fdef.isSensor = true;

                String target = object.getProperties().get("targetMap", String.class);
                float sx = object.getProperties().get("spawnX", -1f, Float.class) / Main.PPM;
                float sy = object.getProperties().get("spawnY", -1f, Float.class) / Main.PPM;

                body.createFixture(fdef).setUserData(new TransitionData(target, sx, sy));
                shape.dispose();
            }
        }

        MapLayer interactLayer = map.getLayers().get("interactables");
        if (interactLayer != null) {
            for (MapObject object : interactLayer.getObjects().getByType(RectangleMapObject.class)) {
                Rectangle rect = ((RectangleMapObject) object).getRectangle();
                float w = rect.width / Main.PPM;
                float h = rect.height / Main.PPM;
                float x = rect.x / Main.PPM;
                float y = rect.y / Main.PPM;

                BodyDef bdef = new BodyDef();
                bdef.type = BodyDef.BodyType.StaticBody;
                bdef.position.set(x + w/2, y + h/2);
                Body body = world.createBody(bdef);

                PolygonShape shape = new PolygonShape();
                shape.setAsBox(w/2, h/2);
                FixtureDef fdef = new FixtureDef();
                fdef.shape = shape;
                fdef.isSensor = true;

                String head = object.getProperties().get("header", "TUTORIAL", String.class);
                String desc = object.getProperties().get("description", "...", String.class);
                boolean hero = object.getProperties().get("isHero", false, Boolean.class);

                InteractableData data = new InteractableData(head, desc, hero, x, y, w, h);
                body.createFixture(fdef).setUserData(data);

                WorldContactListener.addSign(data);

                shape.dispose();
            }
        }

        MapLayer runeLayer = map.getLayers().get("runes");
        if (runeLayer != null) {
            for (MapObject object : runeLayer.getObjects().getByType(RectangleMapObject.class)) {
                Rectangle rect = ((RectangleMapObject) object).getRectangle();
                float w = rect.getWidth() / Main.PPM;
                float h = rect.getHeight() / Main.PPM;
                float worldX = rect.getX() / Main.PPM;
                float worldY = rect.getY() / Main.PPM;

                BodyDef bdef = new BodyDef();
                bdef.type = BodyDef.BodyType.StaticBody;
                bdef.position.set(worldX + w/2, worldY + h/2);
                Body body = world.createBody(bdef);

                PolygonShape shape = new PolygonShape();
                shape.setAsBox(w/2, h/2);
                FixtureDef fdef = new FixtureDef();
                fdef.shape = shape;
                fdef.isSensor = true;

                String type = object.getProperties().get("elementType", "NONE", String.class);
                boolean isSpawner = object.getProperties().get("isSpawner", false, Boolean.class);
                boolean isContainer = object.getProperties().get("isContainer", false, Boolean.class);
                int runeID = object.getProperties().get("runeID", 0, Integer.class);

                RuneData data = new RuneData(type, isSpawner, isContainer, runeID, worldX, worldY, w, h);
                body.createFixture(fdef).setUserData(data);

                WorldContactListener.addRune(data);

                shape.dispose();
            }
        }
    }
}
