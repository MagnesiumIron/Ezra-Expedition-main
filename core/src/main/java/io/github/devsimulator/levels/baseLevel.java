package io.github.devsimulator.levels;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.physics.box2d.World;
import io.github.devsimulator.controllers.SandManager;
import io.github.devsimulator.entities.Enemy;
import io.github.devsimulator.entities.Player;

public abstract class baseLevel {
    public String mapPath = "unknown";
    public TiledMap map;
    public OrthogonalTiledMapRenderer mapRenderer;

    protected World world;
    protected Player player;
    protected SandManager sandManager;

    public com.badlogic.gdx.utils.Array<Enemy> enemies = new com.badlogic.gdx.utils.Array<>();
    public abstract void loadLevel(World world, Player player, SandManager sandManager);
    public abstract void update(float dt);
    public abstract void checkScriptedEvents();

    public void renderBackground(OrthographicCamera camera) {
        if (mapRenderer != null) {
            mapRenderer.setView(camera);
            mapRenderer.render();
        }
    }

    public void updateEntities(float dt) {
        for (Enemy e : enemies) {
            e.update(dt, sandManager);
        }
    }

    public void renderEntities(com.badlogic.gdx.graphics.g2d.SpriteBatch batch) {
        for (Enemy e : enemies) {
            e.draw(batch);
        }
    }

    public void dispose() {
        if (map != null) map.dispose();
        if (mapRenderer != null) mapRenderer.dispose();
        for (Enemy e : enemies) e.dispose();
    }
}
