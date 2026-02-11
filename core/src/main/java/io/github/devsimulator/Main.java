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

/*Context:
* BODIES = fundamental object. Representative ng any physical thing na ilalagay sa loob ng game
* KEYS = trigger object; Siya ung usually ginagamit for bodiesToDestroy function since need siya "pick-upin" hence must be removed from
*           the game.
* DOORS = Blocks the player's path kasi need muna madestroy (or collected rather) all the keys*/

public class Main extends ApplicationAdapter {
    public static final float PPM = 32f; // 32 PIXELS
    private SpriteBatch batch; //batch is the main object
    private World world;
    private Box2DDebugRenderer b2dr;
    private OrthographicCamera camera; //camera follower to the player's movement
    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private Player player; // player entity
    private SandManager sandManager; //based kayo here this one handles the cellular automata simulation
    private Texture hudTexture;

    @Override
    public void create() {
        batch = new SpriteBatch();
        world = new World(new Vector2(0, -9.8f), true); // Gravity is handled in Player.java then we put it to sleep if no interaction or feedback from the player to save processing operations
        world.setContactListener(new WorldContactListener()); // REGISTER LISTENER (basically to identify whether naachieve ba goal, also to detect collisions
        b2dr = new Box2DDebugRenderer();


        map = new TmxMapLoader().load("level1test.tmx");//load the first level
        mapRenderer = new OrthogonalTiledMapRenderer(map);
        camera = new OrthographicCamera(); //camera view is set to the resolution 960x640
        camera.setToOrtho(false, 960, 640);

        tilemapmanager.createBoundaries(map, world); //generation for static walls from the tiled map
        sandManager = new SandManager(); //simul the sand element based on the may layer
        sandManager.initLevel(map);

        //spawn player
        player = new Player(world);

        // HUD Texture
        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(1, 1, 1, 1);
        pix.fill();
        hudTexture = new Texture(pix);
        pix.dispose();
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1f);

        /*this part is the physics itself and game rules. feel free to modify it*/
        world.step(1/60f, 6, 2);

        // implemented safe body destruction since we cant destroy some box2d obj inside the collision listener
        //check the queue then destroy after physics step
        if (WorldContactListener.bodiesToDestroy.size > 0) {
            for (Body b : WorldContactListener.bodiesToDestroy) {
                // If it's a KEY, also find and destroy all bodies
                if ("KEY".equals(b.getFixtureList().first().getUserData())) {
                    destroyAllDoors();
                }
                world.destroyBody(b);
            }
            WorldContactListener.bodiesToDestroy.clear(); //reset queue
        }
        /*Sand cellular automata update*/
        sandManager.update();
        if (player != null) player.update(Gdx.graphics.getDeltaTime(), sandManager.sim);

        // Camera tracker centralized to player
        if(player != null) {
            camera.position.x = player.b2body.getPosition().x * PPM;
            camera.position.y = player.b2body.getPosition().y * PPM;
            camera.update();
        }

        // ========================Rendering=========================================
        mapRenderer.setView(camera); //new view of the camera when new map is updated

        // drawing sand/liquid
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        sandManager.render(batch);
        batch.end();

        // drawing static map and player
        mapRenderer.render();
        batch.begin();
        if (player != null) player.draw(batch);
        drawHUD();
        batch.end();
    }

    private void destroyAllDoors() {
        Array<Body> bodies = new Array<Body>();
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
        batch.setColor(0,0,0,1);
        batch.draw(hudTexture, x, y, 204, 14);
        batch.setColor(1,0,0,1);
        batch.draw(hudTexture, x+2, y+2, 200 * player.getMassPercentage(), 10);
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
    }
}
