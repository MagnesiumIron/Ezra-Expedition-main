package io.github.devsimulator;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import io.github.devsimulator.controllers.SandManager;
import io.github.devsimulator.entities.Player;
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

    // Physics Accumulator & UI
    private float accumulator = 0;
    private static final float TIME_STEP = 1/60f;
    private Matrix4 uiMatrix;

    //Sounds & ambience
    public static Sound jumpSound;
    public static Music menuTheme;
    public static Music ambience;
    public static Sound assimilationIN;
    public static Sound assimilationOUT;
    public static Music level1Ambience;

    @Override
    public void create() {

        jumpSound = Gdx.audio.newSound(Gdx.files.internal("sounds/jump.wav"));
        assimilationIN  = Gdx.audio.newSound(Gdx.files.internal("sounds/assimilationIN.wav"));
        assimilationOUT = Gdx.audio.newSound(Gdx.files.internal("sounds/assimilationOUT.wav"));

        menuTheme = Gdx.audio.newMusic(Gdx.files.internal("music/menu_theme.mp3"));
        menuTheme.setLooping(true);
        menuTheme.setVolume(0.4f);

        ambience = Gdx.audio.newMusic(Gdx.files.internal("music/caveambience.mp3"));
        ambience.setLooping(true);
        ambience.setVolume(0.4f);

        level1Ambience = Gdx.audio.newMusic(Gdx.files.internal("music/level1ambience.mp3"));
        level1Ambience.setLooping(true);
        level1Ambience.setVolume(0.3f);

        menuTheme.play();

        batch = new SpriteBatch();
        world = new World(new Vector2(0, -9.8f), true);
        world.setContactListener(new WorldContactListener());

        camera = new OrthographicCamera();
        viewport = new FitViewport(720, 480, camera);
        viewport.apply();
        uiMatrix = new Matrix4().setToOrtho2D(0, 0, 720, 480);

        sandManager = new SandManager();
        player = new Player(world);

        mapMgr = new mapManager(world, player, sandManager);
        mapMgr.changeLevel(new prologuespawn(), 100 / PPM, 200 / PPM);

        mainMenu = new io.github.devsimulator.helper.mainMenu(player);
        pauseMenu = new io.github.devsimulator.helper.pauseMenu(player, mainMenu);

        bgTexture = new Texture("barUI_holder.png");
        hpTexture = new Texture("barUI_hp.png");
        arTexture = new Texture("barUI_ar.png");
        hpRegion = new TextureRegion(hpTexture);
        arRegion = new TextureRegion(arTexture);

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
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1f);

        if (!mainMenu.isStarted) {
            mainMenu.render(dt);
        } else {
            pauseMenu.update();

            if (!pauseMenu.isPaused && !isTutorialReading) {
                stateTime += dt;

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
                updateCameraPosition(dt);
            } else if (isTutorialReading) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
                    isTutorialReading = false;
                }
            }

            AnimatedTiledMapTile.updateAnimationBaseTime();

            // 1. Draw the actual Tiled map elements (This is what draws your configured runes!)
            mapMgr.renderBackground(camera);

            batch.setProjectionMatrix(camera.combined);
            batch.begin();

            // 2. Draw Cellular Automata
            sandManager.render(batch);

            // 3. Draw Proximity UI Prompts (Hovering arrows/keys)
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

            // 4. Draw Player
            if (player != null) player.draw(batch);
            batch.end();

            // 5. Draw HUD using Static Matrix
            batch.setProjectionMatrix(uiMatrix);
            batch.begin();
            drawHUD();
            batch.end();

            // 6. Draw Stages (Tutorials / Menus)
            if (isTutorialReading) {
                tutorialGui.render(dt);
            }
            pauseMenu.render(dt);
        }
    }

    private void drawInteractionPrompt(float objWorldX, float objWorldY, float objWidth, float objHeight) {
        if (hoverAnimation == null) return;
        TextureRegion frame = hoverAnimation.getKeyFrame(stateTime, true);
        float pulse = 0.8f + com.badlogic.gdx.math.MathUtils.sin(stateTime * 6f) * 0.2f;
        float bobOffset = com.badlogic.gdx.math.MathUtils.sin(stateTime * 4f) * 4f;

        batch.setColor(1f, 1f, 1f, pulse);

        float drawX = (objWorldX * PPM) + (objWidth * PPM / 2f) - (frame.getRegionWidth() / 2f);
        float drawY = (objWorldY * PPM) + (objHeight * PPM) + 16f + bobOffset;

        batch.draw(frame, drawX, drawY, frame.getRegionWidth() / 2f, frame.getRegionHeight() / 2f,
            frame.getRegionWidth(), frame.getRegionHeight(), 1f, 1f, 180f);

        batch.setColor(Color.WHITE);
    }

    private void drawHUD() {
        if(player == null) return;

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

    private void updateCameraPosition(float dt) {
        if (player == null) return;
        float targetX = player.b2body.getPosition().x * PPM;
        float targetY = player.b2body.getPosition().y * PPM;

        float lerpAlpha = 6.0f * dt;

        float newX = com.badlogic.gdx.math.MathUtils.lerp(camera.position.x, targetX, lerpAlpha);
        float newY = com.badlogic.gdx.math.MathUtils.lerp(camera.position.y, targetY, lerpAlpha);

        TiledMap currentMap = mapMgr.currentLevel != null ? mapMgr.currentLevel.map : null;
        if (currentMap != null) {
            float mapPixelWidth = currentMap.getProperties().get("width", Integer.class) * currentMap.getProperties().get("tilewidth", Integer.class);
            float mapPixelHeight = currentMap.getProperties().get("height", Integer.class) * currentMap.getProperties().get("tileheight", Integer.class);
            newX = com.badlogic.gdx.math.MathUtils.clamp(newX, viewport.getWorldWidth()/2f, Math.max(viewport.getWorldWidth()/2f, mapPixelWidth - viewport.getWorldWidth()/2f));
            newY = com.badlogic.gdx.math.MathUtils.clamp(newY, viewport.getWorldHeight()/2f, Math.max(viewport.getWorldHeight()/2f, mapPixelHeight - viewport.getWorldHeight()/2f));
        }
        camera.position.set(newX, newY, 0);
        camera.update();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        if (mainMenu != null) mainMenu.stage.getViewport().update(width, height, true);
        if (pauseMenu != null) pauseMenu.stage.getViewport().update(width, height, true);
        if (tutorialGui != null) tutorialGui.resize(width, height);
    }

    @Override
    public void dispose() {
        batch.dispose();
        world.dispose();
        sandManager.dispose();
        bgTexture.dispose();
        hpTexture.dispose();
        arTexture.dispose();
        pauseMenu.dispose();
        mainMenu.dispose();
        mapMgr.dispose();
        if(tutorialGui != null) tutorialGui.dispose();
        if(hoverSheet != null) hoverSheet.dispose();
        if(font != null) font.dispose();
        if(jumpSound != null) jumpSound.dispose();
        if(assimilationIN != null) assimilationIN.dispose();
        if(assimilationOUT != null) assimilationOUT.dispose();
        if(menuTheme != null) menuTheme.dispose();
        if(ambience != null) ambience.dispose();
        if(level1Ambience != null) level1Ambience.dispose();
    }
}
