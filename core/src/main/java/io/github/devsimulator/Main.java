package io.github.devsimulator;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
<<<<<<< Updated upstream
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
=======
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
<<<<<<< Updated upstream
>>>>>>> Stashed changes
=======
>>>>>>> Stashed changes
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import io.github.devsimulator.controllers.SandManager;
import io.github.devsimulator.entities.Player;
<<<<<<< Updated upstream
<<<<<<< Updated upstream
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
=======
=======
>>>>>>> Stashed changes
import io.github.devsimulator.helper.*;
import io.github.devsimulator.levels.mapManager;
import io.github.devsimulator.levels.prologuespawn;

public class Main extends ApplicationAdapter {
    public static final float PPM = 32f;

    private SpriteBatch batch;
    private OrthographicCamera camera;
    public Viewport viewport;

    public World world;
    public mapManager mapMgr;
    public Player player;
    public SandManager sandManager;

    private Texture bgTexture, hpTexture, arTexture;
    private TextureRegion hpRegion, arRegion;
    private pauseMenu pauseMenu;
    private io.github.devsimulator.helper.mainMenu mainMenu;
    private tutorialGUI tutorialGui;

    public boolean isTutorialReading = false;
    private float stateTime = 0;
    private Texture hoverSheet;
    private Animation<TextureRegion> hoverAnimation;
    private BitmapFont font;
    private float accumulator = 0;
    private static final float TIME_STEP = 1/60f;
    private Matrix4 uiMatrix;

    // Rune Textures
    private Texture texChargerActive;
    private Texture texChargerConsumed;
    private Texture texTriggerReady;
    private Texture texTriggerPressed;
<<<<<<< Updated upstream
>>>>>>> Stashed changes
=======
>>>>>>> Stashed changes

    @Override
    public void create() {
        batch = new SpriteBatch();
        world = new World(new Vector2(0, -9.8f), true); // Gravity is handled in Player.java then we put it to sleep if no interaction or feedback from the player to save processing operations
        world.setContactListener(new WorldContactListener()); // REGISTER LISTENER (basically to identify whether naachieve ba goal, also to detect collisions
        b2dr = new Box2DDebugRenderer();

<<<<<<< Updated upstream
=======
        camera = new OrthographicCamera();
        viewport = new FitViewport(720, 480, camera);
        viewport.apply();
        uiMatrix = new Matrix4().setToOrtho2D(0, 0, 720, 480);
<<<<<<< Updated upstream
>>>>>>> Stashed changes
=======
>>>>>>> Stashed changes

        map = new TmxMapLoader().load("level1test.tmx");//load the first level
        mapRenderer = new OrthogonalTiledMapRenderer(map);
        camera = new OrthographicCamera(); //camera view is set to the resolution 960x640
        camera.setToOrtho(false, 960, 640);

        tilemapmanager.createBoundaries(map, world); //generation for static walls from the tiled map
        sandManager = new SandManager(); //simul the sand element based on the may layer
        sandManager.initLevel(map);

        //spawn player
        player = new Player(world);

<<<<<<< Updated upstream
        // HUD Texture
        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(1, 1, 1, 1);
        pix.fill();
        hudTexture = new Texture(pix);
        pix.dispose();
=======
        mapMgr = new mapManager(world, player, sandManager);
        mapMgr.changeLevel(new prologuespawn(), 100 / PPM, 200 / PPM);

        mainMenu = new io.github.devsimulator.helper.mainMenu(player);
        pauseMenu = new io.github.devsimulator.helper.pauseMenu(player, mainMenu);

        bgTexture = new Texture("barUI_holder.png");
        hpTexture = new Texture("barUI_hp.png");
        arTexture = new Texture("barUI_ar.png");
        hpRegion = new TextureRegion(hpTexture);
        arRegion = new TextureRegion(arTexture);

        try { texChargerActive = new Texture("rune_active.png"); }
        catch (Exception e) { texChargerActive = createFallbackTexture(Color.GOLDENROD); }
        try { texChargerConsumed = new Texture("rune_dead.png"); }
        catch (Exception e) { texChargerConsumed = createFallbackTexture(Color.WHITE); }
        try { texTriggerReady = new Texture("trigger_up.png"); }
        catch (Exception e) { texTriggerReady = createFallbackTexture(Color.TAN); }
        try { texTriggerPressed = new Texture("trigger_down.png"); }
        catch (Exception e) { texTriggerPressed = createFallbackTexture(Color.FOREST); }

        // Load Font and Tutorial GUI
        try {
            font = new BitmapFont(Gdx.files.internal("fantasyfontt.fnt"));
            tutorialGui = new tutorialGUI(font);
        } catch (Exception e) {
            Gdx.app.error("Main", "Font/GUI initialization failed: " + e.getMessage());
            font = new BitmapFont();
            tutorialGui = new tutorialGUI(font);
        }

        // Load Hover Effect
        try {
            hoverSheet = new Texture("menubtn_hovereffect.png");
            int frameCount = hoverSheet.getWidth() / 14;
            TextureRegion[][] tmp = TextureRegion.split(hoverSheet, 14, 9);
            TextureRegion[] frames = new TextureRegion[frameCount];
            System.arraycopy(tmp[0], 0, frames, 0, frameCount);
            hoverAnimation = new Animation<>(0.15f, frames);
            hoverAnimation.setPlayMode(Animation.PlayMode.LOOP);
        } catch (Exception e) {
            Gdx.app.error("Main", "Hover animation failed: " + e.getMessage());
        }
>>>>>>> Stashed changes
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1f);

