package io.github.devsimulator.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import io.github.devsimulator.Main;
import io.github.devsimulator.elements.*;
import io.github.devsimulator.helper.PhysicSim;

public class Player {

    private static final int CELL_SIZE = 4;
    private static final int RADIUS_OFFSET = 10 / CELL_SIZE;

    // Physics constraints
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

    // HP System
    public float hp = 100.0f;
    public final float MAX_HP = 100.0f;
    private final float SUFFOCATION_RATE = 25.0f; // Loses 25 HP per second when trapped

    // Environmental Tracking Flags
    private boolean isGrounded = false;
    private boolean isSubmergedNormal = false; // Trapped in matter without ability
    private boolean isFloatingInElement = false; // Assimilated and inside matching matter

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

        // 1. MAIN BODY (The Circle)
        FixtureDef fdef = new FixtureDef();
        CircleShape shape = new CircleShape();
        shape.setRadius(9 / Main.PPM);

        fdef.shape = shape;
        fdef.friction = 0.0f;

        b2body.createFixture(fdef).setUserData("PLAYER");
        shape.dispose();

        // --- 2. ADD THE FOOT SENSOR ---
        PolygonShape footShape = new PolygonShape();

        // Make a tiny box. We make it slightly narrower than his body (6px instead of 9px)
        // so it doesn't accidentally scrape the walls when he falls down a tight shaft.
        // We position it exactly at the bottom of the circle: (0, -9)
        footShape.setAsBox(6 / Main.PPM, 2 / Main.PPM, new Vector2(0, -9 / Main.PPM), 0);

        FixtureDef footDef = new FixtureDef();
        footDef.shape = footShape;
        footDef.isSensor = true; // This makes it pass through objects instead of bouncing

        b2body.createFixture(footDef).setUserData("FOOT_SENSOR");
        footShape.dispose();
        // ------------------------------

