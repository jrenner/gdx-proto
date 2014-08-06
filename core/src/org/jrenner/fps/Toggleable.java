package org.jrenner.fps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.GdxRuntimeException;

/** A Toggleable setting that maintains state between application startups/shutdowns */
public class Toggleable {
	public static ArrayMap<String, Toggleable> list;
	public boolean value;
	public String name;

	/** Bullet physics debug draw */
	public static final String DEBUG_DRAW = "debug-draw";
	/** libgdx GLProfiler info to log */
	public static final String PROFILE_GL = "profile-gl";
	public static final String FREE_CAMERA = "free-camera";
	public static final String CONTROL_PLAYER = "control-player";
	public static final String MOUSE_LOOK = "mouse-look";


	public static void init() {
		list = new ArrayMap<>();
		loadFromPrefs();
	}

	private static void loadFromPrefs() {
		Preferences p = getPrefs();
		create(DEBUG_DRAW, p.getBoolean(DEBUG_DRAW, false));
		create(PROFILE_GL, p.getBoolean(PROFILE_GL, true));
		create(FREE_CAMERA, p.getBoolean(FREE_CAMERA, true));
		create(CONTROL_PLAYER, p.getBoolean(CONTROL_PLAYER, true));
		create(MOUSE_LOOK, p.getBoolean(MOUSE_LOOK, true));

		set(CONTROL_PLAYER, true);
		set(FREE_CAMERA, false);
		set(MOUSE_LOOK, true);
	}

	public static void saveToPrefs() {
		Preferences p = getPrefs();
		p.putBoolean(DEBUG_DRAW, Toggleable.getValue(DEBUG_DRAW));
		p.putBoolean(PROFILE_GL, Toggleable.getValue(PROFILE_GL));
		p.flush();
	}

	private static Preferences getPrefs() {
		return Tools.getPrefs("toggleables");
	}

	public static void create(String name, boolean startValue) {
		Toggleable t = new Toggleable();
		t.name = name;
		list.put(name, t);
		set(t.name, startValue);
	}

	public static void toggle(String name) {
		Toggleable t = getToggleable(name);
		t.toggle();
		saveToPrefs();
	}

	public static void set(String name, boolean value) {
		Toggleable t = getToggleable(name);
		t.value = value;
		Log.debug("SET " + name + ": " + value);
		// special cases
		if (name.equals(MOUSE_LOOK)) {
			if (t.value) {
				Gdx.input.setCursorCatched(true);
			} else {
				Gdx.input.setCursorCatched(false);
			}
		}
		// when posessing the player, always control the player
		if (name.equals(FREE_CAMERA)) {
			if (!getValue(FREE_CAMERA)) {
				set(CONTROL_PLAYER, true);
			}
		}
	}

	public static Toggleable getToggleable(String name) {
		Toggleable t = list.get(name);
		if (t == null) throw new GdxRuntimeException("no toggleable found with name: " + name);
		return t;
	}

	public static boolean getValue(String name) {
		Toggleable t = getToggleable(name);
		return t.value;
	}

	public void toggle() {
		set(name, !value);
		Log.debug("TOGGLE " + name + ": " + value);
	}

	public static boolean debugDraw() {	return getValue(DEBUG_DRAW); }
	public static boolean profileGL() { return getValue(PROFILE_GL); }
	public static boolean freeCamera() { return getValue(FREE_CAMERA); }
	public static boolean controlPlayer() { return getValue(CONTROL_PLAYER); }
	public static boolean mouseLook() { return getValue(MOUSE_LOOK); }
}
