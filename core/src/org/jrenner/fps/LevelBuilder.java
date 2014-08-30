package org.jrenner.fps;

import org.jrenner.fps.headless.HeadlessModel;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.utils.Array;

public class LevelBuilder {
	public static LevelBuilder inst;
	private static Array<Model> models;
	public static Array<LevelStatic> staticPieces; // server only: stores level geometry for sending to clients on connect

	private static ModelBuilder mb;

	public static void init() {
		staticPieces = new Array<>();
		mb = new ModelBuilder();
		models = new Array<>();
	}

	public LevelBuilder() {
		inst = this;
	}

	public static Array<ModelInstance> staticGeometry;

	public static void createLevel() {
		//Terrain.createPlaneTerrain();
		TerrainChunk chunk = Terrain.createMeshTerrain(Assets.manager.get("models/terrain/terrain.g3db", Model.class));
		Terrain.setChunk(chunk, 0, 0, null);
		
		if (Main.isServer()) {
			// server creates static models here, client will create the models when received from server upon connection
			createStaticModels();
		}
	}

	/** client builds statics, probably based on info from server */
	public static void buildStatics(LevelStatic[] statics) {
		if (staticGeometry == null) {
			staticGeometry = new Array<>();
		}
		Log.debug("client building statics received from server: " + statics.length);
		ModelBuilder mb = new ModelBuilder();
		mb.begin();
		for (LevelStatic stat : statics) {
			Model model = Assets.manager.get(stat.modelName, Model.class);
			setupStaticModel(model.meshParts, stat.mtx, true);
			Node node = mb.node("piece", model);
			stat.mtx.getTranslation(tmp);
			node.translation.set(tmp);
			node.rotation.set(stat.mtx.getRotation(q));
		}
		Model finalModel = mb.end();
		ModelInstance instance = new ModelInstance(finalModel);
		staticGeometry.add(instance);
	}

	private static Matrix4 mtx = new Matrix4();
	private static Vector3 tmp = new Vector3();
	private static Quaternion q = new Quaternion();

	/** bullet bodies are offset from model instances by a 90 degree rotation on X axis, boolean set to true handles this */
	private static void setupStaticModel(Array<MeshPart> meshParts, Matrix4 matrix, boolean performVisualToPhysicalRotation) {
		//Log.debug("create static model at: " + matrix.getTranslation(tmp));
		btCollisionObject obj = new btCollisionObject();
		btCollisionShape shape = new btBvhTriangleMeshShape(meshParts);
		obj.setCollisionShape(shape);
		Physics.applyStaticGeometryCollisionFlags(obj);
		mtx.set(matrix);
		if (performVisualToPhysicalRotation) {
			mtx.rotate(Vector3.X, -90);
		}
		obj.setWorldTransform(mtx);
		Physics.inst.addStaticGeometryToWorld(obj);
	}

	private static void createStaticModels() {
		staticGeometry = new Array<>();
		if (Main.isClient()) {
			mb = new ModelBuilder();
			mb.begin();
		}
		Array<String> modelChoices = new Array<>();
		modelChoices.addAll("models/gate.g3db", "models/strange-ramp2.g3db");
		//Array<Model> modelChoices = new Array<>();
		//modelChoices.add(Assets.manager.get("gate.g3db", Model.class));
		//modelChoices.add(Assets.manager.get("strange-ramp1.g3db", Model.class));
		//modelChoices.add(Assets.manager.get("strange-ramp2.g3db", Model.class));
		Quaternion quat = new Quaternion();
		Matrix4 mtx = new Matrix4();
		Vector3 translation = new Vector3();
		//float lo = 10f;
		//float hi = GameWorld.WORLD_WIDTH;
		float x = 20f;
		float z = 0f;
		int numOfStaticObjects = 20;
		for (int i = 0; i < numOfStaticObjects; i++) {
			x = MathUtils.random(10f, 200f);
			z = MathUtils.random(10f, 200f);
			quat.setEulerAngles(MathUtils.random(360f), MathUtils.random(360f), MathUtils.random(360f));
			String modelName = modelChoices.random();
			// bullet builds its physics shape using meshparts
			Array<MeshPart> meshParts = null;
			Node node = null;
			if (Main.isClient()) {
				Model model = Assets.manager.get(modelName, Model.class);
				meshParts = model.meshParts;
				node = mb.node("thing", model);
			} else { // the regular Model class requires a gl context, doesn't work on headless
				HeadlessModel model = Assets.manager.get(modelName, HeadlessModel.class);
				meshParts = model.meshParts;
			}
			translation.set(x, 0f, z);
			mtx.set(quat);
			mtx.setTranslation(translation);
			setupStaticModel(meshParts, mtx, true);
			if (Main.isServer()) {
				LevelStatic levelStatic = new LevelStatic();
				levelStatic.modelName = modelName;
				levelStatic.mtx.set(mtx);
				staticPieces.add(levelStatic);
			}
			if (Main.isClient()) {
				node.translation.set(translation);
				node.rotation.set(quat);
			}
		}
		if (Main.isClient()) {
			Model model = mb.end();
			ModelInstance gate = new ModelInstance(model);
			staticGeometry.add(gate);
		}
	}

	public static void dispose() {
		for (Model model : models) {
			Tools.dispose(model);
		}
		models.clear();
	}
}
