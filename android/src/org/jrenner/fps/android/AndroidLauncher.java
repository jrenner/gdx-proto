package org.jrenner.fps.android;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import org.jrenner.fps.Main;
import org.jrenner.fps.utils.CommandArgs;

public class AndroidLauncher extends AndroidApplication {
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		config.r = 8;
		config.g = 8;
		config.b = 8;
		config.a = 8;
		String[] fakeArgs = {};
		CommandArgs.ScreenSize screenSize = CommandArgs.process(fakeArgs);
		initialize(new Main(), config);
	}
}
