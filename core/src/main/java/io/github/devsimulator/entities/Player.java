package io.github.devsimulator.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import io.github.devsimulator.Main;
import io.github.devsimulator.controllers.SandManager;
import io.github.devsimulator.elements.*;
import io.github.devsimulator.helper.PhysicSim;
import io.github.devsimulator.helper.WorldContactListener;
import io.github.devsimulator.helper.tilemapmanager;
import io.github.devsimulator.helper.tilemapmanager.RuneData;

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

    public enum State { NORMAL, DIRT_FORM, LIQUID_FORM, GAS_FORM }

    public Body b2body;
    private Texture texture;

    public State currentState = State.NORMAL;
    public float assimilationMeter = 0.0f;
    public final float MAX_ASSIMILATION = 100.0f;
    public boolean isAlive = true;

    public float hp = 100.0f;
    public final float MAX_HP = 100.0f;
    private final float SUFFOCATION_RATE = 25.0f;

    // --- RUNE SYSTEM VARIABLES ---
    public int assimilationCharges = 0;
    public String storedElement = "NONE";

    private boolean isGrounded = false;
    private boolean isSubmergedNormal = false;
    private boolean isFloatingInElement = false;
    private boolean interactedThisFrame = false;

    private int jumpCounter = 0;
    private final int MAX_JUMPS = 2;

    private final float ASSIMILATION_RATE = 15.0f;
    private final float RECOVERY_RATE = 10.0f;

    public Player(World world) {
        definePlayer(world);
        try {
            texture = new Texture("player.png");
        } catch (Exception e) {
            Gdx.app.error("Player", "Texture missing");
        }
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
        footShape.setAsBox(-1 / Main.PPM, 1 / Main.PPM, new Vector2(0, -9 / Main.PPM), 0);
        FixtureDef footDef = new FixtureDef();
        footDef.shape = footShape;
        footDef.isSensor = true;
        b2body.createFixture(footDef).setUserData("FOOT_SENSOR");
        footShape.dispose();

        b2body.setGravityScale(GRAVITY_NORMAL);
    }

    public void update(float dt, SandManager sandMgr) {
        if (!isAlive || sandMgr == null) return;

        isSubmergedNormal = false;
        isFloatingInElement = false;

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
        handleMovement();
        applyVariableGravity();
        updateStats(dt);
    }

    private void handleMovement() {
        Vector2 vel = b2body.getLinearVelocity();
        float targetX = 0;
        float desiredY = vel.y;

        if (isFloatingInElement) {
            // Horizontal Movement
            if (Gdx.input.isKeyPressed(Input.Keys.A)) targetX = -MOVE_SPEED;
            if (Gdx.input.isKeyPressed(Input.Keys.D)) targetX = MOVE_SPEED;

            if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
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
            float lerpFactor = isGrounded ? 0.12f : 0.05f;

            float desiredX = com.badlogic.gdx.math.MathUtils.lerp(vel.x, targetX, lerpFactor);

            if (jumpBufferTimer > 0 && coyoteTimer > 0) {
                desiredY = currentJumpSpeed;
                jumpBufferTimer = 0;
                coyoteTimer = 0;
                jumpCounter = 1;
            } else if (jumpBufferTimer > 0 && jumpCounter < MAX_JUMPS && coyoteTimer <= 0) {
                desiredY = currentJumpSpeed;
                jumpBufferTimer = 0;
                jumpCounter++;
            }

            b2body.setLinearVelocity(desiredX, desiredY);
        }
    }

    private void applyVariableGravity() {
        if (isFloatingInElement) {
            b2body.setGravityScale(0);
            return;
        }
        Vector2 vel = b2body.getLinearVelocity();
        boolean holdingJump = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.SPACE) || Gdx.input.isKeyPressed(Input.Keys.UP);

        if (vel.y > 0 && !holdingJump) b2body.setGravityScale(GRAVITY_FALL * 2);
        else if (vel.y < 0) b2body.setGravityScale(GRAVITY_FALL);
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

        // 1. Grounding Logic
        boolean standingOnWall = WorldContactListener.footContacts > 0;
        Element feet = getSafeElement(sim, simX, simY - RADIUS_OFFSET);
        boolean standingOnElement = (feet != null && !(feet instanceof EmptyCell));

        if (currentState == State.DIRT_FORM || currentState == State.LIQUID_FORM) {
            isGrounded = standingOnWall;
        } else {
            isGrounded = standingOnWall || standingOnElement;
        }

        boolean isResting = Math.abs(b2body.getLinearVelocity().y) < 0.05f;
        if (isGrounded && isResting) {
            jumpCounter = 0;
        }

        if (currentState == State.NORMAL) {
            displaceTerrain(sim);
        }

        Element center = getSafeElement(sim, simX, simY);
        if (center != null && !(center instanceof EmptyCell)) {
            applyElementEffects(center);
        }

        // --- INTERACTION LOGIC ---
        Vector2 pixelPos = new Vector2(b2body.getPosition().x * Main.PPM, b2body.getPosition().y * Main.PPM);

        // FIX: Constantly check for nearby signs and runes EVERY frame!
        WorldContactListener.sortSignsByDistance(pixelPos);
        WorldContactListener.sortRunesByDistance(pixelPos);

        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            if (WorldContactListener.closestSign != null) {

                // --- NEW DISTANCE CHECK ---
                // Calculate how far Ezra is from the sign in pixels
                float dist = pixelPos.dst(
                    WorldContactListener.closestSign.worldX * Main.PPM,
                    WorldContactListener.closestSign.worldY * Main.PPM
                );

                // 64 pixels is about 2 tiles. If further than that, ignore the press!
                if (dist < 64f) {
                    interactedThisFrame = true;
                    return;
                } else {
                    Gdx.app.log("Player", "Too far from sign to read: " + dist);
                }
            }
        }

        // KEY 2: [E] for RUNES and TRANSFORMATION
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            tilemapmanager.RuneData rune = WorldContactListener.closestRune;
            boolean processedRune = false;

            // --- NEW DISTANCE CHECK ---
            // Calculate distance from Ezra's current pixel position to the rune
            float dist = -1;
            if (rune != null) {
                dist = pixelPos.dst(rune.worldX * Main.PPM, rune.worldY * Main.PPM);
            }

            // Only allow interaction if Ezra is within 64 pixels (2 tiles)
            if (rune != null && dist < 64f && !rune.isContainer) {
                String type = rune.elementType != null ? rune.elementType.toUpperCase() : "NONE";

                if (rune.isSpawner) {
                    if (sandMgr.completedGeysers.contains(rune.runeID, false)) {
                        Gdx.app.log("Player", "This geyser is exhausted.");
                    } else if (currentState != State.NORMAL) {
                        boolean match = (currentState == State.DIRT_FORM && (type.equals("DIRT") || type.equals("SAND"))) ||
                            (currentState == State.LIQUID_FORM && type.equals("WATER"));

                        if (match) {
                            sandMgr.toggleSpawner(rune);
                            processedRune = true;
                        } else {
                            Gdx.app.log("Player", "Wrong form! Need " + type + " (Dist: " + dist + ")");
                        }
                    }
                } else if (currentState == State.NORMAL) {
                    if (!type.equals("NONE")) {
                        assimilationCharges = 3;
                        storedElement = type;
                        Gdx.app.log("Player", "Gained 3 " + storedElement + " charges!");
                        processedRune = true;
                    }
                }
            }

            // Handle Transformation if no rune was activated
            // (This part stays the same so Ezra can still transform anywhere)
            if (!processedRune) {
                if (assimilationCharges > 0 && currentState == State.NORMAL) {
                    assimilationCharges--;
                    if (storedElement.equals("DIRT") || storedElement.equals("SAND")) {
                        currentState = State.DIRT_FORM;
                    } else if (storedElement.equals("WATER")) {
                        currentState = State.LIQUID_FORM;
                    }
                } else if (currentState != State.NORMAL) {
                    currentState = State.NORMAL;
                }
            }
        }
    }

    private void applyElementEffects(Element e) {
        // Reset flags
        isSubmergedNormal = false;
        isFloatingInElement = false;

        if (currentState == State.DIRT_FORM && (e instanceof Sand || e instanceof Dirt)) {
            isFloatingInElement = true;
            // --- THE "NUDGE" FIX ---
            // If Ezra is moving, give him a tiny extra force to "push"
            // through those invisible micro-corners of the sand pixels.
            Vector2 vel = b2body.getLinearVelocity();
            if (Math.abs(vel.x) > 0.1f) {
                b2body.applyLinearImpulse(new Vector2(vel.x * 0.01f, 0), b2body.getWorldCenter(), true);
            }
        }
        else if (currentState == State.LIQUID_FORM && e instanceof Water) {
            isFloatingInElement = true;
        }
        else {
            isSubmergedNormal = true;
        }
    }

    private void updateStats(float dt) {
        if (currentState != State.NORMAL) {
            assimilationMeter = Math.min(MAX_ASSIMILATION, assimilationMeter + (ASSIMILATION_RATE * dt));
            if (assimilationMeter >= MAX_ASSIMILATION) {
                hp -= SUFFOCATION_RATE * dt;
                if (hp <= 0) triggerDeath("Overload");
            }
        } else {
            assimilationMeter = Math.max(0, assimilationMeter - (RECOVERY_RATE * dt));
        }

        if (isSubmergedNormal) {
            hp -= SUFFOCATION_RATE * dt;
            if (hp <= 0) triggerDeath("Suffocated / Drowned");
        } else {
            hp = Math.min(MAX_HP, hp + (RECOVERY_RATE * 0.5f * dt));
        }
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

    public void triggerDeath(String reason) {
        isAlive = false;
        b2body.setTransform(100/Main.PPM, 200/Main.PPM, 0);
        b2body.setLinearVelocity(0,0);
        assimilationMeter = 0;
        hp = MAX_HP;
        currentState = State.NORMAL;
        isAlive = true;
    }

    private Element getSafeElement(PhysicSim sim, int x, int y) {
        if (sim == null) return null;
        try { return sim.getElement(x, y); } catch (Exception e) { return null; }
    }

    // --- HUD HELPER METHOD (FIXED) ---
    public float getMassPercentage() {
        return assimilationMeter / MAX_ASSIMILATION;
    }

    public void draw(SpriteBatch batch) {
        if (texture == null) return;
        switch (currentState) {
            case DIRT_FORM: batch.setColor(0.6f, 0.4f, 0.2f, 1f); break;
            case LIQUID_FORM: batch.setColor(0.2f, 0.2f, 0.8f, 1f); break;
            default: batch.setColor(1, 1, 1, 1); break;
        }
        batch.draw(texture, b2body.getPosition().x * Main.PPM - texture.getWidth() / 2f, b2body.getPosition().y * Main.PPM - texture.getHeight() / 2f);
        batch.setColor(1, 1, 1, 1);
    }
}
