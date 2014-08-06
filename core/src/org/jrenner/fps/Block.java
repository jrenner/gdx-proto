package org.jrenner.fps;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.utils.Array;

public class Block {
	public static Array<Block> blocks;
	ModelInstance instance;
	btCollisionObject body;
	private Vector3 dimen = new Vector3();
	private boolean dynamic;

	public static void init() {
		Block.blocks = new Array<>();
	}

	public Block(ModelInstance instance, int width, int height, int depth, boolean dynamic) {
		this.dynamic = dynamic;
		this.instance = instance;
		dimen.set(width, height, depth);
		tmp.set(dimen).scl(0.5f);
		body = Physics.inst.createBoxObject(tmp);
		Physics.applyStaticGeometryCollisionFlags(body);
		mtx.set(instance.transform);
		mtx.getTranslation(translation);
		translation.add(tmp);
		mtx.setToTranslation(translation);
		body.setWorldTransform(mtx);
		blocks.add(this);
		if (dynamic) {
			Physics.inst.addDynamicEntityToWorld(body);
		} else {
			Physics.inst.addStaticGeometryToWorld(body);
		}
	}

	private static Vector3 tmp = new Vector3();
	private static Vector3 translation = new Vector3();
	private static Matrix4 mtx = new Matrix4();

	public static void updateAll() {
		for (Block block : blocks) {
			block.update();
		}
	}

	public void update() {
		if (!dynamic) return;
		mtx.set(body.getWorldTransform());
		mtx.getTranslation(translation);
		translation.sub(tmp.set(dimen).scl(0.5f));
		mtx.setToTranslation(translation);
		instance.transform.setTranslation(translation);
	}
}