        /*this part is the physics itself and game rules. feel free to modify it*/
        world.step(1/60f, 6, 2);

<<<<<<< Updated upstream
        // implemented safe body destruction since we cant destroy some box2d obj inside the collision listener
        //check the queue then destroy after physics step
        if (WorldContactListener.bodiesToDestroy.size > 0) {
            for (Body b : WorldContactListener.bodiesToDestroy) {
                // If it's a KEY, also find and destroy all bodies
                if ("KEY".equals(b.getFixtureList().first().getUserData())) {
                    destroyAllDoors();
=======
            if (!pauseMenu.isPaused && !isTutorialReading) {
                stateTime += dt;
                //world.step(1/60f, 6, 2);
                accumulator += Math.min(dt, 0.25f);
                while (accumulator >= TIME_STEP) {
                    world.step(TIME_STEP, 6, 2);
                    accumulator -= TIME_STEP;
                }

                if (WorldContactListener.pendingTransition != null) {
                    tilemapmanager.TransitionData data = WorldContactListener.pendingTransition;
                    mapMgr.transitionToMap(data.targetMap, data.spawnX, data.spawnY);
                    WorldContactListener.pendingTransition = null;
                }

                mapMgr.update(dt);
                sandManager.update();

                if (player != null) {
                    player.update(dt, sandManager);
                    if (player.hasJustInteractedWithSign()) {
                        tilemapmanager.InteractableData sign = WorldContactListener.closestSign;
                        if (sign != null) {
                            tutorialGui.show(sign.header, sign.description);
                            isTutorialReading = true;
                        }
                    }
                }
                updateCameraPosition();
            } else if (isTutorialReading) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
                    isTutorialReading = false;
>>>>>>> Stashed changes
                }
                world.destroyBody(b);
            }
<<<<<<< Updated upstream
            WorldContactListener.bodiesToDestroy.clear(); //reset queue
=======

            mapMgr.renderBackground(camera);

            batch.setProjectionMatrix(camera.combined);
            batch.begin();

            sandManager.render(batch); //draws cellular automata

            for (tilemapmanager.RuneData rune : WorldContactListener.allRunes) {
                Texture texToDraw = null;

                if (!rune.isSpawner && !rune.isContainer) {
                    // 1. CHARGER RUNES (Grants elements to Ezra)
                    texToDraw = rune.isConsumed ? texChargerConsumed : texChargerActive;
                }
                else if (rune.isSpawner && !rune.isContainer) {
                    // 2. TRIGGER RUNES (Buttons/Switches on the floor)
                    boolean isTriggered = sandManager.completedGeysers.contains(rune.runeID, false);
                    for (tilemapmanager.RuneData activeContainer : sandManager.activeContainers) {
                        if (activeContainer.runeID == rune.runeID) {
                            isTriggered = true;
                            break;
                        }
                    }
                    texToDraw = isTriggered ? texTriggerPressed : texTriggerReady;
                }

                // Draw the selected texture exactly over the TiledMap rectangle coordinates
                if (texToDraw != null) {
                    batch.draw(texToDraw, rune.worldX * PPM, rune.worldY * PPM, rune.width * PPM, rune.height * PPM);
                }
            }
            /*if (!isTutorialReading && WorldContactListener.closestSign != null && WorldContactListener.closestSign.isHero) {
                drawHeroArrow();
            }*/
            if (player != null && !isTutorialReading) {
                Vector2 pPos = new Vector2(player.b2body.getPosition().x * PPM, player.b2body.getPosition().y * PPM);

                // Check Closest Sign
                if (WorldContactListener.closestSign != null) {
                    float distSign = pPos.dst(WorldContactListener.closestSign.worldX * PPM, WorldContactListener.closestSign.worldY * PPM);
                    if (distSign < 64f) {
                        drawInteractionPrompt(WorldContactListener.closestSign.worldX, WorldContactListener.closestSign.worldY,
                            WorldContactListener.closestSign.width, WorldContactListener.closestSign.height);
                    }
                }

                // Check Closest Rune
                if (WorldContactListener.closestRune != null) {
                    float distRune = pPos.dst(WorldContactListener.closestRune.worldX * PPM, WorldContactListener.closestRune.worldY * PPM);
                    if (distRune < 64f && !WorldContactListener.closestRune.isConsumed && !WorldContactListener.closestRune.isContainer) {
                        drawInteractionPrompt(WorldContactListener.closestRune.worldX, WorldContactListener.closestRune.worldY,
                            WorldContactListener.closestRune.width, WorldContactListener.closestRune.height);
                    }
                }
            }

<<<<<<< Updated upstream
=======
            mapMgr.renderBackground(camera);

            batch.setProjectionMatrix(camera.combined);
            batch.begin();

            sandManager.render(batch); //draws cellular automata

            for (tilemapmanager.RuneData rune : WorldContactListener.allRunes) {
                Texture texToDraw = null;

                if (!rune.isSpawner && !rune.isContainer) {
                    // 1. CHARGER RUNES (Grants elements to Ezra)
                    texToDraw = rune.isConsumed ? texChargerConsumed : texChargerActive;
                }
                else if (rune.isSpawner && !rune.isContainer) {
                    // 2. TRIGGER RUNES (Buttons/Switches on the floor)
                    boolean isTriggered = sandManager.completedGeysers.contains(rune.runeID, false);
                    for (tilemapmanager.RuneData activeContainer : sandManager.activeContainers) {
                        if (activeContainer.runeID == rune.runeID) {
                            isTriggered = true;
                            break;
                        }
                    }
                    texToDraw = isTriggered ? texTriggerPressed : texTriggerReady;
                }

                // Draw the selected texture exactly over the TiledMap rectangle coordinates
                if (texToDraw != null) {
                    batch.draw(texToDraw, rune.worldX * PPM, rune.worldY * PPM, rune.width * PPM, rune.height * PPM);
                }
            }
            /*if (!isTutorialReading && WorldContactListener.closestSign != null && WorldContactListener.closestSign.isHero) {
                drawHeroArrow();
            }*/
            if (player != null && !isTutorialReading) {
                Vector2 pPos = new Vector2(player.b2body.getPosition().x * PPM, player.b2body.getPosition().y * PPM);

                // Check Closest Sign
                if (WorldContactListener.closestSign != null) {
                    float distSign = pPos.dst(WorldContactListener.closestSign.worldX * PPM, WorldContactListener.closestSign.worldY * PPM);
                    if (distSign < 64f) {
                        drawInteractionPrompt(WorldContactListener.closestSign.worldX, WorldContactListener.closestSign.worldY,
                            WorldContactListener.closestSign.width, WorldContactListener.closestSign.height);
                    }
                }

                // Check Closest Rune
                if (WorldContactListener.closestRune != null) {
                    float distRune = pPos.dst(WorldContactListener.closestRune.worldX * PPM, WorldContactListener.closestRune.worldY * PPM);
                    if (distRune < 64f && !WorldContactListener.closestRune.isConsumed && !WorldContactListener.closestRune.isContainer) {
                        drawInteractionPrompt(WorldContactListener.closestRune.worldX, WorldContactListener.closestRune.worldY,
                            WorldContactListener.closestRune.width, WorldContactListener.closestRune.height);
                    }
                }
            }

>>>>>>> Stashed changes
            if (player != null) player.draw(batch);
            batch.end();

            batch.setProjectionMatrix(uiMatrix);
            batch.begin();
            drawHUD();
            batch.end();

            // Stages go here
            if (isTutorialReading) {
                tutorialGui.render(dt);
            }
            pauseMenu.render(dt);
>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
<<<<<<< Updated upstream
    private void destroyAllDoors() {
        Array<Body> bodies = new Array<Body>();
        world.getBodies(bodies);
        for (Body b : bodies) {
            if (b.getFixtureList().size > 0 && "DOOR".equals(b.getFixtureList().first().getUserData())) {
                world.destroyBody(b);
            }
        }
=======
=======
>>>>>>> Stashed changes
    /*private void drawHeroArrow() {
        tilemapmanager.InteractableData sign = WorldContactListener.closestSign;
        TextureRegion frame = hoverAnimation.getKeyFrame(stateTime, true);
        float pulse = 0.7f + MathUtils.sin(stateTime * 5f) * 0.3f;
        batch.setColor(1, 1, 1, pulse);
        batch.draw(frame, (sign.worldX * PPM) - 16 + (sign.width * PPM / 2), (sign.worldY * PPM) + (sign.height * PPM) + 20, 16, 16, 32, 32, 1f, 1f, 180f);
        batch.setColor(Color.WHITE);
    }*/

    private Texture createFallbackTexture(Color color) {//temp graphics when rune's graphics are not yet set or nadelete file for example
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture generatedTex = new Texture(pixmap);
        pixmap.dispose();
        return generatedTex;
    }
        //ADDED THIS NEW METHOD: Floating object lang when the player is getting nearer the interactable objects
    private void drawInteractionPrompt(float objWorldX, float objWorldY, float objWidth, float objHeight) {
        TextureRegion frame = hoverAnimation.getKeyFrame(stateTime, true);
        float pulse = 0.8f + com.badlogic.gdx.math.MathUtils.sin(stateTime * 6f) * 0.2f;
        float bobOffset = com.badlogic.gdx.math.MathUtils.sin(stateTime * 4f) * 4f;

        batch.setColor(1f, 1f, 1f, pulse);

        // Calculate the center top of the object, adding the bob offset
        float drawX = (objWorldX * PPM) + (objWidth * PPM / 2f) - (frame.getRegionWidth() / 2f);
        float drawY = (objWorldY * PPM) + (objHeight * PPM) + 16f + bobOffset;

        // Draw the frame pointing down (rotation 180)
        batch.draw(frame, drawX, drawY, frame.getRegionWidth() / 2f, frame.getRegionHeight() / 2f,
            frame.getRegionWidth(), frame.getRegionHeight(), 1f, 1f, 180f);

        batch.setColor(Color.WHITE);
<<<<<<< Updated upstream
>>>>>>> Stashed changes
=======
>>>>>>> Stashed changes
    }

    private void drawHUD() {
        if(player == null) return;
<<<<<<< Updated upstream
<<<<<<< Updated upstream
        float x = camera.position.x - 400;
        float y = camera.position.y + 250;
        batch.setColor(0,0,0,1);
        batch.draw(hudTexture, x, y, 204, 14);
        batch.setColor(1,0,0,1);
        batch.draw(hudTexture, x+2, y+2, 200 * player.getMassPercentage(), 10);
        batch.setColor(1,1,1,1);
=======
=======
>>>>>>> Stashed changes

        /* float x = camera.position.x - 325;
        float y = camera.position.y + 140; */
        float x = 20;
        float y = 380;

        batch.draw(bgTexture, x, y);
        float hpPercent = player.hp / player.MAX_HP;
        hpRegion.setRegion(0, 0, Math.round(154f * hpPercent), hpTexture.getHeight());
        batch.draw(hpRegion, x + 101f, y + 43f);
        float arPercent = player.getMassPercentage();
        arRegion.setRegion(0, 0, Math.round(154f * arPercent), arTexture.getHeight());
        batch.draw(arRegion, x + 101f, y + 26f);
    }

    private void updateCameraPosition() {
        if (player == null) return;
        float targetX = player.b2body.getPosition().x * PPM;
        float targetY = player.b2body.getPosition().y * PPM;
        TiledMap currentMap = mapMgr.currentLevel != null ? mapMgr.currentLevel.map : null;
        if (currentMap != null) {
            float mapPixelWidth = currentMap.getProperties().get("width", Integer.class) * currentMap.getProperties().get("tilewidth", Integer.class);
            float mapPixelHeight = currentMap.getProperties().get("height", Integer.class) * currentMap.getProperties().get("tileheight", Integer.class);
            float clampedX = MathUtils.clamp(targetX, viewport.getWorldWidth()/2f, Math.max(viewport.getWorldWidth()/2f, mapPixelWidth - viewport.getWorldWidth()/2f));
            float clampedY = MathUtils.clamp(targetY, viewport.getWorldHeight()/2f, Math.max(viewport.getWorldHeight()/2f, mapPixelHeight - viewport.getWorldHeight()/2f));
            camera.position.set(clampedX, clampedY, 0);
        }
        camera.update();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        if (mainMenu != null) mainMenu.stage.getViewport().update(width, height, true);
        if (pauseMenu != null) pauseMenu.stage.getViewport().update(width, height, true);
        if (tutorialGui != null) tutorialGui.resize(width, height);
>>>>>>> Stashed changes
    }

    @Override
    public void dispose() {
        batch.dispose();
        world.dispose();
        map.dispose();
        mapRenderer.dispose();
        sandManager.dispose();
<<<<<<< Updated upstream
        if(hudTexture != null) hudTexture.dispose();
=======
        bgTexture.dispose();
        hpTexture.dispose();
        arTexture.dispose();
        pauseMenu.dispose();
        mainMenu.dispose();
        mapMgr.dispose();

        // Destroy the dynamic rune textures to prevent memory leaks
        if(texChargerActive != null) texChargerActive.dispose();
        if(texChargerConsumed != null) texChargerConsumed.dispose();
        if(texTriggerReady != null) texTriggerReady.dispose();
        if(texTriggerPressed != null) texTriggerPressed.dispose();

        if(tutorialGui != null) tutorialGui.dispose();
        if(hoverSheet != null) hoverSheet.dispose();
        if(font != null) font.dispose();
>>>>>>> Stashed changes
    }
}
