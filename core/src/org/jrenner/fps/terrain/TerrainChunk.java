package org.jrenner.fps.terrain;

import org.jrenner.fps.Physics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

public class TerrainChunk implements Disposable{
	private static final BoundingBox box = new BoundingBox();
	public Vector3 position;
	public Vector3 dimensions;
	public float radius;
	
	public ModelInstance modelInstance;
	public btCollisionObject body;
	
	private Array<ModelInstance> sceneObjects;
	
	public TerrainChunk() {
		this.sceneObjects = new Array<ModelInstance>();
	}
	
	AnimationController con;
	public TerrainChunk(ModelInstance model, btCollisionObject obj) {
		this();
		this.modelInstance = model;
		this.body = obj;
		calculateBoundingBox();
		con = new AnimationController(modelInstance);
		//con.action("ocean|walk", 10, 1, null, 1);
		con.animate("grid|GridAction.001", -1, null, 2);
	}
	
	// Render a chunk
	public void render(ModelBatch batch, Environment env) {
		batch.render(modelInstance, env);
		con.update(Gdx.graphics.getDeltaTime());
		
		for (ModelInstance m : sceneObjects) {
			batch.render(m, env);
		}
	}
	
	public TerrainChunk setNormalMap(Texture normal) {
		modelInstance.materials.get(0).set(TextureAttribute.createNormal(normal));
		return this;
	}
	
	public TerrainChunk setSpecularColor(Color specular) {
		modelInstance.materials.get(0).set(ColorAttribute.createSpecular(specular));
		return this;
	}
	
	public TerrainChunk setModelInstance (ModelInstance modelInstance) {
		this.modelInstance = modelInstance;
		calculateBoundingBox();
		return this;
	}
	
	// TODO Add collision
	public TerrainChunk addStaticModel(Model model) {
		sceneObjects.add(new ModelInstance(model));
		return this;
	}
	
	// TODO Add collision
	public TerrainChunk addStaticModel(Model model, String node) {
		sceneObjects.add(new ModelInstance(model, node));
		return this;
	}

	private void calculateBoundingBox() {
		modelInstance.calculateTransforms();
		modelInstance.calculateBoundingBox(box);
		dimensions = new Vector3(box.getDimensions());
		radius = dimensions.len() / 2f;
		position = new Vector3(box.getCenter());
	}
	
	public boolean isVisible(Camera cam) {
		return cam.frustum.sphereInFrustum(position, radius);
	}
	
	@Override
	public void dispose () {
		Physics.inst.removeBody(body);
		// This might be bad...
		modelInstance.model.dispose();
	}
	
}
