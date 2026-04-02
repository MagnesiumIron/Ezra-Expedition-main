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

    // size nung sand pixel need magmatch sa physicSim
    private static final int CELL_SIZE = 4;
    private static final int RADIUS_OFFSET = 10 / CELL_SIZE;

    // Physics constraints
    private static final float MOVE_SPEED = 6.0f; //max horizontal speed
    private static final float JUMP_SPEED = 9.0f; //instant vertical velocity
    private static final float GRAVITY_NORMAL = 3.0f; //gravity while jumping
    private static final float GRAVITY_FALL = 5.0f; //higher gravity when falling
    private static final float TERMINAL_VELOCITY = -12.0f; //capped speed lang

    // Added coyote time to allow jumping ng 0.1s when nagwalk off sa ledge
    private final float coyoteTime = 0.1f;
    private final float jumpBuffer = 0.1f;

    // mutable timers
    private float coyoteTimer = 0;
    private float jumpBufferTimer = 0;

    public enum State { NORMAL, DIRT_FORM, LIQUID_FORM, GAS_FORM } //state management of the elements

    // BOX2D components
    public Body b2body;
    private Texture texture;

    public State currentState = State.NORMAL; //current element
    public float assimilationMeter = 0.0f;
    public final float MAX_ASSIMILATION = 100.0f; //will die if reaches max
    public boolean isAlive = true;

    // Physics State
    private boolean isGrounded = false;
    private int jumpCounter = 0;
    private final int MAX_JUMPS = 2;

    // Health
    private final float ASSIMILATION_RATE = 15.0f;
    private final float RECOVERY_RATE = 10.0f;

    public Player(World world) {
        // World is only needed here, so we pass it to definePlayer and forget it
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
<<<<<<< Updated upstream
=======
        shape.dispose();

        PolygonShape footShape = new PolygonShape();
        footShape.setAsBox(4 / Main.PPM, 1 / Main.PPM, new Vector2(0, -9 / Main.PPM), 0);

        FixtureDef footDef = new FixtureDef();
        footDef.shape = footShape;
        footDef.isSensor = true;
        b2body.createFixture(footDef).setUserData("FOOT_SENSOR");
        footShape.dispose();

>>>>>>> Stashed changes
        b2body.setGravityScale(GRAVITY_NORMAL);
        shape.dispose();
    }

    public void update(float dt, PhysicSim sim) {
        if (!isAlive) return;

        // for input buffering
        if (isGrounded) coyoteTimer = coyoteTime;
        else coyoteTimer -= dt;

        if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            jumpBufferTimer = jumpBuffer;
        } else {
            jumpBufferTimer -= dt;
        }

        //
        if (sim != null) interactWithEnvironment(sim); //check ground/element
        handleMovement(sim); //movement of the player
        applyVariableGravity(); //adjust gravity according to the fall state
        limitMapBounds(); // to prevent out of screen
        updateAssimilation(dt); //stats
    }

    private void handleMovement(PhysicSim sim) {
        Vector2 vel = b2body.getLinearVelocity();
        float desiredX = 0;

        // INSTANT Horizontal movement
        if (Gdx.input.isKeyPressed(Input.Keys.A)) desiredX = -MOVE_SPEED;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) desiredX = MOVE_SPEED;

<<<<<<< Updated upstream
<<<<<<< Updated upstream
        // Jump Logic for responsive feeling
        if (jumpBufferTimer > 0 && coyoteTimer > 0) {
            vel.y = JUMP_SPEED;
            jumpBufferTimer = 0;
            coyoteTimer = 0;
            jumpCounter = 1;
        }
        else if (jumpBufferTimer > 0 && jumpCounter < MAX_JUMPS && coyoteTimer <= 0) {//double jump
            vel.y = JUMP_SPEED;
            jumpBufferTimer = 0;
            jumpCounter++;
        }
=======
=======
>>>>>>> Stashed changes
            if (Gdx.input.isKeyPressed(Input.Keys.W) /*|| Gdx.input.isKeyPressed(Input.Keys.UP)*/) {
                desiredY = JUMP_SPEED * 0.5f;
            } else if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
                desiredY = -JUMP_SPEED * 0.5f;
            } else {
                desiredY = -1.0f;
            }
