package io.github.devsimulator.helper;

import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;

public class WorldContactListener implements ContactListener {

    // List of bodies (Doors/Keys) to destroy after physics step
    public static Array<Body> bodiesToDestroy = new Array<Body>();

    @Override
    public void beginContact(Contact contact) {
        Fixture a = contact.getFixtureA();
        Fixture b = contact.getFixtureB();

<<<<<<< Updated upstream
        //collect all keys
        if (isPlayer(a) && isKey(b)) collectKey(b);
        else if (isPlayer(b) && isKey(a)) collectKey(a);

        // accomplished the goal
        if (isPlayer(a) && isGoal(b) || isPlayer(b) && isGoal(a)) {
            System.out.println("Level Accomplished");
=======
        // 1. Foot Contacts (For Jumping)
        if ("FOOT_SENSOR".equals(fa.getUserData()) || "FOOT_SENSOR".equals(fb.getUserData())) {
            footContacts++;
        }

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

        // Transition Logic
        if (data instanceof tilemapmanager.TransitionData) {
            pendingTransition = (tilemapmanager.TransitionData) data;
>>>>>>> Stashed changes
        }
    }

<<<<<<< Updated upstream
<<<<<<< Updated upstream
    private void collectKey(Fixture keyFixture) {
        System.out.println("KEY has been collected! Doors opening...");
        bodiesToDestroy.add(keyFixture.getBody()); // Destroy the key
        // Logic to destroy the door will be handled in Main loop by checking user data
    }

    private boolean isPlayer(Fixture f) { return "PLAYER".equals(f.getUserData()); }
    private boolean isKey(Fixture f) { return "KEY".equals(f.getUserData()); }
    private boolean isGoal(Fixture f) { return "GOAL".equals(f.getUserData()); }

    @Override public void endContact(Contact contact) {}
=======
>>>>>>> Stashed changes
=======
>>>>>>> Stashed changes
    @Override public void preSolve(Contact contact, Manifold oldManifold) {}
    @Override public void postSolve(Contact contact, ContactImpulse impulse) {}
}
