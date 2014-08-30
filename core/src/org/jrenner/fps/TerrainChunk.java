package org.jrenner.fps;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

public class TerrainChunk implements Disposable{
	public ModelInstance modelInstance;
	public btCollisionObject body;
	
	private Array<ModelInstance> sceneObjects;
	
	// Render a chunk
	public void render(ModelBatch batch, Environment env) {
		batch.render(modelInstance, env);
		
		for (ModelInstance m : sceneObjects) {
			batch.render(m, env);
		}
	}
	
	public void setNormalMap(Texture normal) {
		modelInstance.materials.get(0).set(TextureAttribute.createNormal(normal));
	}
	
	public void setSpecularColor(Color specular) {
		modelInstance.materials.get(0).set(ColorAttribute.createSpecular(specular));
	}
	
	@Override
	public void dispose () {
		Physics.inst.removeBody(body);
	}
	
}
