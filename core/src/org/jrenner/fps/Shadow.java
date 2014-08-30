package org.jrenner.fps;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/** blob shadow */
public class Shadow {
	private static Model model;
	public ModelInstance modelInstance;
	public static Array<Shadow> list;

	public static void init() {
		list = new Array<>();
		ModelBuilder mb = new ModelBuilder();
		Vector3 norm = new Vector3(0f, 1f, 0f);
		Texture texture = Assets.manager.get("textures/shadow.png", Texture.class);
		Material material = new Material(TextureAttribute.createDiffuse(texture));
		material.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 0.7f));
		//material.set(new DepthTestAttribute(0)); // disable depth testing
		long attr = Usage.Position | Usage.TextureCoordinates;
		float s = 1f;
		model = mb.createRect(
				-s, 0f, -s,// bl
				-s, 0f, s, // tl
				s, 0f, s,  // tr
				s, 0f, -s,  // br
				norm.x, norm.y, norm.z,
				material,
				attr
		);
	}

	public Shadow(float size) {
		modelInstance = new ModelInstance(model);
		modelInstance.transform.scale(size, 1f, size);
		list.add(this);
	}

	public static final float MAX_SHADOW_HEIGHT = 100f;

	private float heightOffset = MathUtils.random(0.01f, 0.03f);

	private static Vector3 tmp = new Vector3();
	
	public void update(Vector3 position) {
		float height = Physics.inst.getShadowHeightAboveGround(position) - heightOffset;
		tmp.set(position.x, position.y - height, position.z);
		modelInstance.transform.setToTranslation(tmp);
		modelInstance.transform.rotate(Vector3.Y, Physics.inst.raycastReport.hitNormal);
	}

	public void removeFromGame() {
		list.removeValue(this, true);
	}
}
