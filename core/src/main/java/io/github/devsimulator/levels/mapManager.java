package io.github.devsimulator.levels;

import com.badlogic.gdx.graphics.OrthographicCamera;
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

        if (targetMapName.contains("prologuespawn.tmx")) nextLevel = new prologuespawn();
        else if (targetMapName.contains("prologue1.tmx")) nextLevel = new prologue1();
        else if (targetMapName.contains("prologue2.tmx")) nextLevel = new prologue2();
        else if (targetMapName.contains("prologueend.tmx")) nextLevel = new prologueend();
        else if (targetMapName.contains("level1fr.tmx") || targetMapName.equals("level1")) nextLevel = new level1();
        else if (targetMapName.contains("level1fr2.tmx")) nextLevel = new level1fr2();
        else if (targetMapName.contains("level2.tmx")) nextLevel = new level2();
        else if (targetMapName.contains("level2-1.tmx")) nextLevel = new level2_1();
        else if (targetMapName.contains("level2end.tmx")) nextLevel = new level2end();
        else if (targetMapName.contains("level3.tmx")) nextLevel = new level3();
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
        WorldContactListener.bodiesToDestroy.clear();
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

        player.saveRoomCheckpoint(spawnX, spawnY);

        if (currentLevel.map != null) {
            // reset Portal graphics on map load
            com.badlogic.gdx.maps.MapLayer closedLayer = currentLevel.map.getLayers().get("portal_closed");
            com.badlogic.gdx.maps.MapLayer openLayer = currentLevel.map.getLayers().get("portal_open");
            if (closedLayer != null) closedLayer.setVisible(true);
            if (openLayer != null) openLayer.setVisible(false);

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

        player.b2body.setTransform(spawnX, spawnY, 0);
        player.b2body.setLinearVelocity(0, 0);
        WorldContactListener.footContacts = 0;
    }

    public void fastRoomReload() {
        player.loadRoomCheckpoint();
        transitionToMap(currentMapPath, player.chkSpawnX, player.chkSpawnY);

        player.b2body.setTransform(player.chkSpawnX, player.chkSpawnY, 0);
        player.b2body.setLinearVelocity(0, 0);
        player.b2body.setAwake(true);
    }

    public void update(float dt) {
        if (!io.github.devsimulator.helper.WorldContactListener.bodiesToDestroy.isEmpty()) {
            for (com.badlogic.gdx.physics.box2d.Body b : io.github.devsimulator.helper.WorldContactListener.bodiesToDestroy) {
                if (b != null) {
                    world.destroyBody(b); // to safely destroy the physics body
                }
            }
            io.github.devsimulator.helper.WorldContactListener.bodiesToDestroy.clear(); // Empty the trash can
        }

        if (currentLevel != null) {
            currentLevel.update(dt);
            currentLevel.updateEntities(dt);
            com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile.updateAnimationBaseTime();

            // KEY ERASURE
            if (player != null && player.triggerKeyVisualRemoval && currentLevel.map != null) {
                player.triggerKeyVisualRemoval = false;

                com.badlogic.gdx.maps.MapLayer layer = currentLevel.map.getLayers().get("keys_visual");

                if (layer instanceof com.badlogic.gdx.maps.tiled.TiledMapTileLayer) {
                    com.badlogic.gdx.maps.tiled.TiledMapTileLayer visualLayer = (com.badlogic.gdx.maps.tiled.TiledMapTileLayer) layer;

                    int cellX = (int) ((player.keyPosToRemove.x * Main.PPM) / visualLayer.getTileWidth());
                    int cellY = (int) ((player.keyPosToRemove.y * Main.PPM) / visualLayer.getTileHeight());

                    com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell cell = visualLayer.getCell(cellX, cellY);
                    if (cell != null) cell.setTile(null);
                }
            }

            // PORTAL ANIMATION LOGIC
            if (player != null && player.triggerPortalVisuals && currentLevel.map != null) {
                player.triggerPortalVisuals = false;

                com.badlogic.gdx.maps.MapLayer closedLayer = currentLevel.map.getLayers().get("portal_closed");
                com.badlogic.gdx.maps.MapLayer openLayer = currentLevel.map.getLayers().get("portal_open");

                if (closedLayer != null) closedLayer.setVisible(false);
                if (openLayer != null) openLayer.setVisible(true);
            }
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
