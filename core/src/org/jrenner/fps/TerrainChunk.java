package org.jrenner.fps;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;

public class TerrainChunk {
	public ModelInstance modelInstance;
	public BoundingBox boundingBox;
	
	private TerrainChunkCallback callback;
	
	public TerrainChunk(Model model) {
		modelInstance = new ModelInstance(model);
		boundingBox = new BoundingBox();
	}
	
	public void setPosition(float x, float y, float z) {
		modelInstance.transform.setTranslation(x, y, z);
		modelInstance.calculateBoundingBox(boundingBox);
	}
	
	public void setPosition(Vector3 position) {
		modelInstance.transform.setTranslation(position);
		modelInstance.calculateBoundingBox(boundingBox);
	}
	
	public void setCallback (TerrainChunkCallback callback) {
		this.callback = callback;
	}
	
	public static interface TerrainChunkCallback {
		void onOk(TerrainChunk chunk);
		
		void onDestory(TerrainChunk chunk);
	}
	
}
