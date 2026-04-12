package io.github.devsimulator.helper;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.LongMap;
import io.github.devsimulator.Main;
import io.github.devsimulator.entities.Enemy;
import io.github.devsimulator.entities.Player;
import io.github.devsimulator.helper.tilemapmanager.InteractableData;
import io.github.devsimulator.helper.tilemapmanager.RuneData;

public class WorldContactListener implements ContactListener {

    public static Player playerInstance;
    public static final float BUCKET_SIZE = 8f;
    public static LongMap<Array<InteractableData>> signHash = new LongMap<>();
    public static LongMap<Array<RuneData>> runeHash = new LongMap<>();
    public static Array<RuneData> allRunes = new Array<>();

    public static InteractableData closestSign = null;
    public static RuneData closestRune = null;

    public static int footContacts = 0;
    public static Array<Body> bodiesToDestroy = new Array<>();

    public static tilemapmanager.TransitionData pendingTransition = null;
    public static boolean pendingFastReload = false;
    public static boolean pendingDemoEnd = false;

    private static long getBucketKey(float x, float y) {
        int bx = (int) (x / BUCKET_SIZE);
        int by = (int) (y / BUCKET_SIZE);
        return (((long) bx) << 32) | (by & 0xffffffffL);
    }

    public static void addSign(InteractableData sign) {
        long key = getBucketKey(sign.worldX, sign.worldY);
        if (!signHash.containsKey(key)) signHash.put(key, new Array<>());
        signHash.get(key).add(sign);
    }

    public static void addRune(RuneData rune) {
        long key = getBucketKey(rune.worldX, rune.worldY);
        if (!runeHash.containsKey(key)) runeHash.put(key, new Array<>());
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
                long key = (((long) (bx + i)) << 32) | ((by + j) & 0xffffffffL);
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
                long key = (((long) (bx + i)) << 32) | ((by + j) & 0xffffffffL);
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
        Object dataA = fa.getUserData();
        Object dataB = fb.getUserData();

        // --- FIX: FOOT SENSOR ONLY COUNTS SOLID GROUND ---
        if ("FOOT_SENSOR".equals(dataA) && !fb.isSensor()) footContacts++;
        else if ("FOOT_SENSOR".equals(dataB) && !fa.isSensor()) footContacts++;

        // Player/Enemy Collision
        if ("PLAYER".equals(dataA) && dataB instanceof Enemy) handleEnemyHit((Enemy) dataB);
        else if ("PLAYER".equals(dataB) && dataA instanceof Enemy) handleEnemyHit((Enemy) dataA);

            // Projectile/Enemy Collision
        else if (dataA instanceof io.github.devsimulator.entities.SandProjectile && dataB instanceof Enemy) {
            handleProjectileHit((io.github.devsimulator.entities.SandProjectile) dataA, (Enemy) dataB);
        } else if (dataB instanceof io.github.devsimulator.entities.SandProjectile && dataA instanceof Enemy) {
            handleProjectileHit((io.github.devsimulator.entities.SandProjectile) dataB, (Enemy) dataA);
        }

        // Projectile hitting walls
        else if (dataA instanceof io.github.devsimulator.entities.SandProjectile && "GROUND".equals(dataB)) {
            ((io.github.devsimulator.entities.SandProjectile) dataA).isDestroyed = true;
        } else if (dataB instanceof io.github.devsimulator.entities.SandProjectile && "GROUND".equals(dataA)) {
            ((io.github.devsimulator.entities.SandProjectile) dataB).isDestroyed = true;
        }

        // Item Drop Pickups
        else if ("PLAYER".equals(dataA) && dataB instanceof io.github.devsimulator.entities.ItemDrop) {
            handleDropPickup((io.github.devsimulator.entities.ItemDrop) dataB);
        } else if ("PLAYER".equals(dataB) && dataA instanceof io.github.devsimulator.entities.ItemDrop) {
            handleDropPickup((io.github.devsimulator.entities.ItemDrop) dataA);
        }

        checkSensor(fa, fb);
    }

    private void handleEnemyHit(Enemy enemy) {
        if (!enemy.isAlive || playerInstance == null) return;
        float knockbackDir = (playerInstance.b2body.getPosition().x < enemy.b2body.getPosition().x) ? -4f : 4f;
        playerInstance.takeDamage(15f, knockbackDir, null);
    }

