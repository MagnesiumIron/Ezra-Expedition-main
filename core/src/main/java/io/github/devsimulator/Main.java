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
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.ScreenUtils;
import io.github.devsimulator.controllers.SandManager;
import io.github.devsimulator.entities.Player;
import io.github.devsimulator.helper.tilemapmanager;

public class Main extends ApplicationAdapter {
    public static final float PPM = 32f;

    private SpriteBatch batch;
    private World world;
    private Box2DDebugRenderer b2dr;

    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private OrthographicCamera camera;

    private Player player;
    private SandManager sandManager;
    private Texture hudTexture;

    @Override
    public void create() {
        batch = new SpriteBatch();

        // 1. Box2D Physics Setup
        world = new World(new Vector2(0, -9.8f), true);
        b2dr = new Box2DDebugRenderer();

        // 2. Map Setup
        map = new TmxMapLoader().load("level1test.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map);
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 960, 640);

        // 3. Create Boundaries (Box2D Walls)
        tilemapmanager.createBoundaries(map, world);

        // 4. Initialize Sand Manager
        sandManager = new SandManager();
        sandManager.initLevel(map);

        // 5. Create Player
        player = new Player(world);

        // 6. Create HUD Texture (1x1 White Pixel)
        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(1, 1, 1, 1);
        pix.fill();
        hudTexture = new Texture(pix);
        pix.dispose();
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1f);

        // --- Logic Updates ---
        world.step(1/60f, 6, 2);
        sandManager.update();

        if (player != null) {
            player.update(Gdx.graphics.getDeltaTime(), sandManager.sim);
        }

        camera.update();
        mapRenderer.setView(camera);

        // --- RENDER PASS 1: SAND (Background) ---
        // We draw the sand first so it sits behind the walls
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        sandManager.render(batch);
        batch.end();

        // --- RENDER PASS 2: MAP (Foreground) ---
        // We draw the map ON TOP of the sand.
        // This hides any messy edges where the sand touches the walls.
        mapRenderer.render();

        // --- RENDER PASS 3: ENTITIES & HUD (Top Layer) ---
        // We open the batch again to draw the player and UI on top of the map
        batch.begin();

        // Draw Player
        if (player != null) player.draw(batch);

        // Draw HUD (Assimilation Meter)
        float barX = 20;
        float barY = 600;
        float barWidth = 200;
        float barHeight = 20;

        // Background (Black)
        batch.setColor(0f, 0f, 0f, 1f);
        batch.draw(hudTexture, barX, barY, barWidth, barHeight);

        // Foreground (Red Risk Level)
        if (player != null) {
            batch.setColor(0.9f, 0.1f, 0.1f, 1f);
            float percentage = player.getMassPercentage();
            if(percentage > 1) percentage = 1;
            if(percentage < 0) percentage = 0;

            batch.draw(hudTexture, barX, barY, barWidth * percentage, barHeight);
        }

        batch.setColor(1, 1, 1, 1); // Reset color
        batch.end();

        // Debug Lines (Optional - comment out to hide green lines)
        b2dr.render(world, camera.combined.cpy().scl(PPM));
    }

    @Override
    public void dispose() {
        batch.dispose();
        world.dispose();
        b2dr.dispose();
        map.dispose();
        mapRenderer.dispose();
        sandManager.dispose();
        if (hudTexture != null) hudTexture.dispose();
    }
}
