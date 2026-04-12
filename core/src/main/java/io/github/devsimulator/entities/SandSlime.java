package io.github.devsimulator.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import io.github.devsimulator.Main;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.math.Vector2;

import java.util.Iterator;

public class SandSlime extends Enemy {
    private Texture sheet;
    private Animation<TextureRegion> slimeAnimation;
    private boolean movingRight = true; // True means looking left due to your sprite orientation
    private float shootInterval = 2.5f;
    private float anticipationTime = 0.5f;
    private float timer = 0;
    protected int health = 3;
    private Player player;
    private Sound shootSound;

    // List to keep track of active projectiles
    private Array<SandProjectile> projectiles;

    public SandSlime(World world, Player player, float x, float y) {
        super(world, x, y);
        b2body.setType(BodyDef.BodyType.StaticBody);
        this.player = player;
        this.projectiles = new Array<>();
        shootSound = Gdx.audio.newSound(Gdx.files.internal("slime_sounds/sand_projectile.wav"));

        sheet = new Texture("enemies/sand_slime.png");
        Array<TextureRegion> frames = new Array<>();
        frames.add(new TextureRegion(sheet, 0, 0, 32, 32));  // Idle
        frames.add(new TextureRegion(sheet, 32, 0, 32, 32)); // Squish (Used for shooting wind-up)
        slimeAnimation = new Animation<>(0.15f, frames);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(8 / Main.PPM, 8 / Main.PPM);

        FixtureDef fdef = new FixtureDef();
        fdef.shape = shape;
        fdef.density = 5.0f; // Very heavy so it doesn't get pushed easily
        fdef.friction = 1.0f;

        b2body.createFixture(fdef).setUserData(this);
        shape.dispose();
    }

    @Override
    public void update(float dt, io.github.devsimulator.controllers.SandManager sandMgr) {
        if (!isAlive || hp <= 0) {
            if (b2body.isActive()) {
                b2body.setActive(false);
            }
            return;
        }

        timer += dt;

        float slimeX = b2body.getPosition().x;
        float playerX = player.b2body.getPosition().x;
        float distance = Math.abs(playerX - slimeX) * Main.PPM;

        // 1. TURRET TRACKING
        if (distance < 200f) {
            // Only update facing direction if not currently winding up a shot
            if (timer < (shootInterval - anticipationTime)) {
                movingRight = (playerX < slimeX);
            }

            // Stop horizontal sliding entirely
            b2body.setLinearVelocity(0, b2body.getLinearVelocity().y);

            // 2. SHOOT EXECUTION
            if (timer >= shootInterval) {
                float pitch = MathUtils.random(0.8f, 1.2f);
                shootSound.play(0.5f, pitch, 0f);
                float spawnX = b2body.getPosition().x + (movingRight ? -0.8f : 0.8f);
                float spawnY = b2body.getPosition().y + 0.2f;

                Vector2 playerPos = player.b2body.getPosition();
                // AIM FIX: Aim just slightly above the player's center (0.5 units)
                // instead of 5.0 units!
                Vector2 target = new Vector2(playerPos.x, playerPos.y );

                projectiles.add(new SandProjectile(world, spawnX, spawnY, target, "SAND", false));
                timer = 0;
            }
        } else {
            timer = 0;
        }

        // 3. PROJECTILE MANAGEMENT
        Iterator<SandProjectile> iter = projectiles.iterator();
        while (iter.hasNext()) {
            SandProjectile proj = iter.next();
            proj.update(dt);
            if (proj.isDestroyed) {
                world.destroyBody(proj.b2body);
                proj.dispose();
                iter.remove();
            }
        }
    }

    @Override
    public void draw(SpriteBatch batch) {
        if (!isAlive) return;

        boolean isSquishing = timer >= (shootInterval - anticipationTime) && timer < shootInterval;
        TextureRegion currentFrame = isSquishing ? slimeAnimation.getKeyFrame(0.16f) : slimeAnimation.getKeyFrame(0.0f);

        if (movingRight && !currentFrame.isFlipX()) currentFrame.flip(true, false);
        if (!movingRight && currentFrame.isFlipX()) currentFrame.flip(true, false);

        batch.draw(currentFrame, (b2body.getPosition().x * Main.PPM) - 25f, (b2body.getPosition().y * Main.PPM) - 31.25f, 50f, 50f);

        // Draw projectiles on top of everything
        for (SandProjectile proj : projectiles) {
            proj.draw(batch);
        }
    }
    @Override
    public void resetState() {
        this.isAlive = true;
        this.health = 3; // Or whatever max health you want
        this.timer = 0;

        // Reset physics body
        this.b2body.setActive(true);
        this.b2body.setTransform(spawnX, spawnY, 0);
        this.b2body.setLinearVelocity(0, 0);
        this.b2body.setAwake(true);

        // Clear existing projectiles so they don't stay on screen after reset
        for (SandProjectile proj : projectiles) {
            if (proj.b2body != null) world.destroyBody(proj.b2body);
            proj.dispose();
        }
        projectiles.clear();
    }
    @Override
    public void dispose() {
        // Clean up the texture sheet
        if (sheet != null) { sheet.dispose();
        }
        if (shootSound != null) shootSound.dispose();
        // Clean up any active projectiles
        for (SandProjectile proj : projectiles) {
            proj.dispose();
        }
    }
}
