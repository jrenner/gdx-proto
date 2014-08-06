package org.jrenner.fps.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import org.jrenner.fps.Main;
import org.jrenner.fps.utils.CommandArgs;

public class DesktopLauncher {
	private static LwjglApplicationConfiguration cfg;
	public static void main (String[] args) {
		cfg = new LwjglApplicationConfiguration();
		cfg.title = "FPS";
		cfg.resizable = true;
		//cfg.samples = 8; // anti-aliasing
		cfg.width = 1024;
		cfg.height = 768;
		cfg.fullscreen = false;
		cfg.r = 8;
		cfg.g = 8;
		cfg.b = 8;
		cfg.a = 8;
		CommandArgs.ScreenSize screenSize = CommandArgs.process(args);
		if (screenSize != null) {
			cfg.width = screenSize.width;
			cfg.height = screenSize.height;
		}
		new LwjglApplication(new Main(), cfg);
	}
}
