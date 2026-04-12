package io.github.devsimulator.helper;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class controlUI extends Table {
    private Runnable onClose;
    private Texture uiTex, uiSlotTex, bgDimTex;
    private BitmapFont font;

    public controlUI(Runnable onClose) {
        this.onClose = onClose;
        this.setFillParent(true);

        // 1. Load Assets (Exactly like saveManager)
        font = new BitmapFont(Gdx.files.internal("fantasyfontt.fnt"));
        uiTex = new Texture("control_UI.png");      // This will be the layout background
        uiSlotTex = new Texture("state_slots.png"); // This is for the button background

        // 2. Dimmed Screen Background (Exactly like saveManager)
        Pixmap dimPix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        dimPix.setColor(0, 0, 0, 0.85f);
        dimPix.fill();
        bgDimTex = new Texture(dimPix);
        this.setBackground(new TextureRegionDrawable(new TextureRegion(bgDimTex)));
        dimPix.dispose();

        // 3. Create the Inner Table (Like slotsTable in saveManager)
        Table contentTable = new Table();
        contentTable.setBackground(new TextureRegionDrawable(new TextureRegion(uiTex)));

        // 4. Setup the BACK Button (Exact copy of saveManager style)
        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.up = new TextureRegionDrawable(new TextureRegion(uiSlotTex));
        btnStyle.font = font;
        btnStyle.fontColor = Color.WHITE;

        TextButton backBtn = new TextButton("BACK", btnStyle);
        backBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                onClose.run();
            }
        });

        // 5. Position the button inside the graphic
        // Since the graphic already has the "Keyboard Controls" text, we just add the button at the bottom.
        // Adjust padTop to push the button down so it doesn't cover the control instructions.
        contentTable.add(backBtn).width(75).height(15).expand().bottom().padBottom(0);

        // 6. Center the contentTable on the dimmed screen
        this.add(contentTable).width(uiTex.getWidth()).height(uiTex.getHeight()).center();
    }

    public void dispose() {
        if (font != null) font.dispose();
        if (uiTex != null) uiTex.dispose();
        if (uiSlotTex != null) uiSlotTex.dispose();
        if (bgDimTex != null) bgDimTex.dispose();
    }
}
