package io.github.devsimulator.levels;

import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.physics.box2d.World;
import io.github.devsimulator.controllers.SandManager;
import io.github.devsimulator.entities.Player;
import io.github.devsimulator.helper.tilemapmanager;

public class prologuespawn extends baseLevel {
    @Override
    public void loadLevel(World world, Player player, SandManager sandManager) {
        this.world = world;
        this.player = player;
        this.sandManager = sandManager;

        map = new TmxMapLoader().load("prologueassets/prologuespawn.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map);

        tilemapmanager.createBoundaries(map, world);
        sandManager.initLevel(map);

        System.out.println("Prologue Spawn Loaded (Hardcoded Coordinates).");
    }

    @Override public void update(float dt) {}
    @Override public void checkScriptedEvents() {}
}
