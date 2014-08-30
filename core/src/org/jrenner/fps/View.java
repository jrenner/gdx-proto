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
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

import org.jrenner.fps.entity.Entity;
import org.jrenner.fps.graphics.EntityDecal;
import org.jrenner.fps.graphics.EntityModel;
import org.jrenner.fps.graphics.ModelManager;
import org.jrenner.fps.particles.Particles;

public class View implements Disposable {
	public static View inst;
	private GL20 gl;
	public static int width, height;
	private PerspectiveCamera camera;
	private Environment environ;
	private PointLight camLight;
	private DirectionalLight dirLight;
	public HUD hud;

	private ModelBatch modelBatch;
	private DecalBatch decalBatch;

	private Color fogColor = new Color(0.5f, 0.55f, 0.5f, 1f);

	private DefaultShaderProvider shaderProvider;

	private static final int MAX_POINTLIGHTS = 1;
	private static final int MAX_DIRECTIONAL_LIGHTS = 2;

	private void initShaders() {
		FileHandle vertexFile = Gdx.files.internal("shaders/vertex.glsl");
		FileHandle fragFile = Gdx.files.internal("shaders/frag.glsl");
		shaderProvider = new DefaultShaderProvider(vertexFile, fragFile);

		shaderProvider.config.numPointLights = 10;
		shaderProvider.config.numDirectionalLights = 2;


		// I don't remember what I was doing with this but it seems like it could be useful later
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
		modelManager.createPlayerModel();
		storeSize();
		inst = this;
		gl = Gdx.graphics.getGL20();
		float fov = 67f;
		camera = new PerspectiveCamera(fov, width(), height());
		camera.far = 200f;
		camera.near = 0.1f;
		resetCamera();

		initShaders();
		modelBatch = new ModelBatch(shaderProvider);
		decalBatch = new DecalBatch(new CameraGroupStrategy(camera));

		environ = new Environment();
		camLight = new PointLight();
		float intensity = 100f;
		camLight.set(new Color(0.2f, 0.2f, 0.2f, 1f), 0f, 0f, 0f, intensity);
		ColorAttribute ambientLight = new ColorAttribute(ColorAttribute.AmbientLight, new Color(0.1f, 0.1f, 0.1f, 1f));
		environ.set(ambientLight);
		ColorAttribute fog = new ColorAttribute(ColorAttribute.Fog);
		fog.color.set(fogColor);
		environ.set(fog);
		environ.add(camLight);
		dirLight = new DirectionalLight();
		dirLight.set(new Color(0.3f, 0.3f, 0.35f, 1f), -0.25f, -0.75f, 0.25f);
		environ.add(dirLight);
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

	public void render() {
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
		updateSky();

		modelBatch.begin(camera);
		if (Sky.isEnabled()) {
			modelBatch.render(Sky.modelInstance);
		}
		for (EntityModel entityModel : EntityModel.list) {
			entityModel.update();
			modelBatch.render(entityModel.modelInstance, environ);
		}
		for (Shadow shadow : Shadow.list) {
			modelBatch.render(shadow.modelInstance, environ);
		}
		if (Box.instance != null) {
			modelBatch.render(Box.instance, environ);
		}
		if (LevelBuilder.staticGeometry != null) {
			for (ModelInstance gate : LevelBuilder.staticGeometry) {
				modelBatch.render(gate, environ);
			}
		}
		for (TerrainChunk terrain : Terrain.chunks) {
			if (terrain != null && terrain.modelInstance != null)
				modelBatch.render(terrain.modelInstance, environ);
		}
		
		modelBatch.end();

		for (EntityDecal entityDecal: EntityDecal.list) {
			entityDecal.update();
			decalBatch.add(entityDecal.decal);
		}
		decalBatch.flush();
		

		modelBatch.begin(camera);
		drawParticleEffects();
		modelBatch.end();

		if (profileGL) {
			Profiler.tick();
		}

		if (Toggleable.debugDraw() && Main.isClient()) {
			Physics.inst.debugDraw();
		}

		hud.draw();
	}

	private void drawParticleEffects() {
		Particles.inst.system.update();
		Particles.inst.system.begin();
		Particles.inst.system.updateAndDraw();
		Particles.inst.system.end();
		modelBatch.render(Particles.inst.system);
	}
	
	private void updateSky() {
		if (Sky.isEnabled()) {
			Sky.update(camera.position);
		}
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

	@Override
	public void dispose() {
		Tools.dispose(modelBatch);
	}
}
