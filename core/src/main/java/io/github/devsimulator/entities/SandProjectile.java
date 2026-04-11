package io.github.devsimulator.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
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
    private float maxLife = 5.0f;

    public String element;
    public boolean isMega;
    public int pierceCount = 1;

    public SandProjectile(World world, float x, float y, Vector2 target, String element, boolean isMega) {
        tex = new Texture("Comun_slime.png");
        this.element = element;
        this.isMega = isMega;

        int size = isMega ? 16 : 8;
        Pixmap pix = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        if(element.equals("WATER")) pix.setColor(0.2f, 0.6f, 1.0f, 1f);
        else if(element.equals("LAVA")) pix.setColor(1.0f, 0.4f, 0.0f, 1f);
        else pix.setColor(0.94f, 0.76f, 0.5f, 1f);

        pix.fillCircle(size/2, size/2, size/2);
        pix.setColor(Color.WHITE);
        pix.fillCircle(size/2, size/2, size/4);

        tex = new Texture(pix);
        pix.dispose();

        float dx = target.x - x;
        float dy = target.y - y;
        Vector2 dir = new Vector2(dx, dy).nor();

        BodyDef bdef = new BodyDef();
        bdef.position.set(x + (dir.x * 0.8f), y + (dir.y * 0.8f));
        bdef.type = BodyDef.BodyType.DynamicBody;
        b2body = world.createBody(bdef);

        CircleShape shape = new CircleShape();
        float radius = isMega ? 12f : 4f;
        shape.setRadius(radius / Main.PPM);

        FixtureDef fdef = new FixtureDef();
        fdef.shape = shape;
        fdef.density = isMega ? 2.0f : 0.5f;
        fdef.isSensor = false;
        fdef.friction = 0.8f;
        fdef.restitution = 0.1f;
        b2body.createFixture(fdef).setUserData(this);
        shape.dispose();

        float speed = isMega ? 8f : 12f;
        if(element.equals("WATER")) {
            speed = 22f;
            b2body.setGravityScale(0);
            pierceCount = isMega ? 99 : 2;
        } else if (element.equals("LAVA")) {
            b2body.setGravityScale(4.0f);
            speed = 2.5f;
            b2body.setAngularDamping(0.2f);
        } else {
            b2body.setGravityScale(4.5f);
            speed = 16f;
            dir.y += 0.4f;
            dir.nor();
        }

        b2body.setLinearVelocity(dir.scl(speed));
    }

    public void update(float dt) {
        lifeTimer += dt;
        if (lifeTimer >= maxLife) {
            isDestroyed = true;
        }
    }

    public void draw(SpriteBatch batch) {
        if (!isDestroyed) {
            float drawSize = isMega ? 24f : 12f;
            batch.draw(tex,
                (b2body.getPosition().x * Main.PPM) - drawSize / 2,
                (b2body.getPosition().y * Main.PPM) - drawSize / 2,
                drawSize / 2, drawSize / 2, // Origin for rotation (center)
                drawSize, drawSize,          // Width and Height
                1f, 1f,                      // Scale
                b2body.getAngle() * com.badlogic.gdx.math.MathUtils.radiansToDegrees, // Rotation!
                0, 0, tex.getWidth(), tex.getHeight(), false, false);
        }
    }

    public void dispose() {
        if (tex != null) tex.dispose();
    }
}
