package io.github.devsimulator.levels;

import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.physics.box2d.World;
import io.github.devsimulator.controllers.SandManager;
import io.github.devsimulator.entities.Player;
import io.github.devsimulator.helper.tilemapmanager;

public class prologue2 extends baseLevel {
    @Override
    public void loadLevel(World world, Player player, SandManager sandManager) {
        this.world = world;
        this.player = player;
        this.sandManager = sandManager;

        map = new TmxMapLoader().load("prologueassets/prologue2.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map);

        tilemapmanager.createBoundaries(map, world);
        sandManager.initLevel(map);

        System.out.println("Prologue Stage 2: Water & Spawners Loaded.");
    }

    @Override public void update(float dt) { checkScriptedEvents(); }
    @Override public void checkScriptedEvents() {
        // Logic to detect if the Water Container is full to open the exit
    }
}
