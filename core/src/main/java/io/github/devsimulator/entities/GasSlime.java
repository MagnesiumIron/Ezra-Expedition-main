package io.github.devsimulator.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import io.github.devsimulator.Main;
import io.github.devsimulator.controllers.SandManager;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;

public class GasSlime extends Enemy {
    private Texture sheet;
    private Animation<TextureRegion> slimeAnimation;

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

        sheet = new Texture("gas_slime.png");
        sheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        Array<TextureRegion> frames = new Array<>();
        frames.add(new TextureRegion(sheet, 0, 0, 32, 32));
        frames.add(new TextureRegion(sheet, 32, 0, 32, 32));
        frames.add(new TextureRegion(sheet, 64, 0, 32, 32));
        slimeAnimation = new Animation<>(0.15f, frames);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(8 / Main.PPM, 8 / Main.PPM);

        FixtureDef fdef = new FixtureDef();
        fdef.shape = shape;
        fdef.density = 0.8f;
        fdef.friction = 0.5f;
        fdef.restitution = 0.1f;

        b2body.createFixture(fdef).setUserData(this);
        b2body.setLinearDamping(1.5f);
        shape.dispose();
    }

    @Override
    public void update(float dt, SandManager sandMgr) {
        if (!isAlive || health <= 0) return;

        if (b2body.getLinearVelocity().y < -0.1f) {
            b2body.setGravityScale(0.3f);
        } else {
            b2body.setGravityScale(1.0f);
        }

        boolean isGrounded = Math.abs(b2body.getLinearVelocity().y) < 0.01f;

        if (isGrounded) {
            timer += dt;
            if (isJumping) isJumping = false;

            float slimeX = b2body.getPosition().x;
            float playerX = player.b2body.getPosition().x;

            if (timer < (jumpInterval - anticipationTime)) {
                movingRight = (playerX < slimeX);
            }

            if (timer >= jumpInterval) {
                float forceX = movingRight ? -jumpForceX : jumpForceX;
                b2body.applyLinearImpulse(new Vector2(forceX, jumpForceY), b2body.getWorldCenter(), true);
                isJumping = true;
                timer = 0;
            }
        } else {
            timer = 0;
        }

        // Sand displacement
        if (sandMgr != null && sandMgr.sim != null) {
            int gridX = (int) ((b2body.getPosition().x * Main.PPM) / 4);
            int gridY = (int) ((b2body.getPosition().y * Main.PPM) / 4);
            int radius = 8 / 4;

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

        boolean isSquishing = timer >= (jumpInterval - anticipationTime);
        TextureRegion currentFrame;

        if (isJumping || Math.abs(b2body.getLinearVelocity().y) > 0.1f) {
            currentFrame = slimeAnimation.getKeyFrame(0.35f);
        } else if (isSquishing) {
            currentFrame = slimeAnimation.getKeyFrame(0.16f);
        } else {
            currentFrame = slimeAnimation.getKeyFrame(0.0f);
        }

        if (movingRight && !currentFrame.isFlipX()) currentFrame.flip(true, false);
        if (!movingRight && currentFrame.isFlipX()) currentFrame.flip(true, false);

        batch.draw(currentFrame,
            (b2body.getPosition().x * Main.PPM) - 25f,
            (b2body.getPosition().y * Main.PPM) - 31f,
            50f, 50f);
    }

    @Override
    public void resetState() {
        this.isAlive = true;
        this.hp = 50f;
        this.timer = 0;
        this.isJumping = false;

        this.b2body.setActive(true);
        this.b2body.setTransform(spawnX, spawnY, 0);
        this.b2body.setLinearVelocity(0, 0);
        this.b2body.setAwake(true);
    }

    @Override
    public void dispose() {
        if (sheet != null) sheet.dispose();
    }
}
