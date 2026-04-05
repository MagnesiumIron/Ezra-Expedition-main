package io.github.devsimulator.helper;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.LongMap;
import io.github.devsimulator.Main;
import io.github.devsimulator.helper.tilemapmanager.InteractableData;
import io.github.devsimulator.helper.tilemapmanager.RuneData;

public class WorldContactListener implements ContactListener {

    public static final float BUCKET_SIZE = 8f;
    public static LongMap<Array<InteractableData>> signHash = new LongMap<>();
    public static LongMap<Array<RuneData>> runeHash = new LongMap<>();
    public static Array<RuneData> allRunes = new Array<>();

    public static InteractableData closestSign = null;
    public static RuneData closestRune = null;

    public static int footContacts = 0;
    public static Array<Body> bodiesToDestroy = new Array<>();
    public static tilemapmanager.TransitionData pendingTransition = null;

    private static long getBucketKey(float x, float y) {
        int bx = (int) (x / BUCKET_SIZE);
        int by = (int) (y / BUCKET_SIZE);
        return (((long) bx) << 32) | (by & 0xffffffffL);
    }

    public static void addSign(InteractableData sign) {
        long key = getBucketKey(sign.worldX, sign.worldY);
        if (!signHash.containsKey(key)) {
            signHash.put(key, new Array<>());
        }
        signHash.get(key).add(sign);
    }

    public static void addRune(RuneData rune) {
        long key = getBucketKey(rune.worldX, rune.worldY);
        if (!runeHash.containsKey(key)) {
            runeHash.put(key, new Array<>());
        }
        runeHash.get(key).add(rune);
        allRunes.add(rune);
    }

    public static void clearHashes() {
        signHash.clear();
        runeHash.clear();
        allRunes.clear();
    }

    public static void sortSignsByDistance(Vector2 playerPos) {
        closestSign = null;
        float minDst = Float.MAX_VALUE;
        float px = playerPos.x / Main.PPM;
        float py = playerPos.y / Main.PPM;

        int bx = (int) (px / BUCKET_SIZE);
        int by = (int) (py / BUCKET_SIZE);

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int queryX = bx + i;
                int queryY = by + j;
                long key = (((long) queryX) << 32) | (queryY & 0xffffffffL);
                Array<InteractableData> bucket = signHash.get(key);

                if (bucket != null) {
                    for (InteractableData sign : bucket) {
                        float dst = playerPos.dst(sign.worldX * Main.PPM, sign.worldY * Main.PPM);
                        if (dst < minDst) {
                            minDst = dst;
                            closestSign = sign;
                        }
                    }
                }
            }
        }
    }

    public static void sortRunesByDistance(Vector2 playerPos) {
        closestRune = null;
        float minDst = Float.MAX_VALUE;
        float px = playerPos.x / Main.PPM;
        float py = playerPos.y / Main.PPM;

        int bx = (int) (px / BUCKET_SIZE);
        int by = (int) (py / BUCKET_SIZE);

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int queryX = bx + i;
                int queryY = by + j;
                long key = (((long) queryX) << 32) | (queryY & 0xffffffffL);
                Array<RuneData> bucket = runeHash.get(key);

                if (bucket != null) {
                    for (RuneData rune : bucket) {
                        float dst = playerPos.dst(rune.worldX * Main.PPM, rune.worldY * Main.PPM);
                        if (dst < minDst) {
                            minDst = dst;
                            closestRune = rune;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void beginContact(Contact contact) {
        Fixture fa = contact.getFixtureA();
        Fixture fb = contact.getFixtureB();

        if ("FOOT_SENSOR".equals(fa.getUserData()) || "FOOT_SENSOR".equals(fb.getUserData())) {
            footContacts++;
        }

        checkSensor(fa, fb);
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
