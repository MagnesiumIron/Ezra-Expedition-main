package io.github.devsimulator.levels;

import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.physics.box2d.World;
import io.github.devsimulator.controllers.SandManager;
import io.github.devsimulator.entities.Player;
import io.github.devsimulator.helper.tilemapmanager;

public class level2end extends baseLevel {

    @Override
    public void loadLevel(World world, Player player, SandManager sandManager) {
        this.world = world;
        this.player = player;
        this.sandManager = sandManager;

        this.mapPath = "level2end.tmx";

        map = new TmxMapLoader().load(this.mapPath);
        mapRenderer = new OrthogonalTiledMapRenderer(map);

        tilemapmanager.createBoundaries(map, world);
        sandManager.initLevel(map);

        System.out.println("Level 2 End (" + this.mapPath + ") Loaded Successfully!");
    }

    @Override
    public void update(float dt) {
        checkScriptedEvents();
    }

    @Override
    public void checkScriptedEvents() {
        // Ready for final Level 2 events, boss logic, or transitions!
    }
}
