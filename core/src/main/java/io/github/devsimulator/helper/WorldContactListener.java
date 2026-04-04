package io.github.devsimulator.helper;

import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import io.github.devsimulator.Main;
import io.github.devsimulator.helper.tilemapmanager.InteractableData;
import io.github.devsimulator.helper.tilemapmanager.RuneData;

public class WorldContactListener implements ContactListener {

    // --- STATIC STORAGE ---
    public static Array<InteractableData> allSigns = new Array<>();
    public static Array<RuneData> allRunes = new Array<>();

    public static InteractableData closestSign = null;
    public static RuneData closestRune = null;

    // --- PHYSICS STATE ---
    public static int footContacts = 0;
    public static Array<Body> bodiesToDestroy = new Array<>();
    public static tilemapmanager.TransitionData pendingTransition = null;

    // --- SORTING LOGIC ---
    public static void sortSignsByDistance(Vector2 playerPos) {
        closestSign = null;
        float minDst = Float.MAX_VALUE;

        for (InteractableData sign : allSigns) {
            float dst = playerPos.dst(sign.worldX * Main.PPM, sign.worldY * Main.PPM);
            if (dst < minDst) {
                minDst = dst;
                closestSign = sign;
            }
        }
    }

    public static void sortRunesByDistance(Vector2 playerPos) {
        closestRune = null;
        float minDst = Float.MAX_VALUE;

        for (RuneData rune : allRunes) {
            float dst = playerPos.dst(rune.worldX * Main.PPM, rune.worldY * Main.PPM);
            if (dst < minDst) {
                minDst = dst;
                closestRune = rune;
            }
        }
    }

    // --- CONTACT LOGIC ---
    @Override
    public void beginContact(Contact contact) {
        Fixture fa = contact.getFixtureA();
        Fixture fb = contact.getFixtureB();

        if ("FOOT_SENSOR".equals(fa.getUserData()) || "FOOT_SENSOR".equals(fb.getUserData())) {
            footContacts++;
        }
        checkSensor(fa, fb);
        checkSurface(fa, fb);
    }

    private void checkSurface(Fixture a, Fixture b) {
        boolean aIsFoot = "FOOT_SENSOR".equals(a.getUserData());
        boolean bIsFoot = "FOOT_SENSOR".equals(b.getUserData());

        if (!aIsFoot && !bIsFoot) return;

        Fixture tileFixture = aIsFoot ? b : a;

        if (tileFixture.getUserData() instanceof MapObject) {
            MapObject obj = (MapObject) tileFixture.getUserData();

            if (obj.getProperties().containsKey("surface")) {
                String surface = obj.getProperties().get("surface", String.class);
                Main.player.currentSurface = surface;
            }
        }

    }

    private void checkSensor(Fixture a, Fixture b) {
        Object dataA = a.getUserData();
        Object dataB = b.getUserData();

        processSensorData(dataA, a.getBody());
        processSensorData(dataB, b.getBody());
    }

    @Override
    public void endContact(Contact contact) {
        Fixture fa = contact.getFixtureA();
        Fixture fb = contact.getFixtureB();

        if ("FOOT_SENSOR".equals(fa.getUserData()) || "FOOT_SENSOR".equals(fb.getUserData())) {
            footContacts--;
        }
    }

    private void processSensorData(Object data, Body body) {
        if (data == null) return;

        if (data instanceof tilemapmanager.TransitionData) {
            pendingTransition = (tilemapmanager.TransitionData) data;
        }

        if ("KEY".equals(data)) {
            bodiesToDestroy.add(body);
        }
    }

    @Override public void preSolve(Contact contact, Manifold oldManifold) {}
    @Override public void postSolve(Contact contact, ContactImpulse impulse) {}
}
