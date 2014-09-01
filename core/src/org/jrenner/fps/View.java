package org.jrenner.fps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

import com.badlogic.gdx.utils.TimeUtils;
import org.jrenner.fps.entity.Entity;
import org.jrenner.fps.graphics.EntityModel;
import org.jrenner.fps.graphics.ModelManager;
import org.jrenner.fps.net.packages.ChatMessage;
import org.jrenner.fps.particles.Particles;
import org.jrenner.fps.terrain.Terrain;
import org.jrenner.fps.terrain.TerrainChunk;

public class View implements Disposable {
	public static View inst;
	private GL20 gl;
	public static int width, height;
	private PerspectiveCamera camera;
	private Environment environ, basicEnviron;
	private PointLight camLight;
	private DirectionalLight dirLight;
	public HUD hud;

	private ModelBatch modelBatch;

	private Color fogColor = new Color(0.3f, 0.4f, 0.3f, 1f);

	private DefaultShaderProvider shaderProvider;


	private void initShaders() {
		FileHandle vertexFile = Gdx.files.internal("shaders/vertex.glsl");
		FileHandle fragFile = Gdx.files.internal("shaders/frag.glsl");
		shaderProvider = new DefaultShaderProvider(vertexFile, fragFile);

		shaderProvider.config.numPointLights = 10;
		shaderProvider.config.numDirectionalLights = 2;


		// check shader compile logs

		/*shader = new ShaderProgram(vertexFile, fragFile);
		ShaderProgram.pedantic = false;
		String log = shader.getLog();
		if (!shader.isCompiled())
			throw new GdxRuntimeException(log);
		if (log!=null && log.length()!=0)
			System.out.println("Shader Log: "+log);*/
	}

	public View() {
		ModelManager modelManager = new ModelManager();
		modelManager.init();
		storeSize();
		inst = this;
		gl = Gdx.graphics.getGL20();
		float fov = 67f;
		camera = new PerspectiveCamera(fov, width(), height());
		// camera.far affects frustrum culling, so a shorter distance can boost performance
		camera.far = 200f;
		camera.near = 0.01f;
		resetCamera();

		initShaders();
		modelBatch = new ModelBatch(shaderProvider);

		environ = new Environment();
		basicEnviron = new Environment();
		camLight = new PointLight();
		float intensity = 100f;
		camLight.set(new Color(0.2f, 0.2f, 0.2f, 1f), 0f, 0f, 0f, intensity);
		ColorAttribute ambientLight = new ColorAttribute(ColorAttribute.AmbientLight, .1f , .1f, .1f ,1);
		environ.set(ambientLight);
		ColorAttribute fog = new ColorAttribute(ColorAttribute.Fog);
		fog.color.set(fogColor);
		environ.set(fog);
		environ.add(camLight);
		dirLight = new DirectionalLight();
		dirLight.set(new Color(0.3f, 0.3f, 0.35f, 1f), -0.25f, -0.75f, 0.25f);
		environ.add(dirLight);

		basicEnviron.set(ColorAttribute.createAmbient(0.3f, 0.3f, 0.3f, 1f));

		if (Toggleable.profileGL()) {
			Profiler.enable();
		}

		hud = new HUD();
		
		Sky.createSkyBox(
			Assets.manager.get("textures/skybox/xpos.png", Texture.class),
			Assets.manager.get("textures/skybox/xneg.png", Texture.class),
			Assets.manager.get("textures/skybox/ypos.png", Texture.class),
			Assets.manager.get("textures/skybox/yneg.png", Texture.class),
			Assets.manager.get("textures/skybox/zpos.png", Texture.class),
			Assets.manager.get("textures/skybox/zneg.png", Texture.class)
		);
	}

	private void updateCamera() {
		//System.out.println("update camera");
		if (!Toggleable.freeCamera() && Main.inst.client.player != null) {
			Entity playerEntity = Main.inst.client.player.entity;
			//System.out.println("cam move to pos: " + DynamicEntity.player.getPosition());
			camera.position.set(playerEntity.getPosition());
			setCameraRotation(playerEntity.getRotation());
		}
		camera.update();
	}

	long lastSwitch = -1;
	RollingArray renderTimes = new RollingArray();
	RollingArray hudTimes = new RollingArray();
	RollingArray entityTimes = new RollingArray();
	RollingArray staticTimes = new RollingArray();
	RollingArray boxTimes = new RollingArray();
	RollingArray skyTimes = new RollingArray();
	RollingArray groundTimes = new RollingArray();

