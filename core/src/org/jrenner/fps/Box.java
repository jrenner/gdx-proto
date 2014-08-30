package org.jrenner.fps;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.utils.Array;

import static com.badlogic.gdx.graphics.VertexAttributes.Usage;

public class Box {
	public static Array<Box> list;
	public static ModelInstance instance;

	private static Vector3 tmp = new Vector3();
	private static Matrix4 mtx = new Matrix4();
	private static Quaternion q = new Quaternion();


	/** create some boxes to fill the level with some test geometry */
	public static void createBoxes(int count) {
		ModelBuilder main = new ModelBuilder();
		ModelBuilder mb = new ModelBuilder();
		Material material = new Material();
		if (Main.isClient()) {
			material.set(TextureAttribute.createDiffuse(Assets.manager.get("textures/marble.jpg", Texture.class)));
		}
		main.begin();
		//float x = GameWorld.WORLD_WIDTH;
		//float y = GameWorld.WORLD_DEPTH;
		for (int i = 0; i < count; i++) {
			//float w = MathUtils.random(minW, maxW);
			float w = 8f;
			float d = 8f;
			float h = (i+1)*5f;
			tmp.set(10f + (w+2) * i, 0f, 10f + (d+2) * i);
			if (Main.isClient()) {
				mb.begin();
				MeshPartBuilder mpb = mb.part("part-" + i, GL20.GL_TRIANGLES, Usage.Position | Usage.Normal | Usage.TextureCoordinates, material);
				mpb.box(w, h, d);
				Model boxModel = mb.end();
				Node node = main.node("box-" + i, boxModel);
				node.translation.set(tmp);
				q.idt();
				node.rotation.set(q);
			}
			//node.translation.set(MathUtils.random(x), 0f, MathUtils.random(y));
			//q.set(Vector3.X, -90);
			mtx.set(q);
			mtx.setTranslation(tmp);
			btCollisionObject obj = Physics.inst.createBoxObject(tmp.set(w/2, h/2, d/2));
			obj.setWorldTransform(mtx);
			Physics.applyStaticGeometryCollisionFlags(obj);
			Physics.inst.addStaticGeometryToWorld(obj);
		}
		Model finalModel = main.end();
		instance = new ModelInstance(finalModel);
	}
}
