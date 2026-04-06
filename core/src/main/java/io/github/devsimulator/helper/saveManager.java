package io.github.devsimulator.helper;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import io.github.devsimulator.entities.Player;
import io.github.devsimulator.Main;

public class saveManager extends Table {
    private Player player;
    private Runnable onClose;
    private Runnable onLoadSuccess;

    private BitmapFont font;
    private Texture uiHolderTex, uiSlotTex;

    private Table modeTable;
    private Table slotsTable;
    private Table confirmTable;

    private boolean isSavingMode = false;
    private int pendingSlot = -1;

    public saveManager(Player player, boolean allowSave, Runnable onClose, Runnable onLoadSuccess) {
        this.player = player;
        this.onClose = onClose;
        this.onLoadSuccess = onLoadSuccess;

        this.setFillParent(true);
        font = new BitmapFont(Gdx.files.internal("fantasyfontt.fnt"));
        uiHolderTex = new Texture("state_UI.png");
        uiSlotTex = new Texture("state_slots.png");

        Pixmap dimPix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        dimPix.setColor(0, 0, 0, 0.85f);
        dimPix.fill();
        this.setBackground(new TextureRegionDrawable(new TextureRegion(new Texture(dimPix))));
        dimPix.dispose();

        modeTable = new Table();
        slotsTable = new Table();
        slotsTable.setBackground(new TextureRegionDrawable(new TextureRegion(uiHolderTex)));
        confirmTable = new Table();

        Stack viewStack = new Stack();
        viewStack.add(modeTable);
        viewStack.add(slotsTable);
        viewStack.add(confirmTable);
        this.add(viewStack).expand().center();

        buildConfirmTable();

        if (allowSave) {
            buildModeTable();
            showModeTable();
        } else {
            isSavingMode = false;
            buildSlotsTable();
            showSlotsTable();
        }
    }

