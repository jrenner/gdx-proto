package org.jrenner.fps.graphics;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import org.jrenner.fps.entity.Entity;

/** 3d model graphical representation of an Entity */
public class EntityModel {
	public static Array<EntityModel> list;
	private Entity entity;
	public ModelInstance modelInstance;

	private static Matrix4 mtx = new Matrix4();

	public EntityModel(Entity entity) {
		this.entity = entity;
		modelInstance = new ModelInstance(ModelManager.inst.getPlayerModel());
		list.add(this);
	}

	public void update() {
		mtx.set(entity.getRotation());
		mtx.setTranslation(entity.getPosition());
		modelInstance.transform.set(mtx);
	}
}
