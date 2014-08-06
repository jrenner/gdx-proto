package org.jrenner.fps.headless;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import org.jrenner.fps.Main;
import org.jrenner.fps.utils.CommandArgs;

public class HeadlessLauncher {
	private static HeadlessApplicationConfiguration cfg;
	public static void main (String[] args) {
		cfg = new HeadlessApplicationConfiguration();
		cfg.renderInterval = 1 / 300f;
		CommandArgs.process(args);
		new HeadlessApplication(new Main(), cfg);
	}
}
