package org.jrenner.fps.input;

import com.badlogic.gdx.*;
import com.badlogic.gdx.Input;
import org.jrenner.fps.Main;

import static com.badlogic.gdx.Input.Buttons;
import static com.badlogic.gdx.Input.Keys.*;

/** Can bind two keyboards keys and one mouse button to an action */
public enum InputBind {
	MoveForward,
	MoveBack,
	MoveRight,
	MoveLeft,
	MoveUp,
	MoveDown,
	YawLeft,
	YawRight,
	PitchUp,
	PitchDown,
	Shoot,
	Jump
	;
	
	private static final int NO_KEY = -1;
	
	public int keycode0 = NO_KEY;
	public int keycode1 = NO_KEY;
	public int mouseButton = NO_KEY;
	
	public boolean isKeyMatch(int code) {
		if (keycode0 != NO_KEY && keycode0 == code) {
			return true;
		} else if (keycode1 != NO_KEY && keycode1 == code) {
			return true;
		}
		return false;
	}
	
	public boolean isMouseMatch(int button) {
		if (mouseButton != NO_KEY) {
			return mouseButton == button;
		}
		return false;
	}
	
	public boolean isPressed() {
		if (keycode0 != NO_KEY && Main.inst.input.isPressed(keycode0)) {
			return true;
		} else if (keycode1 != NO_KEY && Main.inst.input.isPressed(keycode1)) {
			return true;
		} else if (mouseButton != NO_KEY && Gdx.input.isButtonPressed(mouseButton)) {
			return true;
		}
		return false;
	}
	
	public static void setDefaultBindings() {
		setBindings(MoveForward, W);
		setBindings(MoveBack, S);
		setBindings(MoveLeft, A);
		setBindings(MoveRight, D);
		setBindings(MoveUp, PAGE_UP);
		setBindings(MoveDown, PAGE_DOWN);

		setBindings(YawLeft, LEFT);
		setBindings(YawRight, RIGHT);
		setBindings(PitchUp, UP);
		setBindings(PitchDown, DOWN);

		setBindings(Shoot, NO_KEY, NO_KEY, Buttons.LEFT);
		setBindings(Jump, SPACE, NO_KEY, Buttons.RIGHT);
	}

	private static void setBindings(InputBind ib, int key1) {
		setBindings(ib, key1, NO_KEY, NO_KEY);
	}

	private static void setBindings(InputBind ib, int key1, int key2) {
		setBindings(ib, key1, key2, NO_KEY);
	}

	private static void setBindings(InputBind ib, int key1, int key2, int mouse) {
		ib.keycode0 = key1;
		ib.keycode1 = key2;
		ib.mouseButton = mouse;
	}
	
	public static InputBind matchByKey(int key0) {
		for (InputBind ib : values()) {
			if (ib.isKeyMatch(key0)) {
				return ib;
			}
		}
		return null;
	}

	public static InputBind matchByMouse(int mouse) {
		for (InputBind ib : values()) {
			if (ib.isMouseMatch(mouse)) {
				return ib;
			}
		}
		return null;
	}
}
