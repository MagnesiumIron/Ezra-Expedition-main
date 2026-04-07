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

        player.saveRoomCheckpoint(spawnX, spawnY);

        // reset player position
        player.b2body.setTransform(spawnX, spawnY, 0);
        player.b2body.setLinearVelocity(0, 0);

        if (currentLevel.map != null) {
            com.badlogic.gdx.maps.MapLayer enemyLayer = currentLevel.map.getLayers().get("enemies");
            if (enemyLayer != null) {
                for (com.badlogic.gdx.maps.MapObject object : enemyLayer.getObjects().getByType(com.badlogic.gdx.maps.objects.RectangleMapObject.class)) {
                    com.badlogic.gdx.math.Rectangle rect = ((com.badlogic.gdx.maps.objects.RectangleMapObject) object).getRectangle();
                    String type = object.getProperties().get("enemyType", "slime", String.class);

                    if (type.equalsIgnoreCase("slime")) {
                        currentLevel.enemies.add(new io.github.devsimulator.entities.Slime(world, player, rect.getX(), rect.getY()));
                    } else if (type.equalsIgnoreCase("gasSlime")) {
                        currentLevel.enemies.add(new io.github.devsimulator.entities.GasSlime(world, player, rect.getX(), rect.getY()));
                    }
                }
            }
        }

        // reset player position
        player.b2body.setTransform(spawnX, spawnY, 0);
        player.b2body.setLinearVelocity(0, 0);

        // reset physics contacts
        WorldContactListener.footContacts = 0;
    }

    public void fastRoomReload() {
        // reset elements present
        if (sandManager != null && sandManager.sim != null) {
            sandManager.sim.clearToPool();
            sandManager.completedGeysers.clear(); //

            if (currentLevel != null && currentLevel.map != null) {
                sandManager.spawnLayer(currentLevel.map, "sand_zones", io.github.devsimulator.elements.ElementType.SAND);
                sandManager.spawnLayer(currentLevel.map, "water_zones", io.github.devsimulator.elements.ElementType.WATER);
                sandManager.spawnLayer(currentLevel.map, "lava_zones", io.github.devsimulator.elements.ElementType.LAVA);
            }
        }

        // unconsume runes
        for (io.github.devsimulator.helper.tilemapmanager.RuneData rune : io.github.devsimulator.helper.WorldContactListener.allRunes) {
            rune.isConsumed = false;
        }

        // revive and teleport enemies
        if (currentLevel != null) {
            for (io.github.devsimulator.entities.Enemy e : currentLevel.enemies) {
                e.resetState();
            }
        }

        player.loadRoomCheckpoint();
        player.b2body.setTransform(player.chkSpawnX, player.chkSpawnY, 0);
        player.b2body.setLinearVelocity(0, 0);
        player.b2body.setAwake(true);
    }

    public void update(float dt) {
        if (currentLevel != null) {
            currentLevel.update(dt);
            currentLevel.updateEntities(dt);
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
