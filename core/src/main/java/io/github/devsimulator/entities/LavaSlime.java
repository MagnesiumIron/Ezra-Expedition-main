package io.github.devsimulator.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import io.github.devsimulator.Main;
import io.github.devsimulator.controllers.SandManager;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;

import java.util.Iterator;

public class LavaSlime extends Enemy {
    private Texture sheet;
    private Animation<TextureRegion> slimeAnimation;

    // Walking and Behavior
    private float walkSpeed = 1.0f;       // Adjust this to make it walk faster or slower
    private float detectionRange = 350f;  // Pixel radius before it notices the player
    private boolean movingRight = true;
    private boolean isWalking = false;

    // Shooting mechanics
    private float shootTimer = 0;
    private float shootInterval = 3.0f;
    public Array<SandProjectile> activeProjectiles;

    protected int health = 4;
    private Player player;

    private Sound slimeSound;
    private Sound shootSound;

    public LavaSlime(World world, Player player, float x, float y) {
        super(world, x, y);
        this.player = player;
        this.activeProjectiles = new Array<>();

        slimeSound = Gdx.audio.newSound(Gdx.files.internal("slime_sounds/lava_slime.wav"));
        shootSound = Gdx.audio.newSound(Gdx.files.internal("slime_sounds/projectile.mp3"));

        sheet = new Texture("enemies/lava_slime.png");
        Array<TextureRegion> frames = new Array<>();
        frames.add(new TextureRegion(sheet, 0, 0, 32, 32));
        frames.add(new TextureRegion(sheet, 32, 0, 32, 32));
        frames.add(new TextureRegion(sheet, 64, 0, 32, 32));

        slimeAnimation = new Animation<>(0.2f, frames);

        PolygonShape shape = new PolygonShape();
        // 50% larger hitbox for the bigger lava slime
        shape.setAsBox(12 / Main.PPM, 12 / Main.PPM);

        FixtureDef fdef = new FixtureDef();
        fdef.shape = shape;
        fdef.density = 1.2f;
        fdef.friction = 0.6f;
        fdef.restitution = 0.05f;

        b2body.createFixture(fdef).setUserData(this);
        b2body.setLinearDamping(1.5f);

        shape.dispose();
    }

    @Override
    public void update(float dt, SandManager sandMgr) {
        if (!isAlive || hp <= 0) {
            if (b2body.isActive()) {
                b2body.setActive(false);
            }
            return;
        }

        shootTimer += dt;

        float slimeX = b2body.getPosition().x;
        float slimeY = b2body.getPosition().y;
        float playerX = player.b2body.getPosition().x;
        float playerY = player.b2body.getPosition().y;

        // Calculate the absolute distance between the slime and the player
        float distance = Vector2.dst(slimeX * Main.PPM, slimeY * Main.PPM, playerX * Main.PPM, playerY * Main.PPM);

        // --- Detection & Walking Logic ---
        if (distance <= detectionRange) {
            isWalking = true;
            movingRight = (playerX > slimeX); // Face the player

            // Apply constant horizontal velocity for walking, keep current gravity (Y velocity)
            float desiredVelocityX = movingRight ? walkSpeed : -walkSpeed;
            b2body.setLinearVelocity(desiredVelocityX, b2body.getLinearVelocity().y);

            // --- Horizontal Shooting Logic ---
            if (shootTimer >= shootInterval) {
                shootTimer = 0;
                float randomPitch = MathUtils.random(0.8f, 1.2f);
                shootSound.play(0.5f, randomPitch, 0f);

                float dirX = movingRight ? 1f : -1f;

                // OFFSET THE SPAWN: Spawn the projectile in front of the slime, and slightly higher up
                // Prevents the fireball from instantly blowing up inside the slime's own hitbox
                float projSpawnX = slimeX + (dirX * 0.3f);
                float projSpawnY = slimeY + 0.1f;

                Vector2 horizontalTarget = new Vector2(projSpawnX + dirX, projSpawnY - 0.2f);

                // Spawn the "Mega" size projectile (true parameter)
                SandProjectile projectile = new SandProjectile(world, projSpawnX, projSpawnY, horizontalTarget, "LAVA", true);
                activeProjectiles.add(projectile);
            }
        } else {
            // Player is out of range, stop moving horizontally
            isWalking = false;
            b2body.setLinearVelocity(0, b2body.getLinearVelocity().y);
        }

        // --- Update Projectiles ---
        Iterator<SandProjectile> iter = activeProjectiles.iterator();
        while (iter.hasNext()) {
            SandProjectile proj = iter.next();
            proj.update(dt);
            if (proj.isDestroyed) {
                if (proj.b2body != null && world != null) {
                    world.destroyBody(proj.b2body);
                }
                proj.dispose();
                iter.remove();
            }
        }

        // --- Sand Displacement (Adjusted for larger size) ---
        if (sandMgr != null && sandMgr.sim != null) {
            int gridX = (int) ((slimeX * Main.PPM) / 4);
            int gridY = (int) ((slimeY * Main.PPM) / 4);
            int radius = 12 / 4; // Increased displacement radius to match larger hitbox

            for (int y = -radius; y <= radius; y++) {
                for (int x = -radius; x <= radius; x++) {
                    if (x*x + y*y < radius*radius) {
                        int px = gridX + x;
                        int py = gridY + y;
                        io.github.devsimulator.elements.Element e = sandMgr.sim.getElement(px, py);

                        if (e != null && !e.isStatic && !(e instanceof io.github.devsimulator.elements.EmptyCell)) {
                            if (sandMgr.sim.isEmpty(px, py + 1)) sandMgr.sim.moveElement(px, py, px, py + 1);
                            else if (sandMgr.sim.isEmpty(px+1, py)) sandMgr.sim.moveElement(px, py, px+1, py);
                            else if (sandMgr.sim.isEmpty(px-1, py)) sandMgr.sim.moveElement(px, py, px-1, py);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void draw(SpriteBatch batch) {
        if (!isAlive) return;

        // By locking the animation to 0.0f, the slime never plays its squish/jump frames.
        // It stays in its base shape, creating a "sliding" effect when it moves.
        TextureRegion currentFrame = slimeAnimation.getKeyFrame(0.0f);

        if (movingRight && currentFrame.isFlipX()) currentFrame.flip(true, false);
        if (!movingRight && !currentFrame.isFlipX()) currentFrame.flip(true, false);

        // Drawing bounds adjusted for the 1.5x larger graphic
        batch.draw(currentFrame,
            (b2body.getPosition().x * Main.PPM) - 37.5f,
            (b2body.getPosition().y * Main.PPM) - 46.8f,
            75f, 75f);

        for (SandProjectile proj : activeProjectiles) {
            proj.draw(batch);
        }
    }

    @Override
    public void resetState() {
        this.isAlive = true;
        this.hp = 50f;
        this.shootTimer = 0;
        this.isWalking = false;

        this.b2body.setActive(true);
        this.b2body.setTransform(spawnX, spawnY, 0);
        this.b2body.setLinearVelocity(0, 0);
        this.b2body.setAwake(true);

        for (SandProjectile proj : activeProjectiles) {
            if (proj.b2body != null) world.destroyBody(proj.b2body);
            proj.dispose();
        }
        activeProjectiles.clear();
    }

    @Override
    public void dispose() {
        if (sheet != null) sheet.dispose();
        if (slimeSound != null) slimeSound.dispose();
        if (shootSound != null) shootSound.dispose();
        for (SandProjectile proj : activeProjectiles) {
            proj.dispose();
        }
    }
}
