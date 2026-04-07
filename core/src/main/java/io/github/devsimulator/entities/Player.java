package io.github.devsimulator.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import io.github.devsimulator.Main;
import io.github.devsimulator.controllers.SandManager;
import io.github.devsimulator.elements.*;
import io.github.devsimulator.helper.PhysicSim;
import io.github.devsimulator.helper.WorldContactListener;
import io.github.devsimulator.helper.tilemapmanager;

public class Player {

    private static final int CELL_SIZE = 4;
    private static final int RADIUS_OFFSET = 10 / CELL_SIZE;

    private static final float MOVE_SPEED = 6.0f;
    private static final float JUMP_SPEED = 9.0f;
    private static final float GRAVITY_NORMAL = 3.0f;
    private static final float GRAVITY_FALL = 5.0f;
    private static final float TERMINAL_VELOCITY = -12.0f;

    private final float coyoteTime = 0.1f;
    private final float jumpBuffer = 0.1f;
    private float coyoteTimer = 0;
    private float jumpBufferTimer = 0;

    public enum State { NORMAL, DIRT_FORM, LIQUID_FORM, GAS_FORM, LAVA_FORM }

    public Body b2body;
    private Texture texture;

    public State currentState = State.NORMAL;
    public float assimilationMeter = 0.0f;
    public final float MAX_ASSIMILATION = 100.0f;
    public boolean isAlive = true;

    public float hp = 100.0f;
    public final float MAX_HP = 100.0f;
    private final float SUFFOCATION_RATE = 6.0f;

    /* --- RUNE SYSTEM VARIABLES ---
    public int assimilationCharges = 0;
    public String storedElement = "NONE";*/

    //NEW RUNE SYSTEM: Can have Two Slots
    public String[] elementSlots = {"NONE", "NONE"};
    public int[] chargeSlots = {0, 0};
    public int maxCharges = 6;
    public int activeSlot = 0; // 0 = Slot 1, 1 = Slot 2 (0-indexing concept)
    public String currentTransformElement = "NONE";

    //COMBAT AND HAZARD Vars
    public float invincibilityTimer = 0f;
    public float stunTimer = 0f;
    public float chargeProgress = 0f;

    private boolean isGrounded = false;
    private boolean isSubmergedNormal = false;
    private boolean isFloatingInElement = false;
    private boolean interactedThisFrame = false;

    private int jumpCounter = 0;
    private final int MAX_JUMPS = 2;

    private final float ASSIMILATION_RATE = 15.0f;
    private final float RECOVERY_RATE = 10.0f;

    private float stepTimer = 0f;
    //public String currentSurface = "default";
    private float lastYpos = 0f;
    private int verticalRestFrames = 0;
    private boolean hasUsedElementBoost = false;

    //checkpoint
    private float chkHp;
    private float chkAssimilation;
    private String[] chkElementSlots = {"NONE", "NONE"};
    private int[] chkChargeSlots = {0, 0};
    private int chkActiveSlot;
    public float chkSpawnX;
    public float chkSpawnY;
    private final Vector2 impulseVec = new Vector2();

    public Sound[] stoneSteps;

    public Player(World world) {
        definePlayer(world);
        try {
            texture = new Texture("player.png");
        } catch (Exception e) {
            Gdx.app.error("Player", "Texture missing");
        }

        stoneSteps = new Sound[] {
            Gdx.audio.newSound(Gdx.files.internal("sounds/stone_step1.wav")),
            Gdx.audio.newSound(Gdx.files.internal("sounds/stone_step2.wav")),
            Gdx.audio.newSound(Gdx.files.internal("sounds/stone_step3.wav"))
        };
    }

