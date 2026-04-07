package io.github.devsimulator.entities;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.physics.box2d.*;
import io.github.devsimulator.Main;

public class ItemDrop {
    public Body b2body;
    public String element;
    public boolean isDestroyed = false;
    private Texture tex;

    public ItemDrop(World world, float x, float y, String element) {
        this.element = element;
        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        if(element.equals("WATER")) pix.setColor(0.2f, 0.6f, 1.0f, 1f);
        else if(element.equals("LAVA")) pix.setColor(1.0f, 0.4f, 0.0f, 1f);
        else pix.setColor(0.94f, 0.76f, 0.5f, 1f);
        pix.fill();
        tex = new Texture(pix);
        pix.dispose();

        BodyDef bdef = new BodyDef();
        bdef.position.set(x, y);
        bdef.type = BodyDef.BodyType.DynamicBody;
        b2body = world.createBody(bdef);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(4 / Main.PPM, 4 / Main.PPM);

        FixtureDef fdef = new FixtureDef();
        fdef.shape = shape;
        fdef.density = 1f;
        fdef.restitution = 0.6f; // make the item bounce
        fdef.isSensor = false;

        b2body.createFixture(fdef).setUserData(this);
        shape.dispose();
        b2body.applyLinearImpulse(new com.badlogic.gdx.math.Vector2((float)(Math.random()*2-1), 4f), b2body.getWorldCenter(), true);
    }

    public void draw(SpriteBatch batch) {
        batch.draw(tex, (b2body.getPosition().x * Main.PPM) - 4, (b2body.getPosition().y * Main.PPM) - 4, 8, 8);
    }

    public void dispose() {
        if(tex != null) tex.dispose();
    }
}
