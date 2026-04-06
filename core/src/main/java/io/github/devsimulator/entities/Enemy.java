package io.github.devsimulator.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import io.github.devsimulator.Main;
import io.github.devsimulator.controllers.SandManager;
import io.github.devsimulator.elements.*;
import io.github.devsimulator.helper.PhysicSim;
import io.github.devsimulator.helper.WorldContactListener;
import io.github.devsimulator.helper.tilemapmanager;
import io.github.devsimulator.helper.tilemapmanager.RuneData;

public abstract class Enemy {
    protected World world;
    public Body b2body;
    public float hp;
    protected boolean isAlive = true;

    public Enemy(World world, float x, float y) {
        this.world = world;
        this.hp = 50f;

        // Just create the body here. Let the child  classes create the SHAPE.
        BodyDef bdef = new BodyDef();
        bdef.position.set(x / Main.PPM, y / Main.PPM);
        bdef.type = BodyDef.BodyType.DynamicBody;
        bdef.fixedRotation = true;
        b2body = world.createBody(bdef);
    }

    public abstract void update(float dt);
    public abstract void draw(SpriteBatch batch);
}