	private void debugRenderTimes() {
		// tied in with the profileGL toggleable for now
		long now = TimeUtils.millis();
		long passed = now - lastSwitch;
		if (passed >= 1000 || lastSwitch < 0) {
			float totalTime = renderTimes.getAverage();
			float hudTime = hudTimes.getAverage();
			float entityTime = entityTimes.getAverage();
			float staticTime = staticTimes.getAverage();
			float boxTime = boxTimes.getAverage();
			float skyTime = skyTimes.getAverage();
			float groundTime = groundTimes.getAverage();
			renderTimes.clear();
			hudTimes.clear();
			entityTimes.clear();
			staticTimes.clear();
			boxTimes.clear();
			skyTimes.clear();
			groundTimes.clear();
			lastSwitch = now;
			String text = String.format("render-times(ms), total: %.1f, hud: %.1f, entity: %.1f, static: %.1f\n" +
					"            boxes: %.1f, sky: %.1f, ground: %.1f",
					totalTime, hudTime, entityTime, staticTime, boxTime, skyTime, groundTime);
			Main.inst.client.sendChatMessage(new ChatMessage(text));
			Log.debug(text);
		}
	}

	boolean debugRenderPerformance = false;

	public void render() {
		if (debugRenderPerformance) {
			debugRenderTimes();
		}
		long start = TimeUtils.millis();
		boolean profileGL = Toggleable.profileGL();
		updateLights();
		if (profileGL) {
			Profiler.reset();
		}
		gl.glViewport(0, 0, width(), height());
		gl.glClearColor(fogColor.r, fogColor.g, fogColor.b, fogColor.a);
		gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		camera.up.set(Vector3.Y);
		updateCamera();

		long skyStart = TimeUtils.millis();
		modelBatch.begin(camera);
		if (Sky.isEnabled()) {
			Sky.update(camera.position);
			modelBatch.render(Sky.modelInstance);
		}
		
		if (Terrain.chunks != null) {
			for (TerrainChunk chunk : Terrain.chunks) {
				chunk.render(modelBatch, environ);
			}
		}
		
		if (LevelBuilder.staticGeometry != null) {
			for (ModelInstance staticGeo : LevelBuilder.staticGeometry) {
				modelBatch.render(staticGeo, environ);
			}
		}
		skyTimes.add((int) TimeUtils.timeSinceMillis(skyStart));

		// includes 3d models and billboards
		long entityStart = TimeUtils.millis();
		visibleEntities = 0;
		for (EntityModel entityModel : EntityModel.list) {
			entityModel.update();
			// don't draw self when in FPS mode
			if (!entityModel.isClientEntity() || Toggleable.freeCamera()) {
				if (entityVisibilityCheck(entityModel)) {
					visibleEntities++;
					modelBatch.render(entityModel.modelInstance, environ);
				}
			}
		}
		entityTimes.add((int) TimeUtils.timeSinceMillis(entityStart));

		long boxStart = TimeUtils.millis();
		if (Box.instance != null) {
			modelBatch.render(Box.instance, environ);
		}
		boxTimes.add((int) TimeUtils.timeSinceMillis(boxStart));

		long staticStart = TimeUtils.millis();
		if (LevelBuilder.staticGeometry != null) {
			for (ModelInstance staticGeo : LevelBuilder.staticGeometry) {
				modelBatch.render(staticGeo, environ);
			}
		}
		staticTimes.add((int) TimeUtils.timeSinceMillis(staticStart));

		/*
		totalGroundPieces = 0;
=======
		long groundStart = TimeUtils.millis();
>>>>>>> FETCH_HEAD
		visibleGroundPieces = 0;
		for (ModelInstance groundPiece : LevelBuilder.groundPieces) {
			if (groundPieceVisibilityCheck(groundPiece)) {
				visibleGroundPieces++;
				modelBatch.render(groundPiece, environ);
			}
		}
<<<<<<< HEAD
		*/
		//groundTimes.add((int) TimeUtils.timeSinceMillis(groundStart));
		modelBatch.end();

		// draw particle effects in a separate batch to make depth testing work better
		modelBatch.begin(camera);
		for (Shadow shadow : Shadow.list) {
			modelBatch.render(shadow.modelInstance, environ);
		}
		drawParticleEffects();
		modelBatch.end();

		if (profileGL) {
			Profiler.tick();
		}

		if (Toggleable.debugDraw() && Main.isClient()) {
			Physics.inst.debugDraw();
		}

		long hudStart = TimeUtils.millis();
		hud.draw();
		long hudTime = TimeUtils.timeSinceMillis(hudStart);
		hudTimes.add((int) hudTime);

		long time = TimeUtils.timeSinceMillis(start);
		renderTimes.add((int) time);
	}

