package io.github.devsimulator;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.ScreenUtils;
import io.github.devsimulator.elements.Dirt;
import io.github.devsimulator.entities.Player;
import io.github.devsimulator.helper.PhysicSim;
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
    private PhysicSim sim;
    private Texture sandTexture;

    @Override
    public void create() {
        batch = new SpriteBatch();
        sim = new PhysicSim(200, 200);
        world = new World(new Vector2(0, -9.8f), true);
        b2dr = new Box2DDebugRenderer();

        map = new TmxMapLoader().load("level1test.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map);

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 960, 640);

        tilemapmanager.createBoundaries(map, world);

        player = new Player(world);

        // Setup sand texture
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.76f, 0.7f, 0.5f, 1f);
        pixmap.fill();
        sandTexture = new Texture(pixmap);
        pixmap.dispose();
        MapLayer collisionLayer = map.getLayers().get("collisions");
        if (collisionLayer != null) {
            for (MapObject object : collisionLayer.getObjects()) {
                if (object instanceof RectangleMapObject) {
                    Rectangle rect = ((RectangleMapObject) object).getRectangle();

                    // Flip Y coordinate for walls
                    float flippedY = 640 - rect.y - rect.height;

                    int simX = (int) (rect.x / (960f / 200f));
                    int simY = (int) (flippedY / (640f / 200f));
                    int simW = (int) (rect.width / (960f / 200f));
                    int simH = (int) (rect.height / (640f / 200f));

                    // Corrected variable names to use simX, simY, etc.
                    for (int i = simX; i < simX + simW; i++) {
                        for (int j = simY; j < simY + simH; j++) {
                            sim.setWall(i, j, true);
                        }
                    }
                }
            }
        }

        // 2. Spawn sand in the caves
        MapLayer sandLayer = map.getLayers().get("sand_zones");
        if (sandLayer != null) {
            for (MapObject object : sandLayer.getObjects()) {
                if (object instanceof RectangleMapObject) {
                    Rectangle rect = ((RectangleMapObject) object).getRectangle();


                    float flippedY = 640 - rect.y - rect.height;

                    int simX = (int) (rect.x / (960f / 200f));
                    int simY = (int) (flippedY / (640f / 200f));
                    int simW = (int) (rect.width / (960f / 200f));
                    int simH = (int) (rect.height / (640f / 200f));

                    sim.fillArea(simX, simY, simW, simH);
                }
            }
        }
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1f);

        world.step(1/60f, 6, 2);
        if (sim != null) sim.update();
        if (player != null) player.update(Gdx.graphics.getDeltaTime(), sim);

        camera.update();
        mapRenderer.setView(camera);
        mapRenderer.render();

        float cellWidth = 960f / 200f;
        float cellHeight = 640f / 200f;

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        for (int y = 0; y < 200; y++) {
            for (int x = 0; x < 200; x++) {
                if (sim.getElement(x, y) instanceof Dirt) {
                    // Remove 'scaleX' variables and use simple cell sizing
                    batch.draw(sandTexture, x * cellWidth, y * cellHeight, cellWidth, cellHeight);
                }
            }
        }

        if (player != null) player.draw(batch);

        // Draw HUD Meter
        float barWidth = 200;
        float barHeight = 20;

// Background (Black)
        batch.setColor(0f, 0f, 0f, 1f);
        batch.draw(sandTexture, 20, 600, barWidth, barHeight);

// Foreground (Risk Level) - Let's make it Red to indicate danger
        batch.setColor(0.9f, 0.1f, 0.1f, 1f);
        batch.draw(sandTexture, 20, 600, barWidth * player.getMassPercentage(), barHeight);

// Reset color
        batch.setColor(1, 1, 1, 1);

        batch.end();

        b2dr.render(world, camera.combined.cpy().scl(PPM));
    }

    @Override
    public void dispose() {
        batch.dispose();
        world.dispose();
        b2dr.dispose();
        map.dispose();
        mapRenderer.dispose();
        if (sandTexture != null) sandTexture.dispose();
    }
}
