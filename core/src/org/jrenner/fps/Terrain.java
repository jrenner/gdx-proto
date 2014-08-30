
package org.jrenner.fps;

import org.jrenner.fps.entity.Entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btStaticPlaneShape;
import com.badlogic.gdx.utils.Array;

public class Terrain {
	// public static Model model;
	private static Matrix4 matrix = new Matrix4();

	public static TerrainChunk[] chunks;

	private static int chunksSqrt;
	private static int chunkNum;
	

	public static void init () {
		int num = 9;

		if ((num & 1) == 0 || ((int)Math.sqrt(num) & 1) == 0) {
			try {
				throw new Exception("Number must be odd and have a odd square root");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			chunks = new TerrainChunk[9]; // 1, 9, 25
			chunkNum = num;
			chunksSqrt = (int)Math.sqrt(chunkNum);
		}
	}

	public static int getEntitysOffsetFromZero (Entity entity) {
		for (TerrainChunk chunk : chunks) {
			if (chunk.boundingBox.contains(entity.getPosition())) {

			}
		}
		return 1;
	}

	public static void setChunk (TerrainChunk chnk, int u, int v, Vector3 offset) {
		int index = (u + (chunksSqrt-1)/2) + ((chunkNum-chunksSqrt) - ((v + (chunksSqrt-1)/2) * chunksSqrt));
		chunks[index] = chnk;
	}

	public static TerrainChunk getChunk (int u, int v) {
		return chunks[(u + (chunksSqrt-1)/2) + ((chunkNum-chunksSqrt) - ((v + (chunksSqrt-1)/2) * chunksSqrt))];
	}

	public static void deleteChunk () {

	}

	public static void translate (float u, float v) { // Translate the chunks, note this will delete some
		
	}

	public static TerrainChunk createMeshTerrain (Model terrain) {
		// physical representation of the ground
		btCollisionObject obj = new btCollisionObject();

		btCollisionShape shape = new btBvhTriangleMeshShape(terrain.meshParts);// TODO load from ModelInstance aswell
		obj.setCollisionShape(shape);
		Physics.applyStaticGeometryCollisionFlags(obj);

		// matrix.setTranslation(0, 0, 0);
		matrix.rotate(Vector3.X, -90);
		obj.setWorldTransform(matrix);
		Physics.inst.addStaticGeometryToWorld(obj);

		return new TerrainChunk(terrain);
	}

	public static void createPlaneTerrain () {
		// graphical representation of the ground
		if (Main.isClient()) {
			Model groundModel = Assets.manager.get("models/ground.g3db", Model.class);
			ModelBuilder mb = new ModelBuilder();
			mb.begin();
			MeshPartBuilder mpb = mb.part("first", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal | Usage.TextureCoordinates,
				groundModel.materials.first());
			Vector3 bl = new Vector3();
			Vector3 tl = new Vector3();
			Vector3 tr = new Vector3();
			Vector3 br = new Vector3();
			Vector3 norm = new Vector3(0f, 1f, 0f);
			int d = 4;
			for (int x = -100; x < GameWorld.WORLD_WIDTH; x += d) {
				if (x % 20 == 0) {
					mpb = mb.part("section-" + x, GL20.GL_TRIANGLES, Usage.Position | Usage.Normal | Usage.TextureCoordinates,
						groundModel.materials.first());
				}
				for (int z = -100; z < GameWorld.WORLD_DEPTH; z += d) {
					bl.set(x, 0, z);
					tl.set(x, 0, z + d);
					tr.set(x + d, 0, z + d);
					br.set(x + d, 0, z);
					mpb.rect(bl, tl, tr, br, norm);
				}
			}
			Model finalModel = mb.end();
			// TODO modelInstance = new ModelInstance(finalModel);
		}

		// physical representation of the ground
		btCollisionObject groundObj = new btCollisionObject();
		btCollisionShape groundShape = new btStaticPlaneShape(Vector3.Y, 0f);
		groundObj.setCollisionShape(groundShape);
		Physics.applyStaticGeometryCollisionFlags(groundObj);
		Physics.inst.addStaticGeometryToWorld(groundObj);
	}

	public static void createHeightmapTerrain (Texture heightmap, Texture texure) {

		btCollisionObject obj = new btCollisionObject();
		Mesh mesh = null;
		Array<MeshPart> parts = new Array<MeshPart>();
		parts.add(new MeshPart(null, mesh, 0, mesh.getNumVertices(), Gdx.gl.GL_TRIANGLES));
		btCollisionShape shape = new btBvhTriangleMeshShape(parts);
		obj.setCollisionShape(shape);
		Physics.applyStaticGeometryCollisionFlags(obj);

		// matrix.setTranslation(0, 0, 0);
		matrix.rotate(Vector3.X, -90);
		obj.setWorldTransform(matrix);
		Physics.inst.addStaticGeometryToWorld(obj);

	}

}
