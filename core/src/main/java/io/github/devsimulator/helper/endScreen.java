package io.github.devsimulator.helper; // Updated package!

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.devsimulator.Main;

public class endScreen implements Screen { // Updated class name!
    private Main game;
    private SpriteBatch batch;
    private BitmapFont fontTitle;
    private BitmapFont fontSub;
    private float alpha = 0f;
    private float stateTime = 0f;

    public endScreen(Main game) {
        this.game = game;
        this.batch = new SpriteBatch();

        this.fontTitle = new BitmapFont();
        this.fontTitle.getData().setScale(2.5f);

        this.fontSub = new BitmapFont();
        this.fontSub.getData().setScale(1.2f);
    }

    @Override
    public void show() { }

    @Override
    public void render(float delta) {
        stateTime += delta;

        // Cinematic Fade-in effect
        if (alpha < 1f) {
            alpha += delta * 0.5f;
            if (alpha > 1f) alpha = 1f;
        }

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.08f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        fontTitle.setColor(1, 1, 1, alpha);
        fontTitle.draw(batch, "THANK YOU FOR PLAYING", Gdx.graphics.getWidth() / 2f - 220, Gdx.graphics.getHeight() / 2f + 50);

        fontSub.setColor(0.7f, 0.7f, 0.7f, alpha);
        fontSub.draw(batch, "Ezra's journey will continue in the full game.", Gdx.graphics.getWidth() / 2f - 180, Gdx.graphics.getHeight() / 2f - 20);

        // Blinking Prompt
        if (alpha >= 1f && (Math.sin(stateTime * 5f) > 0)) {
            fontSub.setColor(1f, 1f, 0.4f, 1f);
            fontSub.draw(batch, "Press ESC or Click to Exit", Gdx.graphics.getWidth() / 2f - 100, Gdx.graphics.getHeight() / 2f - 100);
        }

        batch.end();

        if (alpha >= 1f && (Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))) {
            Gdx.app.exit();
        }
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        fontTitle.dispose();
        fontSub.dispose();
    }
}
