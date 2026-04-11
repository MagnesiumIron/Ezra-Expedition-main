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
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
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

    // Helper method to generate a clean TextButton style for all buttons
    private TextButton.TextButtonStyle getSharedBtnStyle() {
        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.up = new TextureRegionDrawable(new TextureRegion(uiSlotTex)); // Using your slot texture for a cleaner look
        btnStyle.font = font;
        btnStyle.fontColor = Color.WHITE;
        return btnStyle;
    }

    private void buildModeTable() {
        modeTable.clear();
        Label.LabelStyle textStyle = new Label.LabelStyle(font, Color.WHITE);
        modeTable.add(new Label("CHOOSE ACTION", textStyle)).padBottom(10).row();

        TextButton.TextButtonStyle btnStyle = getSharedBtnStyle();

        TextButton saveBtn = new TextButton("SAVE", btnStyle);
        TextButton loadBtn = new TextButton("LOAD", btnStyle);
        TextButton backBtn = new TextButton("BACK", btnStyle);

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
        btnLayout.add(saveBtn).width(75).height(15).padBottom(5).row();
        btnLayout.add(loadBtn).width(75).height(15).row();

        modeTable.add(btnLayout).padBottom(10).row();
        modeTable.add(backBtn).width(50).height(15);
    }

    private void buildSlotsTable() {
        slotsTable.clear();
        Label.LabelStyle textStyle = new Label.LabelStyle(font, Color.WHITE);
        slotsTable.add(new Label(isSavingMode ? "SAVE GAME" : "LOAD GAME", textStyle)).padBottom(5).row();

        for (int i = 1; i <= 3; i++) {
            final int slotNum = i;
            Preferences prefs = Gdx.app.getPreferences("EzraSave_" + i);
            boolean hasData = prefs.getBoolean("hasData", false);

            Stack slotStack = new Stack();
            slotStack.add(new Image(uiSlotTex));

            Table textTable = new Table();
            if (hasData) {
                String rawMap = prefs.getString("mapName", "UNKNOWN").toUpperCase();
                String cleanMap = "UNKNOWN";
                if (rawMap.contains("PROLOGUE")) cleanMap = "PROLOGUE";
                else if (rawMap.contains("LEVEL1")) cleanMap = "LEVEL ONE";
                else if (rawMap.contains("LEVEL2")) cleanMap = "LEVEL TWO";

                int hp = (int) prefs.getFloat("hp", 0);

                // grab the elements
                String s1 = prefs.getString("slot1_element", "NONE").toUpperCase();
                String s2 = prefs.getString("slot2_element", "NONE").toUpperCase();
                if(s1.length() > 4) s1 = s1.substring(0, 4);
                if(s2.length() > 4) s2 = s2.substring(0, 4);

                String slotNumeral = (i == 1) ? "I" : ((i == 2) ? "II" : "III");

                textTable.add(new Label("SAVE " + slotNumeral, textStyle)).padBottom(1).row();
                textTable.add(new Label("HP " + hp, textStyle)).padBottom(1).row();
                textTable.add(new Label(cleanMap, textStyle)).padBottom(2).row();
                textTable.add(new Label("EQ I " + s1 + "   EQ II " + s2, textStyle));
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

        TextButton.TextButtonStyle btnStyle = getSharedBtnStyle();
        TextButton backBtn = new TextButton("BACK", btnStyle);
        backBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (modeTable.getCells().size > 0) showModeTable();
                else onClose.run();
            }
        });

        slotsTable.add(backBtn).width(75).height(15).padTop(5);
    }

    private void buildConfirmTable() {
        confirmTable.clear();

        Label.LabelStyle titleStyle = new Label.LabelStyle(font, Color.WHITE);
        Label msg = new Label("O V E R W R I T E   D A T A", titleStyle);

        TextButton.TextButtonStyle btnStyle = getSharedBtnStyle();

        TextButton yesBtn = new TextButton("YES", btnStyle);
        TextButton noBtn  = new TextButton("NO", btnStyle);

        yesBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                executeSave(pendingSlot);
                showSlotsTable();
            }
        });

        noBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                pendingSlot = -1;
                showSlotsTable();
            }
        });

        Table btnRow = new Table();
        btnRow.add(yesBtn).width(40).height(15).padRight(10);
        btnRow.add(noBtn).width(40).height(15);

        confirmTable.add(msg).padBottom(20).row();
        confirmTable.add(btnRow);
    }

    private void executeSave(int slot) {
        Preferences prefs = Gdx.app.getPreferences("EzraSave_" + slot);
        prefs.putBoolean("hasData", true);
        prefs.putFloat("playerX", player.b2body.getPosition().x);
        prefs.putFloat("playerY", player.b2body.getPosition().y);
        prefs.putFloat("hp", player.hp);
        prefs.putFloat("assimilation", player.assimilationMeter);
        prefs.putString("mapName", io.github.devsimulator.levels.mapManager.currentMapPath);

        prefs.putString("slot1_element", player.elementSlots[0]);
        prefs.putString("slot2_element", player.elementSlots[1]);
        prefs.putInteger("slot1_charges", player.chargeSlots[0]);
        prefs.putInteger("slot2_charges", player.chargeSlots[1]);
        prefs.putInteger("activeSlot", player.activeSlot);

        prefs.flush();
        buildSlotsTable();
    }

    private void executeLoad(int slot) {
        Preferences prefs = Gdx.app.getPreferences("EzraSave_" + slot);

        float savedX = prefs.getFloat("playerX");
        float savedY = prefs.getFloat("playerY");
        String savedMap = prefs.getString("mapName", "prologueassets/prologuespawn.tmx");

        player.b2body.setTransform(savedX, savedY, 0);
        player.hp = prefs.getFloat("hp");
        player.assimilationMeter = prefs.getFloat("assimilation");

        player.elementSlots[0] = prefs.getString("slot1_element", "NONE");
        player.elementSlots[1] = prefs.getString("slot2_element", "NONE");
        player.chargeSlots[0] = prefs.getInteger("slot1_charges", 0);
        player.chargeSlots[1] = prefs.getInteger("slot2_charges", 0);
        player.activeSlot = prefs.getInteger("activeSlot", 0);

        WorldContactListener.pendingTransition = new io.github.devsimulator.helper.tilemapmanager.TransitionData(
            savedMap, savedX, savedY, false
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
