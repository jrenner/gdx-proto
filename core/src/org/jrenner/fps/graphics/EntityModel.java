package org.jrenner.fps.graphics;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import org.jrenner.fps.Main;
import org.jrenner.fps.entity.Entity;

/** 3d model graphical representation of an Entity */
public class EntityModel {
	public static Array<EntityModel> list;
	public Entity entity;
	public ModelInstance modelInstance;

	protected static Matrix4 mtx = new Matrix4();

	public EntityModel(Entity entity) {
		this.entity = entity;
		list.add(this);
		createModelInstance();
	}

	protected void createModelInstance() {
		modelInstance = new ModelInstance(ModelManager.inst.getPlayerModel());
	}

	public void update() {
		mtx.set(entity.getRotation());
		mtx.setTranslation(entity.getPosition());
		modelInstance.transform.set(mtx);
	}

	/** don't draw the client's model (don't see yourself) */
	public boolean isClientEntity() {
		if (!Main.isClient() || Main.inst.client.player == null) {
			return false;
		} else {
			return entity == Main.inst.client.player.entity;
		}
	}
}
