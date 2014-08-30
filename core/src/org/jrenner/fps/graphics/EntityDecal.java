package org.jrenner.fps.graphics;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import org.jrenner.fps.Assets;
import org.jrenner.fps.View;
import org.jrenner.fps.entity.Entity;

public class EntityDecal {
	public static Array<EntityDecal> list;
	private Entity entity;
	public Decal decal;

	public EntityDecal(Entity entity) {
		this.entity = entity;
		TextureAtlas atlas = Assets.manager.get("texture-packs/monsters.atlas", TextureAtlas.class);
		TextureRegion texReg = atlas.getRegions().random();
		float w = 2f;
		float h = 2f;
		decal = Decal.newDecal(w, h, texReg, true);
		list.add(this);
	}

	private static Vector3 tmp = new Vector3();

	public void update() {
		decal.setPosition(entity.getPosition());
		tmp.set(View.inst.getCamera().position);
		tmp.y = entity.getPosition().y;
		decal.lookAt(tmp, View.inst.getCamera().up);
	}
}