package io.github.devsimulator.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import io.github.devsimulator.Main;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;

public class Slime extends Enemy {
    // TEXTURE & ANIMATION VARIABLES
    private Texture sheet;
    private Animation<TextureRegion> slimeAnimation;
    private float stateTimer = 0;

    // PHYSICS & LOGIC VARIABLES
    private float jumpForceX = 0.8f;
    private float jumpForceY = 1.5f;
    private boolean movingRight = true;
    private float jumpInterval = 2.0f;
    private float anticipationTime = 1.0f;
    private boolean isJumping = false;
    private float timer = 0;
    protected int health = 3; //Placeholder value, will adjust depending on Ezra's damage
    private Player player;


    public Slime(World world, Player player, float x, float y) {
        super(world, x, y); // Enemy creates the Body
        this.player = player;
        this.health = 3;

        //Manual 32 x 32 texture slicing
        sheet = new Texture("Comun_slime.png");
        Array<TextureRegion> frames = new Array<>();
        frames.add(new TextureRegion(sheet, 0, 0, 32, 32));  // Idle
        frames.add(new TextureRegion(sheet, 32, 0, 32, 32)); // Squish
        frames.add(new TextureRegion(sheet, 64, 0, 32, 32)); // Jumping

        slimeAnimation = new Animation<>(0.15f, frames);

        // DEFINES THE PHYSICS SHAPE
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(8 / Main.PPM, 8 / Main.PPM);

        FixtureDef fdef = new FixtureDef();
        fdef.shape = shape;
        fdef.density = 1.0f;
        fdef.friction = 0.5f;
        fdef.restitution = 0.1f;

        b2body.createFixture(fdef).setUserData(this);
        b2body.setLinearDamping(1.2f); // Keeps them from sliding like ice

        this.timer = (float) (Math.random() * jumpInterval);
        shape.dispose();
    }

    @Override
    public void update(float dt) {
        if (!isAlive || health <= 0) return;

        timer += dt;
        stateTimer += dt;
        b2body.setGravityScale(1.0f);

        // 1. GROUNDED & LANDING CHECK (UNTOUCHED - Keeps it smooth)
        boolean isGrounded = Math.abs(b2body.getLinearVelocity().y) < 0.01f;
        if (isJumping && isGrounded && b2body.getLinearVelocity().y <= 0) {
            isJumping = false;
        }

        // 2. TARGETING MATH
        float slimeX = b2body.getPosition().x;
        float playerX = player.b2body.getPosition().x;
        float distance = Math.abs(playerX - slimeX) * Main.PPM;

        // 3. BRAIN: DECIDE DIRECTION
        if (isGrounded) {
            if (distance < 500f) {
                // Look at Ezra (Using < because your sprite is inverted)
                movingRight = (playerX < slimeX);
            } else {
                // Patrol only if Ezra is far away
                float currentX = b2body.getPosition().x * Main.PPM;
                if (movingRight && currentX > 600) {
                    movingRight = false;
                } else if (!movingRight && currentX < 100) {
                    movingRight = true;
                }
            }

            // 4. ANTICIPATION PHASE (The "Squish")
            if (isGrounded && timer >= (jumpInterval - anticipationTime)) {
                // Stop horizontal drift
                b2body.setLinearVelocity(0, b2body.getLinearVelocity().y);
            }

            // 5. THE JUMP EXECUTION
            if (timer >= jumpInterval && isGrounded) {
                // LOGIC FIX: If 'movingRight' is TRUE (Facing Left), we need NEGATIVE force.
                // If 'movingRight' is FALSE (Facing Right), we need POSITIVE force.
                float forceX = movingRight ? -jumpForceX : jumpForceX;

                b2body.applyLinearImpulse(new Vector2(forceX, jumpForceY), b2body.getWorldCenter(), true);
                isJumping = true;
                timer = 0;
            }
        }
    }
    @Override
    public void draw(SpriteBatch batch) {
        if (!isAlive) return;

        // Use our state boolean to decide the frame (No flickering at the peak!)
        boolean isSquishing = timer >= (jumpInterval - anticipationTime);

        TextureRegion currentFrame;

        if (isJumping) {
            // Safe call: Frame 2 (Jump)
            currentFrame = slimeAnimation.getKeyFrame(0.35f);
        } else if (isSquishing) {
            // Safe call: Frame 1 (Squish)
            currentFrame = slimeAnimation.getKeyFrame(0.16f);
        } else {
            // Safe call: Frame 0 (Idle)
            currentFrame = slimeAnimation.getKeyFrame(0.0f);
        }

        // HANDLE FLIPPING
        if (movingRight && !currentFrame.isFlipX()) currentFrame.flip(true, false);
        if (!movingRight && currentFrame.isFlipX()) currentFrame.flip(true, false);

        // DRAWING
        float width = 50f;
        float height = 50f;

        batch.draw(currentFrame,
            (b2body.getPosition().x * Main.PPM) - width / 2f,
            (b2body.getPosition().y * Main.PPM) - height / 2f - (height / 8f),
            width,
            height);
    }
}
