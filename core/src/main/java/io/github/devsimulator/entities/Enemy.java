package io.github.devsimulator.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.physics.box2d.*;
import io.github.devsimulator.Main;
import io.github.devsimulator.controllers.SandManager;

public abstract class Enemy {
    protected World world;

    // set to public so the World Listener would be able to access this
    public Body b2body;

    public float hp;
    public boolean isAlive = true;

    //initial coordinates
    public float spawnX;
    public float spawnY;

    public Enemy(World world, float x, float y) {
        this.world = world;
        this.hp = 50f;

        this.spawnX = x / Main.PPM;
        this.spawnY = y / Main.PPM;

        BodyDef bdef = new BodyDef();
        bdef.position.set(x / Main.PPM, y / Main.PPM);
        bdef.type = BodyDef.BodyType.DynamicBody;
        bdef.fixedRotation = true;
        b2body = world.createBody(bdef);
    }

    public void takeDamage(float damage, float knockbackDir) {
        if (!isAlive) return;
        this.hp -= damage;
        if (this.hp <= 0) {
            this.isAlive = false;
            this.b2body.setLinearVelocity(0, 0);
        } else {
            this.b2body.setLinearVelocity(0, this.b2body.getLinearVelocity().y);
            this.b2body.applyLinearImpulse(new com.badlogic.gdx.math.Vector2(knockbackDir, 3f), this.b2body.getWorldCenter(), true);
        }
    }

    // enemies will displace the terrain
    public abstract void update(float dt, SandManager sandMgr);
    public abstract void draw(SpriteBatch batch);
    public abstract void dispose();
    public abstract void resetState();
}
