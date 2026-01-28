package io.github.devsimulator.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import io.github.devsimulator.Main;
import io.github.devsimulator.elements.Dirt;
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
    public float assimilationMeter = 0.0f; // 0 to 100
    public final float MAX_ASSIMILATION = 100.0f;
    public boolean isAlive = true;

    // Balance Constants (Tweak these to make the game harder/easier)
    private final float ASSIMILATION_RATE = 15.0f; // Risk increases by 15 per second
    private final float RECOVERY_RATE = 10.0f;     // Risk drops by 10 per second

    public Player(World world) {
        this.world = world;
        definePlayer();
        texture = new Texture("player.png"); // Ensure this asset exists!
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
        if (!isAlive) return; // Stop logic if dead

        handleInput(); // Movement

        // 3. New State Logic from Pseudocode
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

        // 4. Interaction Logic (Eating/Assimilating)
        if (sim != null) {
            interactWithEnvironment(sim);
        }
    }

    private void handleInput() {
        // Basic Movement
        if (Gdx.input.isKeyPressed(Input.Keys.A) && b2body.getLinearVelocity().x >= -2) {
            b2body.applyLinearImpulse(new Vector2(-0.2f, 0), b2body.getWorldCenter(), true);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) && b2body.getLinearVelocity().x <= 2) {
            b2body.applyLinearImpulse(new Vector2(0.2f, 0), b2body.getWorldCenter(), true);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.W) && b2body.getLinearVelocity().y == 0) {
            b2body.applyLinearImpulse(new Vector2(0, 8f), b2body.getWorldCenter(), true);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && b2body.getLinearVelocity().y == 0) {
            b2body.applyLinearImpulse(new Vector2(0, 8f), b2body.getWorldCenter(), true);
        }

        // MANUAL TRANSFORMATION KEYS (For Testing)
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) currentState = State.NORMAL;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) transformTo(State.DIRT_FORM);
    }

    private void interactWithEnvironment(PhysicSim sim) {
        // Calculate player position in the Sim Grid
        int simX = (int) (b2body.getPosition().x * Main.PPM / (960f / 200f));
        int simY = (int) (b2body.getPosition().y * Main.PPM / (640f / 200f));

        for (int i = -3; i <= 3; i++) {
            for (int j = -3; j <= 3; j++) {
                // Get the element we are touching
                Element e = sim.getElement(simX + i, simY + j);

                if (e != null) {
                    // Logic: If we touch Dirt, and we are NOT Dirt Form, we eat it?
                    // OR: If we press a button, we become it.

                    // For now: Just clear the path so we don't get stuck
                    sim.assimilateElement(simX + i, simY + j);
                }
            }
        }
    }

    // Helper to switch states
    public void transformTo(State newState) {
        this.currentState = newState;
        // Add physics changes here later (e.g., disable gravity for Gas)
    }

    public void triggerDeath(String reason) {
        isAlive = false;
        System.out.println("GAME OVER: " + reason);
        // Reset for now so you can keep playing
        assimilationMeter = 0;
        currentState = State.NORMAL;
        isAlive = true;
    }

    public void draw(SpriteBatch batch) {
        // VISUAL FEEDBACK: Change color based on State
        switch (currentState) {
            case DIRT_FORM: batch.setColor(0.6f, 0.4f, 0.2f, 1f); break; // Brown
            case LIQUID_FORM: batch.setColor(0.2f, 0.2f, 0.8f, 1f); break; // Blue
            case GAS_FORM: batch.setColor(0.8f, 0.8f, 0.8f, 0.5f); break; // Transparent
            default: batch.setColor(1, 1, 1, 1); break; // Normal
        }

        batch.draw(texture,
            b2body.getPosition().x * Main.PPM - texture.getWidth() / 2,
            b2body.getPosition().y * Main.PPM - texture.getHeight() / 2);

        // RESET Color so we don't tint the rest of the game
        batch.setColor(1, 1, 1, 1);
    }

    // Helper for the HUD
    public float getMassPercentage() {
        return assimilationMeter / MAX_ASSIMILATION;
    }
}
