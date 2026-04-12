package io.github.devsimulator.helper;

import com.badlogic.gdx.Gdx;
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
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.devsimulator.Main;

public class endScreen {
    private Main game;
    private Stage stage;
    private BitmapFont font;
    private Texture bgDimTex, btnTex;

    public endScreen(Main game) {
        this.game = game;
        // 1. Setup Stage with fixed resolution - this prevents the "blank screen" issue
        this.stage = new Stage(new FitViewport(720, 480));

        // Ensure the stage gets input immediately
        Gdx.input.setInputProcessor(stage);

        // 2. Load Assets
        font = new BitmapFont(Gdx.files.internal("fantasyfontt.fnt"));
        btnTex = new Texture("state_slots.png"); // Reusing your slot texture for buttons

        // 3. Create the dimmed background
        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(0, 0, 0, 0.85f);
        pix.fill();
        bgDimTex = new Texture(pix);
        pix.dispose();

        // 4. Build the UI Layout
        Table root = new Table();
        root.setFillParent(true);
        root.setBackground(new TextureRegionDrawable(new TextureRegion(bgDimTex)));

        // Styles
        Label.LabelStyle titleStyle = new Label.LabelStyle(font, Color.GOLD);
        Label.LabelStyle textStyle = new Label.LabelStyle(font, Color.WHITE);

        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.up = new TextureRegionDrawable(new TextureRegion(btnTex));
        btnStyle.font = font;
        btnStyle.fontColor = Color.WHITE;
        btnStyle.overFontColor = Color.GOLD;

        // UI Elements
        Label titleLabel = new Label("DEMO COMPLETED", titleStyle);
        titleLabel.setFontScale(1.5f);

        Label thanksLabel = new Label("THANKS FOR PLAYING EZRA'S EXPEDITION!", textStyle);
        thanksLabel.setFontScale(1.2f);

        Label subLabel = new Label("THE JOURNEY WILL CONTINUE IN THE NEW GAME.", textStyle);
        subLabel.setFontScale(1f);

        TextButton menuBtn = new TextButton("RETURN TO MENU", btnStyle);
        menuBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.returnToMenu(); // Call the fix we just made!
            }
        });

        // Add elements to the Table
        root.add(titleLabel).padBottom(20).row( );
        root.add(thanksLabel).padBottom(10).row();
        root.add(subLabel).padBottom(50).row();
        root.add(menuBtn).width(200).height(50);

        stage.addActor(root);
    }

    public void render(float dt) {
        // Safety: Keep input focused on this screen
        if (Gdx.input.getInputProcessor() != stage) {
            Gdx.input.setInputProcessor(stage);
        }

        stage.act(dt);
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        stage.dispose();
        font.dispose();
        bgDimTex.dispose();
        btnTex.dispose();
    }
}
