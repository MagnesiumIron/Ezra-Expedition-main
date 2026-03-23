package io.github.devsimulator;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
import com.badlogic.gdx.math.MathUtils;

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
    private Texture bgTexture, hpTexture, arTexture;
    private TextureRegion hpRegion, arRegion;


    private pauseMenu pauseMenu;
    private io.github.devsimulator.helper.mainMenu mainMenu;

    public static String checkpointMap = "level1test.tmx";
    public static float checkpointX = 100 / PPM;
    public static float checkpointY = 200 / PPM;
    public static float checkpointHP = 100;
    public static float checkpointAssim = 0;
    @Override
    public void create() {
        batch = new SpriteBatch();
        world = new World(new Vector2(0, -9.8f), true);
        world.setContactListener(new WorldContactListener());
        b2dr = new Box2DDebugRenderer();

        map = new TmxMapLoader().load("level1test.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map);
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 720, 480);

        tilemapmanager.createBoundaries(map, world);
        sandManager = new SandManager();
        sandManager.initLevel(map);

        player = new Player(world);


        mainMenu = new io.github.devsimulator.helper.mainMenu(player);
        pauseMenu = new io.github.devsimulator.helper.pauseMenu(player, mainMenu);

        bgTexture = new Texture("barUI_holder.png");
        hpTexture = new Texture("barUI_hp.png");
        arTexture = new Texture("barUI_ar.png");

        hpRegion = new TextureRegion(hpTexture);
        arRegion = new TextureRegion(arTexture);
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

                    checkpointMap = data.targetMap;
                    checkpointX = data.spawnX;
                    checkpointY = data.spawnY;
                    checkpointHP = player.hp;
                    checkpointAssim = player.assimilationMeter;

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

                if (player != null) {
                    //Camera Follow Ezra typeshet
                    float targetX = player.b2body.getPosition().x * PPM;
                    float targetY = player.b2body.getPosition().y * PPM;

                    /* This makes it so we don't look at void and if ezra near border of map
                     it just doesn't go beyond the boundaries/resolution */
                    if (map != null) {
                        // Get the total map size in pixels
                        int mapWidth = map.getProperties().get("width", Integer.class);
                        int mapHeight = map.getProperties().get("height", Integer.class);
                        int tileWidth = map.getProperties().get("tilewidth", Integer.class);
                        int tileHeight = map.getProperties().get("tileheight", Integer.class);

                        float mapPixelWidth = mapWidth * tileWidth;
                        float mapPixelHeight = mapHeight * tileHeight;

                        float camHalfWidth = camera.viewportWidth / 2f;
                        float camHalfHeight = camera.viewportHeight / 2f;

                        camera.position.x = MathUtils.clamp(targetX, camHalfWidth, mapPixelWidth - camHalfWidth);
                        camera.position.y = MathUtils.clamp(targetY, camHalfHeight, mapPixelHeight - camHalfHeight);
                    } else {
                        camera.position.x = targetX;
                        camera.position.y = targetY;
                    }

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
            // Where to draw the HP/AR holder
        float x = camera.position.x - 325;
        float y = camera.position.y + 140;
            // This draws it lol
        batch.draw(bgTexture, x, y);

        float hpPercent = player.hp / player.MAX_HP;
        int currentHpWidth = Math.round(154f * hpPercent);
            //HP/AR region to tell where to draw the HP and AR quite literally
        hpRegion.setRegion(0, 0, currentHpWidth, hpTexture.getHeight());
            // Don't change please I did trial and error to find this XD
        float hpOffsetX = 101f;
        float hpOffsetY = 43f;
        batch.draw(hpRegion, x + hpOffsetX, y + hpOffsetY);

        float arPercent = player.getMassPercentage();
        int currentArWidth = Math.round(154f * arPercent);

        arRegion.setRegion(0, 0, currentArWidth, arTexture.getHeight());
            //Same with this
        float arOffsetX = 101f;
        float arOffsetY = 26f;
        batch.draw(arRegion, x + arOffsetX, y + arOffsetY);
    }

    @Override
    public void dispose() {
        batch.dispose();
        world.dispose();
        map.dispose();
        mapRenderer.dispose();
        sandManager.dispose();
        if(bgTexture != null) bgTexture.dispose();
        if(hpTexture != null) hpTexture.dispose();
        if(arTexture != null) arTexture.dispose();
        if(pauseMenu != null) pauseMenu.dispose();
        if(mainMenu != null) mainMenu.dispose();
    }
}
