package io.github.devsimulator.helper;

import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;

public class WorldContactListener implements ContactListener {

    public static Array<Body> bodiesToDestroy = new Array<>();
    public static tilemapmanager.TransitionData pendingTransition = null;
    public static int footContacts = 0;

    @Override
    public void beginContact(Contact contact) {
        Fixture a = contact.getFixtureA();
        Fixture b = contact.getFixtureB();

        // Foot Sensor touches a Wall
        if ((isFootSensor(a) && isWall(b)) || (isFootSensor(b) && isWall(a))) {
            footContacts++;
        }

        // Map Transition Sensor
        if (isPlayer(a) && isTransition(b)) {
            pendingTransition = (tilemapmanager.TransitionData) b.getUserData();
        } else if (isPlayer(b) && isTransition(a)) {
            pendingTransition = (tilemapmanager.TransitionData) a.getUserData();
        }

        // Collect Keys
        if (isPlayer(a) && isKey(b)) collectKey(b);
        else if (isPlayer(b) && isKey(a)) collectKey(a);

        // Goal
        if ((isPlayer(a) && isGoal(b)) || (isPlayer(b) && isGoal(a))) {
            System.out.println("Level Accomplished");
        }
    }

    @Override
    public void endContact(Contact contact) {
        Fixture a = contact.getFixtureA();
        Fixture b = contact.getFixtureB();

        if ((isFootSensor(a) && isWall(b)) || (isFootSensor(b) && isWall(a))) {
            footContacts--;
        }
    }

    private void collectKey(Fixture keyFixture) {
        System.out.println("KEY has been collected! Doors opening...");
        bodiesToDestroy.add(keyFixture.getBody());
    }

    private boolean isPlayer(Fixture f) { return "PLAYER".equals(f.getUserData()); }
    private boolean isKey(Fixture f) { return "KEY".equals(f.getUserData()); }
    private boolean isGoal(Fixture f) { return "GOAL".equals(f.getUserData()); }
    private boolean isFootSensor(Fixture f) { return "FOOT_SENSOR".equals(f.getUserData()); }
    private boolean isWall(Fixture f) { return "WALL".equals(f.getUserData()); }
    private boolean isTransition(Fixture f) {
        return f.getUserData() instanceof tilemapmanager.TransitionData;
    }

    @Override public void preSolve(Contact contact, Manifold oldManifold) {}
    @Override public void postSolve(Contact contact, ContactImpulse impulse) {}
}
