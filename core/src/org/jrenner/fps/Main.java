package org.jrenner.fps;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.TimeUtils;

import org.jrenner.fps.effects.BulletHit;
import org.jrenner.fps.entity.Entity;
import org.jrenner.fps.event.ClientEventManager;
import org.jrenner.fps.graphics.EntityDecal;
import org.jrenner.fps.input.GestureHandler;
import org.jrenner.fps.input.Input;
import org.jrenner.fps.move.Movement;
import org.jrenner.fps.net.AbstractServer;
import org.jrenner.fps.net.LocalServer;
import org.jrenner.fps.net.NetManager;
import org.jrenner.fps.net.NetServer;
import org.jrenner.fps.net.ServerType;
import org.jrenner.fps.net.client.AbstractClient;
import org.jrenner.fps.net.client.LocalClient;
import org.jrenner.fps.net.client.NetClient;
import org.jrenner.fps.particles.Particles;
import org.jrenner.fps.utils.Pooler;

public class Main extends ApplicationAdapter {
	public static Main inst;
	private View view;
	public Input input;
	// TODO move to a field of AbstractClient
	public ClientEventManager clientEventManager;
	private Physics physics;
	public AbstractServer server;
	public NetManager netManager;
	public AbstractClient client;
	public static boolean hasClient;
	public InputMultiplexer inputMulti;
	public static Application.ApplicationType Platform;
	public static ServerType serverType;
	
	@Override
	public void create () {
		Log.setLevel(Log.DEBUG);
		Platform = Gdx.app.getType();
		// we can pretend to be Android while running on desktop to test mobile features
		// such as mobile-specific input
		// Platform = Application.ApplicationType.Android;
		Assets assets = new Assets();
		assets.loadAll();
		Log.debug("finished loading assets");
		physics = new Physics();
		initializeSubModules();
		frame = 0;
		inst = this;
		if (isClient()) {
			view = new View();
			new Particles();
			input = new Input();
			inputMulti = new InputMultiplexer();
			inputMulti.addProcessor(View.inst.hud.stage);
			inputMulti.addProcessor(input);
			if (isMobile()) {
				inputMulti.addProcessor(GestureHandler.createGestureHandler());
			}
			Gdx.input.setInputProcessor(inputMulti);
		}
		LevelBuilder.createLevel();
		Box.createBoxes(20, 1, 10, 1, 10);

		setupNetwork();
	}

	private void setupNetwork() {
		netManager = new NetManager();
		if (isOnlineServer()) {
			server = new NetServer();
		} else if (isLocalServer()) {
			server = new LocalServer();
		}
		if (isServer()) {
			server.setupGame();
		}
		if (isClient()) {
			clientEventManager = new ClientEventManager();
			if (isLocalServer()) {
				client = new LocalClient();
				client.connectToServer();
			} else {
				client = new NetClient();
			}
		}
		if (!isOnlineServer() && !isClient()) {
			throw new GdxRuntimeException("isServer and isClient are both false! At least one must be true");
		}
	}

	public static int frame;

	public static boolean pause = false;

	@Override
	public void render () {
		try {
			if (pause) {
				Tools.sleep(10);
				return;
			}
			frame++;
			if (isServer()) {
				server.update();
			}
			if (isClient()) {
				clientEventManager.process();
				view.render();
				input.process();
				client.update();
			}

			updateWorld();
		} catch (Exception e) {
			Gdx.input.setCursorCatched(false);
			throw new GdxRuntimeException(e);
		}
	}

	private float accumulatedPhysicsTime;
	private long lastPhysicsTime = -1;

	public void updateWorld() {
		long start = TimeUtils.millis();
		if (lastPhysicsTime == -1) {
			lastPhysicsTime = start;
			return;
		}
		float delta = (start - lastPhysicsTime) / 1000f;
		lastPhysicsTime = start;
		accumulatedPhysicsTime += delta;
		//Log.debug("physics delta: " + delta);
		//Log.debug("accumualted physics time: " + accumulatedPhysicsTime);
		while (accumulatedPhysicsTime >= Physics.TIME_STEP) {
			physics.run();
			accumulatedPhysicsTime -= Physics.TIME_STEP;
			Main.updateSubModules(Physics.TIME_SCALE);
			Entity.updateAll(Physics.TIME_SCALE);
		}
		physicsTime = (int) TimeUtils.timeSinceMillis(start);
	}

	public static int physicsTime;

	@Override
	public void resize(int width, int height) {
		if (isClient()) {
			View.inst.storeSize();
		}
	}


	// TODO check around to make sure all things are getting disposed, in all classes
	// bullet stuff, models, etc
	@Override
	public void dispose() {
		if (View.inst != null) {
			View.inst.dispose();
		}
		Assets.inst.dispose();
		LevelBuilder.dispose();
		Movement.disposeAll();
		physics.dispose();
	}

	private void initializeSubModules() {
		// lots of static objects are assigned at startup
		// this saves us the pain of Android keeping static objects alive
		// after app closure
		Log.init();
		Pooler.init();
		if (isClient()) {
			Toggleable.init();
		}
		LevelBuilder.init();
		Terrain.init();
		Block.init();
		if (isClient()) {
			Shadow.init();
			Sky.init();
		}
		Entity.init();
		Movement.init();

	}

	public static void updateSubModules(float timeStep) {
		BulletHit.updateAll(timeStep);
	}

	public static boolean isClient() { return hasClient; }

	public static boolean isOnlineServer() { return serverType == ServerType.Online; }

	public static boolean isLocalServer() { return serverType == ServerType.Local; }

	public static boolean isServer() { return isOnlineServer() || isLocalServer(); }

	public static boolean isMobile() {
		return isAndroid() || isIOS();
	}

	public static boolean isAndroid() {
		return Platform == Application.ApplicationType.Android;
	}

	public static boolean isIOS() {
		return Platform == Application.ApplicationType.iOS;
	}

	public static boolean isDesktop() {
		return Platform == Application.ApplicationType.Desktop;
	}

	public static NetClient getNetClient() {
		return (NetClient) inst.client;
	}
}
