package io.github.devsimulator;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.graphics.GL20;
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
    private com.badlogic.gdx.graphics.g2d.GlyphLayout sharedLayout;
    private Vector2 cachedPlayerPos;

    private Texture bgTexture, hpTexture, arTexture;
    private TextureRegion hpRegion, arRegion;
    private Texture blankPixel;
    private pauseMenu pauseMenu;
    private io.github.devsimulator.helper.mainMenu mainMenu;
    private tutorialGUI tutorialGui;

    public boolean isTutorialReading = false;
    private float stateTime = 0;
    private Texture hoverSheet;
    private BitmapFont font;

    //shooter mechanism
    private float combatChargeTimer = 0f;
    private boolean isChargingCombat = false;

    private float accumulator = 0;
    private static final float TIME_STEP = 1/60f;
    private Matrix4 uiMatrix;

    //transition
    private com.badlogic.gdx.graphics.glutils.ShapeRenderer shapeRenderer;
    private float fadeAlpha = 0f;
    private boolean fadingOut = false;
    private boolean fadingIn = false;
    private io.github.devsimulator.helper.tilemapmanager.TransitionData queuedTransition = null;

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
        shapeRenderer = new com.badlogic.gdx.graphics.glutils.ShapeRenderer();
        world = new World(new Vector2(0, -9.8f), true);
        world.setContactListener(new WorldContactListener());

        camera = new OrthographicCamera();
        viewport = new FitViewport(720, 480, camera);
        viewport.apply();
        uiMatrix = new Matrix4().setToOrtho2D(0, 0, 720, 480);

        sandManager = new SandManager();
        player = new Player(world);
        WorldContactListener.playerInstance = player;
        sharedLayout = new com.badlogic.gdx.graphics.g2d.GlyphLayout();
        cachedPlayerPos = new Vector2();

        mapMgr = new mapManager(world, player, sandManager);
        mapMgr.changeLevel(new prologuespawn(), 100 / PPM, 200 / PPM);

        mainMenu = new io.github.devsimulator.helper.mainMenu(player);
        pauseMenu = new io.github.devsimulator.helper.pauseMenu(player, mainMenu);

        bgTexture = new Texture("barUI_holder.png");
        hpTexture = new Texture("barUI_hp.png");
        arTexture = new Texture("barUI_ar.png");
        hpRegion = new TextureRegion(hpTexture);
        arRegion = new TextureRegion(arTexture);

        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(Color.WHITE);
        pix.fill();
        blankPixel = new Texture(pix);
        pix.dispose();

        try {
            font = new BitmapFont(Gdx.files.internal("fantasyfontt.fnt"));
            font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            tutorialGui = new tutorialGUI(font);
        } catch (Exception e) {
            Gdx.app.error("Main", "Font/GUI initialization failed: " + e.getMessage());
            font = new BitmapFont();
            tutorialGui = new tutorialGUI(font);
        }

        try {
            hoverSheet = new Texture("menubtn_hovereffect.png");
            int frameCount = hoverSheet.getWidth() / 14;
            TextureRegion[][] tmp = TextureRegion.split(hoverSheet, 14, 9);
            TextureRegion[] frames = new TextureRegion[frameCount];
            System.arraycopy(tmp[0], 0, frames, 0, frameCount);
            Animation<TextureRegion> hoverAnimation = new Animation<>(0.15f, frames);
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

                if (WorldContactListener.pendingTransition != null && !fadingOut && !fadingIn) {
                    queuedTransition = WorldContactListener.pendingTransition;
                    WorldContactListener.pendingTransition = null;
                    fadingOut = true;
                    player.b2body.setLinearVelocity(0, 0);
                }

                if (fadingOut) {
                    fadeAlpha += dt * 2.5f;
                    if (fadeAlpha >= 1f) {
                        fadeAlpha = 1f;
                        mapMgr.transitionToMap(queuedTransition.targetMap, queuedTransition.spawnX, queuedTransition.spawnY);
                        fadingOut = false;
                        fadingIn = true;
                    }
                } else if (fadingIn) {
                    fadeAlpha -= dt * 2.5f;
                    if (fadeAlpha <= 0f) {
                        fadeAlpha = 0f;
                        fadingIn = false;
                    }
                }

                boolean isCinematicPlaying = fadingOut || fadingIn;
                boolean isPlayerDead = (player != null && player.hp <= 0);

                if (!isCinematicPlaying && !isPlayerDead) {
                    accumulator += Math.min(dt, 0.25f);
                    while (accumulator >= TIME_STEP) {
                        world.step(TIME_STEP, 6, 2);
                        accumulator -= TIME_STEP;
                    }

                    if (WorldContactListener.pendingFastReload) {
                        mapMgr.fastRoomReload();
                        WorldContactListener.pendingFastReload = false;
                    }

                    mapMgr.update(dt);
                    sandManager.update();

                    if (player != null) {
                        player.update(dt, sandManager);

                        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && mapMgr.currentLevel != null) {
                            if (player.currentState == Player.State.NORMAL && player.chargeSlots[player.activeSlot] > 0 && !player.elementSlots[player.activeSlot].equals("NONE")) {
                                isChargingCombat = true;
                                combatChargeTimer += dt;
                                player.chargeProgress = combatChargeTimer;

                                if (combatChargeTimer >= 1.0f && player.chargeSlots[player.activeSlot] >= 3) {
                                    player.invincibilityTimer = 0.1f;
                                }
                            } else {
                                isChargingCombat = false;
                                combatChargeTimer = 0f;
                                player.chargeProgress = 0f;
                            }
                        } else if (isChargingCombat) {
                            boolean isMega = (combatChargeTimer >= 1.0f && player.chargeSlots[player.activeSlot] >= 3);
                            com.badlogic.gdx.math.Vector3 mousePos = new com.badlogic.gdx.math.Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
                            viewport.unproject(mousePos);

                            player.executeShoot(mousePos.x / PPM, mousePos.y / PPM, mapMgr.currentLevel.projectiles, world, isMega);

                            isChargingCombat = false;
                            combatChargeTimer = 0f;
                            player.chargeProgress = 0f;
                        }

                        if (player.hasJustInteractedWithSign()) {
                            tilemapmanager.InteractableData sign = WorldContactListener.closestSign;
                            if (sign != null) {
                                tutorialGui.show(sign.header, sign.description);
                                isTutorialReading = true;
                            }
                        }
                    }
                }

                if (isPlayerDead) {
                    if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
                        mapMgr.fastRoomReload();
                        player.hp = 100f;
                    } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                        mainMenu.isStarted = false;
                        player.hp = 100f;
                    }
                }

                updateCameraPosition(dt);
            } else if (isTutorialReading) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
                    isTutorialReading = false;
                }
            }

            AnimatedTiledMapTile.updateAnimationBaseTime();
            mapMgr.renderBackground(camera);

            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            sandManager.render(batch);

            if (mapMgr.currentLevel != null) {
                mapMgr.currentLevel.renderEntities(batch);
            }

            String currentPrompt = getString();

            if (font != null) {
                for (tilemapmanager.RuneData rune : WorldContactListener.allRunes) {
                    if (!rune.isConsumed && !rune.elementType.equals("NONE") && !rune.isSpawner && !rune.isContainer) {

                        float bobbingOffset = MathUtils.sin(stateTime * 4f) * 4f;
                        float rX = (rune.worldX * PPM) + (rune.width * PPM / 2f);
                        float rY = (rune.worldY * PPM) + (rune.height * PPM) + 25f + bobbingOffset;
                        font.getData().setScale(0.5f);

                        String el = rune.elementType;
                        float textX = rX - (sharedLayout.width / 2f);

                        // DRAW BLACK DROP SHADOW FIRST
                        font.setColor(0f, 0f, 0f, 0.8f);
                        font.draw(batch, el, textX + 2f, rY - 2f); // Offset by 2 pixels

                        // DRAW THE MAIN COLOR
                        if (el.equals("WATER")) font.setColor(0.2f, 0.6f, 1.0f, 1f);
                        else if (el.equals("LAVA")) font.setColor(1.0f, 0.4f, 0.0f, 1f);
                        else font.setColor(0.6f, 0.4f, 0.2f, 1f);

                        font.draw(batch, el, textX, rY);
                    }
                }
                font.setColor(Color.WHITE);
                font.getData().setScale(1.0f);
            }

            if (player != null) player.draw(batch);
            batch.end();

            batch.setProjectionMatrix(uiMatrix);
            batch.begin();
            drawHUD(currentPrompt);
            batch.end();

            if (isTutorialReading) {
                tutorialGui.render(dt);
            }
            pauseMenu.render(dt);
        }

        if (player != null && player.hp <= 0) {
            // Calculate screen boundaries based on the actual camera viewport
            float viewX = camera.position.x - viewport.getWorldWidth() / 2f;
            float viewY = camera.position.y - viewport.getWorldHeight() / 2f;
            float vWidth = viewport.getWorldWidth();
            float vHeight = viewport.getWorldHeight();
            float centerX = camera.position.x;
            float centerY = camera.position.y;

            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);

            shapeRenderer.setColor(0, 0, 0, 0.85f);
            shapeRenderer.rect(viewX, viewY, vWidth, vHeight);

            shapeRenderer.setColor(0.6f, 0.0f, 0.0f, 0.4f);
            float bannerHeight = vHeight * 0.3f;
            shapeRenderer.rect(viewX, centerY - (bannerHeight / 2f), vWidth, bannerHeight);

            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);

            batch.setProjectionMatrix(camera.combined);
            batch.begin();

            // --- 1. "YOU DIED" TITLE WITH DROP SHADOW ---
            font.getData().setScale(1.2f);
            String titleText = "Y O U   D I E D";
            sharedLayout.setText(font, titleText);
            float titleX = centerX - (sharedLayout.width / 2f);
            float titleY = centerY + (vHeight * 0.08f);

            font.setColor(0f, 0f, 0f, 0.8f); // Thick Black Shadow
            font.draw(batch, titleText, titleX + 2f, titleY - 2f);

            font.setColor(1.0f, 0.2f, 0.2f, 1f); // Blood Red
            font.draw(batch, titleText, titleX, titleY);

            // --- 2. SUBTITLES WITH DROP SHADOW (NO BRACKETS!) ---
            font.getData().setScale(0.5f);

            String promptF = "PRESS F TO RESPAWN";
            sharedLayout.setText(font, promptF);
            float fX = centerX - (sharedLayout.width / 2f);
            float fY = centerY - (vHeight * 0.04f);

            font.setColor(0f, 0f, 0f, 0.8f); // Black Shadow
            font.draw(batch, promptF, fX + 1f, fY - 1f);

            font.setColor(com.badlogic.gdx.graphics.Color.WHITE); // White Text
            font.draw(batch, promptF, fX, fY);

            String promptEsc = "PRESS ESC TO WITHDRAW";
            sharedLayout.setText(font, promptEsc);
            float escX = centerX - (sharedLayout.width / 2f);
            float escY = centerY - (vHeight * 0.1f);

            font.setColor(0f, 0f, 0f, 0.8f); // Black Shadow
            font.draw(batch, promptEsc, escX + 1f, escY - 1f);

            font.setColor(com.badlogic.gdx.graphics.Color.WHITE); // White Text
            font.draw(batch, promptEsc, escX, escY);

            // Reset scale
            font.getData().setScale(1.0f);

            batch.end();
        }

        if (fadeAlpha > 0f) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0, fadeAlpha);

            float viewX = camera.position.x - viewport.getWorldWidth() / 2f;
            float viewY = camera.position.y - viewport.getWorldHeight() / 2f;
            shapeRenderer.rect(viewX, viewY, viewport.getWorldWidth(), viewport.getWorldHeight());

            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    private String getString() {
        String currentPrompt = "";
        if (player != null && !isTutorialReading) {
            // ZERO-ALLOCATION VECTOR UPDATING
            cachedPlayerPos.set(player.b2body.getPosition().x * PPM, player.b2body.getPosition().y * PPM);

            if (WorldContactListener.closestSign != null) {
                float distSign = cachedPlayerPos.dst(WorldContactListener.closestSign.worldX * PPM, WorldContactListener.closestSign.worldY * PPM);
                if (distSign < 64f) currentPrompt = "Press [F] to read";
            }
            if (WorldContactListener.closestRune != null) {
                float distRune = cachedPlayerPos.dst(WorldContactListener.closestRune.worldX * PPM, WorldContactListener.closestRune.worldY * PPM);
                if (distRune < 64f && !WorldContactListener.closestRune.isConsumed && !WorldContactListener.closestRune.isContainer) {

                    tilemapmanager.RuneData rune = WorldContactListener.closestRune;
                    if (rune.isSpawner) {
                        currentPrompt = "Press [E] to trigger";
                    } else {
                        boolean hasType = player.elementSlots[0].equals(rune.elementType) || player.elementSlots[1].equals(rune.elementType);
                        boolean isFull = !player.elementSlots[0].equals("NONE") && !player.elementSlots[1].equals("NONE");

                        if (isFull && !hasType) {
                            currentPrompt = "INVENTORY FULL [Press Q to Drop]";
                        } else {
                            currentPrompt = "Press [E] to absorb";
                        }
                    }
                }
            }
        }
        return currentPrompt;
    }

    private void drawHUD(String currentPrompt) {
        if(player == null) return;
        float hx = 20;
        float hy = 380;

        batch.draw(bgTexture, hx, hy);
        float hpPercent = player.hp / player.MAX_HP;
        hpRegion.setRegion(0, 0, Math.round(154f * hpPercent), hpTexture.getHeight());
        batch.draw(hpRegion, hx + 101f, hy + 43f);
        float arPercent = player.getMassPercentage();
        arRegion.setRegion(0, 0, Math.round(154f * arPercent), arTexture.getHeight());
        batch.draw(arRegion, hx + 101f, hy + 26f);

        float startX = viewport.getWorldWidth() - 170f;
        float topY = viewport.getWorldHeight() - 100f;

        if (font != null && blankPixel != null) {
            font.setColor(Color.LIGHT_GRAY);
            font.getData().setScale(0.25f);
            font.draw(batch, "[Q] TO DROP", startX + 15f, topY + 70f);
            font.setColor(Color.WHITE);

            for (int i = 0; i < 2; i++) {
                String el = player.elementSlots[i];
                int charges = player.chargeSlots[i];
                boolean isActive = (player.activeSlot == i);

                float size = isActive ? 64f : 48f;
                float x = startX + (i * 75f);
                float y = topY - (isActive ? 8f : 0f);

                batch.setColor(0.1f, 0.1f, 0.1f, 0.8f);
                batch.draw(blankPixel, x, y, size, size);

                Color elColor = new Color(0.3f, 0.3f, 0.3f, 1f);
                elColor = switch (el) {
                    case "WATER" -> new Color(0.2f, 0.6f, 1.0f, 1f);
                    case "LAVA" -> new Color(1.0f, 0.4f, 0.0f, 1f);
                    case "DIRT", "SAND" -> new Color(0.6f, 0.4f, 0.2f, 1f);
                    default -> elColor;
                };

                batch.setColor(elColor.r, elColor.g, elColor.b, 0.5f);
                batch.draw(blankPixel, x + 4, y + 4, size - 8, size - 8);

                batch.setColor(isActive ? Color.WHITE : Color.DARK_GRAY);
                float borderThick = isActive ? 3f : 2f;
                batch.draw(blankPixel, x, y, size, borderThick);
                batch.draw(blankPixel, x, y + size - borderThick, size, borderThick);
                batch.draw(blankPixel, x, y, borderThick, size);
                batch.draw(blankPixel, x + size - borderThick, y, borderThick, size);

                font.getData().setScale(0.3f);
                font.setColor(isActive ? Color.WHITE : Color.LIGHT_GRAY);
                font.draw(batch, String.valueOf(i + 1), x + 8, y + size - 8);

                if (!el.equals("NONE")) {
                    font.getData().setScale(isActive ? 0.6f : 0.4f);
                    font.setColor(elColor);
                    float numX = x + size - (isActive ? 22f : 16f);
                    float numY = y + (isActive ? 24f : 18f);
                    font.draw(batch, String.valueOf(charges), numX, numY);
                }
            }
            batch.setColor(Color.WHITE);
            font.getData().setScale(1.0f);
        }

        // center subtitle
        if (currentPrompt != null && !currentPrompt.isEmpty() && font != null) {
            font.getData().setScale(0.5f);
            sharedLayout.setText(font, currentPrompt);
            float screenCenterX = viewport.getWorldWidth() / 2f;
            float bottomY = 60f;
            font.draw(batch, currentPrompt, screenCenterX - (sharedLayout.width / 2f), bottomY);

            font.getData().setScale(1.0f);
        }
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
        if(blankPixel != null) blankPixel.dispose();
    }
}