>>>>>>> Stashed changes

        // Apply movemement
        b2body.setLinearVelocity(desiredX, vel.y);

<<<<<<< Updated upstream
        // Interaction by pressing 'E' to absorb the element
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            tryAbsorbElement(sim);
=======
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
            } else if (jumpBufferTimer > 0 && jumpCounter < MAX_JUMPS && coyoteTimer <= 0) {
                desiredY = currentJumpSpeed;
                jumpBufferTimer = 0;
                jumpCounter++;
            }

            b2body.setLinearVelocity(desiredX, desiredY);
>>>>>>> Stashed changes
        }
    }
    //mario style gravity
    private void applyVariableGravity() {
        Vector2 vel = b2body.getLinearVelocity();
        boolean holdingJump = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.SPACE);

<<<<<<< Updated upstream
<<<<<<< Updated upstream
        if (vel.y > 0 && !holdingJump) { //short hop if action is rising but not holding jump
            b2body.setGravityScale(GRAVITY_FALL * 2);
        } else if (vel.y < 0) {
            b2body.setGravityScale(GRAVITY_FALL); //heavy fall
        } else {
            b2body.setGravityScale(GRAVITY_NORMAL); //normal
        }
=======
=======
>>>>>>> Stashed changes
        if (vel.y > 0 && !holdingJump) b2body.setGravityScale(GRAVITY_FALL * 2.5f);
        else if (vel.y < 0) b2body.setGravityScale(GRAVITY_FALL * 1.5f);
        else b2body.setGravityScale(GRAVITY_NORMAL);
>>>>>>> Stashed changes

        if (vel.y < TERMINAL_VELOCITY) {
            b2body.setLinearVelocity(vel.x, TERMINAL_VELOCITY);
        }
    }

    private void interactWithEnvironment(PhysicSim sim) { //solid and sand pixel ground detection method
        int simX = (int) (b2body.getPosition().x * Main.PPM / CELL_SIZE);
        int simY = (int) (b2body.getPosition().y * Main.PPM / CELL_SIZE);

        Element feet = getSafeElement(sim, simX, simY - RADIUS_OFFSET);
        //standing on the pixel
        boolean onSimulatedGround = (feet != null && !(feet instanceof EmptyCell));
        boolean onSolidBlock = Math.abs(b2body.getLinearVelocity().y) < 0.01f;

        isGrounded = onSimulatedGround || onSolidBlock;

        displaceTerrain(sim);

        Element center = getSafeElement(sim, simX, simY);
        if (center != null) applyElementEffects(center);
    }

    public void tryAbsorbElement(PhysicSim sim) {
        if (sim == null) return;
        int simX = (int) (b2body.getPosition().x * Main.PPM / CELL_SIZE);
        int simY = (int) (b2body.getPosition().y * Main.PPM / CELL_SIZE);

        Element e = getSafeElement(sim, simX, simY);
        if (e == null) e = getSafeElement(sim, simX, simY - RADIUS_OFFSET);

        if (e instanceof Sand || e instanceof Dirt) {
            currentState = State.DIRT_FORM;
        } else if (e instanceof Water) {
            currentState = State.LIQUID_FORM;
        } else {
            currentState = State.NORMAL;
        }
    }

    private void displaceTerrain(PhysicSim sim) {//whenever the player overlaps with the sand, it'll push the sand up or sideways
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

    private void applyElementEffects(Element e) {
        if (currentState == State.NORMAL && e instanceof Water) triggerDeath("Drowned");
        if (currentState == State.DIRT_FORM && e instanceof Water) {
            b2body.setLinearDamping(10f);
        }
    }

    private void updateAssimilation(float dt) {
        if (currentState != State.NORMAL) {
            assimilationMeter += ASSIMILATION_RATE * dt;
            if (assimilationMeter >= MAX_ASSIMILATION) triggerDeath("Overload");
        } else {
            assimilationMeter = Math.max(0, assimilationMeter - (RECOVERY_RATE * dt));
        }
    }

    public void triggerDeath(String reason) {
        System.out.println("DIED: " + reason);
        isAlive = false;
        b2body.setTransform(100/Main.PPM, 200/Main.PPM, 0);
        b2body.setLinearVelocity(0,0);
        assimilationMeter = 0;
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
