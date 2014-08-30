
package org.jrenner.fps.terrain;

import org.jrenner.fps.Assets;
import org.jrenner.fps.Log;
import org.jrenner.fps.Main;
import org.jrenner.fps.Physics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btStaticPlaneShape;
import com.badlogic.gdx.utils.Array;

public class Terrain {
	public static Array<TerrainChunk> chunks;

	public static void init () {
		chunks = new Array<TerrainChunk>();
	}

	private static Matrix4 mtx = new Matrix4();

	public static void addChunk (TerrainChunk chunk, int x, int z) {
		mtx.setToTranslation(x, 0, z);
		chunk.modelInstance.transform.set(mtx);

		// Physics
		mtx.rotate(Vector3.X, -90);
		
		if (chunk.body != null)
			chunk.body.setWorldTransform(mtx);

		// Add chunk to list
		chunks.add(chunk);
	}

	// === Chunk creation === //

	/**
	 * Create a Terrain Chunk and use the Model for both visual and Collision
	 * @param model
	 * @return
	 */
	public static TerrainChunk CreateMeshChunk (Model model) {
		return CreateMeshChunk(model, model);
	}
	
	/**
	 * 
	 * @param model the model which is used for rendering
	 * @param collisionMesh a mesh used for collision
	 * @return
	 */
	public static TerrainChunk CreateMeshChunk (Model model, Model collisionMesh) {
		// Creates collision out the collision mesh
		btCollisionObject obj = new btCollisionObject();
		btCollisionShape shape = new btBvhTriangleMeshShape(collisionMesh.meshParts);
		obj.setCollisionShape(shape);
		Physics.applyStaticGeometryCollisionFlags(obj);
		Physics.inst.addStaticGeometryToWorld(obj);

		return new TerrainChunk(new ModelInstance(model), obj);
	}
	
	/**
	 * Use this for memory optimization and performance. It takes only one model which needs to have a collision
	 * mesh and a visual node
	 * 
	 * @param model
	 * @param visualNode node name of the visual mesh used for rendering
	 * @param collisionNode node name of the collision mesh
	 * @return
	 */
	public static TerrainChunk CreateMeshChunk (Model model, String visualNode, String collisionNode ) {
		Array<NodePart> nodes = model.getNode(collisionNode).parts;
		Array<MeshPart> parts = new Array<MeshPart>();
		
		for (NodePart part : nodes) {
			parts.add(part.meshPart);
		}
		
		// Creates collision out the collision mesh
		btCollisionObject obj = new btCollisionObject();
		btCollisionShape shape = new btBvhTriangleMeshShape(parts);
		obj.setCollisionShape(shape);
		Physics.applyStaticGeometryCollisionFlags(obj);
		Physics.inst.addStaticGeometryToWorld(obj);

		return new TerrainChunk(new ModelInstance(model, visualNode), obj);
	}

	/**
	 * Return a heightmap chunk
	 * TODO NOT IMPLEMENTED! 
	 * @param heightmap
	 * @param tex
	 * @param width
	 * @param height
	 * @return
	 */
	public static TerrainChunk CreateHeightMapChunk (FileHandle heightmap, Texture tex, int width, int height) {
		HeightMap hp = new HeightMap(heightmap, 10, 10, false, 3);
		
		HeightMapModel md = new HeightMapModel(hp);
		//Model model = md.ground;
		 md.ground.materials.get(0).set(TextureAttribute.createDiffuse(tex));
		 
		//btCollisionObject obj = new btCollisionObject();
		//btCollisionShape shape = new btBvhTriangleMeshShape(model.meshParts);
		//obj.setCollisionShape(shape);
		////Physics.applyStaticGeometryCollisionFlags(obj);
		//Physics.inst.addStaticGeometryToWorld(obj);
		TerrainChunk ch = new  TerrainChunk();
		ch.setModelInstance(md.ground);
		
		return ch;
	}

	/**
	 * Creates a plane, used for testing
	 * @param size the size of the plane
	 * @return
	 */
	public static TerrainChunk CreatePlaneChunk (float size) {
		TerrainChunk chunk = new TerrainChunk();
		// graphical representation of the ground
		Log.debug("createLevel - create ground");
		if (Main.isClient()) {
			ModelBuilder mb = new ModelBuilder();
			mb.begin();
			Vector3 bl = new Vector3();
			Vector3 tl = new Vector3();
			Vector3 tr = new Vector3();
			Vector3 br = new Vector3();
			Vector3 norm = new Vector3(0f, 1f, 0f);
			// the size of each rect that makes up the ground
			Texture groundTex = Assets.manager.get("textures/ground1.jpg", Texture.class);
			Material groundMat = new Material(TextureAttribute.createDiffuse(groundTex));
			MeshPartBuilder mpb = mb.part("ground", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal | Usage.TextureCoordinates,
				groundMat);
			float u1 = 0f;
			float v1 = 0f;
			float u2 = size / 5f;
			float v2 = size / 5f;
			mpb.setUVRange(u1, v1, u2, v2);
			bl.set(0, 0, 0);
			tl.set(0, 0, size);
			tr.set(size, 0, size);
			br.set(size, 0, 0);
			// mpb.rect(bl, tl, tr, br, norm);
			int divisions = ((int)size) / 4;
			mpb.patch(bl, tl, tr, br, norm, divisions, divisions);
			Model groundModel = mb.end();
			chunk.modelInstance = new ModelInstance(groundModel);
		}

		// physical representation of the ground
		btCollisionObject groundObj = new btCollisionObject();
		btCollisionShape groundShape = new btStaticPlaneShape(Vector3.Y, 0f);
		groundObj.setCollisionShape(groundShape);
		Physics.applyStaticGeometryCollisionFlags(groundObj);
		Physics.inst.addStaticGeometryToWorld(groundObj);

		chunk.body = groundObj;
		return chunk;
	}

}
