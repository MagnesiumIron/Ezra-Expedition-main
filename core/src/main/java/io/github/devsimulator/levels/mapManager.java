package io.github.devsimulator.levels;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import io.github.devsimulator.Main;
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

        WorldContactListener.allSigns.clear();
        WorldContactListener.allRunes.clear();
        WorldContactListener.closestSign = null;
        WorldContactListener.closestRune = null;

        Array<Body> bodies = new Array<>();
        world.getBodies(bodies);
        for (Body b : bodies) {
            if (b != player.b2body) {
                world.destroyBody(b);
            }
        }


        currentLevel = newLevel;
        currentLevel.loadLevel(world, player, sandManager);
        currentMapPath = newLevel.mapPath;


        if (sandManager != null && currentLevel.map != null) {
            sandManager.initLevel(currentLevel.map);
        }


        player.b2body.setTransform(spawnX, spawnY, 0);
        player.b2body.setLinearVelocity(0, 0);


        WorldContactListener.footContacts = 0;

        //AMBIENCE SYSTEM
        String name = newLevel.getClass().getSimpleName();

        boolean isPrologue =
            name.equals("prologuespawn") ||
                name.equals("prologue1") ||
                name.equals("prologue2") ||
                name.equals("prologueend");

        boolean isLevel1 = name.equals("level1");
        if (Main.ambience != null && Main.ambience.isPlaying()) {
            Main.ambience.stop();
        }
        if (Main.level1Ambience != null && Main.level1Ambience.isPlaying()) {
            Main.level1Ambience.stop();
        }
        if (isPrologue) {
            if (Main.ambience != null && !Main.ambience.isPlaying()) {
                Main.ambience.play();
            }
        } else if (isLevel1) {
            if (Main.level1Ambience != null && !Main.level1Ambience.isPlaying()) {
                Main.level1Ambience.play();
            }
        }
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
