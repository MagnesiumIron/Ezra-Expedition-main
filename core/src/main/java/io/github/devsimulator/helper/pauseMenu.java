package io.github.devsimulator.helper;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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

public class pauseMenu {
    public Stage stage;
    private Player player;
    private mainMenu mainMenu; // Reference to the main menu
    public boolean isPaused = false;
    private BitmapFont font;

    public pauseMenu(Player player, mainMenu mainMenu) {
        this.player = player;
        this.mainMenu = mainMenu;
        stage = new Stage(new ScreenViewport());
        font = new BitmapFont();

        Pixmap btnPix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        btnPix.setColor(0.4f, 0.4f, 0.4f, 1f);
        btnPix.fill();
        TextureRegionDrawable buttonBg = new TextureRegionDrawable(new TextureRegion(new Texture(btnPix)));
        btnPix.dispose();

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = buttonBg;
        buttonStyle.font = font;

        Label title = new Label("GAME PAUSED", new Label.LabelStyle(font, Color.WHITE));
        TextButton resumeBtn = new TextButton("Resume", buttonStyle);
        TextButton saveBtn = new TextButton("Save Game", buttonStyle);
        TextButton loadBtn = new TextButton("Load Game", buttonStyle);

        // --- NEW EXIT BUTTON ---
        TextButton exitToMenuBtn = new TextButton("Exit to Main Menu", buttonStyle);

        resumeBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { togglePause(); }
        });

        saveBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { saveGame(); }
        });

        loadBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { loadGame(); }
        });

        exitToMenuBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                togglePause(); // Unpause the game engine behind the scenes
                mainMenu.isStarted = false; // Tell Main to show the menu
                Gdx.input.setInputProcessor(mainMenu.stage); // Give mouse control back to Main Menu
            }
        });

        Table table = new Table();
        table.setFillParent(true);

        Pixmap bgPix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        bgPix.setColor(0, 0, 0, 0.7f);
        bgPix.fill();
        table.setBackground(new TextureRegionDrawable(new TextureRegion(new Texture(bgPix))));
        bgPix.dispose();

        table.add(title).padBottom(40).row();
        table.add(resumeBtn).width(200).height(40).pad(10).row();
        table.add(saveBtn).width(200).height(40).pad(10).row();
        table.add(loadBtn).width(200).height(40).pad(10).row();
        table.add(exitToMenuBtn).width(200).height(40).pad(10); // Replaced Desktop with Menu

        stage.addActor(table);
    }

    public void update() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            togglePause();
        }
    }

    public void togglePause() {
        isPaused = !isPaused;
        if (isPaused) {
            Gdx.input.setInputProcessor(stage);
        } else {
            Gdx.input.setInputProcessor(null);
        }
    }

    public void render(float dt) {
        if (isPaused) {
            stage.act(dt);
            stage.draw();
        }
    }

    private void saveGame() {
        Preferences prefs = Gdx.app.getPreferences("EzrasExpeditionSave");
        prefs.putFloat("playerX", player.b2body.getPosition().x);
        prefs.putFloat("playerY", player.b2body.getPosition().y);
        prefs.putFloat("hp", player.hp);
        prefs.putFloat("assimilation", player.assimilationMeter);
        prefs.flush();
        System.out.println("Game Saved Successfully!");
    }

    private void loadGame() {
        Preferences prefs = Gdx.app.getPreferences("EzrasExpeditionSave");
        if (prefs.contains("playerX")) {
            float x = prefs.getFloat("playerX");
            float y = prefs.getFloat("playerY");
            player.b2body.setTransform(x, y, 0);
            player.hp = prefs.getFloat("hp");
            player.assimilationMeter = prefs.getFloat("assimilation");

            System.out.println("Game Loaded Successfully!");
            togglePause();
        } else {
            System.out.println("No save data found!");
        }
    }

    public void dispose() {
        stage.dispose();
        font.dispose();
    }
}