	private void drawParticleEffects() {
		Particles.inst.system.update();
		Particles.inst.system.begin();
		Particles.inst.system.draw();
		Particles.inst.system.end();
		modelBatch.render(Particles.inst.system);
	}
	
	private void updateLights() {
		camLight.position.set(camera.position);
	}

	public static int width() { return Gdx.graphics.getWidth(); }
	public static int height() { return Gdx.graphics.getHeight(); }

	public static float screenSizeRatio() { return width() / 1920f; }

	public void resetCamera() {
		camera.position.set(0f, 3f, -3f);
		camera.lookAt(0f, 0f, 0f);
		Log.debug("reset camera");
	}

	public PerspectiveCamera getCamera() {
		return camera;
	}

	private Vector3 tmp = new Vector3();
	private Quaternion q = new Quaternion();
	private Matrix4 mtx = new Matrix4();

	public void transCamera(Direction.Translation dir) {
		float transStep = 0.1f;
		switch (dir) {
			case Forward:
				tmp.set(camera.direction);
				camera.position.add(tmp.nor().scl(transStep));
				break;
			case Back:
				tmp.set(camera.direction);
				camera.position.add(tmp.nor().scl(-transStep));
				break;
			case Right:
				tmp.set(camera.direction);
				camera.position.add(tmp.crs(camera.up).nor().scl(transStep));
				break;
			case Left:
				tmp.set(camera.direction);
				camera.position.add(tmp.crs(camera.up).nor().scl(-transStep));
				break;
			case Up:
				tmp.set(camera.up).nor();
				tmp.scl(transStep);
				camera.position.add(tmp);
				break;
			case Down:
				tmp.set(camera.up).nor().scl(-1f);
				tmp.scl(transStep);
				camera.position.add(tmp);
				break;
		}
	}

	public void rotateCamera(Direction.Rotation dir, float step) {
		switch (dir) {
			case PitchUp:
				tmp.set(camera.direction);
				camera.direction.rotate(tmp.crs(camera.up), step);
				break;
			case PitchDown:
				tmp.set(camera.direction);
				camera.direction.rotate(tmp.crs(camera.up), -step);
				break;
			case YawRight:
				camera.view.getRotation(q);
				q.setEulerAngles(-step, 0f, 0f);
				mtx.set(q);
				camera.direction.prj(mtx);
				break;
			case YawLeft:
				camera.view.getRotation(q);
				q.setEulerAngles(step, 0f, 0f);
				mtx.set(q);
				camera.direction.prj(mtx);
				break;
		}
	}

	public void setCameraRotation(Quaternion q) {
		mtx.set(q);
		camera.direction.set(Vector3.Z);
		camera.direction.prj(mtx);
	}

	public void storeSize() {
		width = Gdx.graphics.getWidth();
		height = Gdx.graphics.getHeight();
	}

	/*
	public static int totalGroundPieces;
=======
	public static int visibleGroundPieces;
	private boolean groundPieceVisibilityCheck(ModelInstance modelInst) {
		float halfWidth = LevelBuilder.groundPieceSize / 2f;
		modelInst.transform.getTranslation(tmp);
		// we want the center of the piece
		tmp.add(halfWidth, 0, halfWidth);
		float radius = LevelBuilder.groundPieceSize;
		return camera.frustum.sphereInFrustum(tmp, LevelBuilder.groundPieceSize);
		// this naive method is useful for debugging to see pop-in/pop-out
		//return camera.frustum.pointInFrustum(tmp);
	}
	*/

	public static int visibleEntities;
	private boolean entityVisibilityCheck(EntityModel entModel) {
		entModel.modelInstance.transform.getTranslation(tmp);
		float radius = entModel.entity.getRadius();
		return camera.frustum.sphereInFrustum(tmp, radius);
	}

	@Override
	public void dispose() {
		Tools.dispose(modelBatch);
	}
}
