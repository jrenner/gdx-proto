package org.jrenner.fps;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btStaticPlaneShape;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import org.jrenner.fps.headless.HeadlessModel;

import static com.badlogic.gdx.graphics.VertexAttributes.Usage;
import static com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo;

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

	public static ModelInstance ground;

	public static void createLevel() {
		// graphical representation of the ground
		Log.debug("createLevel - create ground");
		if (Main.isClient()) {
			Model groundModel = Assets.manager.get("models/ground.g3db", Model.class);
			ModelBuilder mb = new ModelBuilder();
			mb.begin();
			MeshPartBuilder mpb = mb.part("first", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal | Usage.TextureCoordinates, groundModel.materials.first());
			Vector3 bl = new Vector3();
			Vector3 tl = new Vector3();
			Vector3 tr = new Vector3();
			Vector3 br = new Vector3();
			Vector3 norm = new Vector3(0f, 1f, 0f);
			// the size of each rect that makes up the ground
			int rectSize = 25;
			int rectCount = 0;
			for (int x = -100; x < GameWorld.WORLD_WIDTH; x += rectSize) {
				if (x % 20 == 0) {
					mpb = mb.part("section-" + x, GL20.GL_TRIANGLES, Usage.Position | Usage.Normal | Usage.TextureCoordinates, groundModel.materials.first());
					float u1 = 0f;
					float v1 = 0f;
					float u2 = rectSize / 5f;
					float v2 = rectSize / 5f;
					mpb.setUVRange(u1, v1, u2, v2);
				}
				for (int z = -100; z < GameWorld.WORLD_DEPTH; z += rectSize) {
					rectCount++;
					bl.set(x, 0, z);
					tl.set(x, 0, z + rectSize);
					tr.set(x + rectSize, 0, z + rectSize);
					br.set(x + rectSize, 0, z);
					mpb.rect(bl, tl, tr, br, norm);
				}
			}
			Log.debug("createLevel - created ground, rect count: " + rectCount);
			Model finalModel = mb.end();
			ground = new ModelInstance(finalModel);
		}

		// physical representation of the ground
		btCollisionObject groundObj = new btCollisionObject();
		btCollisionShape groundShape = new btStaticPlaneShape(Vector3.Y, 0f);
		groundObj.setCollisionShape(groundShape);
		Physics.applyStaticGeometryCollisionFlags(groundObj);
		Physics.inst.addStaticGeometryToWorld(groundObj);

		if (Main.isServer()) {
			Log.debug("createLevel - create static models");
			// server creates static models here, client will create the models when received from server upon connection
			createStaticModels();
		}

		Log.debug("createLevel - create boxes");
		Box.createBoxes(20, 1, 10, 1, 10);
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
			x = MathUtils.random(10f, 100f);
			z = MathUtils.random(10f, 100f);
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