    private void definePlayer(World world) {
        BodyDef bdef = new BodyDef();
        bdef.position.set(100 / Main.PPM, 200 / Main.PPM);
        bdef.type = BodyDef.BodyType.DynamicBody;
        bdef.fixedRotation = true;
        b2body = world.createBody(bdef);

        FixtureDef fdef = new FixtureDef();
        CircleShape shape = new CircleShape();
        shape.setRadius(9 / Main.PPM);
        fdef.shape = shape;
        fdef.friction = 0.0f;
        b2body.createFixture(fdef).setUserData("PLAYER");
        shape.dispose();

        PolygonShape footShape = new PolygonShape();
        footShape.setAsBox(4 / Main.PPM, 1 / Main.PPM, new Vector2(0, -9 / Main.PPM), 0);

        FixtureDef footDef = new FixtureDef();
        footDef.shape = footShape;
        footDef.isSensor = true;
        b2body.createFixture(footDef).setUserData("FOOT_SENSOR");
        footShape.dispose();

        b2body.setGravityScale(GRAVITY_NORMAL);
    }
    private void playFootstep(float dt) {
        // no sound if it's within the element zone
        if (!isGrounded || isFloatingInElement) return;
        // will only play if the player is moving horizontally
        if (Math.abs(b2body.getLinearVelocity().x) < 0.1f) return;

        stepTimer += dt;
        if (stepTimer >= 0.35f) { //timing between the steps
            stepTimer = 0;
            // Play a random stone step sound
            int index = MathUtils.random(0, stoneSteps.length - 1);
            stoneSteps[index].play(0.3f);
        }
    }

    public void update(float dt, SandManager sandMgr) {
        if (!isAlive || sandMgr == null) return;

        isSubmergedNormal = false;
        isFloatingInElement = false;

        if (invincibilityTimer > 0) invincibilityTimer -= dt;

        if (isGrounded) coyoteTimer = coyoteTime;
        else coyoteTimer -= dt;

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
            Gdx.input.isKeyJustPressed(Input.Keys.W) ||
            Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            jumpBufferTimer = jumpBuffer;
        } else {
            jumpBufferTimer -= dt;
        }

