package io.github.devsimulator;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import io.github.devsimulator.controllers.SandManager;
import io.github.devsimulator.entities.Player;
import io.github.devsimulator.helper.WorldContactListener;
import io.github.devsimulator.helper.tilemapmanager;
import io.github.devsimulator.helper.pauseMenu;

public class Main extends ApplicationAdapter {
    public static final float PPM = 32f; // 32 PIXELS
    private SpriteBatch batch;
    public World world;
    private Box2DDebugRenderer b2dr;
    private OrthographicCamera camera;
    public TiledMap map;
    public OrthogonalTiledMapRenderer mapRenderer;
    public Player player;
    public SandManager sandManager;
    private Texture hudTexture;


    private pauseMenu pauseMenu;
    private io.github.devsimulator.helper.mainMenu mainMenu;

    @Override
    public void create() {
        batch = new SpriteBatch();
        world = new World(new Vector2(0, -9.8f), true);
        world.setContactListener(new WorldContactListener());
        b2dr = new Box2DDebugRenderer();

        map = new TmxMapLoader().load("level1test.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map);
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 960, 640);

        tilemapmanager.createBoundaries(map, world);
        sandManager = new SandManager();
        sandManager.initLevel(map);

        player = new Player(world);


        mainMenu = new io.github.devsimulator.helper.mainMenu(player);
        pauseMenu = new io.github.devsimulator.helper.pauseMenu(player, mainMenu);

        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(1, 1, 1, 1);
        pix.fill();
        hudTexture = new Texture(pix);
        pix.dispose();
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1f);

        if (!mainMenu.isStarted) {
            mainMenu.render(Gdx.graphics.getDeltaTime());
        }
        else {
            pauseMenu.update();
            if (!pauseMenu.isPaused) {
                world.step(1/60f, 6, 2);

                if (WorldContactListener.pendingTransition != null) {
                    tilemapmanager.TransitionData data = WorldContactListener.pendingTransition;
                    tilemapmanager.loadLevel(this, data.targetMap, data.spawnX, data.spawnY);

                    WorldContactListener.pendingTransition = null;
                    WorldContactListener.bodiesToDestroy.clear();
                }
                else if (WorldContactListener.bodiesToDestroy.size > 0) {
                    for (Body b : WorldContactListener.bodiesToDestroy) {
                        if (b.getFixtureList().size > 0 && "KEY".equals(b.getFixtureList().first().getUserData())) {
                            destroyAllDoors();
                        }
                        world.destroyBody(b);
                    }
                    WorldContactListener.bodiesToDestroy.clear();
                }

                sandManager.update();
                if (player != null) player.update(Gdx.graphics.getDeltaTime(), sandManager.sim);

                if(player != null) {
                    camera.position.x = player.b2body.getPosition().x * PPM;
                    camera.position.y = player.b2body.getPosition().y * PPM;
                    camera.update();
                }
            }

            mapRenderer.setView(camera);

            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            sandManager.render(batch);
            batch.end();

            mapRenderer.render();

            b2dr.render(world, camera.combined);

            batch.begin();
            if (player != null) player.draw(batch);
            drawHUD();
            batch.end();

            pauseMenu.render(Gdx.graphics.getDeltaTime());
        }
    }

    private void destroyAllDoors() {
        Array<Body> bodies = new Array<>();
        world.getBodies(bodies);
        for (Body b : bodies) {
            if (b.getFixtureList().size > 0 && "DOOR".equals(b.getFixtureList().first().getUserData())) {
                world.destroyBody(b);
            }
        }
    }

    private void drawHUD() {
        if(player == null) return;
        float x = camera.position.x - 400;
        float y = camera.position.y + 250;

        // Assimilation Bar (Red)
        batch.setColor(0,0,0,1);
        batch.draw(hudTexture, x, y, 204, 14);

        batch.setColor(1,0,0,1);
        batch.draw(hudTexture, x+2, y+2, 200 * player.getMassPercentage(), 10);

        // HP Bar (Green)
        batch.setColor(0,0,0,1);
        batch.draw(hudTexture, x, y - 20, 204, 14);

        batch.setColor(0,1,0,1);
        batch.draw(hudTexture, x+2, y - 18, 200 * (player.hp / player.MAX_HP), 10);

        batch.setColor(1,1,1,1);
    }

    @Override
    public void dispose() {
        batch.dispose();
        world.dispose();
        map.dispose();
        mapRenderer.dispose();
        sandManager.dispose();
        if(hudTexture != null) hudTexture.dispose();
        if(pauseMenu != null) pauseMenu.dispose();
        if(mainMenu != null) mainMenu.dispose();
    }
}
