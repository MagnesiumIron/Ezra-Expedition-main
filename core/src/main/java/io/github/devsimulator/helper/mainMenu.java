package io.github.devsimulator.helper;

import com.badlogic.gdx.Gdx;
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
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.devsimulator.entities.Player;
import io.github.devsimulator.Main;

public class mainMenu {
    public Stage stage;
    public boolean isStarted = false;
    private BitmapFont font;
    private Player player;

    private Texture continueTex, saveLoadTex, withdrawTex, hoverSheet, menuBg;
    private Animation<TextureRegion> hoverAnimation;
    private float stateTime = 0f;
    private Actor hoveredButton = null;

    private Table mainTable;
    private saveManager saveManager;
    private Label title;

    public mainMenu(Player player) {
        this.player = player;
        stage = new Stage(new FitViewport(360, 240));
        font = new BitmapFont(Gdx.files.internal("fantasyfontt.fnt"));

        menuBg = new Texture(Gdx.files.internal("bg_menu.png"));
        continueTex = new Texture("menubtn_continue.png");
        saveLoadTex = new Texture("menubtn_saveload.png");
        withdrawTex = new Texture("menubtn_withdraw.png");
        hoverSheet = new Texture("menubtn_hovereffect.png");
        int frameCount = hoverSheet.getWidth() / 14;
        TextureRegion[][] tmp = TextureRegion.split(hoverSheet, 14, 9);
        TextureRegion[] frames = new TextureRegion[frameCount];
        System.arraycopy(tmp[0], 0, frames, 0, frameCount);
        hoverAnimation = new Animation<>(0.15f, frames);
        hoverAnimation.setPlayMode(Animation.PlayMode.LOOP);

        ImageButton continueBtn = new ImageButton(new TextureRegionDrawable(new TextureRegion(continueTex)));
        ImageButton loadBtn = new ImageButton(new TextureRegionDrawable(new TextureRegion(saveLoadTex)));
        ImageButton withdrawBtn = new ImageButton(new TextureRegionDrawable(new TextureRegion(withdrawTex)));

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
        loadBtn.addListener(hoverListener);
        withdrawBtn.addListener(hoverListener);

        continueBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { startGame(); }
        });

        loadBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                boolean hasAnySave = false;
                for (int i = 1; i <= 3; i++) {
                    if (Gdx.app.getPreferences("EzraSave_" + i).getBoolean("hasData", false)) hasAnySave = true;
                }

                if (!hasAnySave) {
                    title.setText("NO SAVES FOUND");
                    title.setColor(Color.RED);
                    Timer.schedule(new Timer.Task() {
                        @Override public void run() { title.setText("EZRA'S EXPEDITION"); title.setColor(Color.WHITE); }
                    }, 2f);
                    return;
                }

                mainTable.setVisible(false);
                saveManager.setVisible(true);
            }
        });

        withdrawBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { Gdx.app.exit(); }
        });

        mainTable = new Table();
        mainTable.setFillParent(true);

        mainTable.setBackground(new TextureRegionDrawable(new TextureRegion(menuBg)));

        title = new Label("EZRA'S EXPEDITION", new Label.LabelStyle(font, Color.WHITE));

        mainTable.add(title).padBottom(30).row();
        mainTable.add(continueBtn).width(58).height(15).pad(5).row();
        mainTable.add(loadBtn).width(58).height(15).pad(5).row();
        mainTable.add(withdrawBtn).width(58).height(15).pad(5);

        saveManager = new saveManager(player, false,
            () -> { saveManager.setVisible(false); mainTable.setVisible(true); },
            () -> { isStarted = true; Gdx.input.setInputProcessor(null); }
        );
        saveManager.setVisible(false);

        stage.addActor(mainTable);
        stage.addActor(saveManager);
        Gdx.input.setInputProcessor(stage);
    }

    private void startGame() {
        float spawnX = 100 / Main.PPM;
        float spawnY = 200 / Main.PPM;

        player.b2body.setTransform(spawnX, spawnY, 0);
        player.hp = player.MAX_HP;
        player.assimilationMeter = 0;

        //Wipe all the data if new game is pressed
        player.elementSlots[0] = "NONE";
        player.elementSlots[1] = "NONE";
        player.chargeSlots[0] = 0;
        player.chargeSlots[1] = 0;
        player.activeSlot = 0;

        io.github.devsimulator.helper.WorldContactListener.pendingTransition =
            new io.github.devsimulator.helper.tilemapmanager.TransitionData("prologuespawn.tmx", spawnX, spawnY);

        isStarted = true;
        Gdx.input.setInputProcessor(null);
    }

    public void render(float dt) {
        if (!isStarted) {
            stage.act(dt);
            stage.draw();

            if (hoveredButton != null && mainTable.isVisible()) {
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
        withdrawTex.dispose();
        hoverSheet.dispose();
        menuBg.dispose();
        if(saveManager != null) saveManager.dispose();
    }
}
