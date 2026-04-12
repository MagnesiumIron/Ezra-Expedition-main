package io.github.devsimulator.levels;

import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.physics.box2d.World;
import io.github.devsimulator.controllers.SandManager;
import io.github.devsimulator.entities.Player;
import io.github.devsimulator.helper.tilemapmanager; // Added this import

public class level3 extends baseLevel {

    public level3() {
        // Ensure this matches the folder structure in your assets
        this.mapPath = "level3assets/level3.tmx";
    }

    @Override
    public void loadLevel(World world, Player player, SandManager sandManager) {
        this.world = world;
        this.player = player;
        this.sandManager = sandManager;

        // 1. Load the map and setup the renderer
        map = new TmxMapLoader().load(mapPath);
        mapRenderer = new OrthogonalTiledMapRenderer(map);

        // 2. THE FIX: Create the static Box2D boundaries from the Tiled map
        // Without this line, Ezra has no floor!
        tilemapmanager.createBoundaries(map, world);

        // 3. Initialize the pixel physics (Sand, Water, Lava, Runes)
        if (sandManager != null) {
            sandManager.player = player;
            sandManager.initLevel(map);
        }

        // 4. Clear existing entities to prevent "ghost" enemies from previous levels
        enemies.clear();
        projectiles.clear();
        drops.clear();

        System.out.println("Level 3 (" + this.mapPath + ") Loaded Successfully!");
    }

    @Override
    public void update(float dt) {
        checkScriptedEvents();
    }

    @Override
    public void checkScriptedEvents() {
        // Place Level 3 specific triggers here (e.g. Boss spawns)
    }
}