    private void handleProjectileHit(io.github.devsimulator.entities.SandProjectile proj, Enemy enemy) {
        if (proj.isDestroyed || !enemy.isAlive) return;

        proj.pierceCount--;
        if (proj.pierceCount <= 0) proj.isDestroyed = true;

        float dmg = proj.isMega ? 75f : 25f;
        float kb = (proj.b2body.getPosition().x < enemy.b2body.getPosition().x) ? 3f : -3f;
        enemy.takeDamage(dmg, kb);
    }

    private void handleDropPickup(io.github.devsimulator.entities.ItemDrop drop) {
        if (drop.isDestroyed || playerInstance == null) return;

        int slot = playerInstance.activeSlot;
        if (playerInstance.elementSlots[slot].equals(drop.element)) {
            playerInstance.chargeSlots[slot] = Math.min(playerInstance.chargeSlots[slot] + 1, playerInstance.maxCharges);
            drop.isDestroyed = true;
            if (Main.assimilationIN != null) Main.assimilationIN.play(0.5f);
        } else if (playerInstance.elementSlots[slot].equals("NONE")) {
            playerInstance.elementSlots[slot] = drop.element;
            playerInstance.chargeSlots[slot] = 1;
            drop.isDestroyed = true;
            if (Main.assimilationIN != null) Main.assimilationIN.play(0.8f);
        }
    }

    private void checkSensor(Fixture a, Fixture b) {
        processSensorData(a, b);
        processSensorData(b, a);
    }

    @Override
    public void endContact(Contact contact) {
        Fixture fa = contact.getFixtureA();
        Fixture fb = contact.getFixtureB();

        if ("FOOT_SENSOR".equals(fa.getUserData()) && !fb.isSensor()) footContacts--;
        else if ("FOOT_SENSOR".equals(fb.getUserData()) && !fa.isSensor()) footContacts--;
    }

    private void processSensorData(Fixture sensorFixture, Fixture touchingFixture) {
        Object sensorData = sensorFixture.getUserData();
        if (sensorData == null || playerInstance == null) return;

        boolean isEzra = (touchingFixture.getBody() == playerInstance.b2body || "PLAYER".equals(touchingFixture.getUserData()));
        if (!isEzra) return;

        // --- MAP TRANSITIONS & DEMO END LOGIC ---
        if (sensorData instanceof tilemapmanager.TransitionData) {
            tilemapmanager.TransitionData tData = (tilemapmanager.TransitionData) sensorData;

            // DEBUG LOG 1: Tells us if the portal was touched and what it's named
            System.out.println("DEBUG: Hit Portal -> targetMap: " + tData.targetMap);

            if (tData.requiresKey && !playerInstance.isPortalOpen) {
                // DEBUG LOG 2: Tells us if Tiled is marking this portal as a locked door
                System.out.println("DEBUG: Blocked! Door is locked (requiresKey is true).");
                playerInstance.showLockedMessage = true;
                return;
            }

            if ("demo_end".equals(tData.targetMap)) {
                // DEBUG LOG 3: Tells us the end screen successfully triggered
                System.out.println("DEBUG: DEMO_END SIGNAL SENT!");
                pendingDemoEnd = true;
            } else {
                pendingTransition = tData;
            }
        }

        // --- KEY COLLECTION ---
        if ("KEY".equals(sensorData)) {
            Body keyBody = sensorFixture.getBody();
            if (!bodiesToDestroy.contains(keyBody, true)) {
                playerInstance.keysCollected++;
                playerInstance.triggerKeyVisualRemoval = true;
                playerInstance.keyPosToRemove.set(keyBody.getPosition().x, keyBody.getPosition().y);

                bodiesToDestroy.add(keyBody);

                if (playerInstance.keysCollected >= playerInstance.totalKeysInLevel) {
                    playerInstance.isPortalOpen = true;
                    playerInstance.triggerPortalVisuals = true;
                }

                if (Main.assimilationIN != null) Main.assimilationIN.play(1.5f);
            }
        }
    }

    @Override public void preSolve(Contact contact, Manifold oldManifold) {}
    @Override public void postSolve(Contact contact, ContactImpulse impulse) {}
}
