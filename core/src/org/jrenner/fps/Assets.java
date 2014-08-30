package org.jrenner.fps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetErrorListener;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader;
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.UBJsonReader;

import org.jrenner.fps.headless.HeadlessG3dModelLoader;
import org.jrenner.fps.headless.HeadlessModel;
import org.jrenner.fps.headless.HeadlessModelLoader;
import org.jrenner.smartfont.SmartFontGenerator;

import static com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
import static com.badlogic.gdx.graphics.Texture.TextureFilter;

public class Assets {
	public static Assets inst;
	public static AssetManager manager;
	public static Skin skin;
	public static Label.LabelStyle chatLabelStyle;

	public Assets() {
		inst = this;
		manager = new AssetManager();
		manager.setErrorListener(new AssetErrorListener() {
			@Override
			public void error(AssetDescriptor assetDescriptor, Throwable throwable) {
				Log.error(assetDescriptor.toString());
				Log.error(throwable.getMessage());
			}
		});
		manager.getLogger().setLevel(Logger.DEBUG);
	}

	public void loadAll() {
		if (Main.isClient()) {
			loadTextures();
		}

		TextureParameter modTexParam = new TextureParameter();
		modTexParam.genMipMaps = true;
		modTexParam.magFilter = TextureFilter.Nearest;
		modTexParam.minFilter = TextureFilter.MipMapNearestNearest;
		modTexParam.wrapU = Texture.TextureWrap.Repeat;
		modTexParam.wrapV = Texture.TextureWrap.Repeat;

		// load headless models for headless server, regular models for client
		if (!Main.isClient()) {
			HeadlessModelLoader.HeadlessModelParameters modelParam = new HeadlessModelLoader.HeadlessModelParameters();
			modelParam.textureParameter = modTexParam;
			manager.setLoader(HeadlessModel.class, new HeadlessG3dModelLoader(new UBJsonReader(), new InternalFileHandleResolver()));

			manager.load("models/gate.g3db", HeadlessModel.class, modelParam);
			manager.load("models/strange-ramp1.g3db", HeadlessModel.class, modelParam);
			manager.load("models/strange-ramp2.g3db", HeadlessModel.class, modelParam);
			manager.load("models/ground1.g3db", HeadlessModel.class, modelParam);
			manager.load("models/level.g3db", HeadlessModel.class, modelParam);
		} else {
			ModelLoader.ModelParameters modelParam = new ModelLoader.ModelParameters();
			modelParam.textureParameter = modTexParam;

			manager.load("models/gate.g3db", Model.class, modelParam);
			manager.load("models/strange-ramp1.g3db", Model.class, modelParam);
			manager.load("models/strange-ramp2.g3db", Model.class, modelParam);
			manager.load("models/ground.g3db", Model.class, modelParam);
			manager.load("models/level.g3db", Model.class, modelParam);
			manager.load("models/skybox.g3db", Model.class, modelParam);
			manager.load("models/terrain/terrain.g3db", Model.class, modelParam);
		}

		if (Main.isClient()) {
			manager.load("ui/ui.json", Skin.class);
		}

		manager.finishLoading();

		if (Main.isClient()){
			skin = manager.get("ui/ui.json", Skin.class);
		}

		if (Main.isClient()) {
			SmartFontGenerator smart = new SmartFontGenerator();
			int size = (int) (30f * View.screenSizeRatio());
			int largeSize = (int) (46f * View.screenSizeRatio());
			//FileHandle fontFile = Gdx.files.internal("Exo-Regular.otf");
			//BitmapFont font = smart.createFont(fontFile, "exo-" + size, size);
			FileHandle fontFile = Gdx.files.internal("fonts/LiberationMono-Regular.ttf");
			BitmapFont font = smart.createFont(fontFile, "lib-mono-" + size, size);
			BitmapFont largeFont = smart.createFont(fontFile, "lib-mono-" + largeSize, largeSize);

			// Skin changes
			skin.get(Label.LabelStyle.class).font = font;
			chatLabelStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
			chatLabelStyle.font = largeFont;
			skin.get(TextButton.TextButtonStyle.class).font = largeFont;
			TextField.TextFieldStyle tfStyle = skin.get(TextField.TextFieldStyle.class);
			tfStyle.background = skin.getDrawable("button-up");
			System.out.println(tfStyle.background);
			tfStyle.font = font;
		}

		System.out.println("finished");

	}

	private void loadTextures() {
		Log.debug("Loading assets");
		TextureParameter textureParam = new TextureParameter();
		textureParam.genMipMaps = true;
		textureParam.magFilter = TextureFilter.Linear;
		textureParam.minFilter = TextureFilter.MipMapLinearLinear;
		textureParam.wrapU = Texture.TextureWrap.Repeat;
		textureParam.wrapV = Texture.TextureWrap.Repeat;
		manager.load("models/ground.jpg", Texture.class, textureParam);
		manager.load("textures/marble.jpg", Texture.class, textureParam);
		manager.load("textures/shadow.png", Texture.class);
		
		//Load Skybox
		manager.load("textures/skybox/xpos.png", Texture.class);
		manager.load("textures/skybox/xneg.png", Texture.class);
		manager.load("textures/skybox/ypos.png", Texture.class);
		manager.load("textures/skybox/yneg.png", Texture.class);
		manager.load("textures/skybox/zpos.png", Texture.class);
		manager.load("textures/skybox/zneg.png", Texture.class);
		
		loadCrawlTextures();
	}

	private void loadCrawlTextures() {
		manager.load("texture-packs/monsters.atlas", TextureAtlas.class);
	}

	public static void loadParticleEffects(ParticleSystem particleSystem) {
		ParticleEffectLoader.ParticleEffectLoadParameter loadParam = new ParticleEffectLoader.ParticleEffectLoadParameter(particleSystem.getBatches());
		ParticleEffectLoader loader = new ParticleEffectLoader(new InternalFileHandleResolver());
		manager.setLoader(ParticleEffect.class, loader);
		manager.load("particle/bullet-hit.pfx", ParticleEffect.class, loadParam);
		manager.load("particle/blue-explosion.pfx", ParticleEffect.class, loadParam);
		manager.finishLoading();
	}

	public void dispose() {
		Tools.dispose(manager);
	}
}
