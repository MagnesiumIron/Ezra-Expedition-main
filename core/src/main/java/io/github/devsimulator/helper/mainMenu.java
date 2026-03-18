package io.github.devsimulator.helper;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.devsimulator.entities.Player;
import io.github.devsimulator.Main;

public class mainMenu {
    public Stage stage;
    public boolean isStarted = false;
    private BitmapFont font;
    private Player player; // Added Player reference

    public mainMenu(Player player) {
        this.player = player;
        stage = new Stage(new ScreenViewport());
        font = new BitmapFont();
        font.getData().setScale(1.5f);

        Pixmap btnPix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        btnPix.setColor(0.3f, 0.3f, 0.3f, 1f);
        btnPix.fill();
        TextureRegionDrawable buttonBg = new TextureRegionDrawable(new TextureRegion(new Texture(btnPix)));
        btnPix.dispose();

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = buttonBg;
        buttonStyle.font = font;

        Label.LabelStyle titleStyle = new Label.LabelStyle(font, Color.GOLD);
        Label title = new Label("EZRA'S EXPEDITION", titleStyle);
        title.setFontScale(2.0f);

        // --- NEW BUTTONS ---
        TextButton startBtn = new TextButton("New Expedition", buttonStyle);
        TextButton loadBtn = new TextButton("Load Save", buttonStyle);
        TextButton exitBtn = new TextButton("Exit Game", buttonStyle);

        startBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { startGame(); }
        });

        loadBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { loadGame(); }
        });

        exitBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { Gdx.app.exit(); }
        });

        Table table = new Table();
        table.setFillParent(true);

        Pixmap bgPix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        bgPix.setColor(0.1f, 0.1f, 0.15f, 1f);
        bgPix.fill();
        table.setBackground(new TextureRegionDrawable(new TextureRegion(new Texture(bgPix))));
        bgPix.dispose();

        table.add(title).padBottom(60).row();
        table.add(startBtn).width(250).height(50).pad(10).row();
        table.add(loadBtn).width(250).height(50).pad(10).row();
        table.add(exitBtn).width(250).height(50).pad(10);

        stage.addActor(table);
        Gdx.input.setInputProcessor(stage);
    }

    private void startGame() {
        // Reset player for a fresh run
        player.b2body.setTransform(100 / Main.PPM, 200 / Main.PPM, 0);
        player.hp = player.MAX_HP;
        player.assimilationMeter = 0;
        player.currentState = Player.State.NORMAL;

        isStarted = true;
        Gdx.input.setInputProcessor(null);
    }

    private void loadGame() {
        Preferences prefs = Gdx.app.getPreferences("EzrasExpeditionSave");
        if (prefs.contains("playerX")) {
            player.b2body.setTransform(prefs.getFloat("playerX"), prefs.getFloat("playerY"), 0);
            player.hp = prefs.getFloat("hp");
            player.assimilationMeter = prefs.getFloat("assimilation");

            System.out.println("Game Loaded from Main Menu!");
            isStarted = true; // Launch the game
            Gdx.input.setInputProcessor(null);
        } else {
            System.out.println("No save data found!");
        }
    }

    public void render(float dt) {
        if (!isStarted) {
            stage.act(dt);
            stage.draw();
        }
    }

    public void dispose() {
        stage.dispose();
        font.dispose();
    }
}
