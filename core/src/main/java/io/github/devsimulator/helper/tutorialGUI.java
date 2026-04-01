package io.github.devsimulator.helper;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class tutorialGUI {
    public Stage stage;
    private Label headerLabel, descLabel, promptLabel;
    private float stateTime = 0;
    private Texture bgTexture;

    public tutorialGUI(BitmapFont font) {
        // MATCHING PAUSE MENU: 360x240 logic size is the fix!
        font.setFixedWidthGlyphs("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 ");
        stage = new Stage(new FitViewport(360, 240));

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0.8f);
        pixmap.fill();
        bgTexture = new Texture(pixmap);
        pixmap.dispose();

        Label.LabelStyle headerStyle = new Label.LabelStyle(font, Color.YELLOW);
        Label.LabelStyle descStyle = new Label.LabelStyle(font, Color.WHITE);
        Label.LabelStyle promptStyle = new Label.LabelStyle(font, Color.LIGHT_GRAY);

        headerLabel = new Label("", headerStyle);
        headerLabel.setAlignment(Align.center);

        descLabel = new Label("", descStyle);
        descLabel.setWrap(true); // Wraps text inside the 280-pixel box below
        descLabel.setAlignment(Align.center);

        promptLabel = new Label("[PRESS F TO CONTINUE]", promptStyle);
        promptLabel.setAlignment(Align.center);

        Table table = new Table();
        table.setFillParent(true);
        table.setBackground(new TextureRegionDrawable(new TextureRegion(bgTexture)));

        // Logic-size widths for a 360-wide screen
        table.add(headerLabel).padBottom(10).width(300).row();
        table.add(descLabel).width(280).padBottom(20).row();
        table.add(promptLabel).row();

        stage.addActor(table);
    }

    public void show(String header, String description) {
        headerLabel.setText(header);
        descLabel.setText(description);
    }

    public void render(float dt) {
        stateTime += dt;
        float pulse = 0.5f + MathUtils.sin(stateTime * 4f) * 0.5f;
        promptLabel.setColor(0.8f, 0.8f, 0.8f, pulse);

        stage.act(dt);
        stage.draw();
    }

    public void resize(int width, int height) {
        // Important: this keeps the stage crisp when you maximize the window
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        stage.dispose();
        if (bgTexture != null) bgTexture.dispose();
    }
}
