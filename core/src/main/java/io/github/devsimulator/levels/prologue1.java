package io.github.devsimulator.levels;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.physics.box2d.World;
import io.github.devsimulator.Main;
import io.github.devsimulator.controllers.SandManager;
import io.github.devsimulator.entities.Player;
import io.github.devsimulator.helper.tilemapmanager;
import io.github.devsimulator.entities.Slime;
import io.github.devsimulator.entities.GasSlime;
import io.github.devsimulator.entities.SandSlime;

public class prologue1 extends baseLevel {

    @Override
    public void loadLevel(World world, Player player, SandManager sandManager) {
        this.world = world;
        this.player = player;
        this.sandManager = sandManager;

        this.mapPath = "prologueassets/prologue1.tmx";
        // Path to your second tutorial map
        map = new TmxMapLoader().load(this.mapPath);
        mapRenderer = new OrthogonalTiledMapRenderer(map);

        tilemapmanager.createBoundaries(map, world);
        sandManager.initLevel(map);

        //adds the slimes and its types into the prologue level
        Slime normalSlime = new Slime(world, player, 500 / Main.PPM, 4000 / Main.PPM);
        // --- NORMAL SLIMES ---
        // Spawning a few at different spots to test movement and jumping
        enemies.add(new Slime(world, player, 4000 / Main.PPM, 4000 / Main.PPM));
        enemies.add(new Slime(world, player, 4500 / Main.PPM, 4000 / Main.PPM));

        // --- GAS SLIME (The Floater) ---
        // Spawned slightly higher (4200) to give it room to float down
        enemies.add(new GasSlime(world, player, 567 / Main.PPM, 4000 / Main.PPM));

         // --- SAND SLIME (The Stationary Turret) ---
        // Positioned at 1000 to keep it as a mid-range obstacle

        System.out.println("Prologue Stage 1: Abilities Loaded.");
    }

    @Override
    public void update(float dt) {
        checkScriptedEvents();
        updateEntities(dt);


    }

    @Override
    public void checkScriptedEvents() {
        // Logic here
    }
}
