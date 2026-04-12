package io.github.devsimulator.helper;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.devsimulator.entities.Player;
import io.github.devsimulator.Main;

public class pauseMenu {
    public Stage stage;
    private Player player;
    private mainMenu mainMenu;
    public boolean isPaused = false;
    private BitmapFont font;

    private Texture continueTex, saveLoadTex, controlTex, retryTex, withdrawTex, hoverSheet;
    private Animation<TextureRegion> hoverAnimation;
    private float stateTime = 0f;
    private Actor hoveredButton = null;

    private Table pauseTable;
    private saveManager saveManager;
    private controlUI controlUI;

    public pauseMenu(Player player, mainMenu mainMenu) {
        this.player = player;
        this.mainMenu = mainMenu;
        stage = new Stage(new FitViewport(360, 240));
        font = new BitmapFont(Gdx.files.internal("fantasyfontt.fnt"));

        // 1. Load Textures
        continueTex = new Texture("menubtn_continue.png");
        saveLoadTex = new Texture("menubtn_saveload.png");
        controlTex = new Texture("control_btn.png");
        retryTex = new Texture("menubtn_retry.png");
        withdrawTex = new Texture("menubtn_withdraw.png");

        // 2. FIX: Initialize the Hover Animation
        hoverSheet = new Texture("menubtn_hovereffect.png");
        int frameCount = hoverSheet.getWidth() / 14;
        TextureRegion[][] tmp = TextureRegion.split(hoverSheet, 14, 9);
        TextureRegion[] frames = new TextureRegion[frameCount];
        for (int i = 0; i < frameCount; i++) {
            frames[i] = tmp[0][i];
        }
        hoverAnimation = new Animation<TextureRegion>(0.15f, frames);
        hoverAnimation.setPlayMode(Animation.PlayMode.LOOP);

        // 3. Setup the Pause Table
        pauseTable = new Table();
        pauseTable.setFillParent(true);
        Pixmap bgPix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        bgPix.setColor(0, 0, 0, 0.7f);
        bgPix.fill();
        pauseTable.setBackground(new TextureRegionDrawable(new TextureRegion(new Texture(bgPix))));
        bgPix.dispose();

        // 4. Create Buttons
        ImageButton continueBtn = new ImageButton(new TextureRegionDrawable(new TextureRegion(continueTex)));
        ImageButton saveLoadBtn = new ImageButton(new TextureRegionDrawable(new TextureRegion(saveLoadTex)));
        ImageButton controlBtn = new ImageButton(new TextureRegionDrawable(new TextureRegion(controlTex)));
        ImageButton retryBtn = new ImageButton(new TextureRegionDrawable(new TextureRegion(retryTex)));
        ImageButton withdrawBtn = new ImageButton(new TextureRegionDrawable(new TextureRegion(withdrawTex)));

        // 5. Hover Logic
        ClickListener hoverListener = new ClickListener() {
            @Override public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                hoveredButton = event.getListenerActor();
            }
            @Override public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                super.exit(event, x, y, pointer, toActor);
                if (hoveredButton == event.getListenerActor()) hoveredButton = null;
            }
        };

        continueBtn.addListener(hoverListener);
        saveLoadBtn.addListener(hoverListener);
        controlBtn.addListener(hoverListener);
        retryBtn.addListener(hoverListener);
        withdrawBtn.addListener(hoverListener);

        // 6. Click Listeners
        continueBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { togglePause(); }
        });

        saveLoadBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                pauseTable.setVisible(false);
                saveManager.setVisible(true);
            }
        });

        controlBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                pauseTable.setVisible(false);
                controlUI.setVisible(true);
            }
        });

        retryBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { retryRoom(); }
        });

        withdrawBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                togglePause();
                mainMenu.isStarted = false;
                Gdx.input.setInputProcessor(mainMenu.stage);
            }
        });

        // 7. Add to Table
        pauseTable.add(new Label("GAME PAUSED", new Label.LabelStyle(font, Color.WHITE))).padBottom(30).row();
        pauseTable.add(continueBtn).width(58).height(15).pad(5).row();
        pauseTable.add(saveLoadBtn).width(58).height(15).pad(5).row();
        pauseTable.add(controlBtn).width(58).height(15).pad(5).row();
        pauseTable.add(retryBtn).width(58).height(15).pad(5).row();
        pauseTable.add(withdrawBtn).width(58).height(15).pad(5);

        // 8. Init Sub-Menus
        saveManager = new saveManager(player, true,
            () -> { saveManager.setVisible(false); pauseTable.setVisible(true); },
            () -> { togglePause(); }
        );
        saveManager.setVisible(false);

        controlUI = new controlUI(() -> {
            controlUI.setVisible(false);
            pauseTable.setVisible(true);
        });
        controlUI.setVisible(false);

        stage.addActor(pauseTable);
        stage.addActor(saveManager);
        stage.addActor(controlUI);
    }

    private void retryRoom() {
        io.github.devsimulator.helper.WorldContactListener.pendingFastReload = true;
        togglePause();
    }

    public void update() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) togglePause();
    }

    public void togglePause() {
        isPaused = !isPaused;
        if (isPaused) {
            pauseTable.setVisible(true);
            saveManager.setVisible(false);
            controlUI.setVisible(false);
            Gdx.input.setInputProcessor(stage);
        } else {
            Gdx.input.setInputProcessor(null);
            hoveredButton = null;
        }
    }

    public void render(float dt) {
        if (isPaused) {
            stage.act(dt);
            stage.draw();

            // FIX: Added null check for hoverAnimation to prevent crashes
            if (hoveredButton != null && pauseTable.isVisible() && hoverAnimation != null) {
                stateTime += dt;
                TextureRegion currentFrame = hoverAnimation.getKeyFrame(stateTime);
                Vector2 pos = hoveredButton.localToStageCoordinates(new Vector2(0, 0));

                stage.getBatch().begin();
                stage.getBatch().draw(currentFrame, pos.x - 20, pos.y + 3);
                stage.getBatch().end();
            }
        }
    }

    public void dispose() {
        stage.dispose();
        font.dispose();
        continueTex.dispose();
        saveLoadTex.dispose();
        retryTex.dispose();
        withdrawTex.dispose();
        hoverSheet.dispose();
        controlTex.dispose();
        if(saveManager != null) saveManager.dispose();
        if(controlUI != null) controlUI.dispose();
    }
}
