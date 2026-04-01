package io.github.devsimulator;

import com.badlogic.gdx.ApplicationAdapter;
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
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import io.github.devsimulator.controllers.SandManager;
import io.github.devsimulator.entities.Player;
import io.github.devsimulator.helper.WorldContactListener;
import io.github.devsimulator.helper.tilemapmanager;
import io.github.devsimulator.helper.pauseMenu;
import io.github.devsimulator.helper.tutorialGUI;
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

    @Override
    public void create() {
        batch = new SpriteBatch();
        world = new World(new Vector2(0, -9.8f), true);
        world.setContactListener(new WorldContactListener());

        camera = new OrthographicCamera();
        viewport = new FitViewport(720, 480, camera);
        viewport.apply();

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
            for (int i = 0; i < frameCount; i++) frames[i] = tmp[0][i];
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
                world.step(1/60f, 6, 2);

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
                }
            }

            mapMgr.renderBackground(camera);
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            sandManager.render(batch);
            if (!isTutorialReading && WorldContactListener.closestSign != null && WorldContactListener.closestSign.isHero) {
                drawHeroArrow();
            }
            batch.end();

            batch.begin();
            if (player != null) player.draw(batch);
            drawHUD();
            batch.end();

            // Stages go here
            if (isTutorialReading) {
                tutorialGui.render(dt);
            }
            pauseMenu.render(dt);
        }
    }

    private void drawHeroArrow() {
        tilemapmanager.InteractableData sign = WorldContactListener.closestSign;
        TextureRegion frame = hoverAnimation.getKeyFrame(stateTime, true);
        float pulse = 0.7f + MathUtils.sin(stateTime * 5f) * 0.3f;
        batch.setColor(1, 1, 1, pulse);
        batch.draw(frame, (sign.worldX * PPM) - 16 + (sign.width * PPM / 2), (sign.worldY * PPM) + (sign.height * PPM) + 20, 16, 16, 32, 32, 1f, 1f, 180f);
        batch.setColor(Color.WHITE);
    }

    private void drawHUD() {
        if(player == null) return;
        float x = camera.position.x - 325;
        float y = camera.position.y + 140;
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
    }
}
