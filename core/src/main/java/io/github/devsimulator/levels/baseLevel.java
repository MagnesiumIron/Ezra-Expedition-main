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
    public com.badlogic.gdx.utils.Array<io.github.devsimulator.entities.ItemDrop> drops = new com.badlogic.gdx.utils.Array<>();
    public com.badlogic.gdx.utils.Array<io.github.devsimulator.entities.SandProjectile> projectiles = new com.badlogic.gdx.utils.Array<>(); //projectile array
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
            boolean wasAlive = e.isAlive;
            e.update(dt, sandManager);
            if (wasAlive && !e.isAlive) {
                // drop ammo (element charge) item
                String dropEl = Math.random() < 0.5 ? "WATER" : "LAVA";
                drops.add(new io.github.devsimulator.entities.ItemDrop(world, e.b2body.getPosition().x, e.b2body.getPosition().y, dropEl));
            }
        }
        for (int i = projectiles.size - 1; i >= 0; i--) { //for updating and destryong old projectiles
            io.github.devsimulator.entities.SandProjectile p = projectiles.get(i);
            p.update(dt);
            if (p.isDestroyed) {
                if (p.element.equals("LAVA") && sandManager != null) {
                    int px = (int)(p.b2body.getPosition().x * io.github.devsimulator.Main.PPM / 4f);
                    int py = (int)(p.b2body.getPosition().y * io.github.devsimulator.Main.PPM / 4f);
                    int radius = p.isMega ? 4 : 1;
                    for(int x = -radius; x <= radius; x++){
                        for(int y = -radius; y <= radius; y++){
                            if(sandManager.sim.isEmpty(px+x, py+y)) {
                                sandManager.sim.setElement(px+x, py+y, io.github.devsimulator.elements.ElementType.LAVA.create(px+x, py+y));
                            }
                        }
                    }
                }
                world.destroyBody(p.b2body);
                p.dispose();
                projectiles.removeIndex(i);
            }
        }
        for (int i = drops.size - 1; i >= 0; i--) {
            io.github.devsimulator.entities.ItemDrop d = drops.get(i);
            if (d.isDestroyed) {
                world.destroyBody(d.b2body);
                d.dispose();
                drops.removeIndex(i);
            }
        }
    }

    public void renderEntities(com.badlogic.gdx.graphics.g2d.SpriteBatch batch) {
        for (Enemy e : enemies) {
            e.draw(batch);
        }
        for (io.github.devsimulator.entities.SandProjectile p : projectiles) {
            p.draw(batch);
        }
        for (io.github.devsimulator.entities.ItemDrop d : drops) {
            d.draw(batch);
        }
    }

    public void dispose() {
        if (map != null) map.dispose();
        if (mapRenderer != null) mapRenderer.dispose();
        for (Enemy e : enemies) e.dispose();
        for (io.github.devsimulator.entities.SandProjectile p : projectiles) p.dispose();
        for (io.github.devsimulator.entities.ItemDrop d : drops) d.dispose();
    }
}
