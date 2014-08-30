
package org.jrenner.fps;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.math.Vector3;

/**
 * @author Caresi Labs
 *	
 *	A static Skybox class
 * Only one Sky instance should be present
 * 
 */

public class Sky {
	private static Model model;
	
	//TODO Entity list for things like Clouds, and future weather system
	//public static Array<ModelInstance> skyEntities;
	public static ModelInstance modelInstance;
	
	private static boolean isEnabled;

	public static void init () {
		isEnabled = false;
		
		// Load managed model
		model = Assets.manager.get("models/skybox.g3db", Model.class);
	}

	public static void createSkyBox (Texture xpos, Texture xneg, Texture ypos, Texture yneg, Texture zpos, Texture zneg) {
		modelInstance = new ModelInstance(model, "Skycube");
		
		// Set material textures
		modelInstance.materials.get(0).set(TextureAttribute.createDiffuse(xpos));
		modelInstance.materials.get(1).set(TextureAttribute.createDiffuse(xneg));
		modelInstance.materials.get(2).set(TextureAttribute.createDiffuse(ypos));
		modelInstance.materials.get(3).set(TextureAttribute.createDiffuse(yneg));
		modelInstance.materials.get(5).set(TextureAttribute.createDiffuse(zpos));
		modelInstance.materials.get(4).set(TextureAttribute.createDiffuse(zneg));
		
		//Disable depth test
		modelInstance.materials.get(0).set(new DepthTestAttribute(0));
		modelInstance.materials.get(1).set(new DepthTestAttribute(0));
		modelInstance.materials.get(2).set(new DepthTestAttribute(0));
		modelInstance.materials.get(3).set(new DepthTestAttribute(0));
		modelInstance.materials.get(4).set(new DepthTestAttribute(0));
		modelInstance.materials.get(5).set(new DepthTestAttribute(0));
		
		isEnabled = true;
	}

	public static void createSkyBox (Texture skybox) {
		modelInstance = new ModelInstance(model, "Skybox");
		
		// Set material texutres and Disable depth test
		modelInstance.materials.get(0).set(TextureAttribute.createDiffuse(skybox));
		modelInstance.materials.get(0).set(new DepthTestAttribute(0));
		
		isEnabled = true;
	}

	public static void createSkySphere () {
		throw new NotImplementedException();
	}

	private static final Vector3 tmp = new Vector3();

	public static void update (Vector3 position) {
		tmp.set(position.x, position.y, position.z);
		modelInstance.transform.setToTranslation(tmp);
	}

	public static void disable () {
		//TODO Make this a little bit nicer?
		modelInstance = null;
		isEnabled = false;
	}
	
	public static boolean isEnabled () {
		return isEnabled;
	}
}
