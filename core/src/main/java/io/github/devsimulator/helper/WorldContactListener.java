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

        //collect all keys
        if (isPlayer(a) && isKey(b)) collectKey(b);
        else if (isPlayer(b) && isKey(a)) collectKey(a);

        // accomplished the goal
        if (isPlayer(a) && isGoal(b) || isPlayer(b) && isGoal(a)) {
            System.out.println("Level Accomplished");
        }
    }

    private void collectKey(Fixture keyFixture) {
        System.out.println("KEY has been collected! Doors opening...");
        bodiesToDestroy.add(keyFixture.getBody()); // Destroy the key
        // Logic to destroy the door will be handled in Main loop by checking user data
    }

    private boolean isPlayer(Fixture f) { return "PLAYER".equals(f.getUserData()); }
    private boolean isKey(Fixture f) { return "KEY".equals(f.getUserData()); }
    private boolean isGoal(Fixture f) { return "GOAL".equals(f.getUserData()); }

    @Override public void endContact(Contact contact) {}
    @Override public void preSolve(Contact contact, Manifold oldManifold) {}
    @Override public void postSolve(Contact contact, ContactImpulse impulse) {}
}