        b2body.setGravityScale(GRAVITY_NORMAL);
    }

    public void update(float dt, PhysicSim sim) {
        if (!isAlive) return;

        // Reset state flags every frame
        isSubmergedNormal = false;
        isFloatingInElement = false;

        if (isGrounded) coyoteTimer = coyoteTime;
        else coyoteTimer -= dt;

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            jumpBufferTimer = jumpBuffer;
        } else {
            jumpBufferTimer -= dt;
        }

        if (sim != null) interactWithEnvironment(sim);
        handleMovement();
        applyVariableGravity();
        limitMapBounds();
        updateStats(dt); // Handles both HP and Assimilation
    }

    private void handleMovement() {
        Vector2 vel = b2body.getLinearVelocity();
        float desiredX = 0;
        float desiredY = vel.y;

        // 1. FREE FLYING (Assimilated into matching element)
        if (isFloatingInElement) {
            if (Gdx.input.isKeyPressed(Input.Keys.A)) desiredX = -MOVE_SPEED;
            if (Gdx.input.isKeyPressed(Input.Keys.D)) desiredX = MOVE_SPEED;
            if (Gdx.input.isKeyPressed(Input.Keys.W)) desiredY = MOVE_SPEED;
            if (Gdx.input.isKeyPressed(Input.Keys.S)) desiredY = -MOVE_SPEED;

            // Stop vertical drift if not pressing W or S
            if (!Gdx.input.isKeyPressed(Input.Keys.W) && !Gdx.input.isKeyPressed(Input.Keys.S)) {
                desiredY = 0;
            }
        }
        // PLAYER MOVEMENT
        else {
            // Apply massive snail penalty if trapped
            float currentMoveSpeed = isSubmergedNormal ? MOVE_SPEED * 0.2f : MOVE_SPEED;
            float currentJumpSpeed = isSubmergedNormal ? JUMP_SPEED * 0.35f : JUMP_SPEED;

            if (Gdx.input.isKeyPressed(Input.Keys.A)) desiredX = -currentMoveSpeed;
            if (Gdx.input.isKeyPressed(Input.Keys.D)) desiredX = currentMoveSpeed;

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
        }

        b2body.setLinearVelocity(desiredX, desiredY);
    }

    private void applyVariableGravity() {
        // Zero gravity when flying through an element you assimilated
        if (isFloatingInElement) {
            b2body.setGravityScale(0);
            return;
        }

        Vector2 vel = b2body.getLinearVelocity();
        boolean holdingJump = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.SPACE);

        if (vel.y > 0 && !holdingJump) {
            b2body.setGravityScale(GRAVITY_FALL * 2);
        } else if (vel.y < 0) {
            b2body.setGravityScale(GRAVITY_FALL);
        } else {
            b2body.setGravityScale(GRAVITY_NORMAL);
        }

        if (vel.y < TERMINAL_VELOCITY) {
            b2body.setLinearVelocity(vel.x, TERMINAL_VELOCITY);
        }
    }

    private void interactWithEnvironment(PhysicSim sim) {
        int simX = (int) (b2body.getPosition().x * Main.PPM / CELL_SIZE);
        int simY = (int) (b2body.getPosition().y * Main.PPM / CELL_SIZE);

        Element feet = getSafeElement(sim, simX, simY - RADIUS_OFFSET);

        boolean onSimulatedGround = (feet != null && !(feet instanceof EmptyCell));
        boolean onSolidBlock = io.github.devsimulator.helper.WorldContactListener.footContacts > 0;

        isGrounded = onSimulatedGround || onSolidBlock;

        // Reset the jump counter if we are safely on the ground
        if (isGrounded) {
            jumpCounter = 0;
        }

        displaceTerrain(sim);

        // Check head/center to see if player is drowning/suffocating
        Element center = getSafeElement(sim, simX, simY);
        if (center != null && !(center instanceof EmptyCell)) {
            applyElementEffects(center);
        }

        // REPLACED BLOCK in interactWithEnvironment()
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            if (currentState != State.NORMAL) {
                // If already transformed, toggle OFF
                currentState = State.NORMAL;
            } else {
                // If normal, try to toggle ON using feet first, then center
                tryAbsorbElement(sim, feet);
                if (currentState == State.NORMAL && center != null) {
                    tryAbsorbElement(sim, center);
                }
            }
        }
    }

    private void applyElementEffects(Element e) {
        if (currentState == State.NORMAL) {
            // Snail mode + taking damage
            isSubmergedNormal = true;
        } else if (currentState == State.DIRT_FORM && (e instanceof Sand || e instanceof Dirt)) {
            // Free fly inside sand
            isFloatingInElement = true;
        } else if (currentState == State.LIQUID_FORM && e instanceof Water) {
            // Free fly inside water
            isFloatingInElement = true;
        } else {
            // Wrong element (e.g., in dirt form but fell in water)
            isSubmergedNormal = true;
        }
    }

    private void tryAbsorbElement(PhysicSim sim, Element e) {
        if (e instanceof Sand || e instanceof Dirt) currentState = State.DIRT_FORM;
        else if (e instanceof Water) currentState = State.LIQUID_FORM;
    }

    private void updateStats(float dt) {
        // 1. Assimilation Overload Check
        if (currentState != State.NORMAL) {
            assimilationMeter = Math.min(MAX_ASSIMILATION, assimilationMeter + (ASSIMILATION_RATE * dt));
            if (assimilationMeter >= MAX_ASSIMILATION) {
                hp -= SUFFOCATION_RATE *dt; // Drain HP
                    if (hp <= 0) {
                    triggerDeath("Overload"); } }
        } else {
            assimilationMeter = Math.max(0, assimilationMeter - (RECOVERY_RATE * dt));
        }

        // 2. HP & Suffocation Check
        if (isSubmergedNormal) {
            hp -= SUFFOCATION_RATE * dt; // Drain HP
            if (hp <= 0) triggerDeath("Suffocated / Drowned");
        } else {
            // Slow HP Regen when safe
            hp = Math.min(MAX_HP, hp + (RECOVERY_RATE * 0.5f * dt));
        }
    }

    private void displaceTerrain(PhysicSim sim) {
        if (sim == null) return;
        float worldX = b2body.getPosition().x * Main.PPM;
        int gridX = (int) (worldX / CELL_SIZE);
        int gridY = (int) (b2body.getPosition().y * Main.PPM / CELL_SIZE);
        int radius = 10 / CELL_SIZE;

        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                if (x*x + y*y < radius*radius) {
                    int px = gridX + x;
                    int py = gridY + y;
                    Element e = sim.getElement(px, py);
                    if (e != null && !e.isStatic) {
                        if (sim.isEmpty(px, py + 1)) sim.moveElement(px, py, px, py + 1);
                        else if (sim.isEmpty(px+1, py)) sim.moveElement(px, py, px+1, py);
                        else if (sim.isEmpty(px-1, py)) sim.moveElement(px, py, px-1, py);
                    }
                }
            }
        }
    }

    private void limitMapBounds() {
        float x = b2body.getPosition().x;
        float y = b2body.getPosition().y;
        float mapW = 960 / Main.PPM;
        float mapH = 640 / Main.PPM;

        if (y > mapH - 1.5f) {
            b2body.setTransform(x, mapH - 1.5f, 0);
            b2body.setLinearVelocity(b2body.getLinearVelocity().x, -1f);
        }
        if (x < 0) b2body.setTransform(0, y, 0);
        if (x > mapW) b2body.setTransform(mapW, y, 0);
    }

    public void triggerDeath(String reason) {
        System.out.println("DIED: " + reason);
        isAlive = false;
        b2body.setTransform(100/Main.PPM, 200/Main.PPM, 0);
        b2body.setLinearVelocity(0,0);

        // Reset Stats
        assimilationMeter = 0;
        hp = MAX_HP;
        currentState = State.NORMAL;

        isAlive = true;
    }

    private Element getSafeElement(PhysicSim sim, int x, int y) {
        if (x < 0 || x >= 240 || y < 0 || y >= 160) return null;
        return sim.getElement(x, y);
    }

    public float getMassPercentage() { return assimilationMeter / MAX_ASSIMILATION; }

    public void draw(SpriteBatch batch) {
        if (texture == null) return;
        switch (currentState) {
            case DIRT_FORM: batch.setColor(0.6f, 0.4f, 0.2f, 1f); break;
            case LIQUID_FORM: batch.setColor(0.2f, 0.2f, 0.8f, 1f); break;
            default: batch.setColor(1, 1, 1, 1); break;
        }
        batch.draw(texture,
            b2body.getPosition().x * Main.PPM - texture.getWidth() / 2f,
            b2body.getPosition().y * Main.PPM - texture.getHeight() / 2f);

        batch.setColor(1, 1, 1, 1);
    }
}
