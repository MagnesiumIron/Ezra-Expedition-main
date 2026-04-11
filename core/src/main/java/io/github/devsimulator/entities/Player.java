package io.github.devsimulator.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
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

    // --- ANIMATION VARIABLES ---
    private Texture idleTexture;
    private TextureRegion idleFrame;
    private Texture playerSheetWalk;
    private Animation<TextureRegion> walkAnimationRight;
    private Texture playerSheet;
    private TextureRegion[] jumpFrames;

    // --- ATTACK ANIMATION VARIABLES ---
    private Texture attackSheet;
    private TextureRegion[] attackFrames;
    public boolean isAttacking = false;
    public int attackCombo = 0;
    private float attackTimer = 0f;
    private final float ATTACK_FRAME_DURATION = 0.06f;

    private boolean facingRight = true;
    private boolean wasGrounded = false;
    private boolean isLanding = false;
    private float landingTimer = 0f;
    private float stateTime = 0f;

    public State currentState = State.NORMAL;
    public float assimilationMeter = 0.0f;
    public final float MAX_ASSIMILATION = 100.0f;
    public boolean isAlive = true;

    public float hp = 100.0f;
    public final float MAX_HP = 100.0f;
    private final float SUFFOCATION_RATE = 6.0f;

    public String[] elementSlots = {"NONE", "NONE"};
    public int[] chargeSlots = {0, 0};
    public int maxCharges = 6;
    public int activeSlot = 0;
    public String currentTransformElement = "NONE";
    public float invincibilityTimer = 0f;
    public float stunTimer = 0f;
    public float chargeProgress = 0f;

    // --- PHYSICS FLAGS ---
    private boolean isGrounded = false;
    private boolean isSubmergedNormal = false; // Drowning (Head covered)
    private boolean isWading = false;          // Wading (Legs covered)
    private boolean isFloatingInElement = false;
    private boolean interactedThisFrame = false;

    private boolean standingOnElement = false;
    private boolean deeplyEmbedded = false;

    private int jumpCounter = 0;
    private final int MAX_JUMPS = 2;

    private final float ASSIMILATION_RATE = 15.0f;
    private final float RECOVERY_RATE = 10.0f;

    private float stepTimer = 0f;
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
            idleTexture = new Texture("player.png");
            idleFrame = new TextureRegion(idleTexture);

            playerSheetWalk = new Texture("player_walk.png");
            TextureRegion[][] tmpWalk = TextureRegion.split(playerSheetWalk, 18, 27);

            Array<TextureRegion> framesWalk = new Array<>();
            for (int i = 0; i < 4; i++) {
                framesWalk.add(tmpWalk[i][0]);
            }
            walkAnimationRight = new Animation<>(0.1f, framesWalk);

            playerSheet = new Texture("player_jump.png");
            TextureRegion[][] tmp = TextureRegion.split(playerSheet, 43, 27);

            jumpFrames = new TextureRegion[7];
            for (int i = 0; i < 7; i++) {
                jumpFrames[i] = tmp[i][0];
            }

            attackSheet = new Texture("player_attack.png");
            TextureRegion[][] tmpAtk = TextureRegion.split(attackSheet, 126, 27);
            attackFrames = new TextureRegion[14];
            for (int i = 0; i < 14; i++) {
                attackFrames[i] = tmpAtk[i][0];
            }

        } catch (Exception e) {
            Gdx.app.error("Player", "Texture missing: " + e.getMessage());
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
        if (!isGrounded || isFloatingInElement) return;
        if (Math.abs(b2body.getLinearVelocity().x) < 0.1f) return;

        stepTimer += dt;
        if (stepTimer >= 0.35f) {
            stepTimer = 0;
            int index = MathUtils.random(0, stoneSteps.length - 1);
            stoneSteps[index].play(0.3f);
        }
    }

    public void update(float dt, SandManager sandMgr) {
        if (!isAlive || sandMgr == null) return;

        if (b2body.getPosition().y * Main.PPM < -50) {
            hp = 0;
            b2body.setLinearVelocity(0, 0);
        }

        // RESET FLAGS AT START OF FRAME
        isSubmergedNormal = false;
        isWading = false;
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

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            triggerMeleeAttack();
        }

        interactWithEnvironment(sandMgr);

        if (isGrounded && !wasGrounded) {
            isLanding = true;
            landingTimer = 0f;
        }
        wasGrounded = isGrounded;

        handleMovement(dt, sandMgr);
        applyVariableGravity();
        updateStats(dt, sandMgr);
        playFootstep(dt);
    }

    public void triggerMeleeAttack() {
        if (!isAttacking) {
            isAttacking = true;
            attackCombo = 1;
            attackTimer = 0f;
        } else if (attackCombo == 1 && attackTimer >= 3 * ATTACK_FRAME_DURATION) {
            attackCombo = 2;
        }
    }

    private void handleMovement(float dt, SandManager sandMgr) {
        if (stunTimer > 0) {
            stunTimer -= dt;
            return;
        }
        Vector2 vel = b2body.getLinearVelocity();
        float targetX = 0;
        float desiredY = vel.y;

        int simX = (int) (b2body.getPosition().x * Main.PPM / CELL_SIZE);
        int simY = (int) (b2body.getPosition().y * Main.PPM / CELL_SIZE);

        boolean blockedLeft = false;
        boolean blockedRight = false;

        for (int y = 0; y <= 3; y++) {
            if (isFirmGround(getSafeElement(sandMgr.sim, simX - 2, simY + y))) blockedLeft = true;
            if (isFirmGround(getSafeElement(sandMgr.sim, simX + 2, simY + y))) blockedRight = true;
        }

        if (isFloatingInElement) {
            jumpBufferTimer = 0;

            if (Gdx.input.isKeyPressed(Input.Keys.A) && !blockedLeft) targetX = -MOVE_SPEED;
            if (Gdx.input.isKeyPressed(Input.Keys.D) && !blockedRight) targetX = MOVE_SPEED;

            if (Gdx.input.isKeyPressed(Input.Keys.W)) {
                desiredY = JUMP_SPEED * 0.5f;
            } else if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
                desiredY = -JUMP_SPEED * 0.5f;
            } else {
                desiredY = -1.0f;
            }

            float desiredX = com.badlogic.gdx.math.MathUtils.lerp(vel.x, targetX, 0.2f);
            b2body.setLinearVelocity(desiredX, desiredY);
        } else {

            // --- NEW: WADING SLOWS YOU DOWN ---
            boolean hindered = isSubmergedNormal || isWading;
            float currentMoveSpeed = hindered ? MOVE_SPEED * 0.3f : MOVE_SPEED;
            float currentJumpSpeed = hindered ? JUMP_SPEED * 0.4f : JUMP_SPEED;

            if (Gdx.input.isKeyPressed(Input.Keys.A) && !blockedLeft) targetX = -currentMoveSpeed;
            if (Gdx.input.isKeyPressed(Input.Keys.D) && !blockedRight) targetX = currentMoveSpeed;

            if (WorldContactListener.footContacts == 0 && desiredY <= 0) {
                if (deeplyEmbedded && currentState == State.NORMAL) {
                    b2body.setTransform(b2body.getPosition().x, b2body.getPosition().y + (CELL_SIZE / Main.PPM), 0);
                    desiredY = 0;
                }
                else if (standingOnElement && currentState == State.NORMAL) {
                    desiredY = 0;
                }
            }

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

    public void takeDamage(float damage, float knockbackDir, SandManager sandMgr) {
        if (invincibilityTimer > 0 || hp <= 0) return;

        hp -= damage;
        invincibilityTimer = 1.0f;

        if (currentState != State.NORMAL) {
            currentState = State.NORMAL;
            currentTransformElement = "NONE";
            if (Main.assimilationOUT != null) Main.assimilationOUT.play(1.2f, 0.7f, 0f);
        }

        if (hp <= 0) {
            hp = 0;
            b2body.setLinearVelocity(0, 0);
        } else {
            b2body.setLinearVelocity(0, b2body.getLinearVelocity().y);
            impulseVec.set(knockbackDir, 4f);
            b2body.applyLinearImpulse(impulseVec, b2body.getWorldCenter(), true);
            stunTimer = 0.3f;
        }
    }

    private void applyVariableGravity() {
        if (isFloatingInElement || ((standingOnElement || deeplyEmbedded) && WorldContactListener.footContacts == 0 && b2body.getLinearVelocity().y <= 0.1f)) {
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
        interactedThisFrame = false;
        return val;
    }

    private boolean isFirmGround(Element e) {
        if (e == null || !e.isSolid || e instanceof EmptyCell) return false;
        if (e instanceof Sand || e instanceof Dirt) return false;
        if (currentState == State.DIRT_FORM && e instanceof Mud) return false;
        return true;
    }

    private void interactWithEnvironment(SandManager sandMgr) {
        PhysicSim sim = sandMgr.sim;
        int simX = (int) (b2body.getPosition().x * Main.PPM / CELL_SIZE);
        int simY = (int) (b2body.getPosition().y * Main.PPM / CELL_SIZE);

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            activeSlot = 0;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            activeSlot = 1;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            if (!elementSlots[activeSlot].equals("NONE")) {
                elementSlots[activeSlot] = "NONE";
                chargeSlots[activeSlot] = 0;
                currentState = State.NORMAL;
                currentTransformElement = "NONE";
            }
        }

        if (b2body.getLinearVelocity().y > 0.5f) {
            WorldContactListener.footContacts = 0;
        }

        boolean standingOnWall = WorldContactListener.footContacts > 0;
        standingOnElement = false;

        if (b2body.getLinearVelocity().y <= 0.1f) {
            int highestFirmGridY = -1;
            for (int xOffset = -1; xOffset <= 1; xOffset++) {
                for (int yOffset = -1; yOffset >= -4; yOffset--) {
                    Element check = getSafeElement(sim, simX + xOffset, simY + yOffset);
                    if (isFirmGround(check)) {
                        highestFirmGridY = Math.max(highestFirmGridY, simY + yOffset);
                    }
                }
            }
            if (highestFirmGridY != -1 && currentState == State.NORMAL) {
                float targetPixelY = (highestFirmGridY * CELL_SIZE) + CELL_SIZE + 9f;
                float targetSurfaceB2DY = targetPixelY / Main.PPM;
                if (b2body.getPosition().y <= targetSurfaceB2DY + 0.05f) {
                    standingOnElement = true;
                    b2body.setTransform(b2body.getPosition().x, targetSurfaceB2DY, 0);
                    b2body.setLinearVelocity(b2body.getLinearVelocity().x, 0);
                    simY = (int) (b2body.getPosition().y * Main.PPM / CELL_SIZE);
                }
            }
        }

        Element feet = getSafeElement(sim, simX, simY - 2);
        boolean touchingSolidPixels = (feet != null && feet.isSolid && !(feet instanceof EmptyCell));

        float currentY = b2body.getPosition().y;
        if(Math.abs(currentY - lastYpos) < 0.005f) {
            verticalRestFrames++;
        } else {
            verticalRestFrames = 0;
        }
        lastYpos = currentY;
        boolean isTrulyResting = verticalRestFrames >= 2;
        boolean notRising = b2body.getLinearVelocity().y <= 0.1f;

        if (currentState == State.DIRT_FORM || currentState == State.LIQUID_FORM) {
            isGrounded = standingOnWall && isTrulyResting && notRising;
        } else {
            isGrounded = (standingOnWall || standingOnElement || touchingSolidPixels) && isTrulyResting && notRising;
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

        // --- NEW: DUAL TIER ELEMENT DETECTION ---
        // We check the legs (y-1) for wading, and the head (y+1) for drowning!
        Element head = getSafeElement(sim, simX, simY + 1);
        Element legs = getSafeElement(sim, simX, simY - 1);

        if (legs != null && !(legs instanceof EmptyCell)) {
            applyElementEffects(legs, sandMgr, false); // false = checking legs
        }
        if (head != null && !(head instanceof EmptyCell)) {
            applyElementEffects(head, sandMgr, true);  // true = checking head
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
        int radius = 12 / CELL_SIZE;

        String myElement = currentTransformElement.toUpperCase();
        if (myElement.equals("NONE")) return;

        boolean reactedThisFrame = false;

        for (int y = -radius - 4; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                if (x*x + y*y <= (radius+4)*(radius+4)) {
                    int px = gridX + x;
                    int py = gridY + y;
                    Element e = getSafeElement(sim, px, py);

                    if (e != null && !(e instanceof EmptyCell)) {
                        String targetElement = e.getClass().getSimpleName().toUpperCase();

                        String key = myElement.compareTo(targetElement) < 0 ?
                            myElement + "_" + targetElement :
                            targetElement + "_" + myElement;

                        ElementType resultType = sim.getAlchemyRecipe(key);
                        if (resultType != null) {
                            sim.setElement(px, py, resultType.create(px, py));
                            e.freeToPool();
                            reactedThisFrame = true;

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
        if (reactedThisFrame) {
            assimilationMeter += 15.0f * Gdx.graphics.getDeltaTime();
        }
    }

    // --- NEW: AFFECTS BY BODY PART ---
    private void applyElementEffects(Element e, SandManager sandMgr, boolean isHead) {
        if (currentState == State.DIRT_FORM && (e instanceof Sand || e instanceof Dirt || e instanceof Mud)) {
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
        } else if (e instanceof Lava) {
            float knockbackDir = (b2body.getLinearVelocity().x > 0) ? -5f : 5f;
            takeDamage(25f, knockbackDir, sandMgr);
        } else if (!e.isSolid || e instanceof Sand || e instanceof Dirt || e instanceof Mud) {
            if (isHead) {
                isSubmergedNormal = true; // Drowning
            } else {
                isWading = true;          // Walking through deep sand
            }
        }
    }

    private void updateStats(float dt, SandManager sandMgr) {
        boolean isOverloaded = false;
        if (currentState != State.NORMAL) {
            assimilationMeter = Math.min(MAX_ASSIMILATION, assimilationMeter + (ASSIMILATION_RATE * dt));
            if (assimilationMeter >= MAX_ASSIMILATION) {
                isOverloaded = true;
                hp -= (SUFFOCATION_RATE * 0.5f) * dt;
                if (hp <= 0) {
                    hp = 0;
                    b2body.setLinearVelocity(0, 0);
                }
            }
        } else {
            assimilationMeter = Math.max(0, assimilationMeter - (RECOVERY_RATE * dt));
        }

        // --- DROWNING ONLY DRAINS HP ---
        if (isSubmergedNormal) {
            hp -= SUFFOCATION_RATE * dt;
            if (hp <= 0) {
                hp = 0;
                b2body.setLinearVelocity(0, 0);
            }
        } else if (currentState == State.NORMAL && !isOverloaded && invincibilityTimer <= 0) {
            hp = Math.min(MAX_HP, hp + (RECOVERY_RATE * 0.5f * dt));
        }
    }

    public void executeShoot(float targetX, float targetY, com.badlogic.gdx.utils.Array<SandProjectile> projList, World world, boolean isMega) {
        if (currentState != State.NORMAL || elementSlots[activeSlot].equals("NONE")) return;

        String el = elementSlots[activeSlot];
        int cost = isMega ? 1 : 0;

        if (chargeSlots[activeSlot] < cost) {
            isMega = false;
            cost = 0;
        }

        chargeSlots[activeSlot] -= cost;

        float px = b2body.getPosition().x;
        float py = b2body.getPosition().y;

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
            projList.add(new SandProjectile(world, px, py, new Vector2(targetX, targetY), el, isMega));
        }

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

        // FIX: Always start pushing from y=0 (Waist level).
        // This ensures the sand at y=-1 (Legs) is never displaced,
        // which allows the Wading slowdown to actually take effect!
        for (int y = 0; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                if (x*x + y*y < radius*radius) {
                    int px = gridX + x;
                    int py = gridY + y;
                    Element e = getSafeElement(sim, px, py);

                    if (e != null && !e.isStatic) {
                        if (isFirmGround(e)) continue;

                        if (y <= 0) {
                            if (x < 0 && getSafeElement(sim, px-1, py) instanceof EmptyCell) sim.moveElement(px, py, px-1, py);
                            else if (x > 0 && getSafeElement(sim, px+1, py) instanceof EmptyCell) sim.moveElement(px, py, px+1, py);
                            else if (getSafeElement(sim, px, py+1) instanceof EmptyCell) sim.moveElement(px, py, px, py+1);
                        } else {
                            if (getSafeElement(sim, px, py+1) instanceof EmptyCell) sim.moveElement(px, py, px, py+1);
                            else if (x < 0 && getSafeElement(sim, px-1, py) instanceof EmptyCell) sim.moveElement(px, py, px-1, py);
                            else if (x > 0 && getSafeElement(sim, px+1, py) instanceof EmptyCell) sim.moveElement(px, py, px+1, py);
                        }
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

    private Element getSafeElement(PhysicSim sim, int x, int y) {
        if (sim == null) return null;
        try { return sim.getElement(x, y); } catch (Exception e) { return null; }
    }

    public float getMassPercentage() {
        return assimilationMeter / MAX_ASSIMILATION;
    }

    public TextureRegion getFrame(float dt) {
        TextureRegion region;
        Vector2 vel = b2body.getLinearVelocity();

        if (isAttacking && attackFrames != null) {
            attackTimer += dt;
            int fIndex = (int)(attackTimer / ATTACK_FRAME_DURATION);

            if (attackCombo == 1) {
                if (fIndex > 6) {
                    isAttacking = false;
                    attackCombo = 0;
                    region = idleFrame;
                } else {
                    region = attackFrames[fIndex];
                }
            } else if (attackCombo == 2) {
                if (fIndex > 13) {
                    isAttacking = false;
                    attackCombo = 0;
                    region = idleFrame;
                } else {
                    region = attackFrames[Math.min(fIndex, 13)];
                }
            } else {
                region = idleFrame;
            }
        }
        else if (!isGrounded) {
            if (vel.y > 0) {
                region = jumpFrames[0];
            } else {
                region = jumpFrames[4];
            }
        } else if (isLanding) {
            landingTimer += dt;
            if (landingTimer < 0.1f) {
                region = jumpFrames[5];
            } else if (landingTimer < 0.2f) {
                region = jumpFrames[6];
            } else {
                isLanding = false;
                region = idleFrame;
            }
        } else {
            if (Math.abs(vel.x) > 0.1f) {
                stateTime += dt;
                region = walkAnimationRight.getKeyFrame(stateTime, true);
            } else {
                stateTime = 0;
                region = idleFrame;
            }
        }

        if (!isAttacking) {
            if (vel.x > 0.1f && !facingRight) facingRight = true;
            else if (vel.x < -0.1f && facingRight) facingRight = false;
        }

        if (!facingRight && !region.isFlipX()) region.flip(true, false);
        else if (facingRight && region.isFlipX()) region.flip(true, false);

        return region;
    }

    public void draw(SpriteBatch batch) {
        if (idleFrame == null) return;

        TextureRegion currentFrame = getFrame(Gdx.graphics.getDeltaTime());
        float offsetX = 0;
        float offsetY = 0;

        if (chargeProgress > 0 && chargeSlots[activeSlot] >= 1) {
            offsetX = (MathUtils.random() - 0.5f) * (chargeProgress * 4f);
            offsetY = (MathUtils.random() - 0.5f) * (chargeProgress * 4f);
            if (chargeProgress >= 1.0f && (chargeProgress % 0.2f < 0.1f)) {
                batch.setColor(1.0f, 1.0f, 0.4f, 1f);
            }
        }

        if (invincibilityTimer > 0 && (invincibilityTimer % 0.2f < 0.1f)) {
            batch.setColor(1.0f, 0.2f, 0.2f, 0.6f);
        } else if (chargeProgress < 1.0f) {
            switch (currentState) {
                case DIRT_FORM: batch.setColor(0.6f, 0.4f, 0.2f, 1f); break;
                case LIQUID_FORM: batch.setColor(0.2f, 0.2f, 0.8f, 1f); break;
                case LAVA_FORM: batch.setColor(1.0f, 0.4f, 0.0f, 1f); break;
                default: batch.setColor(1, 1, 1, 1); break;
            }
        }

        float width = currentFrame.getRegionWidth();
        float height = currentFrame.getRegionHeight();

        float drawX = (b2body.getPosition().x * Main.PPM) - width / 2f + offsetX;
        float drawY = (b2body.getPosition().y * Main.PPM) - height / 2f + offsetY;

        if (isAttacking) {
            float ATTACK_SHIFT_X = 35f;

            if (facingRight) {
                drawX += ATTACK_SHIFT_X;
            } else {
                drawX -= ATTACK_SHIFT_X;
            }
        }

        batch.draw(currentFrame, drawX, drawY);

        batch.setColor(1, 1, 1, 1);
    }

    public void dispose() {
        if(idleTexture != null) idleTexture.dispose();
        if(playerSheetWalk != null) playerSheetWalk.dispose();
        if(playerSheet != null) playerSheet.dispose();
        if(attackSheet != null) attackSheet.dispose();

        if (stoneSteps != null) {
            for (Sound s : stoneSteps) s.dispose();
        }
    }
}
