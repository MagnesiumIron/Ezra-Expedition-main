package io.github.devsimulator.helper;

// LibGDX Core and Math Imports
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;

// Box2D Physics Imports
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

// Your Main class import (to access PPM)
import io.github.devsimulator.Main;

public class tilemapmanager {
    public static void createBoundaries(TiledMap map, World world) {
        // Look for the "collisions" layer you made in Tiled
        MapLayer layer = map.getLayers().get("collisions");

        if (layer == null) {
            Gdx.app.log("MAP_ERROR", "Layer 'collisions' not found! Check Tiled layer names.");
            return;
        }
        for (MapObject object : layer.getObjects()) {
            if (object instanceof RectangleMapObject) {
                Rectangle rect = ((RectangleMapObject) object).getRectangle();

                BodyDef bdef = new BodyDef();
                bdef.type = BodyDef.BodyType.StaticBody; // Static means it won't move

                // Center the body (Tiled uses bottom-left, Box2D uses center)
                float x = (rect.getX() + rect.getWidth() / 2) / Main.PPM;
                float y = (rect.getY() + rect.getHeight() / 2) / Main.PPM;
                bdef.position.set(x, y);

                Body body = world.createBody(bdef);
                PolygonShape shape = new PolygonShape();
                shape.setAsBox(rect.getWidth() / 2 / Main.PPM, rect.getHeight() / 2 / Main.PPM);

                body.createFixture(shape, 1.0f);
                shape.dispose();
            }
        }
    }
}
