package org.jrenner.fps.graphics;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import org.jrenner.fps.entity.Entity;

/** testing */
public class EntityBillboard extends EntityModel {

	public EntityBillboard(Entity entity) {
		super(entity);
	}

	@Override
	protected void createModelInstance() {
		modelInstance = new ModelInstance(ModelManager.inst.getBillboardModel());
	}
}
