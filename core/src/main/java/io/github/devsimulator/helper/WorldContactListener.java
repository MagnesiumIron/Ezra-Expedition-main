package io.github.devsimulator.helper;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import io.github.devsimulator.Main; // Fixes the "Cannot find Main" error
import io.github.devsimulator.helper.tilemapmanager.InteractableData;
import io.github.devsimulator.helper.tilemapmanager.RuneData;

public class WorldContactListener implements ContactListener {

    // --- STATIC STORAGE (The "Phonebook" of the current map) ---
    public static Array<InteractableData> allSigns = new Array<>();
    public static Array<RuneData> allRunes = new Array<>();

    // These store the absolute closest object to Ezra at any given moment
    public static InteractableData closestSign = null;
    public static RuneData closestRune = null;

    // --- PHYSICS STATE ---
    public static int footContacts = 0;
    public static Array<Body> bodiesToDestroy = new Array<>();
    public static tilemapmanager.TransitionData pendingTransition = null;

    // --- SORTING LOGIC (Calculates distance every frame) ---
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

        // 1. Foot Contacts (For Jumping)
        if (fa.getUserData() == "FOOT" || fb.getUserData() == "FOOT") footContacts++;

        // 2. Sensors (Transitions, Keys, Runes, Signs)
        checkSensor(fa, fb);
    }

    private void checkSensor(Fixture a, Fixture b) {
        Object dataA = a.getUserData();
        Object dataB = b.getUserData();

        // Check A
        processSensorData(dataA, a.getBody());
        // Check B
        processSensorData(dataB, b.getBody());
    }

    private void processSensorData(Object data, Body body) {
        if (data == null) return;

        // Transition Logic
        if (data instanceof tilemapmanager.TransitionData) {
            pendingTransition = (tilemapmanager.TransitionData) data;
        }

        if ("KEY".equals(data)) {
            bodiesToDestroy.add(body);
        }

    }

    @Override
    public void endContact(Contact contact) {
        Fixture fa = contact.getFixtureA();
        Fixture fb = contact.getFixtureB();
        if (fa.getUserData() == "FOOT" || fb.getUserData() == "FOOT") footContacts--;
    }

    @Override public void preSolve(Contact contact, Manifold oldManifold) {}
    @Override public void postSolve(Contact contact, ContactImpulse impulse) {}
}
