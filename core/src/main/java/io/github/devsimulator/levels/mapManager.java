package io.github.devsimulator.levels;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import io.github.devsimulator.controllers.SandManager;
import io.github.devsimulator.entities.Player;
import io.github.devsimulator.helper.WorldContactListener;

public class mapManager {
    public static String currentMapPath = "prologueassets/prologuespawn.tmx";
    public baseLevel currentLevel;
    private World world;
    private Player player;
    private SandManager sandManager;

    public mapManager(World world, Player player, SandManager sandManager) {
        this.world = world;
        this.player = player;
        this.sandManager = sandManager;
    }

    public void transitionToMap(String targetMapName, float spawnX, float spawnY) {
        baseLevel nextLevel = null;

        // Routing logic
        if (targetMapName.contains("prologuespawn.tmx")) nextLevel = new prologuespawn();
        else if (targetMapName.contains("prologue1.tmx")) nextLevel = new prologue1();
        else if (targetMapName.contains("prologue2.tmx")) nextLevel = new prologue2();
        else if (targetMapName.contains("prologueend.tmx")) nextLevel = new prologueend();
        else if (targetMapName.contains("level1fr.tmx") || targetMapName.equals("level1")) nextLevel = new level1();
        else if (targetMapName.contains("level1fr2.tmx")) nextLevel = new level1fr2();
        else if (targetMapName.contains("level2.tmx")) nextLevel = new level2();

        if (nextLevel != null) {
            changeLevel(nextLevel, spawnX, spawnY);
        } else {
            System.err.println("WARNING: mapManager doesn't know how to route: " + targetMapName);
        }
    }

    public void changeLevel(baseLevel newLevel, float spawnX, float spawnY) {
        if (currentLevel != null) {
            currentLevel.dispose();
        }

        WorldContactListener.clearHashes();
        WorldContactListener.closestSign = null;
        WorldContactListener.closestRune = null;

        Array<Body> bodies = new Array<>();
        world.getBodies(bodies);
        for (Body b : bodies) {
            // Keep Ezra, destroy everything else
            if (b != player.b2body) {
                world.destroyBody(b);
            }
        }

        currentLevel = newLevel;
        // load level
        currentLevel.loadLevel(world, player, sandManager);
        currentMapPath = newLevel.mapPath;

        // call sandmanager to update the tile map
        if (sandManager != null && currentLevel.map != null) {
            sandManager.initLevel(currentLevel.map);
        }

        // reset player position
        player.b2body.setTransform(spawnX, spawnY, 0);
        player.b2body.setLinearVelocity(0, 0);

        // reset physics contacts
        WorldContactListener.footContacts = 0;
    }

    public void update(float dt) {
        if (currentLevel != null) {
            currentLevel.update(dt);
            AnimatedTiledMapTile.updateAnimationBaseTime();
        }
    }

    public void renderBackground(OrthographicCamera camera) {
        if (currentLevel != null) {
            currentLevel.renderBackground(camera);
        }
    }

    public void dispose() {
        if (currentLevel != null) {
            currentLevel.dispose();
        }
    }
}
