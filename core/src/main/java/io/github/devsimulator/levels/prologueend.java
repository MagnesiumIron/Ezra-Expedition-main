package io.github.devsimulator.levels;

import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.physics.box2d.World;
import io.github.devsimulator.controllers.SandManager;
import io.github.devsimulator.entities.Player;
import io.github.devsimulator.helper.tilemapmanager;

public class prologueend extends baseLevel {
    @Override
    public void loadLevel(World world, Player player, SandManager sandManager) {
        this.world = world;
        this.player = player;
        this.sandManager = sandManager;

        map = new TmxMapLoader().load("prologueassets/prologueend.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map);

        tilemapmanager.createBoundaries(map, world);
        sandManager.initLevel(map);

        System.out.println("Prologue Final Stage Loaded. Ready for Level 1 transition.");
    }

    @Override public void update(float dt) { checkScriptedEvents(); }
    @Override public void checkScriptedEvents() {
        // Scripted event for the "Falling from the Sky" transition to Level 1
    }
}
