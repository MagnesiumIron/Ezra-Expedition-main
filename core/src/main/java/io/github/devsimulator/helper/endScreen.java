package io.github.devsimulator.helper;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;
import io.github.devsimulator.Main;

public class endScreen {
    private Main game;
    private SpriteBatch batch;
    private BitmapFont font;
    private GlyphLayout layout;
    private Texture bgDimTex;

    private float alpha = 0f;
    private float stateTime = 0f;

    public endScreen(Main game) {
        this.game = game;
        this.batch = new SpriteBatch();
        this.layout = new GlyphLayout();

        // Load your custom font
        try {
            this.font = new BitmapFont(Gdx.files.internal("fantasyfontt.fnt"));
        } catch (Exception e) {
            Gdx.app.error("endScreen", "Font load failed, using default: " + e.getMessage());
            this.font = new BitmapFont();
        }

        // Create the cinematic dimmed background
        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(0, 0, 0, 0.85f);
        pix.fill();
        this.bgDimTex = new Texture(pix);
        pix.dispose();
    }

    public void render(float dt) {
        stateTime += dt;

        // Fade-in logic
        if (alpha < 1f) {
            alpha += dt * 0.5f;
            if (alpha > 1f) alpha = 1f;
        }

        // Use the game's UI viewport settings for perfect scaling
        batch.setProjectionMatrix(game.viewport.getCamera().combined);
        batch.begin();

        // 1. Draw Background Dim
        batch.setColor(1, 1, 1, alpha);
        batch.draw(bgDimTex, 0, 0, game.viewport.getWorldWidth(), game.viewport.getWorldHeight());

        float centerX = game.viewport.getWorldWidth() / 2f;
        float centerY = game.viewport.getWorldHeight() / 2f;

        // 2. Draw Title
        font.getData().setScale(1.2f);
        font.setColor(Color.WHITE);
        font.getColor().a = alpha;
        String title = "DEMO COMPLETED";
        layout.setText(font, title);
        font.draw(batch, title, centerX - layout.width / 2f, centerY + 50);

        // 3. Draw Subtitle
        font.getData().setScale(0.5f);
        font.setColor(Color.LIGHT_GRAY);
        font.getColor().a = alpha;
        String sub = "Ezra's journey will continue in the full game.";
        layout.setText(font, sub);
        font.draw(batch, sub, centerX - layout.width / 2f, centerY - 10);

        // 4. Draw Blinking Prompt
        if (alpha >= 1f && (Math.sin(stateTime * 4f) > 0)) {
            font.setColor(Color.GOLD);
            String prompt = "Press ESC to return to Menu";
            layout.setText(font, prompt);
            font.draw(batch, prompt, centerX - layout.width / 2f, centerY - 80);
        }

        batch.end();
        batch.setColor(Color.WHITE); // Reset batch color

        // 5. Input Handling
        if (alpha >= 1f && (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.justTouched())) {
            // Reset game state and return to menu
            game.isDemoEnded = false;
            // Assuming your mainMenu is accessible in Main
            // game.mainMenu.isStarted = false;
            // Gdx.input.setInputProcessor(game.mainMenu.stage);

            // For now, exit just to be safe as per your code
            Gdx.app.exit();
        }
    }

    public void resize(int width, int height) {
        // Main.java handles the viewport update, but we ensure our batch is ready
    }

    public void dispose() {
        batch.dispose();
        if (font != null) font.dispose();
        if (bgDimTex != null) bgDimTex.dispose();
    }
}