        interactWithEnvironment(sandMgr);
        handleMovement(dt);
        applyVariableGravity();
        updateStats(dt, sandMgr);
        playFootstep(dt);
    }

    private void handleMovement(float dt) {
        if (stunTimer > 0) { //added a stun lock so if player is hit by a hazard it'll prevent their movement
            stunTimer -= dt;
            return;
        }
        Vector2 vel = b2body.getLinearVelocity();
        float targetX = 0;
        float desiredY = vel.y;

        if (isFloatingInElement) {
            // Horizontal Movement
            if (Gdx.input.isKeyPressed(Input.Keys.A)) targetX = -MOVE_SPEED;
            if (Gdx.input.isKeyPressed(Input.Keys.D)) targetX = MOVE_SPEED;

            if (Gdx.input.isKeyPressed(Input.Keys.W) /*|| Gdx.input.isKeyPressed(Input.Keys.UP)*/) {
                desiredY = JUMP_SPEED * 0.5f;
            } else if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
                desiredY = -JUMP_SPEED * 0.5f;
            } else {
                desiredY = -1.0f;
            }

            float desiredX = com.badlogic.gdx.math.MathUtils.lerp(vel.x, targetX, 0.2f);
            b2body.setLinearVelocity(desiredX, desiredY);
        } else {
            float currentMoveSpeed = isSubmergedNormal ? MOVE_SPEED * 0.3f : MOVE_SPEED;
            float currentJumpSpeed = isSubmergedNormal ? JUMP_SPEED * 0.4f : JUMP_SPEED;

            if (Gdx.input.isKeyPressed(Input.Keys.A)) targetX = -currentMoveSpeed;
            if (Gdx.input.isKeyPressed(Input.Keys.D)) targetX = currentMoveSpeed;
            //float lerpFactor = isGrounded ? 0.12f : 0.05f;
            float lerpFactor = isGrounded ? 0.30f : 0.05f;
            if (targetX == 0 && isGrounded) {
                lerpFactor = 0.60f;
            }

            float desiredX = com.badlogic.gdx.math.MathUtils.lerp(vel.x, targetX, lerpFactor);

            if (jumpBufferTimer > 0 && coyoteTimer > 0) {
                desiredY = currentJumpSpeed;
                jumpBufferTimer = 0;
                coyoteTimer = 0;
                jumpCounter = 1;
                verticalRestFrames = 0;
                if (Main.jumpSound != null) Main.jumpSound.play(0.9f + (float)Math.random() * 0.2f, 0.9f + (float)Math.random() * 0.3f, 0f);
            } else if (jumpBufferTimer > 0 && jumpCounter < MAX_JUMPS && coyoteTimer <= 0) {
                desiredY = currentJumpSpeed;
                jumpBufferTimer = 0;
                jumpCounter++;
                if (Main.jumpSound != null) Main.jumpSound.play(0.9f + (float)Math.random() * 0.2f, 0.9f + (float)Math.random() * 0.3f, 0f);
            }
            b2body.setLinearVelocity(desiredX, desiredY);
        }
    }
        //NEW COMBAT SYSTEM
    public void takeDamage(float amount, float knockbackDirX, SandManager sandMgr) {
        if (invincibilityTimer > 0) return; // Immune to damage while flashing

        hp -= amount;
        invincibilityTimer = 1.5f; // 1.5 seconds of invincibility
        stunTimer = 0.3f; // 0.3 seconds of losing keyboard control

        // will go back to previous form if he takes damage
        if (currentState != State.NORMAL) {
            currentState = State.NORMAL;
            currentTransformElement = "NONE";
            if (Main.assimilationOUT != null) Main.assimilationOUT.play(1.2f, 0.7f, 0f); // Play high pitch error
        }

        if (hp <= 0) {
            triggerDeath("Hazard", sandMgr);
        } else {
            // Apply physical knockback bounce
            b2body.setLinearVelocity(knockbackDirX, JUMP_SPEED * 0.7f);
        }
    }

    private void applyVariableGravity() {
        if (isFloatingInElement) {
            b2body.setGravityScale(0);
            return;
        }
        Vector2 vel = b2body.getLinearVelocity();
        boolean holdingJump = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.SPACE) || Gdx.input.isKeyPressed(Input.Keys.UP);

        if (vel.y > 0 && !holdingJump) b2body.setGravityScale(GRAVITY_FALL * 2.5f);
        else if (vel.y < 0) b2body.setGravityScale(GRAVITY_FALL * 1.5f);
        else b2body.setGravityScale(GRAVITY_NORMAL);

        if (vel.y < TERMINAL_VELOCITY) b2body.setLinearVelocity(vel.x, TERMINAL_VELOCITY);
    }

    public boolean hasJustInteractedWithSign() {
        boolean val = interactedThisFrame;
        interactedThisFrame = false; // Reset it immediately after Main reads it
        return val;
    }

    private void interactWithEnvironment(SandManager sandMgr) {
        PhysicSim sim = sandMgr.sim;
        int simX = (int) (b2body.getPosition().x * Main.PPM / CELL_SIZE);
        int simY = (int) (b2body.getPosition().y * Main.PPM / CELL_SIZE);

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            activeSlot = 0;
            Gdx.app.log("Player", "Switched to Slot 1: " + elementSlots[activeSlot]);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            activeSlot = 1;
            Gdx.app.log("Player", "Switched to Slot 2: " + elementSlots[activeSlot]);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            if (!elementSlots[activeSlot].equals("NONE")) {
                Gdx.app.log("Player", "Dropped " + elementSlots[activeSlot] + " from Slot " + (activeSlot + 1));
                elementSlots[activeSlot] = "NONE";
                chargeSlots[activeSlot] = 0;
                currentState = State.NORMAL;
                currentTransformElement = "NONE";
            }
        }

        boolean standingOnWall = WorldContactListener.footContacts > 0;
        Element feet = getSafeElement(sim, simX, simY - RADIUS_OFFSET);
        boolean standingOnElement = (feet != null && !(feet instanceof EmptyCell));

        float currentY = b2body.getPosition().y;
        if(Math.abs(currentY - lastYpos) < 0.005f) {
            verticalRestFrames++;
        } else {
            verticalRestFrames = 0;
        }
        lastYpos = currentY;

        boolean isTrulyResting = verticalRestFrames >= 2;

        if (currentState == State.DIRT_FORM || currentState == State.LIQUID_FORM) {
            isGrounded = standingOnWall && isTrulyResting;
        } else {
            isGrounded = (standingOnWall || standingOnElement) && isTrulyResting;
        }

        if (isGrounded) {
            hasUsedElementBoost = false;
            jumpCounter = 0;
        }

        if (currentState == State.NORMAL) {
            displaceTerrain(sim);
        } else {
            triggerPlayerAlchemy(sim);
        }

        Element center = getSafeElement(sim, simX, simY);
        if (center != null && !(center instanceof EmptyCell)) {
            applyElementEffects(center, sandMgr);
        }

        Vector2 pixelPos = new Vector2(b2body.getPosition().x * Main.PPM, b2body.getPosition().y * Main.PPM);
        WorldContactListener.sortSignsByDistance(pixelPos);
        WorldContactListener.sortRunesByDistance(pixelPos);

        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            if (WorldContactListener.closestSign != null) {
                float dist = pixelPos.dst(
                    WorldContactListener.closestSign.worldX * Main.PPM,
                    WorldContactListener.closestSign.worldY * Main.PPM
                );
                if (dist < 64f) {
                    interactedThisFrame = true;
                    return;
                }
            }
        }

        // RUNES AND TRANSFORMATION INTERACTION
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            tilemapmanager.RuneData rune = WorldContactListener.closestRune;
            float dist = -1;
            if (rune != null) {
                dist = pixelPos.dst(rune.worldX * Main.PPM, rune.worldY * Main.PPM);
            }

            if (rune != null && dist < 64f && !rune.isContainer && !rune.isConsumed) {
                String type = rune.elementType != null ? rune.elementType.toUpperCase() : "NONE";

                if (rune.isSpawner) {
                    if (!sandMgr.completedGeysers.contains(rune.runeID, false) && currentState != State.NORMAL) {
                        Gdx.app.log("Player", "This geyser is exhausted.");
                        boolean match = (currentState == State.DIRT_FORM && (type.equals("DIRT") || type.equals("SAND"))) ||
                            (currentState == State.LIQUID_FORM && type.equals("WATER")) ||
                            (currentState == State.LAVA_FORM && type.equals("LAVA"));

                        if (match) {
                            sandMgr.toggleSpawner(rune);
                        }
                    }
                } else if (currentState == State.NORMAL) {
                    if (!type.equals("NONE")) {

                        int existingSlot = -1;
                        if (elementSlots[0].equals(type)) existingSlot = 0;
                        else if (elementSlots[1].equals(type)) existingSlot = 1;

                        if (existingSlot != -1) {
                            chargeSlots[existingSlot] += 3;
                            activeSlot = existingSlot;
                            rune.isConsumed = true;
                        } else {
                            int emptySlot = -1;
                            if (elementSlots[activeSlot].equals("NONE")) emptySlot = activeSlot;
                            else if (activeSlot == 0 && elementSlots[1].equals("NONE")) emptySlot = 1;
                            else if (activeSlot == 1 && elementSlots[0].equals("NONE")) emptySlot = 0;

                            if (emptySlot != -1) {
                                chargeSlots[emptySlot] = 3;
                                elementSlots[emptySlot] = type;
                                activeSlot = emptySlot;
                                rune.isConsumed = true;
                            }
                        }
                    }
                }
            }
        }

        // LEFT SHIFT KEY to USE ELEMENTAL ABILITY
        if (Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT)) {
            if (chargeSlots[activeSlot] > 0 && currentState == State.NORMAL) {
                chargeSlots[activeSlot]--;

                if (Main.assimilationIN != null) {
                    Main.assimilationIN.play(0.8f);
                }
                String currentActiveElement = elementSlots[activeSlot];
                currentTransformElement = currentActiveElement;

                switch (currentActiveElement) {
                    case "DIRT", "SAND" -> currentState = State.DIRT_FORM;
                    case "WATER" -> currentState = State.LIQUID_FORM;
                    case "LAVA" -> currentState = State.LAVA_FORM;
                }
            } else if (currentState != State.NORMAL) {
                if (isGrounded || isFloatingInElement || hasUsedElementBoost) {
                    currentState = State.NORMAL;
                    currentTransformElement = "NONE";
                    if (Main.assimilationOUT != null) Main.assimilationOUT.play(0.8f);
                    if (chargeSlots[activeSlot] <= 0) elementSlots[activeSlot] = "NONE";
                } else {
                    Vector2 vel = b2body.getLinearVelocity();
                    b2body.setLinearVelocity(vel.x, JUMP_SPEED * 1.6f);
                    hasUsedElementBoost = true;

                    if (Main.jumpSound != null) Main.jumpSound.play(1f, 1.3f, 0f);
                    if (Main.assimilationOUT != null) Main.assimilationOUT.play(0.8f);
                    currentState = State.NORMAL;
                    currentTransformElement = "NONE";
                    if (chargeSlots[activeSlot] <= 0) elementSlots[activeSlot] = "NONE";
                }
            }
        }
    }

    private void triggerPlayerAlchemy(PhysicSim sim) {
        float worldX = b2body.getPosition().x * Main.PPM;
        int gridX = (int) (worldX / CELL_SIZE);
        int gridY = (int) (b2body.getPosition().y * Main.PPM / CELL_SIZE);
        int radius = 12 / CELL_SIZE; // player size

        String myElement = currentTransformElement;
        if (myElement.equals("NONE")) return;

        boolean reactedThisFrame = false;

        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                if (x*x + y*y <= radius*radius) {
                    int px = gridX + x;
                    int py = gridY + y;
                    Element e = getSafeElement(sim, px, py);

                    if (e != null && !(e instanceof EmptyCell)) {
                        String targetElement = e.getClass().getSimpleName().toUpperCase();

                        // Alphabetize the combination to check the dictionary
                        String key = myElement.compareTo(targetElement) < 0 ?
                            myElement + "_" + targetElement :
                            targetElement + "_" + myElement;

                        ElementType resultType = sim.getAlchemyRecipe(key);
                        if (resultType != null) {
                            // Convert the terrain pixel if there is a chemical reaction
                            sim.setElement(px, py, resultType.create(px, py));
                            e.freeToPool();
                            reactedThisFrame = true;

                            // Spawn some steam/smoke off the reaction
                            if (MathUtils.random(100) < 5) {
                                Smoke smoke = SandManager.smokePool.obtain();
                                smoke.init(px, py + 1);
                                sim.setElement(px, py + 1, smoke);
                            }
                        }
                    }
                }
            }
        }
        // having a chemical reaction with the environment drains the player's energy quickly!
        if (reactedThisFrame) {
            assimilationMeter += 15.0f * Gdx.graphics.getDeltaTime();
        }
    }

    private void applyElementEffects(Element e, SandManager sandMgr) {
        // Reset flags
        isSubmergedNormal = false;
        isFloatingInElement = false;

        if (currentState == State.DIRT_FORM && (e instanceof Sand || e instanceof Dirt)) {
            isFloatingInElement = true;
            Vector2 vel = b2body.getLinearVelocity();
            if (Math.abs(vel.x) > 0.1f) {
                impulseVec.set(vel.x * 0.01f, 0);
                b2body.applyLinearImpulse(impulseVec, b2body.getWorldCenter(), true);
            }
        } else if (currentState == State.LIQUID_FORM && e instanceof Water) {
            isFloatingInElement = true;
        } else if (currentState == State.LAVA_FORM && e instanceof Lava) {
            isFloatingInElement = true;
        } else if (e instanceof Lava) { //HAZARD DETECTION
            // If the player touches Lava and is NOT in their Lava Form, they'll get burned and knocked back
            float knockbackDir = (b2body.getLinearVelocity().x > 0) ? -5f : 5f;
            takeDamage(25f, knockbackDir, sandMgr);
        } else {
            isSubmergedNormal = true;
        }
    }

    private void updateStats(float dt, SandManager sandMgr) {
        boolean isOverloaded = false;
        if (currentState != State.NORMAL) {
            assimilationMeter = Math.min(MAX_ASSIMILATION, assimilationMeter + (ASSIMILATION_RATE * dt));
            if (assimilationMeter >= MAX_ASSIMILATION) {
                isOverloaded = true;
                hp -= (SUFFOCATION_RATE * 0.5f) * dt;
                if (hp <= 0) triggerDeath("Overload", sandMgr);
            }
        } else {
            assimilationMeter = Math.max(0, assimilationMeter - (RECOVERY_RATE * dt));
        }

        if (isSubmergedNormal) {
            hp -= SUFFOCATION_RATE * dt;
            if (hp <= 0) triggerDeath("Suffocated / Drowned", sandMgr);
        } else if (currentState == State.NORMAL && !isOverloaded && invincibilityTimer <= 0) {
            hp = Math.min(MAX_HP, hp + (RECOVERY_RATE * 0.5f * dt));
        }
    }

    public void executeShoot(float targetX, float targetY, com.badlogic.gdx.utils.Array<SandProjectile> projList, World world, boolean isMega) {
        if (currentState != State.NORMAL || chargeSlots[activeSlot] <= 0 || elementSlots[activeSlot].equals("NONE")) return;

        String el = elementSlots[activeSlot];
        int cost = isMega ? 3 : 1;
        if (chargeSlots[activeSlot] < cost) { isMega = false; cost = 1; } // for failsafe

        chargeSlots[activeSlot] -= cost;
        if (chargeSlots[activeSlot] <= 0) elementSlots[activeSlot] = "NONE";

        float px = b2body.getPosition().x;
        float py = b2body.getPosition().y;

        // sand blast
        if ((el.equals("DIRT") || el.equals("SAND")) && !isMega) {
            for(int i = -1; i <= 1; i++) {
                float angleOffset = i * 0.2f;
                float dx = targetX - px; float dy = targetY - py;
                float dist = (float)Math.sqrt(dx*dx + dy*dy);
                float angle = (float)Math.atan2(dy, dx) + angleOffset;

                Vector2 spreadTarget = new Vector2(px + (float)Math.cos(angle)*dist, py + (float)Math.sin(angle)*dist);
                projList.add(new SandProjectile(world, px, py, spreadTarget, el, false));
            }
        } else {
            // 2 shots: standard or mega (feature shot)
            projList.add(new SandProjectile(world, px, py, new Vector2(targetX, targetY), el, isMega));
        }

        // recoil knockback to the player
        float selfKnock = isMega ? -6f : -2f;
        float dirX = (targetX > px) ? 1 : -1;
        impulseVec.set(dirX * selfKnock, 0);
        b2body.applyLinearImpulse(impulseVec, b2body.getWorldCenter(), true);

        if (Main.jumpSound != null) Main.jumpSound.play(isMega ? 0.6f : 1.2f, isMega ? 0.5f : 2.0f, 0f);
    }

    private void displaceTerrain(PhysicSim sim) {
        float worldX = b2body.getPosition().x * Main.PPM;
        int gridX = (int) (worldX / CELL_SIZE);
        int gridY = (int) (b2body.getPosition().y * Main.PPM / CELL_SIZE);
        int radius = 10 / CELL_SIZE;

        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                if (x*x + y*y < radius*radius) {
                    int px = gridX + x;
                    int py = gridY + y;
                    Element e = getSafeElement(sim, px, py);
                    if (e != null && !e.isStatic) {
                        if (getSafeElement(sim, px, py + 1) instanceof EmptyCell) sim.moveElement(px, py, px, py + 1);
                        else if (getSafeElement(sim, px+1, py) instanceof EmptyCell) sim.moveElement(px, py, px+1, py);
                        else if (getSafeElement(sim, px-1, py) instanceof EmptyCell) sim.moveElement(px, py, px-1, py);
                    }
                }
            }
        }
    }

    public void saveRoomCheckpoint(float spawnX, float spawnY) {
        this.chkHp = this.hp;
        this.chkAssimilation = this.assimilationMeter;
        this.chkElementSlots[0] = this.elementSlots[0];
        this.chkElementSlots[1] = this.elementSlots[1];
        this.chkChargeSlots[0] = this.chargeSlots[0];
        this.chkChargeSlots[1] = this.chargeSlots[1];
        this.chkActiveSlot = this.activeSlot;
        this.chkSpawnX = spawnX;
        this.chkSpawnY = spawnY;
    }

    public void loadRoomCheckpoint() {
        this.hp = this.chkHp;
        this.assimilationMeter = this.chkAssimilation;
        this.elementSlots[0] = this.chkElementSlots[0];
        this.elementSlots[1] = this.chkElementSlots[1];
        this.chargeSlots[0] = this.chkChargeSlots[0];
        this.chargeSlots[1] = this.chkChargeSlots[1];
        this.activeSlot = this.chkActiveSlot;
        this.currentState = State.NORMAL;
        this.currentTransformElement = "NONE";
    }

    public void triggerDeath(String reason, SandManager sandMgr) {
        if (sandMgr != null && sandMgr.sim != null) {
            int gridX = (int) (b2body.getPosition().x * Main.PPM / CELL_SIZE);
            int gridY = (int) (b2body.getPosition().y * Main.PPM / CELL_SIZE);

            for(int i = -2; i <= 2; i++) {
                for(int j = -2; j <= 2; j++) {
                    if (sandMgr.sim.isEmpty(gridX + i, gridY + j)) {
                        Smoke smoke = SandManager.smokePool.obtain();
                        smoke.init(gridX + i, gridY + j);
                        sandMgr.sim.setElement(gridX + i, gridY + j, smoke);
                    }
                }
            }
        }

        isAlive = false;
        io.github.devsimulator.helper.WorldContactListener.pendingFastReload = true;
        isAlive = true;
    }

    private Element getSafeElement(PhysicSim sim, int x, int y) {
        if (sim == null) return null;
        try { return sim.getElement(x, y); } catch (Exception e) { return null; }
    }

    public float getMassPercentage() {
        return assimilationMeter / MAX_ASSIMILATION;
    }

    public void draw(SpriteBatch batch) {
        if (texture == null) return;
        float offsetX = 0;
        float offsetY = 0;

        if (chargeProgress > 0 && chargeSlots[activeSlot] >= 3) {
            offsetX = (MathUtils.random() - 0.5f) * (chargeProgress * 4f);
            offsetY = (MathUtils.random() - 0.5f) * (chargeProgress * 4f);
            if (chargeProgress >= 1.0f && (chargeProgress % 0.2f < 0.1f)) {
                batch.setColor(1.0f, 1.0f, 0.4f, 1f);
            }
        }

        if (invincibilityTimer > 0 && (invincibilityTimer % 0.2f < 0.1f)) {
            batch.setColor(1.0f, 0.2f, 0.2f, 0.6f);
        } else {
            switch (currentState) {
                case DIRT_FORM: batch.setColor(0.6f, 0.4f, 0.2f, 1f); break;
                case LIQUID_FORM: batch.setColor(0.2f, 0.2f, 0.8f, 1f); break;
                case LAVA_FORM: batch.setColor(1.0f, 0.4f, 0.0f, 1f); break;
                default: batch.setColor(1, 1, 1, 1); break;
            }
        }
        batch.draw(texture,
            (b2body.getPosition().x * Main.PPM - texture.getWidth() / 2f) + offsetX,
            (b2body.getPosition().y * Main.PPM - texture.getHeight() / 2f) + offsetY);
        batch.setColor(1, 1, 1, 1);
    }
}
