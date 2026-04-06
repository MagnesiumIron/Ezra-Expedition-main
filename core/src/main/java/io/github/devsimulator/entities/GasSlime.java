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

public class GasSlime extends Enemy {
    private Texture sheet;
    private Animation<TextureRegion> slimeAnimation;

    // PHYSICS CONSTANTS
    private float jumpForceX = 0.6f;
    private float jumpForceY = 2.5f;
    private boolean movingRight = true;
    private float jumpInterval = 3.0f;
    private float anticipationTime = 1.0f;
    private boolean isJumping = false;

    private float timer = 0;
    protected int health = 3;
    private Player player;

    public GasSlime(World world, Player player, float x, float y) {
        super(world, x, y);
        this.player = player;

        // Load texture with Nearest filter for crisp pixel art
        sheet = new Texture("gas_slime.png");
        sheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        Array<TextureRegion> frames = new Array<>();
        frames.add(new TextureRegion(sheet, 0, 0, 32, 32));  // Idle
        frames.add(new TextureRegion(sheet, 32, 0, 32, 32)); // Squish
        frames.add(new TextureRegion(sheet, 64, 0, 32, 32)); // Jump/Air
        slimeAnimation = new Animation<>(0.15f, frames);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(8 / Main.PPM, 8 / Main.PPM);

        FixtureDef fdef = new FixtureDef();
        fdef.shape = shape;
        fdef.density = 0.8f;
        fdef.friction = 0.5f;
        fdef.restitution = 0.1f;

        b2body.createFixture(fdef).setUserData(this);
        b2body.setLinearDamping(1.5f); // Air resistance for that "gaseous" feel
        shape.dispose();
    }

    @Override
    public void update(float dt) {
        if (!isAlive || health <= 0) return;

        // 1. FLOAT LOGIC
        // If falling, gravity is low. If rising or grounded, gravity is normal.
        if (b2body.getLinearVelocity().y < -0.1f) {
            b2body.setGravityScale(0.3f);
        } else {
            b2body.setGravityScale(1.0f);
        }

        // 2. GROUNDED & TIMER LOGIC
        boolean isGrounded = Math.abs(b2body.getLinearVelocity().y) < 0.01f;

        if (isGrounded) {
            timer += dt; // Timer only counts up when the slime is touching the floor
            if (isJumping) isJumping = false;

            float slimeX = b2body.getPosition().x;
            float playerX = player.b2body.getPosition().x;

            // Track player direction until the "Squish" starts
            if (timer < (jumpInterval - anticipationTime)) {
                movingRight = (playerX < slimeX);
            }

            // Execute Jump
            if (timer >= jumpInterval) {
                float forceX = movingRight ? -jumpForceX : jumpForceX;
                b2body.applyLinearImpulse(new Vector2(forceX, jumpForceY), b2body.getWorldCenter(), true);
                isJumping = true;
                timer = 0;
            }
        } else {
            // Reset timer while in air to force a full idle/squish cycle upon landing
            timer = 0;
        }
    }

    @Override
    public void draw(SpriteBatch batch) {
        if (!isAlive) return;

        // Decide which frame to show based on state
        boolean isSquishing = timer >= (jumpInterval - anticipationTime);
        TextureRegion currentFrame;

        if (isJumping || Math.abs(b2body.getLinearVelocity().y) > 0.1f) {
            currentFrame = slimeAnimation.getKeyFrame(0.35f); // Air frame
        } else if (isSquishing) {
            currentFrame = slimeAnimation.getKeyFrame(0.16f); // Squish frame
        } else {
            currentFrame = slimeAnimation.getKeyFrame(0.0f);  // Idle frame
        }

        // Handle sprite flipping
        if (movingRight && !currentFrame.isFlipX()) currentFrame.flip(true, false);
        if (!movingRight && currentFrame.isFlipX()) currentFrame.flip(true, false);

        // Standard draw call (Cleaned up from the decimal offsets)
        batch.draw(currentFrame,
            (b2body.getPosition().x * Main.PPM) - 25f,
            (b2body.getPosition().y * Main.PPM) - 31f,
            50f, 50f);
    }
}