    private void buildModeTable() {
        modeTable.clear();
        Label.LabelStyle textStyle = new Label.LabelStyle(font, Color.WHITE);
        modeTable.add(new Label("CHOOSE ACTION", textStyle)).padBottom(20).row();

        Pixmap tempPix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        tempPix.setColor(0.3f, 0.3f, 0.3f, 1f);
        tempPix.fill();
        ImageButton.ImageButtonStyle tempStyle = new ImageButton.ImageButtonStyle();
        tempStyle.up = new TextureRegionDrawable(new TextureRegion(new Texture(tempPix)));

        ImageButton saveBtn = new ImageButton(tempStyle);
        ImageButton loadBtn = new ImageButton(tempStyle);
        ImageButton backBtn = new ImageButton(tempStyle);

        saveBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                isSavingMode = true;
                buildSlotsTable();
                showSlotsTable();
            }
        });

        loadBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                isSavingMode = false;
                buildSlotsTable();
                showSlotsTable();
            }
        });

        backBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { onClose.run(); }
        });

        Table btnLayout = new Table();
        btnLayout.add(new Label("SAVE", textStyle)).padRight(10);
        btnLayout.add(saveBtn).width(40).height(15).row();
        btnLayout.add(new Label("LOAD", textStyle)).padRight(10).padTop(10);
        btnLayout.add(loadBtn).width(40).height(15).padTop(10).row();

        modeTable.add(btnLayout).padBottom(20).row();
        modeTable.add(backBtn).width(40).height(15);
    }

    private void buildSlotsTable() {
        slotsTable.clear();
        Label.LabelStyle textStyle = new Label.LabelStyle(font, Color.WHITE);
        slotsTable.add(new Label(isSavingMode ? "SAVE GAME" : "LOAD GAME", textStyle)).padBottom(10).row();

        for (int i = 1; i <= 3; i++) {
            final int slotNum = i;
            Preferences prefs = Gdx.app.getPreferences("EzraSave_" + i);
            boolean hasData = prefs.getBoolean("hasData", false);

            Stack slotStack = new Stack();
            slotStack.add(new Image(uiSlotTex));

            Table textTable = new Table();
            if (hasData) {
                String map = prefs.getString("mapName", "Unknown");
                int hp = (int) prefs.getFloat("hp", 0);
                textTable.add(new Label("SAVE " + i, textStyle)).padBottom(2).row();
                textTable.add(new Label("HP: " + hp, textStyle)).padBottom(2).row();
                textTable.add(new Label(map, textStyle));
            } else {
                textTable.add(new Label("NO DATA", textStyle));
            }
            slotStack.add(textTable);

            slotStack.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    if (isSavingMode) {
                        if (hasData) {
                            pendingSlot = slotNum;
                            showConfirmTable();
                        } else {
                            executeSave(slotNum);
                        }
                    } else {
                        if (hasData) executeLoad(slotNum);
                    }
                }
            });
            slotsTable.add(slotStack).pad(5).row();
        }

        Pixmap tempPix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        tempPix.setColor(0.4f, 0.4f, 0.4f, 1f);
        tempPix.fill();
        ImageButton.ImageButtonStyle backStyle = new ImageButton.ImageButtonStyle();
        backStyle.up = new TextureRegionDrawable(new TextureRegion(new Texture(tempPix)));

        ImageButton backBtn = new ImageButton(backStyle);
        backBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (modeTable.getCells().size > 0) showModeTable();
                else onClose.run();
            }
        });
        slotsTable.add(backBtn).width(40).height(15).padTop(10);
    }

    private void buildConfirmTable() {
        confirmTable.clear();
        Label.LabelStyle textStyle = new Label.LabelStyle(font, Color.WHITE);

        Table box = new Table();
        box.setBackground(new TextureRegionDrawable(new TextureRegion(uiHolderTex)));
        box.add(new Label("OVERWRITE DATA?", textStyle)).colspan(2).padBottom(20).row();

        Pixmap tempPix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        tempPix.setColor(0.3f, 0.3f, 0.3f, 1f);
        tempPix.fill();
        ImageButton.ImageButtonStyle tempStyle = new ImageButton.ImageButtonStyle();
        tempStyle.up = new TextureRegionDrawable(new TextureRegion(new Texture(tempPix)));

        ImageButton yesBtn = new ImageButton(tempStyle);
        ImageButton noBtn = new ImageButton(tempStyle);

        yesBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                executeSave(pendingSlot);
                showSlotsTable();
            }
        });

        noBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                pendingSlot = -1;
                showSlotsTable();
            }
        });

        box.add(yesBtn).width(40).height(15).padRight(10);
        box.add(noBtn).width(40).height(15);
        confirmTable.add(box);
    }

    private void executeSave(int slot) {
        Preferences prefs = Gdx.app.getPreferences("EzraSave_" + slot);
        prefs.putBoolean("hasData", true);
        prefs.putFloat("playerX", player.b2body.getPosition().x);
        prefs.putFloat("playerY", player.b2body.getPosition().y);
        prefs.putFloat("hp", player.hp);
        prefs.putFloat("assimilation", player.assimilationMeter);

        // UPDATED: Use correct map path
        prefs.putString("mapName", io.github.devsimulator.levels.mapManager.currentMapPath);

        prefs.flush();
        buildSlotsTable();
    }

    private void executeLoad(int slot) {
        Preferences prefs = Gdx.app.getPreferences("EzraSave_" + slot);

        float savedX = prefs.getFloat("playerX");
        float savedY = prefs.getFloat("playerY");

        // UPDATED: Safe default to the correct map path
        String savedMap = prefs.getString("mapName", "prologueassets/prologuespawn.tmx");

        player.b2body.setTransform(savedX, savedY, 0);
        player.hp = prefs.getFloat("hp");
        player.assimilationMeter = prefs.getFloat("assimilation");

        WorldContactListener.pendingTransition = new io.github.devsimulator.helper.tilemapmanager.TransitionData(
            savedMap, savedX, savedY
        );

        onLoadSuccess.run();
    }

    private void showModeTable() { modeTable.setVisible(true); slotsTable.setVisible(false); confirmTable.setVisible(false); }
    private void showSlotsTable() { modeTable.setVisible(false); slotsTable.setVisible(true); confirmTable.setVisible(false); }
    private void showConfirmTable() { modeTable.setVisible(false); slotsTable.setVisible(false); confirmTable.setVisible(true); }

    public void dispose() {
        font.dispose();
        uiHolderTex.dispose();
        uiSlotTex.dispose();
    }
}
