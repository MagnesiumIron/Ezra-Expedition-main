package io.github.devsimulator.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import io.github.devsimulator.Main;
import io.github.devsimulator.elements.Element;
import io.github.devsimulator.helper.PhysicSim;

public class Player {
    // 1. Define the States
    public enum State { NORMAL, DIRT_FORM, LIQUID_FORM, GAS_FORM }

    public World world;
    public Body b2body;
    private Texture texture;

    // 2. State Variables
    public State currentState = State.NORMAL;
    public float assimilationMeter = 0.0f;
    public final float MAX_ASSIMILATION = 100.0f;
    public boolean isAlive = true;

    // Balance Constants
    private final float ASSIMILATION_RATE = 15.0f;
    private final float RECOVERY_RATE = 10.0f;

    public Player(World world) {
        this.world = world;
        definePlayer();
        // Make sure "player.png" is in your assets folder!
        try {
            texture = new Texture("player.png");
        } catch (Exception e) {
            System.err.println("CRITICAL: player.png not found. Using fallback.");
        }
    }

    public void definePlayer() {
        BodyDef bdef = new BodyDef();
        bdef.position.set(100 / Main.PPM, 400 / Main.PPM);
        bdef.type = BodyDef.BodyType.DynamicBody;
        b2body = world.createBody(bdef);

        FixtureDef fdef = new FixtureDef();
        CircleShape shape = new CircleShape();
        shape.setRadius(10 / Main.PPM); // Radius 10

        fdef.shape = shape;
        fdef.friction = 0.5f;
        b2body.createFixture(fdef);
        shape.dispose();
    }

    public void update(float dt, PhysicSim sim) {
        if (!isAlive) return;

        handleInput();

        // 3. State Logic (Risk vs Recovery)
        if (currentState != State.NORMAL) {
            // INCREASE RISK: While transformed, you risk hardening
            assimilationMeter += ASSIMILATION_RATE * dt;

            if (assimilationMeter >= MAX_ASSIMILATION) {
                triggerDeath("Hardened permanently into matter");
            }
        } else {
            // RECOVER: Slowly normalize when in human form
            assimilationMeter = Math.max(0, assimilationMeter - (RECOVERY_RATE * dt));
        }

        // 4. Interaction Logic
        if (sim != null) {
            interactWithEnvironment(sim);
        }
    }

    private void handleInput() {
        Vector2 vel = b2body.getLinearVelocity();

        // Horizontal Movement
        if (Gdx.input.isKeyPressed(Input.Keys.A) && vel.x >= -2) {
            b2body.applyLinearImpulse(new Vector2(-0.2f, 0), b2body.getWorldCenter(), true);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) && vel.x <= 2) {
            b2body.applyLinearImpulse(new Vector2(0.2f, 0), b2body.getWorldCenter(), true);
        }

        // Jump Logic (Fixed with Tolerance)
        boolean jumpPressed = Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE);

        if (jumpPressed && Math.abs(vel.y) < 0.2f) {
            b2body.applyLinearImpulse(new Vector2(0, 8f), b2body.getWorldCenter(), true);
        }

        // MANUAL TRANSFORMATION KEYS (For Testing)
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) transformTo(State.NORMAL);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) transformTo(State.DIRT_FORM);
    }

    private void interactWithEnvironment(PhysicSim sim) {
        // UPDATE: Changed to match the 240x160 grid size
        // Screen Width 960 / Grid Width 240 = 4.0
        // Screen Height 640 / Grid Height 160 = 4.0
        float cellWidth = 4.0f;
        float cellHeight = 4.0f;

        // Get Player center in Grid Coordinates
        int simX = (int) (b2body.getPosition().x * Main.PPM / cellWidth);
        int simY = (int) (b2body.getPosition().y * Main.PPM / cellHeight);

        // Check what is at our center
        Element e = sim.getElement(simX, simY);

        if (e != null) {
            // --- WE ARE INSIDE MATTER (Sand/Water) ---

            if (currentState == State.DIRT_FORM) {
                // ABILITY: "Swim" through matter
                // No gravity so you don't sink, low drag so you can move
                b2body.setGravityScale(0f);
                b2body.setLinearDamping(2f);
            } else {
                // NORMAL: "Quicksand" Effect
                // Reduced gravity so you fall slow, High drag so you are stuck
                b2body.setGravityScale(0.3f);
                b2body.setLinearDamping(8f);
            }
        } else {
            // --- WE ARE IN AIR ---
            // Reset to normal physics
            b2body.setGravityScale(1f);
            b2body.setLinearDamping(0f);
        }
    }

    public void transformTo(State newState) {
        this.currentState = newState;
    }

    public void triggerDeath(String reason) {
        isAlive = false;
        System.out.println("GAME OVER: " + reason);
        // Reset for testing purposes
        assimilationMeter = 0;
        currentState = State.NORMAL;
        isAlive = true;
    }

    public void draw(SpriteBatch batch) {
        if (texture == null) return;

        // VISUAL FEEDBACK: Tint based on form
        switch (currentState) {
            case DIRT_FORM: batch.setColor(0.6f, 0.4f, 0.2f, 1f); break; // Brown
            case LIQUID_FORM: batch.setColor(0.2f, 0.2f, 0.8f, 1f); break; // Blue
            case GAS_FORM: batch.setColor(0.8f, 0.8f, 0.8f, 0.5f); break; // Transparent
            default: batch.setColor(1, 1, 1, 1); break; // Normal
        }

        batch.draw(texture,
            b2body.getPosition().x * Main.PPM - texture.getWidth() / 2,
            b2body.getPosition().y * Main.PPM - texture.getHeight() / 2);

        // Reset Color
        batch.setColor(1, 1, 1, 1);
    }

    public float getMassPercentage() {
        return assimilationMeter / MAX_ASSIMILATION;
    }
}
