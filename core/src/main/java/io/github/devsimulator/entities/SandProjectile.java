package io.github.devsimulator.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import io.github.devsimulator.Main;

public class SandProjectile {
    public Body b2body;
    private Texture tex;
    public boolean isDestroyed = false;
    private float lifeTimer = 0;
    private float maxLife = 2.0f; // Destroys itself after 2 seconds

    public SandProjectile(World world, float x, float y, Vector2 target) {
        // Use the placeholder texture you provided
        tex = new Texture("slime_ball.png");

        BodyDef bdef = new BodyDef();
        bdef.position.set(x, y);
        bdef.type = BodyDef.BodyType.DynamicBody;
        b2body = world.createBody(bdef);

        CircleShape shape = new CircleShape();
        shape.setRadius(4 / Main.PPM);

        FixtureDef fdef = new FixtureDef();
        fdef.shape = shape;
        fdef.density = 0.5f;
        fdef.isSensor = false; // Makes it pass through walls/enemies, acting only as a hit detector

        b2body.createFixture(fdef).setUserData(this);
        shape.dispose();

        // 3. THE AIMING MATH (This replaces the old shootLeft logic)
        float dx = target.x - x; // Distance on X
        float dy = target.y - y;

        // We want a little arc, so we give it a constant upward boost (1.5f)
        // and a horizontal boost based on how far Ezra is (dx * 0.5f)
        float forceX = dx * 0.05f;
        float forceY = 0.01f;

        b2body.applyLinearImpulse(new Vector2(forceX, forceY), b2body.getWorldCenter(), true);

        // Make it feel like heavy sand
        b2body.setGravityScale(1.8f);
    }

    public void update(float dt) {
        lifeTimer += dt;
        if (lifeTimer >= maxLife) {
            isDestroyed = true;
        }
    }

    public void draw(SpriteBatch batch) {
        if (!isDestroyed) {
            batch.draw(tex, (b2body.getPosition().x * Main.PPM) - 8, (b2body.getPosition().y * Main.PPM) - 8, 16, 16);
        }
    }

    public void dispose() {
        tex.dispose();
    }
}
